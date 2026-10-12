package com.appian.hydrator.hydrate;

import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

record Block(Path path, AsynchronousFileChannel channel, long offset, int readSize, long hydratedSize,
             AtomicLong remainingBlocksInFile, AtomicBoolean fileHadError) {

    boolean isLastBlockInFile() {
        return remainingBlocksInFile.decrementAndGet() == 0;
    }
}
