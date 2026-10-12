package com.appian.hydrator.hydrate;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Caps the number of in-flight reads at a target rate. Every second, {@link #permitsToIssue()}
 * reports how many new reads are needed to bring the in-flight count back up to the target,
 * so a slow storage backend that only completed 200 of the last 3000 reads gets exactly 200
 * new reads issued rather than a fresh batch of 3000.
 */
final class IopsThrottle {

    private final int target;
    private final AtomicInteger inFlight = new AtomicInteger(0);

    IopsThrottle(int target) {
        this.target = target;
    }

    int permitsToIssue() {
        return Math.max(0, target - inFlight.get());
    }

    void onIssued() {
        inFlight.incrementAndGet();
    }

    void onCompleted() {
        inFlight.decrementAndGet();
    }

    int inFlight() {
        return inFlight.get();
    }
}
