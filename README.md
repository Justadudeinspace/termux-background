# Termux Background

Termux Background provides a small Android companion UI and a Termux command line tool for managing terminal background settings. The CLI is the supported way to apply changes: Android keeps each app's private files isolated, so the companion cannot safely write directly into Termux's private `~/.termux` directory. The Android UI currently provides dependency status and a preview, and directs users to the CLI for changes.

## Requirements

- Termux and Termux:API from the same trusted distribution source, for the Android companion's dependency checks.
- Android 7.0 (API 24) or later for the companion app.
- Termux with `termux-reload-settings` available for applying CLI changes.

The project builds against Android SDK 35 and targets Android 15. Physical-device checks are still needed for Android 15 and newer.

## Install

Install the APK from a project release or build it yourself:

```sh
adb install termux-background-1.1.0.apk
```

Install the Debian package from inside Termux:

```sh
dpkg -i termux-background_1.1.0_all.deb
```

## CLI usage

```sh
termux-background set --image ~/Pictures/terminal.png --opacity 0.8 --blur false --animation scroll
termux-background status
termux-background reload
termux-background reset
termux-background help
termux-background version
```

`set` accepts PNG or JPEG images, opacity from 0 through 1, `true`/`false` blur, and `none`/`scroll` animation. It installs the selected image at `~/.termux/background.png`, updates only the background keys in `termux.properties`, and reloads settings. `reset` removes those managed keys and image while retaining unrelated properties. A reload failure is reported even though the files may already have been written.

Exit codes: `0` success, `1` general failure, `2` invalid arguments, `3` missing dependency, `4` file access failure, and `5` configuration/reload failure.

## Build from source

Requires JDK 17 and Android SDK platform/build tools 35:

```sh
git clone https://github.com/Justadudeinspace/termux-background.git
cd termux-background
./gradlew clean assembleDebug
./gradlew assembleRelease
./build-deb.sh
./tests/cli_test.sh
```

Release APKs are unsigned unless signing credentials are supplied externally. Set `ANDROID_KEYSTORE_PATH`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, and `ANDROID_KEY_PASSWORD` in the environment. Never commit keystores or credentials. GitHub Actions accepts the same values through repository secrets (`ANDROID_KEYSTORE_BASE64` for the keystore file). The previously committed signing key is removed and must be considered compromised; apps signed with a replacement key cannot update installations signed by that old key, so those installations must be uninstalled before installing a newly signed build.

## Troubleshooting

- **Termux missing:** install Termux before using its configuration tools.
- **Reload command missing:** install/update Termux tools so `termux-reload-settings` is available.
- **Image type check unavailable:** install the Termux `file` package (also declared as a Debian dependency).
- **CLI says image unreadable:** use a path readable by the Termux shell, such as a file in `$HOME`; Android picker URIs are not shell paths.
- **Reload failed:** the config was written but may not yet be active; run `termux-background reload` after resolving the Termux command issue.
- **Android companion says configuration is inaccessible:** this is Android app sandbox isolation. Apply settings from the Termux CLI as shown above.

The companion's Diagnostics option provides app, OS, and dependency information for bug reports. It does not include file contents or credentials.
