package com.appian.hydrator.scan;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public final class DirectoryScanner {

    private final int parallelism;
    private final boolean verbose;

    public DirectoryScanner(int parallelism, boolean verbose) {
        this.parallelism = parallelism;
        this.verbose = verbose;
    }

    public ScanStats scan(List<Path> roots) {
        DirectoryQueue queue = new DirectoryQueue();
        for (Path root : roots) {
            queue.push(root);
        }

        ConcurrentLinkedQueue<ScanStats> workerResults = new ConcurrentLinkedQueue<>();
        AtomicLong directoriesProgress = new AtomicLong(0);
        AtomicLong filesProgress = new AtomicLong(0);

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<?> reporter = executor.submit(() -> reportProgress(directoriesProgress, filesProgress));

            List<Future<?>> futures = new ArrayList<>();
            for (int worker = 0; worker < parallelism; worker++) {
                futures.add(executor.submit(() ->
                        workerResults.add(runWorker(queue, directoriesProgress, filesProgress))));
            }
            for (Future<?> future : futures) {
                future.get();
            }
            reporter.cancel(true);
        } catch (ExecutionException e) {
            throw new RuntimeException(e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        ScanStats total = ScanStats.empty();
        for (ScanStats partial : workerResults) {
            total = total.merge(partial);
        }
        return total;
    }

    private void reportProgress(AtomicLong directoriesProgress, AtomicLong filesProgress) {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                TimeUnit.SECONDS.sleep(10);
                IO.println("[scan] progress: " + directoriesProgress.get() + " directorie(s), "
                        + filesProgress.get() + " file(s) scanned so far");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private ScanStats runWorker(DirectoryQueue queue, AtomicLong directoriesProgress, AtomicLong filesProgress) {
        long directoriesScanned = 0;
        long filesFound = 0;
        long errors = 0;

        while (!queue.isFinished() && !Thread.currentThread().isInterrupted()) {
            Path directory = queue.pollOrNull();
            if (directory == null) {
                continue;
            }
            try {
                long filesInDirectory = scanDirectory(directory, queue);
                if (filesInDirectory < 0) {
                    errors++;
                } else {
                    directoriesScanned++;
                    filesFound += filesInDirectory;
                    directoriesProgress.incrementAndGet();
                    filesProgress.addAndGet(filesInDirectory);
                }
            } finally {
                queue.markDone();
            }
        }

        return new ScanStats(directoriesScanned, filesFound, errors);
    }

    /**
     * Lists a single directory, pushing any subdirectories back onto the
     * queue. Returns the number of regular files found, or -1 if the
     * directory could not be read.
     */
    private long scanDirectory(Path directory, DirectoryQueue queue) {
        if (verbose) {
            IO.println("[scan] " + Thread.currentThread() + " listing " + directory);
        }

        long fileCount = 0;
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(directory)) {
            for (Path entry : entries) {
                boolean isSymlink = Files.isSymbolicLink(entry);
                if (isSymlink) {
                    if (verbose) {
                        IO.println("[scan] skipping symlink " + entry);
                    }
                    continue;
                }

                boolean isDirectory = Files.isDirectory(entry);
                if (isDirectory) {
                    queue.push(entry);
                } else {
                    fileCount++;
                    if (verbose) {
                        IO.println("[scan] file " + entry);
                    }
                }
            }
            return fileCount;
        } catch (IOException e) {
            if (verbose) {
                IO.println("[scan] error reading " + directory + ": " + e.getMessage());
            }
            return -1;
        }
    }
}
