package com.appian.hydrator.cli;

import java.util.List;

public record CliConfig(Mode mode, int parallel, int iops, long minSize, boolean verbose, List<String> locations) {

    public static final int DEFAULT_PARALLEL = 64;
    public static final int DEFAULT_IOPS = 3000;
    public static final long DEFAULT_MIN_SIZE = 10485760L;
}
