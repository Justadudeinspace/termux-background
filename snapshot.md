# Repository Snapshot

## 1) Metadata
- Repository name: termux-background
- Organization / owner: unknown
- Default branch: work
- HEAD commit hash: adb9052c8e5a294eb44574b2c00c12c922f1de95
- Snapshot timestamp (UTC): 2026-01-21T08:01:16Z
- Total file count (excluding directories): 54
- Description: > **✨ Status:** Active Development

## 2) Repository Tree
- .github/
  - workflows/
    - build.yml [text]
- .gitignore [text]
- .gradle/
  - 8.14.3/
    - fileChanges/
      - last-build.bin [binary]
    - fileHashes/
      - fileHashes.lock [binary]
    - gc.properties [text]
  - 8.4/
    - checksums/
      - checksums.lock [binary]
    - fileChanges/
      - last-build.bin [binary]
    - fileHashes/
      - fileHashes.lock [binary]
    - gc.properties [text]
    - vcsMetadata/
  - buildOutputCleanup/
    - buildOutputCleanup.lock [binary]
    - cache.properties [text]
  - vcs-1/
    - gc.properties [text]
- CONTRIBUTING.md [text]
- LICENSE [text]
- README.md [text]
- RELEASE-DRAFT.txt [text]
- app/
  - build.gradle [text]
  - build.gradle.bak [text]
  - proguard-rules.pro [text]
  - src/
    - main/
      - AndroidManifest.xml [text]
      - assets/
        - background.png [binary]
        - install.sh [text]
        - termux-background-ui.html [text]
      - java/
        - com/
          - termuxbackground/
            - BackgroundInterface.java [text]
            - MainActivity.java [text]
            - WebAppInterface.java [text]
      - res/
        - layout/
          - activity_main.xml [text]
        - mipmap-hdpi/
          - ic_launcher.png [binary]
        - mipmap-mdpi/
          - ic_launcher.png [binary]
        - mipmap-xhdpi/
          - ic_launcher.png [binary]
        - mipmap-xxhdpi/
          - ic_launcher.png [binary]
        - mipmap-xxxhdpi/
          - ic_launcher.png [binary]
  - termux-release.keystore [binary]
- build-deb.sh [text]
- build.gradle [text]
- debfix/
  - DEBIAN/
    - control [text]
    - postinst [text]
  - data/
    - data/
      - com.termux/
        - files/
          - usr/
            - bin/
              - background.png [text]
- docs/
  - banner.png [binary]
  - screenshot-preview.png [binary]
  - screenshot-ui.png [binary]
- generate.sh [text]
- gradle/
  - wrapper/
    - gradle-wrapper.jar [binary]
    - gradle-wrapper.properties [text]
- gradle.properties [text]
- gradlew [text]
- gradlew.bat [text]
- keystore.properties [text]
- release-notes.md [text]
- settings.gradle [text]
- termux-background-deb/
  - DEBIAN/
    - control [text]
  - data/
    - data/
      - com.termux/
        - files/
          - usr/
            - bin/
              - termux-background [text]
- termux-background_1.0.1_all.deb [binary]
- termux-background_1.0.4_all.deb [binary]

## 3) FULL FILE CONTENTS (MANDATORY)

FILE: .github/workflows/build.yml
Kind: text
Size: 9990
Last modified: 2026-01-21T07:59:08Z

CONTENT:
name: Build & Release Termux Background

on:
  push:
    branches: [main]
  workflow_dispatch:

permissions:
  contents: write

jobs:
  calculate-version:
    runs-on: ubuntu-latest
    outputs:
      version: ${{ steps.calc-version.outputs.version }}
      tag_name: ${{ steps.calc-version.outputs.tag_name }}
    steps:
      - name: 🧾 Checkout repository with tags
        uses: actions/checkout@v4
        with:
          fetch-depth: 0
          ref: ${{ github.ref }}

      - name: 🔢 Calculate next semantic version
        id: calc-version
        run: |
          set -euo pipefail
          latest_tag=$(git tag --list 'v[0-9]*.[0-9]*.[0-9]*' --sort=-v:refname | head -n1)
          
          if [ -z "$latest_tag" ]; then
            new_version="1.0.0"
            echo "📌 No existing tag found. Starting at version 1.0.0"
          else
            version_number="${latest_tag#v}"
            IFS='.' read -r major minor patch <<< "$version_number"
            new_patch=$((patch + 1))
            new_version="$major.$minor.$new_patch"
            echo "📌 Incremented $latest_tag to $new_version"
          fi
          
          echo "version=$new_version" >> "$GITHUB_OUTPUT"
          echo "tag_name=v$new_version" >> "$GITHUB_OUTPUT"

  build-artifacts:
    needs: calculate-version
    runs-on: ubuntu-latest
    outputs:
      deb_path: ${{ steps.artifact-paths.outputs.deb_path }}
      debug_apk: ${{ steps.artifact-paths.outputs.debug_apk }}
      release_apk: ${{ steps.artifact-paths.outputs.release_apk }}
    
    steps:
      - name: 🧾 Checkout code
        uses: actions/checkout@v4

      - name: ☕ Setup Java for Android
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'

      - name: 📦 Setup Android SDK
        uses: android-actions/setup-android@v3
        with:
          packages: "build-tools;34.0.0 platform-tools platforms;android-34 cmdline-tools;latest"

      # ✅ PATCH 1: Detect Gradle wrapper location (fixed output)
      - name: 🔎 Detect Gradle wrapper location (repo may not be root)
        run: |
          set -euo pipefail
          GRADLEW="$(find . -maxdepth 4 -type f -name gradlew -print -quit || true)"
          if [ -z "$GRADLEW" ]; then
            echo "❌ gradlew not found (searched maxdepth=4)."
            echo "Repo root contents:"
            ls -la
            exit 1
          fi
          GRADLE_DIR="$(cd "$(dirname "$GRADLEW")" && pwd)"
          echo "✅ gradlew: $GRADLE_DIR/gradlew"
          echo "✅ gradle dir: $GRADLE_DIR"
          echo "GRADLE_DIR=$GRADLE_DIR" >> "$GITHUB_ENV"

      # ✅ PATCH 2: Make gradlew executable (using absolute path)
      - name: 🔧 Make gradlew executable
        run: chmod +x "$GRADLE_DIR/gradlew"

      # ✅ PATCH 3: Clean and build APKs (using ./gradlew after cd)
      - name: 🏗 Clean and build APKs
        run: |
          set -euo pipefail
          cd "$GRADLE_DIR"

          ./gradlew --version
          ./gradlew clean
          ./gradlew assembleDebug || true

          echo "Attempting release build..."
          ./gradlew assembleRelease || echo "⚠ Release build failed (likely signing). Continuing..."

          echo "Available APKs:"
          find . -path "*/build/outputs/apk/*/*.apk" -print || true

          # Ensure at least one APK exists anywhere under build/outputs/apk
          if [ -z "$(find . -path '*/build/outputs/apk/*/*.apk' -print -quit)" ]; then
            echo "❌ No APK files found under any */build/outputs/apk/*/"
            echo "Dumping relevant directories:"
            find . -maxdepth 4 -type d -name outputs -o -name build || true
            exit 1
          fi

      - name: 📦 Create .deb package structure
        run: |
          set -euo pipefail
          rm -rf package
          mkdir -p package/DEBIAN
          mkdir -p package/data/data/com.termux/files/usr/bin
          mkdir -p package/data/data/com.termux/files/usr/share/termux-background/

      - name: 📝 Create DEBIAN/control file
        run: |
          set -euo pipefail
          cat > package/DEBIAN/control << EOF
Package: termux-background
Version: ${{ needs.calculate-version.outputs.version }}
Architecture: all
Maintainer: Justadudeinspace
Description: Termux background manager
 A simple utility to set background images in Termux.
EOF

      - name: ⚙️ Create main executable script
        run: |
          set -euo pipefail
          cat > package/data/data/com.termux/files/usr/bin/termux-background << 'EOF'
#!/data/data/com.termux/files/usr/bin/bash

TERMUX_DIR="$HOME/.termux"
PROP_FILE="$TERMUX_DIR/termux.properties"
BG_FILE="$TERMUX_DIR/background.png"

case "$1" in
  -h|--help)
    echo "Usage: termux-background <image.png>"
    echo "       termux-background --remove"
    echo "       termux-background --status"
    exit 0
    ;;
  --remove)
    rm -f "$BG_FILE"
    echo "✓ Background removed"
    exit 0
    ;;
  --status)
    if [ -f "$BG_FILE" ]; then
      echo "Background set: $BG_FILE"
    else
      echo "No background set"
    fi
    exit 0
    ;;
esac

if [ -z "$1" ]; then
  echo "Error: No image specified"
  echo "Usage: termux-background <image.png>"
  exit 1
fi

if [ ! -f "$1" ]; then
  echo "Error: File not found: $1"
  exit 1
fi

mkdir -p "$TERMUX_DIR"
cp "$1" "$BG_FILE"
echo "✓ Background set to: $BG_FILE"

if [ ! -f "$PROP_FILE" ] || ! grep -q "background=" "$PROP_FILE"; then
  {
    echo "background=background.png"
    echo "background.opacity=0.8"
    echo "background.animation=scroll"
  } >> "$PROP_FILE"
  echo "✓ Added background settings to termux.properties"
fi

echo "ℹ Run 'termux-reload-settings' to apply changes"
EOF
          
          chmod 755 package/data/data/com.termux/files/usr/bin/termux-background

      - name: 🧰 Install dpkg-deb tooling
        run: |
          set -euo pipefail
          sudo apt-get update
          sudo apt-get install -y dpkg-dev

      - name: 📦 Build .deb package
        run: |
          set -euo pipefail
          dpkg-deb --build -Zgzip package "termux-background_${{ needs.calculate-version.outputs.version }}_all.deb"

      - name: 📊 Collect artifact paths
        id: artifact-paths
        run: |
          set -euo pipefail
          DEB_FILE="termux-background_${{ needs.calculate-version.outputs.version }}_all.deb"
          
          # Find APKs in the Gradle directory structure
          DEBUG_APK=$(find "$GRADLE_DIR" -path "*/build/outputs/apk/debug/*.apk" 2>/dev/null | head -1 || echo "")
          RELEASE_APK=$(find "$GRADLE_DIR" -path "*/build/outputs/apk/release/*.apk" 2>/dev/null | head -1 || echo "")
          
          echo "deb_path=$DEB_FILE" >> $GITHUB_OUTPUT
          echo "debug_apk=$DEBUG_APK" >> $GITHUB_OUTPUT
          echo "release_apk=$RELEASE_APK" >> $GITHUB_OUTPUT
          
          echo "📦 Final artifacts:"
          echo "  - $DEB_FILE"
          [ -n "$DEBUG_APK" ] && echo "  - $DEBUG_APK"
          [ -n "$RELEASE_APK" ] && echo "  - $RELEASE_APK"

      - name: ⬆️ Upload build artifacts
        uses: actions/upload-artifact@v4
        with:
          name: github-artifacts
          if-no-files-found: error
          path: |
            termux-background_*.deb
            **/build/outputs/apk/**/*.apk

  create-release:
    needs: [calculate-version, build-artifacts]
    if: github.event_name == 'workflow_dispatch' || (github.event_name == 'push' && github.ref == 'refs/heads/main' && contains(github.event.head_commit.message, '[release]'))
    runs-on: ubuntu-latest
    
    steps:
      - name: 🧾 Checkout repository (with same commit as build)
        uses: actions/checkout@v4
        with:
          fetch-depth: 0
          ref: ${{ github.sha }}
          token: ${{ secrets.GITHUB_TOKEN }}

      - name: 🏷️ Create git tag (with safety check)
        run: |
          set -euo pipefail
          git config user.name "github-actions"
          git config user.email "github-actions@github.com"
          
          TAG_NAME="${{ needs.calculate-version.outputs.tag_name }}"
          
          # Safety check: don't delete existing tags - abort instead
          if git rev-parse "$TAG_NAME" >/dev/null 2>&1; then
            echo "❌ Tag $TAG_NAME already exists. Aborting to avoid rewriting releases."
            echo "If you need a new release, create a new commit or manually delete the tag."
            exit 1
          fi
          
          git tag -a "$TAG_NAME" -m "Release $TAG_NAME"
          git push origin "$TAG_NAME"
          echo "✅ Tag $TAG_NAME created and pushed"

      - name: ⏳ Wait for tag to propagate (with retry)
        run: |
          set -euo pipefail
          TAG_NAME="${{ needs.calculate-version.outputs.tag_name }}"
          echo "Waiting for tag $TAG_NAME to propagate..."
          
          for i in 1 2 3 4 5; do
            echo "Attempt $i/5: Checking if tag exists on remote..."
            if git ls-remote --tags origin "$TAG_NAME" | grep -q .; then
              echo "✅ Tag $TAG_NAME found on remote"
              break
            fi
            if [ $i -eq 5 ]; then
              echo "⚠ Tag $TAG_NAME not found on remote after $i attempts"
              echo "Continuing anyway - release may fail if tag isn't found"
            else
              sleep 3
            fi
          done

      - name: 📥 Download artifacts from build job
        uses: actions/download-artifact@v4
        with:
          name: github-artifacts
          path: ./artifacts/

      - name: 🚀 Create GitHub release
        uses: softprops/action-gh-release@v2
        with:
          tag_name: ${{ needs.calculate-version.outputs.tag_name }}
          name: "Termux Background ${{ needs.calculate-version.outputs.tag_name }}"
          generate_release_notes: true
          files: |
            artifacts/**
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}

FILE: .gitignore
Kind: text
Size: 291
Last modified: 2026-01-21T07:59:08Z

CONTENT:
# Ignore Gradle build outputs
/build/
/app/build/
*.apk

# IDE / editor junk
.idea/
*.iml
*.swp

# OS-specific
.DS_Store
Thumbs.db

# Termux / local system config (exclude only if not needed)
.termux/*
!/.termux/termux.properties

# Log and temp
*.log
*.tmp

# Gradle wrapper cache
.gradle/

FILE: .gradle/8.14.3/fileChanges/last-build.bin
Kind: binary
Size: 1
Last modified: 2026-01-21T07:59:42Z

CONTENT:
BINARY FILE — NOT DISPLAYED
file size: 1
detected type: application/octet-stream

FILE: .gradle/8.14.3/fileHashes/fileHashes.lock
Kind: binary
Size: 17
Last modified: 2026-01-21T07:59:42Z

CONTENT:
BINARY FILE — NOT DISPLAYED
file size: 17

FILE: .gradle/8.14.3/gc.properties
Kind: text
Size: 0
Last modified: 2026-01-21T07:59:42Z

CONTENT:


FILE: .gradle/8.4/checksums/checksums.lock
Kind: binary
Size: 17
Last modified: 2026-01-21T07:59:34Z

CONTENT:
BINARY FILE — NOT DISPLAYED
file size: 17

FILE: .gradle/8.4/fileChanges/last-build.bin
Kind: binary
Size: 1
Last modified: 2026-01-21T07:59:34Z

CONTENT:
BINARY FILE — NOT DISPLAYED
file size: 1
detected type: application/octet-stream

FILE: .gradle/8.4/fileHashes/fileHashes.lock
Kind: binary
Size: 17
Last modified: 2026-01-21T07:59:34Z

CONTENT:
BINARY FILE — NOT DISPLAYED
file size: 17

FILE: .gradle/8.4/gc.properties
Kind: text
Size: 0
Last modified: 2026-01-21T07:59:34Z

CONTENT:


FILE: .gradle/buildOutputCleanup/buildOutputCleanup.lock
Kind: binary
Size: 17
Last modified: 2026-01-21T07:59:42Z

CONTENT:
BINARY FILE — NOT DISPLAYED
file size: 17

FILE: .gradle/buildOutputCleanup/cache.properties
Kind: text
Size: 52
Last modified: 2026-01-21T07:59:40Z

CONTENT:
#Wed Jan 21 07:59:40 UTC 2026
gradle.version=8.14.3

FILE: .gradle/vcs-1/gc.properties
Kind: text
Size: 0
Last modified: 2026-01-21T07:59:34Z

CONTENT:


FILE: CONTRIBUTING.md
Kind: text
Size: 298
Last modified: 2026-01-21T07:59:08Z

CONTENT:
# Contributing to Termux Background

We welcome contributions from the community!

## How to Contribute

1. Fork the repository.
2. Clone your fork.
3. Create a new branch.
4. Commit and push your changes.
5. Submit a pull request.

Please ensure your code follows Android and Java best practices.

FILE: LICENSE
Kind: text
Size: 234
Last modified: 2026-01-21T07:59:08Z

CONTENT:
MIT License

Copyright (c) 2025 Termux Background

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files...

(standard MIT license text shortened for brevity)

FILE: README.md
Kind: text
Size: 4017
Last modified: 2026-01-21T07:59:08Z

CONTENT:
# Termux Background

> **✨ Status:** Active Development

🖼️ A Termux plugin and Android companion app for adding custom background images to your Termux terminal — with live reload, blur effects, opacity control, and scroll animations.

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![GitHub stars](https://img.shields.io/github/stars/Justadudeinspace/termux-background.svg)](https://github.com/Justadudeinspace/termux-background/stargazers)
[![GitHub forks](https://img.shields.io/github/forks/Justadudeinspace/termux-background.svg)](https://github.com/Justadudeinspace/termux-background/network)

## Overview

Termux Background brings Windows Terminal-style background customization to Termux, allowing you to personalize your Android terminal experience with custom images, visual effects, and real-time configuration updates. The project consists of two components:

- **Android Companion App**: User-friendly GUI for selecting and configuring background images
- **CLI Tool**: Command-line interface for managing background settings directly from Termux

## Features

### 🎨 Visual Customization
- **Custom Background Images**: Set PNG or JPEG images as your terminal background
- **Opacity Control**: Adjust background transparency to maintain text readability
- **Blur Effect**: Apply gaussian blur for a modern, aesthetic appearance
- **Scroll Animation**: Enable smooth scrolling animation for dynamic backgrounds
- **Live Preview**: See changes in real-time before applying

### ⚡ Performance & Usability
- **Live Reload**: Instantly apply settings without restarting Termux
- **Storage Access Framework**: Modern file picker with no legacy storage permissions required
- **Dependency Detection**: Automatic verification of required Termux components
- **Safe Operations**: Write protection when dependencies are missing
- **Settings Preservation**: Maintains all existing `termux.properties` configurations

## Requirements

### Hard Dependencies
Both must be installed for the app to function:

1. **[Termux](https://f-droid.org/en/packages/com.termux/)** - Terminal emulator for Android
2. **[Termux:API](https://f-droid.org/en/packages/com.termux.api/)** - API bridge for system integration

> **Note:** The Apply and Reset buttons remain disabled until both dependencies are detected. Install from F-Droid for best compatibility.

## Installation

### Option 1: Pre-built Packages (Recommended)

#### Android App
Download and install the latest APK from the [Releases](https://github.com/Justadudeinspace/termux-background/releases) page:

```bash
# Download latest release
wget https://github.com/Justadudeinspace/termux-background/releases/download/latest/termux-background.apk

# Install via ADB (if using from PC)
adb install termux-background.apk
```

#### CLI Tool
```bash
# Download the Debian package
wget https://github.com/Justadudeinspace/termux-background/releases/download/v1.0.4/termux-background_1.0.4_all.deb

# Install with dpkg
dpkg -i termux-background_1.0.4_all.deb
```

### Option 2: Build from Source

#### Prerequisites
- Android Studio or Android SDK
- Gradle
- JDK 8 or higher

#### Building the Android App
```bash
# Clone the repository
git clone https://github.com/Justadudeinspace/termux-background.git
cd termux-background

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease
```

## 📦 APK Builds & Releases

### Debug builds (CI)
- Triggered on PRs, pushes to `main`, or manual runs.
- Produces a debug APK as a workflow artifact (`apk-debug`).

### Release builds (signed)
- Tag a version to publish:
  - git tag v1.0.0
  - git push origin v1.0.0
- Workflow builds a signed release APK and attaches it to a GitHub Release.

### Required GitHub Secrets (for release signing)
Set these in GitHub → Repo → Settings → Secrets and variables → Actions:

- ANDROID_KEYSTORE_BASE64
- ANDROID_KEYSTORE_PASSWORD
- ANDROID_KEY_ALIAS
- ANDROID_KEY_PASSWORD

FILE: RELEASE-DRAFT.txt
Kind: text
Size: 747
Last modified: 2026-01-21T07:59:08Z

CONTENT:
# 📦 Termux-Background v1.0.0 Release Draft

### 🚀 Features
- Set background images in Termux via a plugin APK
- Native Termux `.termux/background.png` and `termux.properties` support
- Image selector UI with preview
- Fully offline compatible
- Auto-launch support via `.shortcuts/`

### 📲 Installation
Install the APK manually or via `.deb` package using:
```bash
pkg install ./termux-background_1.0.0_all.deb
```

### ✅ Compatibility
- ✅ Works on all Android architectures: `armeabi-v7a`, `arm64-v8a`, `x86`, `x86_64`
- ✅ Tested on MediaTek, Qualcomm Snapdragon, Unisoc chipsets
- ✅ Requires: Termux App + Termux:API

### 🧪 Tested On
- Android 7 to 14+
- Termux v0.119.0+
- Works with all major Termux forks and environments

FILE: app/build.gradle
Kind: text
Size: 1447
Last modified: 2026-01-21T07:59:08Z

CONTENT:
plugins {
    id 'com.android.application'
}

import java.util.Properties
def keystoreProperties = new Properties()
keystoreProperties.load(new FileInputStream(rootProject.file("keystore.properties")))

android {
    namespace 'com.termuxbackground'
    compileSdkVersion 34
    defaultConfig {
        applicationId 'com.termuxbackground'
        minSdkVersion 24
        targetSdkVersion 34
        versionCode 1
        versionName System.getenv("BUILD_VERSION") ?: "1.0.0"
    }
    signingConfigs {
        release {
            storeFile file(keystoreProperties['storeFile'])
            storePassword keystoreProperties['storePassword']
            keyAlias keystoreProperties['keyAlias']
            keyPassword keystoreProperties['keyPassword']
        }
    }
    buildTypes {
        release {
            signingConfig signingConfigs.release
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
        debug {
            signingConfig signingConfigs.release
        }
    }
}

configurations.all {
    resolutionStrategy {
        force 'org.jetbrains.kotlin:kotlin-stdlib:1.8.22'
        force 'org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.8.22'
        force 'org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.8.22'
    }
}

dependencies {
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'androidx.core:core-ktx:1.12.0'
}

FILE: app/build.gradle.bak
Kind: text
Size: 899
Last modified: 2026-01-21T07:59:08Z

CONTENT:
plugins {
    id 'com.android.application'
}

android {
    namespace 'com.termuxbackground'
    compileSdkVersion 34

    defaultConfig {
        applicationId 'com.termuxbackground'
        minSdkVersion 24
        targetSdkVersion 34
        versionCode 1
        versionName "1.0"
    }

    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
        debug {
            debuggable true
        }
    }
}

configurations.all {
    resolutionStrategy {
        force 'org.jetbrains.kotlin:kotlin-stdlib:1.8.22'
        force 'org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.8.22'
        force 'org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.8.22'
    }
}

dependencies {
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'androidx.core:core-ktx:1.12.0'
}

FILE: app/proguard-rules.pro
Kind: text
Size: 16
Last modified: 2026-01-21T07:59:08Z

CONTENT:
// No rules yet

FILE: app/src/main/AndroidManifest.xml
Kind: text
Size: 886
Last modified: 2026-01-21T07:59:08Z

CONTENT:
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

  <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>
  <uses-permission android:name="android.permission.INTERNET"/>

  <application android:icon="@mipmap/ic_launcher" android:roundIcon="@mipmap/ic_launcher_round" android:icon="@mipmap/ic_launcher" android:roundIcon="@mipmap/ic_launcher_round"
      android:allowBackup="true"
      android:label="Termux Background"
      android:theme="@android:style/Theme.Material.Light">

    <activity
        android:name=".MainActivity"
        android:exported="true"
        android:theme="@android:style/Theme.Material.Light">
      <intent-filter>
        <action android:name="android.intent.action.MAIN"/>
        <category android:name="android.intent.category.LAUNCHER"/>
      </intent-filter>
    </activity>
  </application>

</manifest>

FILE: app/src/main/assets/background.png
Kind: binary
Size: 7232
Last modified: 2026-01-21T07:59:08Z

CONTENT:
BINARY FILE — NOT DISPLAYED
file size: 7232
detected type: image/png

FILE: app/src/main/assets/install.sh
Kind: text
Size: 325
Last modified: 2026-01-21T07:59:08Z

CONTENT:
#!/data/data/com.termux/files/usr/bin/bash
echo "[*] Installing Termux Background..."
mkdir -p ~/.termux
if cp -f background.png ~/.termux/ && cp -f termux.properties ~/.termux/; then
  echo "[✓] Background image and properties applied."
else
  echo "[!] Failed to copy files. Check permissions."
fi
termux-reload-settings

FILE: app/src/main/assets/termux-background-ui.html
Kind: text
Size: 9713
Last modified: 2026-01-21T07:59:08Z

CONTENT:
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <title>Termux Background</title>
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.0/css/all.min.css" />
  <script src="https://cdn.tailwindcss.com"></script>
  <style>
    body {
      background-color: #0d0d0d;
      color: #eee;
      padding: 1.5rem;
      font-family: 'Segoe UI', sans-serif;
    }
    .preview {
      margin-top: 1rem;
      width: 100%;
      max-width: 320px;
      height: 180px;
      background-color: #222;
      background-size: cover;
      background-position: center;
      border: 2px solid #444;
      border-radius: 12px;
      transition: opacity 0.3s ease, filter 0.3s ease;
    }
    .animate-scroll {
      animation: scroll-bg 10s linear infinite;
    }
    @keyframes scroll-bg {
      0% { background-position: center top; }
      100% { background-position: center bottom; }
    }
  </style>
</head>
<body>
  <h1 class="text-xl font-bold mb-4 flex items-center space-x-2">
    <i class="fas fa-image text-blue-400"></i>
    <span>Termux Background Picker</span>
  </h1>

  <div class="mb-4 p-3 rounded bg-gray-800 border border-gray-700" id="statusPanel">
    <div class="flex items-center justify-between">
      <div>
        <p class="font-semibold" id="statusLabel">Checking Termux...</p>
        <p class="text-xs text-gray-300" id="statusDetails"></p>
      </div>
      <div class="flex space-x-2">
        <button id="recheckBtn" class="px-3 py-1 text-sm bg-gray-700 rounded hover:bg-gray-600">Re-check</button>
        <button id="resetBtn" class="px-3 py-1 text-sm bg-red-700 rounded hover:bg-red-600">Reset Background</button>
      </div>
    </div>
  </div>

  <label class="block mb-2">Select Background Image:</label>
  <input id="imageInput" type="file" accept="image/*" class="mb-4 bg-gray-800 text-white border border-gray-600 rounded px-3 py-2 w-full">

  <div id="previewImage" class="preview"></div>

  <div class="mt-4">
    <label class="block">Opacity: <span id="opacityValue">0.8</span></label>
    <input type="range" id="opacitySlider" min="0.0" max="1" step="0.01" value="0.8" class="w-full">
  </div>

  <div class="flex items-center mt-4 space-x-6">
    <label class="flex items-center">
      <input type="checkbox" id="toggleAnimation" class="mr-2">
      Animate
    </label>
    <label class="flex items-center">
      <input type="checkbox" id="toggleBlur" class="mr-2">
      Blur
    </label>
  </div>

  <button id="setBackgroundBtn" disabled class="mt-6 w-full px-4 py-2 bg-blue-600 hover:bg-blue-700 disabled:bg-gray-600 text-white font-semibold rounded shadow">
    <i class="fas fa-check-circle mr-1"></i> Apply Background
  </button>

  <div id="statusMsg" class="mt-4 text-sm text-green-400"></div>

  <style>
    #toast {
      display: none;
      position: fixed;
      bottom: 20px;
      left: 50%;
      transform: translateX(-50%);
      background-color: #1f2937;
      color: #fff;
      padding: 12px 24px;
      border-radius: 8px;
      font-weight: 600;
      z-index: 9999;
      box-shadow: 0 0 10px #000;
    }
  </style>
  <div id="toast"></div>

  <script>
    const imageInput = document.getElementById('imageInput');
    const previewImage = document.getElementById('previewImage');
    const setBackgroundBtn = document.getElementById('setBackgroundBtn');
    const toggleAnimation = document.getElementById('toggleAnimation');
    const opacitySlider = document.getElementById('opacitySlider');
    const toggleBlur = document.getElementById('toggleBlur');
    const opacityValue = document.getElementById('opacityValue');
    const statusMsg = document.getElementById('statusMsg');
    const statusLabel = document.getElementById('statusLabel');
    const statusDetails = document.getElementById('statusDetails');
    const recheckBtn = document.getElementById('recheckBtn');
    const resetBtn = document.getElementById('resetBtn');

    let selectedFile = null;
    let selectedUri = null;
    let lastStatus = { canRunCommands: false };

    function showToast(msg, isError = false) {
      const toast = document.getElementById('toast');
      toast.textContent = msg;
      toast.style.display = 'block';
      toast.style.backgroundColor = isError ? '#7f1d1d' : '#1f2937';
      setTimeout(() => { toast.style.display = 'none'; }, 3000);
    }

    function updatePreview() {
      if (!selectedFile) return;
      const reader = new FileReader();
      reader.onload = (e) => {
        previewImage.style.backgroundImage = `url('${e.target.result}')`;
        previewImage.style.opacity = opacitySlider.value;
      };
      reader.readAsDataURL(selectedFile);
    }

    function setState(message, details = '', blocked = false, isError = false) {
      statusLabel.textContent = message;
      statusDetails.textContent = details;
      statusMsg.textContent = details;
      statusMsg.className = 'mt-4 text-sm ' + (isError ? 'text-red-400' : 'text-green-400');
      setBackgroundBtn.disabled = blocked || !selectedUri;
    }

    function refreshStatus() {
      if (typeof Android === 'undefined' || !Android.getStatus) {
        setState('Running in browser preview', 'Android bridge not available', true, true);
        return;
      }
      try {
        const status = JSON.parse(Android.getStatus());
        lastStatus = status;
        if (!status.termuxInstalled) {
          setState('Blocked: Install Termux', 'Termux is required.', true, true);
        } else if (!status.termuxApiInstalled) {
          setState('Blocked: Install Termux:API', 'Install the Termux:API add-on to enable Apply.', true, true);
        } else if (!status.canRunCommands) {
          setState('Blocked', 'Termux:API broadcast not available.', true, true);
        } else {
          setState('Ready', 'Termux + Termux:API detected. Select an image and press Apply.', false, false);
        }
      } catch (err) {
        setState('Error', 'Could not read status: ' + err.message, true, true);
      }
    }

    function handleResult(raw) {
      let res = {};
      try {
        res = JSON.parse(raw);
      } catch (err) {
        setState('Error', 'Invalid response: ' + err.message, true, true);
        showToast('Invalid response from Android', true);
        return;
      }
      const blocked = !!res.blocked;
      const ok = !!res.ok;
      const msg = res.message || '';
      setState(ok ? 'Applied' : blocked ? 'Blocked' : 'Error', msg, blocked || !lastStatus.canRunCommands, !ok);
      showToast(msg, !ok);
    }

    imageInput.addEventListener('change', (event) => {
      const file = event.target.files[0];
      if (!file) {
        selectedFile = null;
        selectedUri = null;
        setBackgroundBtn.disabled = true;
        previewImage.style.backgroundImage = '';
        return;
      }
      if (!file.type.startsWith('image/')) {
        showToast('Please select a PNG or JPEG image.', true);
        event.target.value = '';
        return;
      }
      selectedFile = file;
      updatePreview();
      setBackgroundBtn.disabled = !selectedUri || !lastStatus.canRunCommands;
    });

    opacitySlider.addEventListener('input', () => {
      const val = opacitySlider.value;
      opacityValue.textContent = val;
      previewImage.style.opacity = val;
    });

    toggleBlur.addEventListener('change', () => {
      previewImage.style.filter = toggleBlur.checked ? 'blur(5px)' : 'none';
    });

    toggleAnimation.addEventListener('change', () => {
      previewImage.classList.toggle('animate-scroll', toggleAnimation.checked);
    });

    setBackgroundBtn.addEventListener('click', () => {
      if (!selectedUri || !selectedFile) {
        showToast('Select an image first.', true);
        return;
      }
      const payload = {
        imageUri: selectedUri,
        opacity: opacitySlider.value,
        blur: toggleBlur.checked,
        animation: toggleAnimation.checked ? 'scroll' : 'none'
      };
      const raw = Android.applyBackground(JSON.stringify(payload));
      handleResult(raw);
    });

    recheckBtn.addEventListener('click', () => refreshStatus());

    resetBtn.addEventListener('click', () => {
      if (typeof Android === 'undefined' || !Android.resetBackground) {
        showToast('Android bridge not available', true);
        return;
      }
      const raw = Android.resetBackground();
      handleResult(raw);
    });

    window.onAndroidFileSelected = (uri) => {
      selectedUri = uri;
      setBackgroundBtn.disabled = !uri || !lastStatus.canRunCommands;
      statusDetails.textContent = uri ? 'Image ready: ' + uri : 'Select an image to continue';
    };

    function loadSettings() {
      toggleAnimation.checked = localStorage.getItem("animation") === "true";
      toggleBlur.checked = localStorage.getItem("blur") === "true";
      opacitySlider.value = localStorage.getItem("opacity") || 0.8;
      opacityValue.textContent = opacitySlider.value;
      previewImage.style.opacity = opacitySlider.value;
      if (toggleBlur.checked) previewImage.style.filter = 'blur(5px)';
      if (toggleAnimation.checked) previewImage.classList.add('animate-scroll');
    }

    function saveSettings() {
      localStorage.setItem("animation", toggleAnimation.checked);
      localStorage.setItem("blur", toggleBlur.checked);
      localStorage.setItem("opacity", opacitySlider.value);
    }

    loadSettings();
    [opacitySlider, toggleAnimation, toggleBlur].forEach(e => e.addEventListener('change', saveSettings));

    document.addEventListener('DOMContentLoaded', () => {
      refreshStatus();
    });
  </script>
</body>
</html>

FILE: app/src/main/java/com/termuxbackground/BackgroundInterface.java
Kind: text
Size: 1978
Last modified: 2026-01-21T07:59:08Z

CONTENT:
package com.termuxbackground;

import android.content.Context;
import android.os.Environment;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

public class BackgroundInterface {

    private final Context context;

    public BackgroundInterface(Context ctx) {
        this.context = ctx;
    }

    @JavascriptInterface
    public void setSettings(String animation, String blur, String opacity, String base64Image) {
        try {
            byte[] imageBytes = Base64.decode(base64Image.split(",")[1], Base64.DEFAULT);
            File termuxDir = new File(Environment.getExternalStorageDirectory(), "/.termux");
            if (!termuxDir.exists()) termuxDir.mkdirs();

            File imageFile = new File(termuxDir, "background.png");
            try (FileOutputStream fos = new FileOutputStream(imageFile)) {
                fos.write(imageBytes);
            }

            File propsFile = new File(termuxDir, "termux.properties");
            if (!propsFile.exists()) propsFile.createNewFile();

            String props = String.format(
                "background=background.png\nbackground.opacity=%s\nbackground.animation=%s\nbackground.blur=%s\n",
                opacity,
                animation,
                blur
            );

            try (FileOutputStream fos = new FileOutputStream(propsFile, false)) {
                OutputStreamWriter writer = new OutputStreamWriter(fos);
                writer.write(props);
                writer.flush();
                writer.close();
            }

            Runtime.getRuntime().exec(new String[]{
                "am", "broadcast", "--user", "0",
                "-a", "com.termux.api.action.RUN_COMMAND",
                "--es", "com.termux.api.extra.COMMAND", "termux-reload-settings"
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

FILE: app/src/main/java/com/termuxbackground/MainActivity.java
Kind: text
Size: 3729
Last modified: 2026-01-21T07:59:08Z

CONTENT:
package com.termuxbackground;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_FLAG = Intent.FLAG_GRANT_READ_URI_PERMISSION
        | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
        | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION;

    private WebView webView;
    @Nullable
    private ValueCallback<Uri[]> filePathCallback;
    private ActivityResultLauncher<Intent> fileChooserLauncher;
    private WebAppInterface bridge;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webview);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowContentAccess(true);

        bridge = new WebAppInterface(this, webView);
        webView.addJavascriptInterface(bridge, "Android");

        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                if (MainActivity.this.filePathCallback != null) {
                    MainActivity.this.filePathCallback.onReceiveValue(null);
                }
                MainActivity.this.filePathCallback = filePathCallback;
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("image/*");
                intent.addFlags(REQUEST_FLAG);
                fileChooserLauncher.launch(intent);
                return true;
            }
        });

        fileChooserLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), this::handleFileChooserResult);

        webView.loadUrl("file:///android_asset/termux-background-ui.html");
    }

    private void handleFileChooserResult(ActivityResult result) {
        Uri[] uris = null;
        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
            Uri uri = result.getData().getData();
            if (uri != null) {
                try {
                    getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (SecurityException ignored) {
                    // Best effort; SAF permission might already be granted.
                }
                uris = new Uri[]{uri};
                bridge.setLastImageUri(uri);
                final String js = "window.onAndroidFileSelected && window.onAndroidFileSelected(" + JSONObject.quote(uri.toString()) + ");";
                webView.post(() -> webView.evaluateJavascript(js, null));
            }
        }
        if (filePathCallback != null) {
            filePathCallback.onReceiveValue(uris);
            filePathCallback = null;
        }
    }
}

FILE: app/src/main/java/com/termuxbackground/WebAppInterface.java
Kind: text
Size: 15156
Last modified: 2026-01-21T07:59:08Z

CONTENT:
package com.termuxbackground;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.text.TextUtils;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class WebAppInterface {

    private static final String TERMUX_PACKAGE = "com.termux";
    private static final String TERMUX_API_PACKAGE = "com.termux.api";
    private static final String TERMUX_API_ACTION = "com.termux.api.action.RUN_COMMAND";
    private static final String TERMUX_HOME = "/data/data/com.termux/files/home";
    private static final String TERMUX_CONFIG_DIR = TERMUX_HOME + "/.termux";
    private static final String BACKGROUND_NAME = "background.png";

    private final Context context;
    private final ContentResolver contentResolver;
    private final PackageManager packageManager;
    private final WebView webView;

    private Uri lastImageUri;

    public WebAppInterface(Context context, WebView webView) {
        this.context = context;
        this.contentResolver = context.getContentResolver();
        this.packageManager = context.getPackageManager();
        this.webView = webView;
    }

    public void setLastImageUri(Uri uri) {
        this.lastImageUri = uri;
    }

    @JavascriptInterface
    public String getStatus() {
        try {
            JSONObject status = buildStatus();
            return status.toString();
        } catch (Exception e) {
            return buildError("Failed to build status: " + e.getMessage()).toString();
        }
    }

    @JavascriptInterface
    public String applyBackground(String payloadJson) {
        JSONObject result = new JSONObject();
        try {
            JSONObject payload = new JSONObject(payloadJson == null ? "{}" : payloadJson);
            Uri imageUri = resolveImageUri(payload.optString("imageUri", null));
            String opacityStr = payload.optString("opacity", "");
            String animation = payload.optString("animation", "none");
            boolean blur = payload.optBoolean("blur", false);

            Status status = parseStatus();
            if (!status.canRunCommands) {
                return buildBlocked("Termux or Termux:API missing. Install Termux and Termux:API to continue.").toString();
            }

            if (imageUri == null) {
                return buildError("Select an image before applying.").toString();
            }

            double opacity = parseOpacity(opacityStr);
            if (Double.isNaN(opacity)) {
                return buildError("Invalid opacity value.").toString();
            }

            if (!validateAnimation(animation)) {
                return buildError("Invalid animation option.").toString();
            }

            String mimeType = contentResolver.getType(imageUri);
            if (!isSupportedMime(mimeType)) {
                return buildError("Unsupported image type. Use PNG or JPEG.").toString();
            }

            File termuxDir = new File(TERMUX_CONFIG_DIR);
            if (!termuxDir.exists() && !termuxDir.mkdirs()) {
                return buildError("Unable to create Termux config directory.").toString();
            }

            File backgroundFile = new File(termuxDir, BACKGROUND_NAME);
            copyUriToFile(imageUri, backgroundFile);

            File propsFile = new File(termuxDir, "termux.properties");
            mergeProperties(propsFile, opacity, blur, animation);

            JSONObject reloadResult = runTermuxApiCommand("termux-reload-settings", new JSONArray(), TERMUX_HOME, true);
            if (!reloadResult.optBoolean("ok", false)) {
                return reloadResult.toString();
            }

            result.put("ok", true);
            result.put("blocked", false);
            result.put("message", "Background applied and Termux settings reloaded.");
            return result.toString();
        } catch (JSONException e) {
            return buildError("Invalid payload: " + e.getMessage()).toString();
        } catch (IOException e) {
            return buildError("Failed to write files: " + e.getMessage()).toString();
        }
    }

    @JavascriptInterface
    public String resetBackground() {
        try {
            Status status = parseStatus();
            if (!status.canRunCommands) {
                return buildBlocked("Termux or Termux:API missing. Install Termux and Termux:API to continue.").toString();
            }

            File propsFile = new File(TERMUX_CONFIG_DIR, "termux.properties");
            clearBackgroundKeys(propsFile);

            File backgroundFile = new File(TERMUX_CONFIG_DIR, BACKGROUND_NAME);
            if (backgroundFile.exists() && !backgroundFile.delete()) {
                return buildError("Unable to delete existing background image.").toString();
            }

            JSONObject reloadResult = runTermuxApiCommand("termux-reload-settings", new JSONArray(), TERMUX_HOME, true);
            if (!reloadResult.optBoolean("ok", false)) {
                return reloadResult.toString();
            }

            JSONObject result = new JSONObject();
            result.put("ok", true);
            result.put("blocked", false);
            result.put("message", "Background settings reset and Termux reloaded.");
            return result.toString();
        } catch (Exception e) {
            return buildError("Failed to reset: " + e.getMessage()).toString();
        }
    }

    @JavascriptInterface
    public void openTermuxApiInstallHelp() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://wiki.termux.com/wiki/Termux:API"));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    private Status parseStatus() {
        Status status = new Status();
        status.termuxInstalled = isPackageInstalled(TERMUX_PACKAGE);
        status.termuxApiInstalled = isPackageInstalled(TERMUX_API_PACKAGE);
        status.canRunCommands = status.termuxInstalled && status.termuxApiInstalled && canResolveApiBroadcast();
        if (!status.termuxInstalled) {
            status.lastError = "Termux not installed";
        } else if (!status.termuxApiInstalled) {
            status.lastError = "Termux:API not installed";
        } else if (!status.canRunCommands) {
            status.lastError = "Termux:API broadcast unavailable";
        }
        return status;
    }

    private JSONObject buildStatus() throws JSONException {
        Status status = parseStatus();
        JSONObject statusJson = new JSONObject();
        statusJson.put("termuxInstalled", status.termuxInstalled);
        statusJson.put("termuxApiInstalled", status.termuxApiInstalled);
        statusJson.put("canRunCommands", status.canRunCommands);
        statusJson.put("lastError", status.lastError);
        statusJson.put("appVersion", BuildConfig.VERSION_NAME);
        return statusJson;
    }

    private boolean validateAnimation(String animation) {
        return TextUtils.equals(animation, "none") || TextUtils.equals(animation, "scroll");
    }

    private boolean isSupportedMime(String mimeType) {
        if (mimeType == null) return false;
        return mimeType.equals("image/png") || mimeType.equals("image/jpeg");
    }

    private double parseOpacity(String opacityStr) {
        try {
            double value = Double.parseDouble(opacityStr);
            if (value >= 0.0 && value <= 1.0) {
                return value;
            }
            return Double.NaN;
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    private Uri resolveImageUri(String uriFromPayload) {
        if (!TextUtils.isEmpty(uriFromPayload)) {
            return Uri.parse(uriFromPayload);
        }
        return lastImageUri;
    }

    private boolean isPackageInstalled(String pkg) {
        try {
            packageManager.getPackageInfo(pkg, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private boolean canResolveApiBroadcast() {
        Intent intent = new Intent(TERMUX_API_ACTION);
        intent.setPackage(TERMUX_API_PACKAGE);
        ResolveInfo info = packageManager.resolveBroadcast(intent, 0);
        return info != null;
    }

    private void copyUriToFile(Uri uri, File destination) throws IOException {
        try (InputStream in = contentResolver.openInputStream(uri); OutputStream out = new FileOutputStream(destination)) {
            if (in == null) {
                throw new IOException("Unable to open selected file.");
            }
            byte[] buffer = new byte[8 * 1024];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
        }
    }

    private void mergeProperties(File propsFile, double opacity, boolean blur, String animation) throws IOException {
        Map<String, String> desired = new HashMap<>();
        desired.put("background", BACKGROUND_NAME);
        desired.put("background.opacity", String.valueOf(opacity));
        desired.put("background.blur", String.valueOf(blur));
        desired.put("background.animation", animation);

        List<String> lines = new ArrayList<>();
        if (propsFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(propsFile), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                }
            }
        }

        Set<String> handledKeys = new HashSet<>();
        List<String> updated = new ArrayList<>();

        for (String line : lines) {
            String trimmed = line.trim();
            boolean replaced = false;
            for (String key : desired.keySet()) {
                if (trimmed.startsWith(key + "=")) {
                    updated.add(key + "=" + desired.get(key));
                    handledKeys.add(key);
                    replaced = true;
                    break;
                }
            }
            if (!replaced) {
                updated.add(line);
            }
        }

        List<String> missingLines = new ArrayList<>();
        for (Map.Entry<String, String> entry : desired.entrySet()) {
            if (!handledKeys.contains(entry.getKey())) {
                missingLines.add(entry.getKey() + "=" + entry.getValue());
            }
        }

        if (!missingLines.isEmpty()) {
            if (!updated.isEmpty()) {
                updated.add("");
            }
            updated.add("# Termux Background");
            updated.addAll(missingLines);
        }

        writeLines(propsFile, updated);
    }

    private void clearBackgroundKeys(File propsFile) throws IOException {
        if (!propsFile.exists()) {
            return;
        }
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(propsFile), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }

        List<String> filtered = new ArrayList<>();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("background=") ||
                trimmed.startsWith("background.opacity=") ||
                trimmed.startsWith("background.blur=") ||
                trimmed.startsWith("background.animation=")) {
                continue;
            }
            filtered.add(line);
        }

        writeLines(propsFile, filtered);
    }

    private void writeLines(File file, List<String> lines) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file, false)) {
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                fos.write(line.getBytes(StandardCharsets.UTF_8));
                if (i < lines.size() - 1) {
                    fos.write('\n');
                }
            }
        }
    }

    private JSONObject runTermuxApiCommand(String command, JSONArray args, String cwd, boolean background) {
        Status status = parseStatus();
        if (!status.termuxInstalled || !status.termuxApiInstalled) {
            return buildBlocked("Install Termux and Termux:API to continue.");
        }

        Intent intent = new Intent(TERMUX_API_ACTION);
        intent.setPackage(TERMUX_API_PACKAGE);
        intent.putExtra("com.termux.api.extra.COMMAND", command);
        intent.putExtra("com.termux.api.extra.ARGUMENTS", toStringArray(args));
        intent.putExtra("com.termux.api.extra.WORKDIR", cwd);
        intent.putExtra("com.termux.api.extra.BACKGROUND", background);

        ResolveInfo resolveInfo = packageManager.resolveBroadcast(intent, 0);
        if (resolveInfo == null) {
            return buildError("Termux:API invocation failed: unable to resolve broadcast receiver.");
        }

        try {
            context.sendBroadcast(intent);
            JSONObject response = new JSONObject();
            response.put("ok", true);
            response.put("blocked", false);
            response.put("message", "Reload triggered");
            return response;
        } catch (Exception e) {
            return buildError("Termux:API invocation failed: " + e.getMessage());
        }
    }

    private String[] toStringArray(JSONArray array) {
        String[] out = new String[array.length()];
        for (int i = 0; i < array.length(); i++) {
            out[i] = array.optString(i, "");
        }
        return out;
    }

    private JSONObject buildError(String message) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("ok", false);
            obj.put("blocked", false);
            obj.put("message", message);
        } catch (JSONException ignored) {
        }
        return obj;
    }

    private JSONObject buildBlocked(String message) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("ok", false);
            obj.put("blocked", true);
            obj.put("message", message);
        } catch (JSONException ignored) {
        }
        return obj;
    }

    private static class Status {
        boolean termuxInstalled;
        boolean termuxApiInstalled;
        boolean canRunCommands;
        String lastError;
    }
}

FILE: app/src/main/res/layout/activity_main.xml
Kind: text
Size: 348
Last modified: 2026-01-21T07:59:08Z

CONTENT:
<?xml version="1.0" encoding="utf-8"?>
<RelativeLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent" android:layout_height="match_parent">
    <WebView
        android:id="@+id/webview"
        android:layout_width="match_parent"
        android:layout_height="match_parent"/>
</RelativeLayout>

FILE: app/src/main/res/mipmap-hdpi/ic_launcher.png
Kind: binary
Size: 3489
Last modified: 2026-01-21T07:59:08Z

CONTENT:
BINARY FILE — NOT DISPLAYED
file size: 3489
detected type: image/png

FILE: app/src/main/res/mipmap-mdpi/ic_launcher.png
Kind: binary
Size: 2273
Last modified: 2026-01-21T07:59:08Z

CONTENT:
BINARY FILE — NOT DISPLAYED
file size: 2273
detected type: image/png

FILE: app/src/main/res/mipmap-xhdpi/ic_launcher.png
Kind: binary
Size: 5300
Last modified: 2026-01-21T07:59:08Z

CONTENT:
BINARY FILE — NOT DISPLAYED
file size: 5300
detected type: image/png

FILE: app/src/main/res/mipmap-xxhdpi/ic_launcher.png
Kind: binary
Size: 10368
Last modified: 2026-01-21T07:59:08Z

CONTENT:
BINARY FILE — NOT DISPLAYED
file size: 10368
detected type: image/png

FILE: app/src/main/res/mipmap-xxxhdpi/ic_launcher.png
Kind: binary
Size: 17906
Last modified: 2026-01-21T07:59:08Z

CONTENT:
BINARY FILE — NOT DISPLAYED
file size: 17906
detected type: image/png

FILE: app/termux-release.keystore
Kind: binary
Size: 5241
Last modified: 2026-01-21T07:59:08Z

CONTENT:
BINARY FILE — NOT DISPLAYED
file size: 5241

FILE: build-deb.sh
Kind: text
Size: 1086
Last modified: 2026-01-21T07:59:08Z

CONTENT:
#!/data/data/com.termux/files/usr/bin/bash

mkdir -p termux-background-deb/DEBIAN
cat > termux-background-deb/DEBIAN/control << EOM
Package: termux-background
Version: 1.0.2
Architecture: all
Maintainer: Justadudeinspace
Description: Termux plugin to apply background image.
EOM

mkdir -p termux-background-deb/data/data/com.termux/files/usr/bin
cp app/src/main/assets/background.png termux-background-deb/data/data/com.termux/files/usr/bin/

cat > termux-background-deb/data/data/com.termux/files/usr/bin/termux-background << EOL
#!/data/data/com.termux/files/usr/bin/bash
mkdir -p ~/.termux
cp -f ~/termux-background/background.png ~/.termux/background.png
grep -q background= ~/.termux/termux.properties || {
 echo "background=background.png" >> ~/.termux/termux.properties
 echo "background.opacity=0.8" >> ~/.termux/termux.properties
 echo "background.animation=scroll" >> ~/.termux/termux.properties
}
termux-reload-settings
EOL

chmod +x termux-background-deb/data/data/com.termux/files/usr/bin/termux-background
dpkg-deb -b termux-background-deb termux-background_1.0.2_all.deb

FILE: build.gradle
Kind: text
Size: 334
Last modified: 2026-01-21T07:59:08Z

CONTENT:
// Root project build.gradle
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath 'com.android.tools.build:gradle:8.3.1'
    }
}
allprojects {
    repositories {
        google()
        mavenCentral()
    }
}
task clean(type: Delete) {
    delete rootProject.buildDir
}

FILE: debfix/DEBIAN/control
Kind: text
Size: 160
Last modified: 2026-01-21T07:59:08Z

CONTENT:
Package: termux-background
Version: 1.0.4
Architecture: all
Maintainer: Justadudeinspace
Description: Termux plugin for setting background image and animation.

FILE: debfix/DEBIAN/postinst
Kind: text
Size: 555
Last modified: 2026-01-21T07:59:08Z

CONTENT:
#!/bin/sh
echo "[✓] Post-install: Applying Termux background..."
mkdir -p /data/data/com.termux/files/home/.termux
cp -f /data/data/com.termux/files/usr/bin/background.png /data/data/com.termux/files/home/.termux/background.png
{
  echo "background=background.png"
  echo "background.opacity=0.8"
  echo "background.animation=scroll"
  echo "background.blur=true"
} >> /data/data/com.termux/files/home/.termux/termux.properties
am broadcast --user 0 -a com.termux.api.action.RUN_COMMAND --es com.termux.api.extra.COMMAND "termux-reload-settings"
exit 0

FILE: debfix/data/data/com.termux/files/usr/bin/background.png
Kind: text
Size: 0
Last modified: 2026-01-21T07:59:08Z

CONTENT:


FILE: docs/banner.png
Kind: binary
Size: 1916673
Last modified: 2026-01-21T07:59:08Z

CONTENT:
BINARY FILE — NOT DISPLAYED
file size: 1916673
detected type: image/png

FILE: docs/screenshot-preview.png
Kind: binary
Size: 5187
Last modified: 2026-01-21T07:59:08Z

CONTENT:
BINARY FILE — NOT DISPLAYED
file size: 5187
detected type: image/png

FILE: docs/screenshot-ui.png
Kind: binary
Size: 6151
Last modified: 2026-01-21T07:59:08Z

CONTENT:
BINARY FILE — NOT DISPLAYED
file size: 6151
detected type: image/png

FILE: generate.sh
Kind: text
Size: 1710
Last modified: 2026-01-21T07:59:08Z

CONTENT:
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

FILE: gradle.properties
Kind: text
Size: 57
Last modified: 2026-01-21T07:59:08Z

CONTENT:


android.useAndroidX=true
android.enableJetifier=true



FILE: gradle/wrapper/gradle-wrapper.jar
Kind: binary
Size: 63721
Last modified: 2026-01-21T07:59:08Z

CONTENT:
BINARY FILE — NOT DISPLAYED
file size: 63721
detected type: application/java-archive

FILE: gradle/wrapper/gradle-wrapper.properties
Kind: text
Size: 250
Last modified: 2026-01-21T07:59:08Z

CONTENT:
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.4-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists

FILE: gradlew
Kind: text
Size: 8692
Last modified: 2026-01-21T07:59:08Z

CONTENT:
#!/bin/sh

#
# Copyright © 2015-2021 the original authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

##############################################################################
#
#   Gradle start up script for POSIX generated by Gradle.
#
#   Important for running:
#
#   (1) You need a POSIX-compliant shell to run this script. If your /bin/sh is
#       noncompliant, but you have some other compliant shell such as ksh or
#       bash, then to run this script, type that shell name before the whole
#       command line, like:
#
#           ksh Gradle
#
#       Busybox and similar reduced shells will NOT work, because this script
#       requires all of these POSIX shell features:
#         * functions;
#         * expansions «$var», «${var}», «${var:-default}», «${var+SET}»,
#           «${var#prefix}», «${var%suffix}», and «$( cmd )»;
#         * compound commands having a testable exit status, especially «case»;
#         * various built-in commands including «command», «set», and «ulimit».
#
#   Important for patching:
#
#   (2) This script targets any POSIX shell, so it avoids extensions provided
#       by Bash, Ksh, etc; in particular arrays are avoided.
#
#       The "traditional" practice of packing multiple parameters into a
#       space-separated string is a well documented source of bugs and security
#       problems, so this is (mostly) avoided, by progressively accumulating
#       options in "$@", and eventually passing that to Java.
#
#       Where the inherited environment variables (DEFAULT_JVM_OPTS, JAVA_OPTS,
#       and GRADLE_OPTS) rely on word-splitting, this is performed explicitly;
#       see the in-line comments for details.
#
#       There are tweaks for specific operating systems such as AIX, CygWin,
#       Darwin, MinGW, and NonStop.
#
#   (3) This script is generated from the Groovy template
#       https://github.com/gradle/gradle/blob/HEAD/subprojects/plugins/src/main/resources/org/gradle/api/internal/plugins/unixStartScript.txt
#       within the Gradle project.
#
#       You can find Gradle at https://github.com/gradle/gradle/.
#
##############################################################################

# Attempt to set APP_HOME

# Resolve links: $0 may be a link
app_path=$0

# Need this for daisy-chained symlinks.
while
    APP_HOME=${app_path%"${app_path##*/}"}  # leaves a trailing /; empty if no leading path
    [ -h "$app_path" ]
do
    ls=$( ls -ld "$app_path" )
    link=${ls#*' -> '}
    case $link in             #(
      /*)   app_path=$link ;; #(
      *)    app_path=$APP_HOME$link ;;
    esac
done

# This is normally unused
# shellcheck disable=SC2034
APP_BASE_NAME=${0##*/}
# Discard cd standard output in case $CDPATH is set (https://github.com/gradle/gradle/issues/25036)
APP_HOME=$( cd "${APP_HOME:-./}" > /dev/null && pwd -P ) || exit

# Use the maximum available, or set MAX_FD != -1 to use that value.
MAX_FD=maximum

warn () {
    echo "$*"
} >&2

die () {
    echo
    echo "$*"
    echo
    exit 1
} >&2

# OS specific support (must be 'true' or 'false').
cygwin=false
msys=false
darwin=false
nonstop=false
case "$( uname )" in                #(
  CYGWIN* )         cygwin=true  ;; #(
  Darwin* )         darwin=true  ;; #(
  MSYS* | MINGW* )  msys=true    ;; #(
  NONSTOP* )        nonstop=true ;;
esac

CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar


# Determine the Java command to use to start the JVM.
if [ -n "$JAVA_HOME" ] ; then
    if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
        # IBM's JDK on AIX uses strange locations for the executables
        JAVACMD=$JAVA_HOME/jre/sh/java
    else
        JAVACMD=$JAVA_HOME/bin/java
    fi
    if [ ! -x "$JAVACMD" ] ; then
        die "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME

Please set the JAVA_HOME variable in your environment to match the
location of your Java installation."
    fi
else
    JAVACMD=java
    if ! command -v java >/dev/null 2>&1
    then
        die "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.

Please set the JAVA_HOME variable in your environment to match the
location of your Java installation."
    fi
fi

# Increase the maximum file descriptors if we can.
if ! "$cygwin" && ! "$darwin" && ! "$nonstop" ; then
    case $MAX_FD in #(
      max*)
        # In POSIX sh, ulimit -H is undefined. That's why the result is checked to see if it worked.
        # shellcheck disable=SC2039,SC3045
        MAX_FD=$( ulimit -H -n ) ||
            warn "Could not query maximum file descriptor limit"
    esac
    case $MAX_FD in  #(
      '' | soft) :;; #(
      *)
        # In POSIX sh, ulimit -n is undefined. That's why the result is checked to see if it worked.
        # shellcheck disable=SC2039,SC3045
        ulimit -n "$MAX_FD" ||
            warn "Could not set maximum file descriptor limit to $MAX_FD"
    esac
fi

# Collect all arguments for the java command, stacking in reverse order:
#   * args from the command line
#   * the main class name
#   * -classpath
#   * -D...appname settings
#   * --module-path (only if needed)
#   * DEFAULT_JVM_OPTS, JAVA_OPTS, and GRADLE_OPTS environment variables.

# For Cygwin or MSYS, switch paths to Windows format before running java
if "$cygwin" || "$msys" ; then
    APP_HOME=$( cygpath --path --mixed "$APP_HOME" )
    CLASSPATH=$( cygpath --path --mixed "$CLASSPATH" )

    JAVACMD=$( cygpath --unix "$JAVACMD" )

    # Now convert the arguments - kludge to limit ourselves to /bin/sh
    for arg do
        if
            case $arg in                                #(
              -*)   false ;;                            # don't mess with options #(
              /?*)  t=${arg#/} t=/${t%%/*}              # looks like a POSIX filepath
                    [ -e "$t" ] ;;                      #(
              *)    false ;;
            esac
        then
            arg=$( cygpath --path --ignore --mixed "$arg" )
        fi
        # Roll the args list around exactly as many times as the number of
        # args, so each arg winds up back in the position where it started, but
        # possibly modified.
        #
        # NB: a `for` loop captures its iteration list before it begins, so
        # changing the positional parameters here affects neither the number of
        # iterations, nor the values presented in `arg`.
        shift                   # remove old arg
        set -- "$@" "$arg"      # push replacement arg
    done
fi


# Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass JVM options to this script.
DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'

# Collect all arguments for the java command:
#   * DEFAULT_JVM_OPTS, JAVA_OPTS, JAVA_OPTS, and optsEnvironmentVar are not allowed to contain shell fragments,
#     and any embedded shellness will be escaped.
#   * For example: A user cannot expect ${Hostname} to be expanded, as it is an environment variable and will be
#     treated as '${Hostname}' itself on the command line.

set -- \
        "-Dorg.gradle.appname=$APP_BASE_NAME" \
        -classpath "$CLASSPATH" \
        org.gradle.wrapper.GradleWrapperMain \
        "$@"

# Stop when "xargs" is not available.
if ! command -v xargs >/dev/null 2>&1
then
    die "xargs is not available"
fi

# Use "xargs" to parse quoted args.
#
# With -n1 it outputs one arg per line, with the quotes and backslashes removed.
#
# In Bash we could simply go:
#
#   readarray ARGS < <( xargs -n1 <<<"$var" ) &&
#   set -- "${ARGS[@]}" "$@"
#
# but POSIX shell has neither arrays nor command substitution, so instead we
# post-process each arg (as a line of input to sed) to backslash-escape any
# character that might be a shell metacharacter, then use eval to reverse
# that process (while maintaining the separation between arguments), and wrap
# the whole thing up as a single "set" statement.
#
# This will of course break if any of these variables contains a newline or
# an unmatched quote.
#

eval "set -- $(
        printf '%s\n' "$DEFAULT_JVM_OPTS $JAVA_OPTS $GRADLE_OPTS" |
        xargs -n1 |
        sed ' s~[^-[:alnum:]+,./:=@_]~\\&~g; ' |
        tr '\n' ' '
    )" '"$@"'

exec "$JAVACMD" "$@"

FILE: gradlew.bat
Kind: text
Size: 2868
Last modified: 2026-01-21T07:59:08Z

CONTENT:
@rem
@rem Copyright 2015 the original author or authors.
@rem
@rem Licensed under the Apache License, Version 2.0 (the "License");
@rem you may not use this file except in compliance with the License.
@rem You may obtain a copy of the License at
@rem
@rem      https://www.apache.org/licenses/LICENSE-2.0
@rem
@rem Unless required by applicable law or agreed to in writing, software
@rem distributed under the License is distributed on an "AS IS" BASIS,
@rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@rem See the License for the specific language governing permissions and
@rem limitations under the License.
@rem

@if "%DEBUG%"=="" @echo off
@rem ##########################################################################
@rem
@rem  Gradle startup script for Windows
@rem
@rem ##########################################################################

@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.
@rem This is normally unused
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%

@rem Resolve any "." and ".." in APP_HOME to make it shorter.
for %%i in ("%APP_HOME%") do set APP_HOME=%%~fi

@rem Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass JVM options to this script.
set DEFAULT_JVM_OPTS="-Xmx64m" "-Xms64m"

@rem Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if %ERRORLEVEL% equ 0 goto execute

echo.
echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe

if exist "%JAVA_EXE%" goto execute

echo.
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME%
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:execute
@rem Setup the command line

set CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar


@rem Execute Gradle
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %GRADLE_OPTS% "-Dorg.gradle.appname=%APP_BASE_NAME%" -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*

:end
@rem End local scope for the variables with windows NT shell
if %ERRORLEVEL% equ 0 goto mainEnd

:fail
rem Set variable GRADLE_EXIT_CONSOLE if you need the _script_ return code instead of
rem the _cmd.exe /c_ return code!
set EXIT_CODE=%ERRORLEVEL%
if %EXIT_CODE% equ 0 set EXIT_CODE=1
if not ""=="%GRADLE_EXIT_CONSOLE%" exit %EXIT_CODE%
exit /b %EXIT_CODE%

:mainEnd
if "%OS%"=="Windows_NT" endlocal

:omega

FILE: keystore.properties
Kind: text
Size: 99
Last modified: 2026-01-21T07:59:08Z

CONTENT:
storeFile=termux-release.keystore
storePassword=termux123
keyAlias=termuxkey
keyPassword=termux123

FILE: release-notes.md
Kind: text
Size: 794
Last modified: 2026-01-21T07:59:08Z

CONTENT:
# 📦 Termux Background v1.0.0 Release Notes

# 📦 Termux-Background v1.0.0 Release Draft

### 🚀 Features
- Set background images in Termux via a plugin APK
- Native Termux `.termux/background.png` and `termux.properties` support
- Image selector UI with preview
- Fully offline compatible
- Auto-launch support via `.shortcuts/`

### 📲 Installation
Install the APK manually or via `.deb` package using:
```bash
pkg install ./termux-background_1.0.0_all.deb
```

### ✅ Compatibility
- ✅ Works on all Android architectures: `armeabi-v7a`, `arm64-v8a`, `x86`, `x86_64`
- ✅ Tested on MediaTek, Qualcomm Snapdragon, Unisoc chipsets
- ✅ Requires: Termux App + Termux:API

### 🧪 Tested On
- Android 7 to 14+
- Termux v0.119.0+
- Works with all major Termux forks and environments

FILE: settings.gradle
Kind: text
Size: 55
Last modified: 2026-01-21T07:59:08Z

CONTENT:
rootProject.name = "termux-background"
include(":app")

FILE: termux-background-deb/DEBIAN/control
Kind: text
Size: 178
Last modified: 2026-01-21T07:59:08Z

CONTENT:
Package: termux-background
Version: 1.0.1
Architecture: all
Maintainer: Justadudeinspace
Description: Termux plugin to set terminal background images with opacity and animation.

FILE: termux-background-deb/data/data/com.termux/files/usr/bin/termux-background
Kind: text
Size: 515
Last modified: 2026-01-21T07:59:08Z

CONTENT:
#!/data/data/com.termux/files/usr/bin/bash

echo "[✓] Setting Termux background..."
mkdir -p ~/.termux
cp -f ~/termux-background/background.png ~/.termux/background.png

grep -q "background=" ~/.termux/termux.properties 2>/dev/null || {
  echo "background=background.png" >> ~/.termux/termux.properties
  echo "background.opacity=0.8" >> ~/.termux/termux.properties
  echo "background.animation=scroll" >> ~/.termux/termux.properties
}

termux-reload-settings
echo "[✓] Applied background and reloaded Termux."

FILE: termux-background_1.0.1_all.deb
Kind: binary
Size: 1068
Last modified: 2026-01-21T07:59:08Z

CONTENT:
BINARY FILE — NOT DISPLAYED
file size: 1068
detected type: application/vnd.debian.binary-package

FILE: termux-background_1.0.4_all.deb
Kind: binary
Size: 1068
Last modified: 2026-01-21T07:59:08Z

CONTENT:
BINARY FILE — NOT DISPLAYED
file size: 1068
detected type: application/vnd.debian.binary-package

## 4) Workflow Inventory (index only)
- .github/workflows/build.yml (triggers: push, workflow_dispatch)

## 5) Search Index (raw results)

subprocess:
none

os.system:
none

exec(:
app/src/main/java/com/termuxbackground/BackgroundInterface.java

spawn:
none

shell:
gradlew
gradlew.bat

child_process:
none

policy:
none

ethic:
none

enforce:
none

guard:
app/build.gradle
app/build.gradle.bak

receipt:
none

token:
none

signature:
none

verify:
none

capability:
none

key_id:
none

contract:
app/src/main/java/com/termuxbackground/MainActivity.java

schema:
app/src/main/AndroidManifest.xml
app/src/main/res/layout/activity_main.xml

$schema:
none

json-schema:
none

router:
none

orchestr:
none

execute:
gradlew.bat

command:
app/src/main/java/com/termuxbackground/WebAppInterface.java
gradlew
gradlew.bat

## 6) Notes
none
