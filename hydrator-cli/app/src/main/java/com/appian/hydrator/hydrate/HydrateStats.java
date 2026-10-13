package com.appian.hydrator.hydrate;

public record HydrateStats(long filesHydrated, long blocksRead, long bytesRead, long bytesHydrated, long errors) {
}
