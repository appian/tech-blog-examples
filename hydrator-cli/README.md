# Hydrator

Deep Dive Blog => [Hydrator: Making EBS Snapshot Restoration Faster Through Prewarming](https://medium.com/appian-tech-blog/hydrator-8c3b430c17f1)

Hydrator is a CLI tool for pre-warming EBS volumes that were restored from a snapshot.

When an EBS volume is restored from a snapshot, its blocks are lazily fetched from S3 the first time they're touched. Until then, reads on that volume are slow. Hydrator forces those blocks to be fetched ahead of time so applications don't pay that cost later.

## Installation

Install the `hydrator` CLI into `~/.local/bin` with:

```sh
curl -fsSL https://raw.githubusercontent.com/appian/tech-blog-examples/refs/heads/main/hydrator-cli/install.sh | sh -s
```

The script detects your OS and architecture and downloads the matching release. Supported platforms are `linux-amd64`, `linux-arm64`, and `darwin-arm64`.

To change the install directory or release tag:

```sh
curl -fsSL https://raw.githubusercontent.com/appian/tech-blog-examples/refs/heads/main/hydrator-cli/install.sh | sh -s -- --dir /usr/local/bin --tag hydrator-cli-v1
```

Run `sh -s -- --help` on the same command to see all options.

## How it works

Hydrator has two modes:

### scan

Walks directories in parallel to warm up file system metadata (similar to running `fd`/`find` with many threads). On a cold volume, listing files is slow because directory metadata itself has to be fetched from S3 block by block. Scanning once with many parallel threads warms this up.

```
hydrator scan --parallel=64 [--verbose] <dir1> [dir2 ...]
```

### hydrate

Warms up file contents. EBS restores data from S3 in 512 KB chunks: touching a single byte inside a chunk causes the whole chunk to be fetched. Hydrator exploits this by issuing a small 2 KB read into every 512 KB block of a file, in parallel, up to a target IOPS rate. This fetches the whole file from S3 using far fewer bytes of actual read throughput than reading the file in full.

```
hydrator hydrate --iops=3000 [--min-size=<size>] [--verbose] <file-or-dir1> [file-or-dir2 ...]
```

- `--iops` sets the target number of read operations per second (matches your EBS volume's provisioned IOPS).
- `--min-size` skips files smaller than the given size (e.g. `10M`, `1G`). Small files are cheap to read in full anyway.

## Building

Requires JDK 25 and GraalVM native-image.

Build a native binary:

```
./gradlew nativeCompile
```

The binary is produced at `app/build/native/nativeCompile/hydrator`.

Alternatively, run it directly through Gradle without building a native image:

```
./gradlew :app:run --args="hydrate --iops=3000 /path/to/warm"
```

## License

MIT. See [LICENSE](LICENSE).
