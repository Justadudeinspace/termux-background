#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CLI="$ROOT/termux-background-deb/data/data/com.termux/files/usr/bin/termux-background"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
mkdir -p "$TMP/bin" "$TMP/config"
printf '#!/bin/sh\nexit 0\n' > "$TMP/bin/termux-reload-settings"
chmod +x "$TMP/bin/termux-reload-settings"
# A valid 1x1 PNG fixture.
printf '%s' 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/pXcAAAAASUVORK5CYII=' | base64 -d > "$TMP/image.png"
printf '# keep this\nfont=JetBrainsMono\nbackground.opacity=0.3\n' > "$TMP/config/termux.properties"
export PATH="$TMP/bin:$PATH" TERMUX_BACKGROUND_CONFIG_DIR="$TMP/config"
export TERMUX_BACKGROUND_VERSION="$(cat "$ROOT/VERSION")"
"$CLI" set --image "$TMP/image.png" --opacity 0.65 --blur true --animation scroll
[[ "$(cat "$TMP/config/termux.properties")" == *'font=JetBrainsMono'* ]]
grep -q '^background.opacity=0.65$' "$TMP/config/termux.properties"
grep -q '^background.animation=scroll$' "$TMP/config/termux.properties"
[[ -f "$TMP/config/background.png" ]]
"$CLI" reset
grep -q '^font=JetBrainsMono$' "$TMP/config/termux.properties"
! grep -q '^background\.' "$TMP/config/termux.properties"
[[ ! -e "$TMP/config/background.png" ]]
if "$CLI" set --image "$TMP/image.png" --opacity 2 >/dev/null 2>&1; then
  printf 'invalid opacity unexpectedly accepted\n' >&2; exit 1
else
  rc=$?
  [[ $rc -eq 2 ]]
fi
if "$CLI" set --image "$TMP/image.png" --animation bad >/dev/null 2>&1; then
  printf 'invalid animation unexpectedly accepted\n' >&2; exit 1
else
  rc=$?
  [[ $rc -eq 2 ]]
fi
truncate -s 52428801 "$TMP/large-image"
if "$CLI" set --image "$TMP/large-image" >/dev/null 2>&1; then
  printf 'oversized image unexpectedly accepted\n' >&2; exit 1
else
  rc=$?
  [[ $rc -eq 2 ]]
fi
[[ "$("$CLI" version)" == '1.1.0' ]]
printf 'CLI tests passed\n'
