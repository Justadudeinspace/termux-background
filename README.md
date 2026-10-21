# Termux Background

[![Android and Debian builds](https://github.com/Justadudeinspace/termux-background/actions/workflows/build.yml/badge.svg)](https://github.com/Justadudeinspace/termux-background/actions/workflows/build.yml)

**Test/Debug Build Under Actions**
*(v1.0.0 Not Operational, go to "Actions," select latest successful build, download Artifact, Unzip for Test/Debug Apk)*

Termux Background provides an Android controller and a Termux CLI. The companion
uses Termux's supported RUN_COMMAND interface to invoke the CLI; all configuration
writes stay inside Termux. Apply/Reset wait for command results.

The companion targets **Termux Monet's documented wallpaper interface**:
`~/.termux/background/background.jpeg` and `background-overlay-color`. Enable
Background Image under Monet Settings → Termux → Termux Style; after Reset,
Monet may disable that switch, so enable it again for the next Apply. Opacity
uses a black overlay. Blur and scroll animation are unsupported by this backend
and disabled in the companion. Upstream Termux has no wallpaper interface.

**Device qualification remains outstanding:** the Monet adapter is based on
[its implementation](https://github.com/Termux-Monet/termux-monet/blob/master/app/src/main/java/com/termux/app/style/TermuxBackgroundManager.java)
and [documented wallpaper support](https://github.com/Termux-Monet/termux-monet#wallpaper-support),
but no exact device/distribution/version has passed the
[device checklist](docs/DEVICE_RC_CHECKLIST.md). CLI completion confirms files
and reload, not rendering.

## Requirements

- Android 7.0 (API 24) or later; builds target SDK 35.
- Initialized Termux Monet retaining Termux >=0.109 the `com.termux` RUN_COMMAND API.
- The CLI DEB from this release and `termux-tools`, `file`, `coreutils` in Termux.
- Companion Run commands in Termux permission and user-enabled
  `allow-external-apps=true` in `~/.termux/termux.properties`, followed by
  `termux-reload-settings`. This permits authorized apps to execute with Termux's
  privileges; grant only to trusted apps. Termux:API is not required.

## Install

Install the APK from a project release or build it yourself:

```sh
adb install termux-background-1.1.0-test.apk
```

Install the Debian package from inside Termux:

```sh
pkg install termux-tools file coreutils
dpkg -i ./termux-background_1.1.0_all.deb
```

Open the companion, tap Authorize, grant the permission, then Re-check. Diagnostics
checks CLI protocol, image transport, configuration and reload. Installations are
manual; the app never downloads or silently installs code. Select a PNG/JPEG
(up to 20 MiB / 40 megapixels); Apply transfers a resized JPEG up to 120 KiB.

## CLI usage

```sh
termux-background --backend monet set --image ~/Pictures/terminal.png --opacity 0.8
termux-background --backend monet status
termux-background --backend monet reset

# Existing legacy CLI remains available for forks implementing its keys:
termux-background set --image ~/Pictures/terminal.png --opacity 0.8 --blur false --animation scroll
termux-background status
termux-background reload
termux-background reset
termux-background help
termux-background version
```

Without `--backend monet`, the legacy `set` accepts PNG or JPEG images, opacity from 0 through 1, `true`/`false` blur, and `none`/`scroll` animation. It installs the selected image at `~/.termux/background.png`, updates only the background keys in `termux.properties`, and reloads settings. `reset` removes those managed keys and image while retaining unrelated properties. A reload failure is reported even though the files may already have been written.

Exit codes: `0` success, `1` general failure, `2` invalid arguments, `3` missing dependency, `4` file access failure, `5` configuration failure, `6` reload failure after write, `7` temporary cleanup failure, and `8` a missing engine dependency.
The companion also uses `probe` (protocol 1) and `set --image-stdin` with bounded base64 input.

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
- **Execution rejected:** grant the companion command permission and enable `allow-external-apps=true` in Termux.
- **Timeout/interrupted operation:** completion is unknown. Force-stop and reopen Termux, then confirm Recover in the companion and run diagnostics.
- **CLI completed but no background:** enable Background Image in Termux Monet settings; upstream Termux cannot render it; see the compatibility limitation above.

The companion's Diagnostics option provides app, OS, and dependency information for bug reports. It does not include file contents or credentials.

See [bridge design and security](docs/ANDROID_TERMUX_BRIDGE.md) and the
[device checklist](docs/DEVICE_RC_CHECKLIST.md). On an ARM host, a native AAPT2
may be required: `./gradlew -Pandroid.aapt2FromMavenOverride=/path/to/native/aapt2 lint test assembleDebug assembleRelease`.

Debug builds are debuggable test APKs with application ID `com.termuxbackground.test`,
version suffix `-test`, and label “Termux Background Test”. They can coexist with
the production app and require their own Termux command permission grant.
