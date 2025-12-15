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
