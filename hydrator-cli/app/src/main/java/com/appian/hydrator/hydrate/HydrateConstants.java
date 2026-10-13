package com.appian.hydrator.hydrate;

public final class HydrateConstants {

    public static final long BLOCK_SIZE = 512L * 1024L;
    public static final int READ_SIZE = 2 * 1024;

    private HydrateConstants() {
    }

    public static long blockCount(long fileSize) {
        if (fileSize <= 0) {
            return 0;
        }
        return (fileSize + BLOCK_SIZE - 1) / BLOCK_SIZE;
    }

    public static long blockOffset(long blockIndex) {
        return blockIndex * BLOCK_SIZE;
    }

    public static int readSizeForBlock(long fileSize, long blockIndex) {
        long offset = blockOffset(blockIndex);
        long remaining = fileSize - offset;
        return (int) Math.min(READ_SIZE, remaining);
    }

    public static long hydratedSizeForBlock(long fileSize, long blockIndex) {
        long offset = blockOffset(blockIndex);
        long remaining = fileSize - offset;
        return Math.min(BLOCK_SIZE, remaining);
    }
}
