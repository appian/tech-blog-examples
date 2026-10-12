package com.appian.hydrator.hydrate;

import com.appian.hydrator.SizeFormat;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Hydrates cold-storage-backed files by issuing a small sampling read into every
 * fixed-size block of each file, forcing the backing storage to fault the whole
 * block in. Reads are async and rate-limited to a target IOPS figure.
 *
 * Pipeline: one feeder thread walks files and enqueues their blocks, one dispatcher
 * thread issues reads up to the IOPS target once per second, and completions land on
 * JDK's async I/O completion threads. No locks are used anywhere; the only shared
 * state is a bounded queue (for backpressure) and a semaphore (for open-file limits),
 * both lock-free from the caller's perspective.
 */
public final class Hydrator {

    private static final int MAX_OPEN_FILES = 256;

    private final long minFileSize;
    private final boolean verbose;

    private final IopsThrottle throttle;
    private final BufferPool bufferPool;
    private final BlockingQueue<Block> readyBlocks;
    private final Semaphore openFileSlots = new Semaphore(MAX_OPEN_FILES);
    private final WorkTracker workTracker = new WorkTracker();

    private final AtomicLong filesHydrated = new AtomicLong(0);
    private final AtomicLong blocksRead = new AtomicLong(0);
    private final AtomicLong bytesRead = new AtomicLong(0);
    private final AtomicLong bytesHydrated = new AtomicLong(0);
    private final AtomicLong errors = new AtomicLong(0);

    public Hydrator(int targetIops, long minFileSize, boolean verbose) {
        this.minFileSize = minFileSize;
        this.verbose = verbose;
        this.throttle = new IopsThrottle(targetIops);
        this.bufferPool = new BufferPool(targetIops);
        this.readyBlocks = new ArrayBlockingQueue<>(Math.max(targetIops * 2, 100));
    }

    public HydrateStats hydrate(List<Path> locations, int parallelism) {
        try (FileIterator files = new FileIterator(locations, parallelism, verbose)) {
            Thread feeder = Thread.ofVirtual().start(() -> feed(files));
            Thread dispatcher = Thread.ofVirtual().start(this::dispatchLoop);
            Thread reporter = Thread.ofVirtual().start(this::reportProgress);

            try {
                workTracker.awaitDone();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                feeder.interrupt();
                dispatcher.interrupt();
                reporter.interrupt();
            }
        }

        return new HydrateStats(filesHydrated.get(), blocksRead.get(), bytesRead.get(), bytesHydrated.get(), errors.get());
    }

    private void feed(FileIterator files) {
        try {
            while (files.hasNext()) {
                FileTask task = files.next();
                if (task.fileSize() < minFileSize) {
                    if (verbose) {
                        IO.println("[hydrate] skipping " + task.path() + " (below min-size)");
                    }
                    continue;
                }
                openFileForReading(task);
            }
        } finally {
            workTracker.feedingDone();
        }
    }

    private void openFileForReading(FileTask task) {
        try {
            openFileSlots.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        AsynchronousFileChannel channel;
        try {
            channel = AsynchronousFileChannel.open(task.path(), StandardOpenOption.READ);
        } catch (IOException e) {
            openFileSlots.release();
            errors.incrementAndGet();
            if (verbose) {
                IO.println("[hydrate] error opening " + task.path() + ": " + e.getMessage());
            }
            return;
        }

        long blockCount = HydrateConstants.blockCount(task.fileSize());
        AtomicLong remaining = new AtomicLong(blockCount);
        AtomicBoolean fileHadError = new AtomicBoolean(false);

        for (long i = 0; i < blockCount; i++) {
            long offset = HydrateConstants.blockOffset(i);
            int readSize = HydrateConstants.readSizeForBlock(task.fileSize(), i);
            long hydratedSize = HydrateConstants.hydratedSizeForBlock(task.fileSize(), i);
            enqueue(new Block(task.path(), channel, offset, readSize, hydratedSize, remaining, fileHadError));
        }
    }

    private void enqueue(Block block) {
        workTracker.blockEnqueued();
        try {
            readyBlocks.put(block);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void dispatchLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            int toIssue = throttle.permitsToIssue();
            for (int i = 0; i < toIssue; i++) {
                Block block = readyBlocks.poll();
                if (block == null) {
                    break;
                }
                issueRead(block);
            }
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private void issueRead(Block block) {
        ByteBuffer buffer;
        try {
            buffer = bufferPool.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
        buffer.limit(block.readSize());
        throttle.onIssued();

        block.channel().read(buffer, block.offset(), block, new CompletionHandler<Integer, Block>() {
            @Override
            public void completed(Integer result, Block block) {
                bufferPool.release(buffer);
                throttle.onCompleted();
                onBlockDone(block, Math.max(0, result), null);
            }

            @Override
            public void failed(Throwable exc, Block block) {
                bufferPool.release(buffer);
                throttle.onCompleted();
                onBlockDone(block, 0, exc);
            }
        });
    }

    private void onBlockDone(Block block, int bytesActuallyRead, Throwable failure) {
        if (failure == null) {
            blocksRead.incrementAndGet();
            bytesRead.addAndGet(bytesActuallyRead);
            bytesHydrated.addAndGet(block.hydratedSize());
            if (verbose) {
                IO.println("[hydrate] read " + bytesActuallyRead + " byte(s) from " + block.path()
                        + " at offset " + block.offset() + " (" + block.hydratedSize() + " byte(s) hydrated)");
            }
        } else {
            block.fileHadError().set(true);
            errors.incrementAndGet();
            if (verbose) {
                IO.println("[hydrate] read failed for " + block.path() + ": " + failure.getMessage());
            }
        }

        if (block.isLastBlockInFile()) {
            if (!block.fileHadError().get()) {
                filesHydrated.incrementAndGet();
            }
            closeQuietly(block.channel());
            openFileSlots.release();
        }

        workTracker.blockCompleted();
    }

    private void closeQuietly(AsynchronousFileChannel channel) {
        try {
            channel.close();
        } catch (IOException ignored) {
        }
    }

    private void reportProgress() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                TimeUnit.SECONDS.sleep(10);
                IO.println("[hydrate] progress: " + filesHydrated.get() + " file(s), "
                        + blocksRead.get() + " block(s), " + SizeFormat.humanReadable(bytesRead.get()) + " read, "
                        + SizeFormat.humanReadable(bytesHydrated.get()) + " hydrated, in-flight="
                        + throttle.inFlight());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
