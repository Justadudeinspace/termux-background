# Termux Background 1.1.0

This release hardens the Termux CLI and Debian package. The CLI validates PNG/JPEG input and option values, preserves unrelated `termux.properties` lines, atomically replaces its managed configuration, reloads settings, and reports reload failures.

The Android companion detects Termux dependencies and previews a selected image. Android's app sandbox prevents direct access to Termux's private home, so use `termux-background` inside Termux to apply and reset settings.

See [README.md](README.md) for installation, usage, build, and secure signing instructions. Android APK builds still require validation with the Android SDK and on a physical device.
