#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
version="${BUILD_VERSION:-$(cat "$ROOT/VERSION")}"
package="$ROOT/termux-background_${version}_all.deb"
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
[[ "$(dpkg-deb -f "$package" Package)" == termux-background ]]
[[ "$(dpkg-deb -f "$package" Version)" == "$version" ]]
[[ "$(dpkg-deb -f "$package" Architecture)" == all ]]
dpkg-deb --extract "$package" "$tmp"
cli="$tmp/data/data/com.termux/files/usr/bin/termux-background"
[[ -x "$cli" ]]
[[ "$(bash "$cli" version)" == "$version" ]]
TERMUX_BACKGROUND_CONFIG_DIR="$tmp/config" bash "$cli" status | grep -q '^No managed background configured$'
[[ "$(find "$tmp" -type f | wc -l)" -eq 1 ]]
printf 'Debian metadata, payload, executable, version and status checks passed\n'
