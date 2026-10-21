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
bash "$CLI" set --image "$TMP/image.png" --opacity 0.65 --blur true --animation scroll
[[ "$(cat "$TMP/config/termux.properties")" == *'font=JetBrainsMono'* ]]
grep -q '^background.opacity=0.65$' "$TMP/config/termux.properties"
grep -q '^background.animation=scroll$' "$TMP/config/termux.properties"
[[ -f "$TMP/config/background.png" ]]
bash "$CLI" reset
grep -q '^font=JetBrainsMono$' "$TMP/config/termux.properties"
! grep -q '^background\.' "$TMP/config/termux.properties"
[[ ! -e "$TMP/config/background.png" ]]
if bash "$CLI" set --image "$TMP/image.png" --opacity 2 >/dev/null 2>&1; then
  printf 'invalid opacity unexpectedly accepted\n' >&2; exit 1
else
  rc=$?
  [[ $rc -eq 2 ]]
fi
if bash "$CLI" set --image "$TMP/image.png" --animation bad >/dev/null 2>&1; then
  printf 'invalid animation unexpectedly accepted\n' >&2; exit 1
else
  rc=$?
  [[ $rc -eq 2 ]]
fi
truncate -s 52428801 "$TMP/large-image"
if bash "$CLI" set --image "$TMP/large-image" >/dev/null 2>&1; then
  printf 'oversized image unexpectedly accepted\n' >&2; exit 1
else
  rc=$?
  [[ $rc -eq 2 ]]
fi
[[ "$(bash "$CLI" version)" == "$TERMUX_BACKGROUND_VERSION" ]]
printf 'CLI tests passed\n'
printf 'VEJHX1RSQU5TRkVSPTE=' | bash "$CLI" probe > "$TMP/probe"
grep -q '^TBG_PROTOCOL=1$' "$TMP/probe"
grep -q '^IMAGE_TRANSFER=VERIFIED$' "$TMP/probe"
grep -q '^RELOAD=COMPLETED$' "$TMP/probe"
base64 < "$TMP/image.png" | tr -d '\n' | bash "$CLI" set --image-stdin --opacity 0.4
cmp "$TMP/image.png" "$TMP/config/background.png"
if printf 'not base64!' | bash "$CLI" set --image-stdin >/dev/null 2>&1; then
  exit 1
else
  [[ $? -eq 4 ]]
fi
if head -c 163841 /dev/zero | tr '\0' A | bash "$CLI" set --image-stdin >/dev/null 2>&1; then
  exit 1
else
  [[ $? -eq 4 ]]
fi
[[ -z "$(find "$TMP/config" -name '.transfer.*' -o -name '.background.*' -o -name '.termux.properties.*')" ]]
printf '# preserved\n  background.opacity =0.2\nfont=keep\n' > "$TMP/config/termux.properties"
bash "$CLI" set --image "$TMP/image.png"
! grep -q '^  background.opacity' "$TMP/config/termux.properties"
grep -q '^font=keep$' "$TMP/config/termux.properties"
printf '#!/bin/sh\nexit 1\n' > "$TMP/bin/termux-reload-settings"
if bash "$CLI" reload >/dev/null 2>&1; then exit 1; else [[ $? -eq 6 ]]; fi
if bash "$CLI" reset >/dev/null 2>&1; then exit 1; else [[ $? -eq 6 ]]; fi
printf 'Bridge CLI regression tests passed\n'
printf '#!/bin/sh\nexit 0\n' > "$TMP/bin/termux-reload-settings"
printf 'font=keep\nbackground.opacity=0.6\n' > "$TMP/config/termux.properties"
base64 < "$TMP/image.png" | tr -d '\n' | bash "$CLI" --backend monet set --image-stdin --opacity 0.5
cmp "$TMP/image.png" "$TMP/config/background/background.jpeg"
grep -q '^background-overlay-color=#80000000$' "$TMP/config/termux.properties"
grep -q '^background.opacity=0.6$' "$TMP/config/termux.properties"
[[ -f "$TMP/config/.termux-background-monet" ]]
printf 'VEJHX1RSQU5TRkVSPTE=' | bash "$CLI" --backend monet probe > "$TMP/probe"
grep -q '^BACKEND=monet$' "$TMP/probe"
grep -q '^BACKGROUND=SET$' "$TMP/probe"
if bash "$CLI" --backend monet set --image "$TMP/image.png" --blur true >/dev/null 2>&1; then exit 1; else [[ $? -eq 2 ]]; fi
if bash "$CLI" --backend monet set --image "$TMP/image.png" --animation scroll >/dev/null 2>&1; then exit 1; else [[ $? -eq 2 ]]; fi
touch "$TMP/config/background/background_portrait.jpeg" "$TMP/config/background/background_landscape.jpeg"
bash "$CLI" --backend monet reset
[[ ! -e "$TMP/config/background/background.jpeg" ]]
[[ ! -e "$TMP/config/background/background_portrait.jpeg" ]]
[[ ! -e "$TMP/config/background/background_landscape.jpeg" ]]
[[ ! -e "$TMP/config/.termux-background-monet" ]]
! grep -q '^background-overlay-color=' "$TMP/config/termux.properties"
grep -q '^font=keep$' "$TMP/config/termux.properties"
grep -q '^background.opacity=0.6$' "$TMP/config/termux.properties"
printf 'Monet backend regression tests passed\n'
printf 'background-overlay-color=#12345678\nfont=keep\n' > "$TMP/config/termux.properties"
bash "$CLI" --backend monet reset
grep -q '^background-overlay-color=#12345678$' "$TMP/config/termux.properties"
printf 'Unmanaged Monet settings preserved\n'
