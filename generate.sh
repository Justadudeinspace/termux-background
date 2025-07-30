#!/bin/bash
# Build Termux-Background.apk offline

set -e

cd "$(dirname "$0")"

echo "[+] Cleaning previous builds..."
rm -rf build dist
mkdir -p build/classes dist/apk

echo "[+] Compiling Java sources..."
find app/src/main/java -name "*.java" > sources.txt
javac -d build/classes @sources.txt

echo "[+] Converting to DEX..."
dx --dex --output=build/classes.dex build/classes

echo "[+] Packaging APK..."
aapt package -f -M app/src/main/AndroidManifest.xml -S app/src/main/res -A app/src/main/assets -I $ANDROID_HOME/platforms/android-33/android.jar -F dist/app.unsigned.apk build

echo "[+] Adding DEX..."
aapt add dist/app.unsigned.apk build/classes.dex

echo "[+] Signing APK..."
apksigner sign --ks my-release-key.jks --ks-pass pass:password --key-pass pass:password --key-alias alias_name --out dist/termux-background.apk dist/app.unsigned.apk

echo "[✓] Done. APK is at dist/termux-background.apk"
