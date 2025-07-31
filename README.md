# Termux Background

A Termux plugin and Android companion app that lets you set a background image in your terminal — just like Windows Terminal. Supports opacity, scroll animation, blur effect, and real-time application using `termux-reload-settings`.

---

## ✨ Features

- 📷 **Select background image** (PNG or JPEG)
- 🎚️ **Opacity control** via slider
- 🌀 **Scroll animation toggle**
- 🌫️ **Blur effect toggle**
- ⚡ Applies instantly using Termux's runtime settings

---

## 🚀 Installation

### 📦 Option A: Install the `.deb` CLI Plugin (recommended)

```bash
wget https://github.com/Justadudeinspace/termux-background/releases/download/v1.0.2/termux-background_1.0.2_all.deb
dpkg -i termux-background_1.0.2_all.deb
```

Then apply background using:

```
termux-background
```

🤖 Option B: Install the Android WebView App (UI-based)

```
adb install app-release.apk
```

Launch the app → choose your image → adjust options → click “Set as Termux Background”.


---

🛠 How It Works

Your selected image is copied to:
~/.termux/background.png

These settings are written to:
~/.termux/termux.properties

```
background=background.png
background.opacity=0.8
background.animation=scroll
background.blur=false
```

Finally, this is executed:

```
termux-reload-settings
```


---

🧪 Debugging

To manually trigger reload or inspect config:

```
termux-reload-settings
cat ~/.termux/termux.properties
```

---

👨‍💻 Development

🔨 Build APK (Android app)

```
./gradlew clean assembleRelease
```

📦 Build .deb plugin for Termux

```
bash build-deb.sh
```

🧪 Test locally

```
adb install -r app/build/outputs/apk/release/app-release.apk
dpkg -i termux-background_1.0.2_all.deb
```

---

📁 File Map

File	Purpose

background.png	Terminal background image
termux.properties	Terminal appearance settings
termux-background (CLI)	Shell script to apply background
termux-background-ui.html	WebView frontend (inside app)



---

📜 License

MIT License © 2025 Justadudeinspace

---

