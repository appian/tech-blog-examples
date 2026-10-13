package com.appian.hydrator.hydrate;

import java.lang.foreign.Arena;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Fixed pool of pre-allocated, page-aligned buffers, each sized for exactly one
 * partial-block read. Buffers are checked out for the duration of a single read
 * and returned immediately on completion, so total buffer memory stays constant
 * regardless of how many files or blocks are processed.
 */
final class BufferPool {

    private final Arena arena = Arena.ofShared();
    private final BlockingQueue<ByteBuffer> available;

    BufferPool(int poolSize) {
        this.available = new ArrayBlockingQueue<>(poolSize);
        for (int i = 0; i < poolSize; i++) {
            available.add(arena.allocate(HydrateConstants.READ_SIZE, HydrateConstants.READ_SIZE).asByteBuffer());
        }
    }

    ByteBuffer acquire() throws InterruptedException {
        return available.take();
    }

    void release(ByteBuffer buffer) {
        buffer.clear();
        available.add(buffer);
    }
}
