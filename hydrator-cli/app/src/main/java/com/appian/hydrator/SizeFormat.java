package com.appian.hydrator;

public final class SizeFormat {

    private static final long KB = 1024L;
    private static final long MB = KB * 1024L;
    private static final long GB = MB * 1024L;
    private static final long TB = GB * 1024L;

    private SizeFormat() {
    }

    public static String humanReadable(long bytes) {
        if (bytes < KB) {
            return bytes + " B";
        }
        if (bytes < MB) {
            return format(bytes, KB, "KB");
        }
        if (bytes < GB) {
            return format(bytes, MB, "MB");
        }
        if (bytes < TB) {
            return format(bytes, GB, "GB");
        }
        return format(bytes, TB, "TB");
    }

    private static String format(long bytes, long unit, String label) {
        return String.format("%.2f %s", bytes / (double) unit, label);
    }
}
