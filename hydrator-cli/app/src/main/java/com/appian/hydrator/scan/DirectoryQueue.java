package com.appian.hydrator.scan;

import java.nio.file.Path;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

final class DirectoryQueue {

    private final BlockingQueue<Path> queue = new LinkedBlockingQueue<>();
    private final AtomicLong pending = new AtomicLong(0);

    void push(Path directory) {
        pending.incrementAndGet();
        queue.add(directory);
    }

    Path pollOrNull() {
        try {
            return queue.poll(50, java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    void markDone() {
        pending.decrementAndGet();
    }

    boolean isFinished() {
        return pending.get() == 0;
    }
}
