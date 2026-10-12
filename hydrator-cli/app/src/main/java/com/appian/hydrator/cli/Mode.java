package com.appian.hydrator.cli;

public enum Mode {
    SCAN,
    HYDRATE;

    static Mode fromString(String value) {
        for (Mode mode : values()) {
            if (mode.name().equalsIgnoreCase(value)) {
                return mode;
            }
        }
        throw new CliParseException("Unknown mode '" + value + "'. Expected one of: scan, hydrate");
    }
}
