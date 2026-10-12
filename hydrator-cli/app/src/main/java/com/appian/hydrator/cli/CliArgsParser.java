package com.appian.hydrator.cli;

import java.util.ArrayList;
import java.util.List;

public final class CliArgsParser {

    private static final String PARALLEL_FLAG = "parallel";
    private static final String IOPS_FLAG = "iops";
    private static final String MIN_SIZE_FLAG = "min-size";
    private static final String VERBOSE_FLAG = "verbose";

    private CliArgsParser() {
    }

    public static CliConfig parse(String[] args) {
        if (args.length == 0) {
            throw new CliParseException("Missing mode. Expected 'scan' or 'hydrate'.");
        }

        Mode mode = Mode.fromString(args[0]);

        int parallel = CliConfig.DEFAULT_PARALLEL;
        int iops = CliConfig.DEFAULT_IOPS;
        long minSize = CliConfig.DEFAULT_MIN_SIZE;
        boolean verbose = false;
        List<String> locations = new ArrayList<>();

        int i = 1;
        while (i < args.length) {
            String arg = args[i];

            if (isFlag(arg)) {
                String name = flagName(arg);
                String inlineValue = flagInlineValue(arg);

                switch (name) {
                    case PARALLEL_FLAG -> {
                        ValueAndAdvance va = resolveValue(name, inlineValue, args, i);
                        parallel = parseInt(name, va.value());
                        i = va.nextIndex();
                    }
                    case IOPS_FLAG -> {
                        ValueAndAdvance va = resolveValue(name, inlineValue, args, i);
                        iops = parseInt(name, va.value());
                        i = va.nextIndex();
                    }
                    case MIN_SIZE_FLAG -> {
                        ValueAndAdvance va = resolveValue(name, inlineValue, args, i);
                        minSize = parseSize(name, va.value());
                        i = va.nextIndex();
                    }
                    case VERBOSE_FLAG -> {
                        verbose = true;
                        i = i + 1;
                    }
                    default -> throw new CliParseException("Unknown flag '--" + name + "'");
                }
            } else {
                locations.add(arg);
                i = i + 1;
            }
        }

        if (locations.isEmpty()) {
            throw new CliParseException("Expected at least one file or directory argument.");
        }

        return new CliConfig(mode, parallel, iops, minSize, verbose, List.copyOf(locations));
    }

    private static boolean isFlag(String arg) {
        return arg.startsWith("--");
    }

    private static String flagName(String arg) {
        String withoutPrefix = arg.substring(2);
        int equalsIndex = withoutPrefix.indexOf('=');
        return equalsIndex >= 0 ? withoutPrefix.substring(0, equalsIndex) : withoutPrefix;
    }

    private static String flagInlineValue(String arg) {
        String withoutPrefix = arg.substring(2);
        int equalsIndex = withoutPrefix.indexOf('=');
        return equalsIndex >= 0 ? withoutPrefix.substring(equalsIndex + 1) : null;
    }

    private static ValueAndAdvance resolveValue(String name, String inlineValue, String[] args, int currentIndex) {
        if (inlineValue != null) {
            return new ValueAndAdvance(inlineValue, currentIndex + 1);
        }
        int nextIndex = currentIndex + 1;
        if (nextIndex >= args.length) {
            throw new CliParseException("Flag '--" + name + "' requires a value.");
        }
        return new ValueAndAdvance(args[nextIndex], nextIndex + 1);
    }

    private static int parseInt(String flagName, String rawValue) {
        try {
            return Integer.parseInt(rawValue);
        } catch (NumberFormatException e) {
            throw new CliParseException("Flag '--" + flagName + "' expects an integer, got '" + rawValue + "'");
        }
    }

    private static long parseSize(String flagName, String rawValue) {
        if (rawValue.isEmpty()) {
            throw new CliParseException("Flag '--" + flagName + "' expects a size, got '" + rawValue + "'");
        }

        char suffix = Character.toUpperCase(rawValue.charAt(rawValue.length() - 1));
        long multiplier = switch (suffix) {
            case 'K' -> 1024L;
            case 'M' -> 1024L * 1024L;
            case 'G' -> 1024L * 1024L * 1024L;
            case 'T' -> 1024L * 1024L * 1024L * 1024L;
            default -> 1L;
        };
        String numberPart = multiplier == 1L ? rawValue : rawValue.substring(0, rawValue.length() - 1);

        try {
            return Long.parseLong(numberPart) * multiplier;
        } catch (NumberFormatException e) {
            throw new CliParseException("Flag '--" + flagName + "' expects a size (e.g. 1024, 10M, 1G), got '" + rawValue + "'");
        }
    }

    private record ValueAndAdvance(String value, int nextIndex) {
    }
}
