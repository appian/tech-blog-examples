package com.appian.hydrator.scan;

public record ScanStats(long directoriesScanned, long filesFound, long errors) {

    public static ScanStats empty() {
        return new ScanStats(0L, 0L, 0L);
    }

    ScanStats merge(ScanStats other) {
        return new ScanStats(
                directoriesScanned + other.directoriesScanned,
                filesFound + other.filesFound,
                errors + other.errors);
    }
}
