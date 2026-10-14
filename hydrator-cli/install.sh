#!/usr/bin/env sh
# install.sh - Install the hydrator CLI from a GitHub release.
#
# Detects the OS and CPU architecture, downloads the matching archive
# (hydrator-<os>-<arch>.tar.gz) from the release tag, and installs the
# binary as "hydrator" into the install directory.
#
# Supported platforms:
#   linux-amd64, linux-arm64, darwin-arm64
#
# Usage:
#   install.sh [--dir <install-dir>] [--tag <release-tag>]
#   install.sh --help
#
# Options:
#   --dir <path>   Install directory.        Default: ~/.local/bin
#   --tag <tag>    GitHub release tag.       Default: hydrator-cli-v1
#   -h, --help     Show usage and exit.
#
# Environment variables (flags take precedence over these):
#   HYDRATOR_INSTALL_DIR   Same as --dir.
#   HYDRATOR_TAG           Same as --tag.
#
# Examples:
#   # Install to ~/.local/bin from the default release
#   ./install.sh
#
#   # Install to a custom directory
#   ./install.sh --dir /usr/local/bin
#
#   # Install a different release
#   ./install.sh --tag hydrator-cli-v2
#
#   # Same as above, using environment variables
#   HYDRATOR_INSTALL_DIR=$HOME/bin HYDRATOR_TAG=hydrator-cli-v2 ./install.sh
#
# If the install directory is not on PATH, the script prints the export line
# to add to your shell profile.
set -eu

REPO_URL="https://github.com/appian/tech-blog-examples"
DEFAULT_TAG="hydrator-cli-v1"
DEFAULT_DIR="${HOME}/.local/bin"
BIN_NAME="hydrator"

INSTALL_DIR="${HYDRATOR_INSTALL_DIR:-${DEFAULT_DIR}}"
TAG="${HYDRATOR_TAG:-${DEFAULT_TAG}}"

die() {
    echo "error: $*" >&2
    exit 1
}

usage() {
    cat <<EOF
Usage: install.sh [--dir <install-dir>] [--tag <release-tag>] [--help]

Options:
  --dir <path>   Directory to install the binary into (default: ${DEFAULT_DIR})
  --tag <tag>    Release tag to download from (default: ${DEFAULT_TAG})
  -h, --help     Show this help and exit

Environment:
  HYDRATOR_INSTALL_DIR   Same as --dir
  HYDRATOR_TAG           Same as --tag
EOF
}

parse_args() {
    while [ $# -gt 0 ]; do
        case "$1" in
            --dir)
                [ $# -ge 2 ] || die "--dir requires a value"
                INSTALL_DIR="$2"
                shift 2
                ;;
            --dir=*)
                INSTALL_DIR="${1#--dir=}"
                shift
                ;;
            --tag)
                [ $# -ge 2 ] || die "--tag requires a value"
                TAG="$2"
                shift 2
                ;;
            --tag=*)
                TAG="${1#--tag=}"
                shift
                ;;
            -h|--help)
                usage
                exit 0
                ;;
            *)
                die "unknown argument: $1 (see --help)"
                ;;
        esac
    done

    [ -n "${INSTALL_DIR}" ] || die "install directory must not be empty"
    [ -n "${TAG}" ] || die "release tag must not be empty"
}

need() {
    command -v "$1" >/dev/null 2>&1 || die "required command not found: $1"
}

detect_os() {
    case "$(uname -s)" in
        Linux) echo "linux" ;;
        Darwin) echo "darwin" ;;
        *) die "unsupported OS: $(uname -s)" ;;
    esac
}

detect_arch() {
    case "$(uname -m)" in
        x86_64|amd64) echo "amd64" ;;
        arm64|aarch64) echo "arm64" ;;
        *) die "unsupported architecture: $(uname -m)" ;;
    esac
}

main() {
    parse_args "$@"

    need curl
    need tar

    os="$(detect_os)"
    arch="$(detect_arch)"

    # Only these combinations are published as release assets.
    case "${os}-${arch}" in
        linux-amd64|linux-arm64|darwin-arm64) ;;
        *) die "no prebuilt binary for ${os}-${arch}" ;;
    esac

    asset="hydrator-${os}-${arch}.tar.gz"
    url="${REPO_URL}/releases/download/${TAG}/${asset}"

    tmp_dir="$(mktemp -d)"
    trap 'rm -rf "${tmp_dir}"' EXIT INT TERM

    echo "Downloading ${asset} from tag ${TAG}..."
    curl -fsSL -o "${tmp_dir}/${asset}" "${url}" || die "download failed: ${url}"

    tar -xzf "${tmp_dir}/${asset}" -C "${tmp_dir}" || die "failed to extract ${asset}"

    src="${tmp_dir}/hydrator-${os}-${arch}"
    [ -f "${src}" ] || die "archive did not contain hydrator-${os}-${arch}"

    mkdir -p "${INSTALL_DIR}"
    install -m 0755 "${src}" "${INSTALL_DIR}/${BIN_NAME}"

    echo "Installed ${BIN_NAME} to ${INSTALL_DIR}/${BIN_NAME}"

    case ":${PATH}:" in
        *":${INSTALL_DIR}:"*) ;;
        *) echo "Note: ${INSTALL_DIR} is not on your PATH. Add it with:"
           echo "  export PATH=\"${INSTALL_DIR}:\$PATH\"" ;;
    esac
}

main "$@"
