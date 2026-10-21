# Upgrade report

## Findings and completed changes

- Removed the committed release keystore and plaintext signing properties; debug uses standard Android debug signing and release signing is optional and environment supplied.
- The former signing key is compromised. The key has been removed from the working tree; release owners must create a fresh key and account for the Android signing identity change.
- Corrected manifest launcher references, duplicate attributes, package visibility, backup exposure, and removed unnecessary storage/network permissions.
- Corrected the AppCompat theme mismatch that could prevent the activity from starting and added R8 keep rules for the JavaScript bridge.
- Updated Android Gradle Plugin/Gradle wrapper and SDK target to a current SDK 35 toolchain; enabled release shrinking.
- Removed duplicate `BackgroundInterface` and unsafe direct access to Termux private paths. The app bridge now accurately reports that configuration changes must run inside Termux.
- Locked WebView navigation to the bundled page, disabled content/universal file access and mixed content, and removed remote UI assets.
- Replaced destructive/duplicated Debian packaging paths with one CLI and package builder. CLI validates inputs, preserves unrelated properties, writes via temporary files, reports reload failure, and provides status/reset/help/version.
- Added a shared `VERSION` source for Android and Debian package builds; tagged CI versions are propagated into packaged CLI output.
- Added CLI behavior coverage and refreshed install/build/troubleshooting documentation.
- Removed stale package binaries and obsolete build artifacts/scripts.

## Validation

- `./tests/cli_test.sh` — passed: apply, unrelated property preservation, reset, image install/removal, invalid opacity/animation, oversized image rejection, and version.
- `./build-deb.sh` — passed; generated Debian package version 1.1.0.
- `./gradlew clean` — passed.
- `./gradlew lint test assembleDebug assembleRelease` — not tested beyond Gradle task setup: Android SDK location is set to `/opt/android-sdk`, but that directory/platform is absent in this environment. Gradle stopped with “SDK location not found”.

## Remaining release work

- The Android app cannot modify Termux's private config under Android's app sandbox. The companion currently offers dependency status/preview but its Apply/Reset operations deliberately report this limitation. A proper cross-app transfer/command protocol is needed before claiming Android UI apply support.
- No Android emulator/device validation was available. Test dependency detection, SAF image preview/cancellation, and CLI application/reload/reset on a physical device.
- Gradle lint, unit tests, debug APK build, and release APK build remain not tested due to missing Android SDK in this environment.
- The exact support behavior of blur and scroll animation depends on the installed Termux build and should be verified on-device.
