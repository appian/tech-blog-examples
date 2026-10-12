package com.appian.hydrator.hydrate;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public final class FileIterator implements Iterator<FileTask>, AutoCloseable {

    private static final int QUEUE_CAPACITY = 4096;
    private static final Object POISON = new Object();

    private final BlockingQueue<Object> files = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
    private final ExecutorService executor;
    private final boolean verbose;

    private FileTask next;
    private boolean finished;

    public FileIterator(List<Path> locations, int parallelism, boolean verbose) {
        this.verbose = verbose;
        this.executor = Executors.newVirtualThreadPerTaskExecutor();

        BlockingQueue<Path> directories = new LinkedBlockingQueue<>();
        AtomicLong pendingDirectories = new AtomicLong(0);

        for (Path location : locations) {
            submitLocation(location, directories, pendingDirectories);
        }

        for (int worker = 0; worker < parallelism; worker++) {
            executor.submit(() -> walkDirectories(directories, pendingDirectories));
        }

        executor.submit(() -> {
            waitUntilFinished(pendingDirectories);
            offer(POISON);
        });
    }

    private void submitLocation(Path location, BlockingQueue<Path> directories, AtomicLong pendingDirectories) {
        try {
            if (Files.isSymbolicLink(location)) {
                if (verbose) {
                    IO.println("[hydrate] skipping symlink " + location);
                }
                return;
            }
            if (Files.isDirectory(location)) {
                pendingDirectories.incrementAndGet();
                directories.add(location);
            } else if (Files.isRegularFile(location)) {
                offer(new FileTask(location, Files.size(location)));
            }
        } catch (IOException e) {
            if (verbose) {
                IO.println("[hydrate] error reading " + location + ": " + e.getMessage());
            }
        }
    }

    private void walkDirectories(BlockingQueue<Path> directories, AtomicLong pendingDirectories) {
        while (pendingDirectories.get() > 0) {
            Path directory = pollOrNull(directories);
            if (directory == null) {
                continue;
            }
            try {
                listDirectory(directory, directories, pendingDirectories);
            } finally {
                pendingDirectories.decrementAndGet();
            }
        }
    }

    private void listDirectory(Path directory, BlockingQueue<Path> directories, AtomicLong pendingDirectories) {
        if (verbose) {
            IO.println("[hydrate] " + Thread.currentThread() + " listing " + directory);
        }

        try (DirectoryStream<Path> entries = Files.newDirectoryStream(directory)) {
            for (Path entry : entries) {
                if (Files.isSymbolicLink(entry)) {
                    if (verbose) {
                        IO.println("[hydrate] skipping symlink " + entry);
                    }
                    continue;
                }

                if (Files.isDirectory(entry)) {
                    pendingDirectories.incrementAndGet();
                    directories.add(entry);
                } else {
                    long size = Files.size(entry);
                    offer(new FileTask(entry, size));
                }
            }
        } catch (IOException e) {
            if (verbose) {
                IO.println("[hydrate] error reading " + directory + ": " + e.getMessage());
            }
        }
    }

    private static Path pollOrNull(BlockingQueue<Path> directories) {
        try {
            return directories.poll(50, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    private void waitUntilFinished(AtomicLong pendingDirectories) {
        while (pendingDirectories.get() > 0) {
            try {
                TimeUnit.MILLISECONDS.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private void offer(Object item) {
        try {
            files.put(item);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public boolean hasNext() {
        if (next != null) {
            return true;
        }
        if (finished) {
            return false;
        }

        Object item;
        try {
            item = files.take();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            finished = true;
            return false;
        }

        if (item == POISON) {
            finished = true;
            return false;
        }

        next = (FileTask) item;
        return true;
    }

    @Override
    public FileTask next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        FileTask result = next;
        next = null;
        return result;
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }
}
