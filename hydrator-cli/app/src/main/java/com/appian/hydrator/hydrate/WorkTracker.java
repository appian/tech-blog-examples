package com.appian.hydrator.hydrate;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;

final class WorkTracker {

    private final AtomicLong outstanding = new AtomicLong(0);
    private volatile boolean feedingDone = false;
    private final CompletableFuture<Void> done = new CompletableFuture<>();

    void blockEnqueued() {
        outstanding.incrementAndGet();
    }

    void blockCompleted() {
        if (outstanding.decrementAndGet() == 0 && feedingDone) {
            done.complete(null);
        }
    }

    void feedingDone() {
        feedingDone = true;
        if (outstanding.get() == 0) {
            done.complete(null);
        }
    }

    void awaitDone() throws InterruptedException {
        try {
            done.get();
        } catch (ExecutionException e) {
            throw new IllegalStateException(e);
        }
    }
}
