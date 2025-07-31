# Termux Background

A Termux plugin that allows you to set a custom background image behind the terminal interface — with support for opacity, blur, and scroll animation.

## 🎨 Features

- 📷 Image preview
- 🔍 JPEG/PNG file selector
- 🎚️ Opacity slider (10%–100%)
- 🌫️ Blur effect toggle
- 🌀 Background scroll animation
- 🛠 Automatically updates `~/.termux/termux.properties`
- 🚀 Triggers `termux-reload-settings` instantly

## 📦 Installation

### Option 1: Install via .deb (recommended for CLI users)

```bash
wget https://github.com/Justadudeinspace/termux-background/releases/download/v1.0.1/termux-background_1.0.1_all.deb
dpkg -i termux-background_1.0.1_all.deb

Then run:

termux-background

Option 2: Install APK

adb install app-release.apk

Open the app from your launcher and apply your background.

🧪 Testing

termux-reload-settings
cat ~/.termux/termux.properties

📁 Files and Paths

FileLocation

Background Image~/.termux/background.png
Properties Config~/.termux/termux.properties
CLI Entrytermux-background


🙏 License

MIT License — © 2025 Justadudeinspace EOF

