#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
VERSION="${BUILD_VERSION:-$(cat "$ROOT/VERSION")}"
[[ "$VERSION" =~ ^[0-9]+(\.[0-9]+)*([+~.-][a-zA-Z0-9]+)*$ ]] || { printf 'Invalid package version\n' >&2; exit 2; }
OUT="$ROOT/termux-background_${VERSION}_all.deb"
command -v dpkg-deb >/dev/null || { printf 'dpkg-deb is required\n' >&2; exit 1; }
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
mkdir -p "$tmp/DEBIAN" "$tmp/data/data/com.termux/files/usr/bin"
sed "s/TERMUX_BACKGROUND_VERSION:-1.1.0/TERMUX_BACKGROUND_VERSION:-$VERSION/" "$ROOT/termux-background-deb/data/data/com.termux/files/usr/bin/termux-background" > "$tmp/data/data/com.termux/files/usr/bin/termux-background"
chmod 0755 "$tmp/data/data/com.termux/files/usr/bin/termux-background"
cat > "$tmp/DEBIAN/control" <<CONTROL
Package: termux-background
Version: $VERSION
Architecture: all
Maintainer: Termux Background contributors
Depends: termux-tools, file, coreutils
Description: Configure a custom Termux terminal background
 A small command line tool to manage Termux background settings.
CONTROL
dpkg-deb --build --root-owner-group "$tmp" "$OUT"
sha256sum "$OUT"
printf 'Built %s\n' "$OUT"
