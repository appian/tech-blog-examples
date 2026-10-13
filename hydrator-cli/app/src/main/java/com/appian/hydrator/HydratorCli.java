package com.appian.hydrator;

import com.appian.hydrator.cli.CliArgsParser;
import com.appian.hydrator.cli.CliConfig;
import com.appian.hydrator.cli.CliParseException;
import com.appian.hydrator.hydrate.HydrateStats;
import com.appian.hydrator.hydrate.Hydrator;
import com.appian.hydrator.scan.DirectoryScanner;
import com.appian.hydrator.scan.ScanStats;

import java.nio.file.Path;
import java.util.List;

public final class HydratorCli {

    public static void main(String[] args) {
        try {
            CliConfig config = CliArgsParser.parse(args);
            run(config);
        } catch (CliParseException e) {
            System.err.println("Error: " + e.getMessage());
            System.err.println(usage());
            System.exit(1);
        }
    }

    private static void run(CliConfig config) {
        if (config.verbose()) {
            IO.println("mode=" + config.mode()
                    + " parallel=" + config.parallel()
                    + " iops=" + config.iops()
                    + " minSize=" + SizeFormat.humanReadable(config.minSize())
                    + " verbose=" + config.verbose()
                    + " locations=" + config.locations());
        }

        switch (config.mode()) {
            case SCAN -> runScan(config);
            case HYDRATE -> runHydrate(config);
        }
    }

    private static void runScan(CliConfig config) {
        List<Path> roots = config.locations().stream().map(Path::of).toList();

        DirectoryScanner scanner = new DirectoryScanner(config.parallel(), config.verbose());
        ScanStats stats = scanner.scan(roots);

        IO.println("Scanned " + stats.directoriesScanned() + " directorie(s), found "
                + stats.filesFound() + " file(s), " + stats.errors() + " error(s)");
    }

    private static void runHydrate(CliConfig config) {
        List<Path> locations = config.locations().stream().map(Path::of).toList();

        Hydrator hydrator = new Hydrator(config.iops(), config.minSize(), config.verbose());
        HydrateStats stats = hydrator.hydrate(locations, config.parallel());

        IO.println("Hydrated " + stats.filesHydrated() + " file(s), " + stats.blocksRead()
                + " block(s), " + SizeFormat.humanReadable(stats.bytesRead()) + " read, "
                + SizeFormat.humanReadable(stats.bytesHydrated()) + " hydrated, "
                + stats.errors() + " error(s)");
    }

    private static String usage() {
        return """
                Usage:
                  hydrator scan --parallel=<n> [--verbose] <dir1> [dir2 ...]
                  hydrator hydrate --iops=<n> [--min-size=<size>] [--verbose] <file-or-dir1> [file-or-dir2 ...]""";
    }
}
