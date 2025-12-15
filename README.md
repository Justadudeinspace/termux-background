# Termux Background (Android companion)

A WebView-based helper that copies a chosen PNG/JPEG into Termux's background and updates `~/.termux/termux.properties`, then reloads settings via **Termux:API**.

## Hard requirements (must be installed)
- [Termux](https://f-droid.org/en/packages/com.termux/)
- [Termux:API](https://f-droid.org/en/packages/com.termux.api/)

> Apply/Reset stay **disabled** until Termux **and** Termux:API are detected. The app blocks writes if dependencies are missing.

## What it does
1. Lets you pick an image with the Storage Access Framework (no legacy storage permissions).
2. Preview + tune opacity, blur, and animation (scroll/none).
3. Writes only when you press **Apply**:
   - `~/.termux/background.png`
   - merges the following keys into `~/.termux/termux.properties` while preserving all other lines/comments:
     - `background=background.png`
     - `background.opacity=<value>`
     - `background.blur=<true|false>`
     - `background.animation=<scroll|none>`
4. Invokes **Termux:API** to broadcast `termux-reload-settings` and surfaces success/failure in the UI.
5. **Reset Background** removes the background-related keys, deletes `background.png`, and triggers reload (also blocked if Termux:API is absent).

## Usage
1. Install Termux and Termux:API from F-Droid.
2. Install this APK (or build locally with `./gradlew assembleRelease`).
3. Open the app:
   - Tap **Re-check** if Apply is disabled.
   - Choose an image → adjust options → press **Apply Background**.
   - Press **Reset Background** to clear the settings and reload.

## Troubleshooting
- **Apply is disabled**: Install or re-open Termux:API; press **Re-check**.
- **Reload failed**: Ensure Termux:API is installed and allowed to receive broadcasts.
- **Unsupported image**: Only PNG/JPEG are accepted.

## Development
- Build: `./gradlew assembleDebug`
- The WebView loads `app/src/main/assets/termux-background-ui.html`.
- Native bridge lives in `app/src/main/java/com/termuxbackground/WebAppInterface.java` and uses the Termux:API broadcast `com.termux.api.action.RUN_COMMAND`.

## License
MIT License © 2025 Justadudeinspace
