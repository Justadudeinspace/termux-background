#!/data/data/com.termux/files/usr/bin/bash
# Rebuild script for Termux-Background Plugin
# Author: Briley.ai - 2025

set -e

echo "[✓] Cleaning old builds..."
rm -rf app/build termux-background.zip

echo "[✓] Validating ~/.termux/termux.properties and reload hook..."
mkdir -p ~/.termux
PROP_FILE=~/.termux/termux.properties

# Ensure basic background line exists
grep -qxF "background=background.png" "$PROP_FILE" 2>/dev/null || {
  echo "background=background.png" >> "$PROP_FILE"
  echo "[+] Set: background=background.png"
}

# Add animation settings if not present
add_prop_if_missing() {
  KEY="$1"
  VAL="$2"
  if ! grep -q "^${KEY}=" "$PROP_FILE" 2>/dev/null; then
    echo "${KEY}=${VAL}" >> "$PROP_FILE"
    echo "[+] Set: ${KEY}=${VAL}"
  else
    echo "[✓] ${KEY} already set"
  fi
}

echo "[✓] Injecting Termux background animation defaults..."
add_prop_if_missing "background.opacity"       "0.8"
add_prop_if_missing "background.scale"         "fit"
add_prop_if_missing "background.blur"          "false"
add_prop_if_missing "background.animation"     "scroll"
add_prop_if_missing "background.animation.speed" "1.0"

termux-reload-settings

echo "[✓] Making gradlew executable..."
chmod +x ./gradlew

echo "[✓] Building APK locally with Gradle..."
./gradlew assembleDebug

APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
if [ -f "$APK_PATH" ]; then
  echo "[✓] Build successful: $APK_PATH"
else
  echo "[✗] APK build failed."
  exit 1
fi

echo "[✓] Zipping repo including hidden files..."
cd ..
zip -r termux-background.zip termux-background -x "*/build/*" -x "*.DS_Store"

echo "[✓] Done. Output files:"
echo "   - APK: $APK_PATH"
echo "   - ZIP: termux-background.zip"
