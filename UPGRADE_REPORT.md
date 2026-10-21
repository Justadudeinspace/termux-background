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

## Implementation continuation (2026-10-21)

- Verified the existing upgrade and retained its signing, manifest, WebView, CLI, packaging and CI changes.
- Researched official Termux RUN_COMMAND documentation and RunCommandService/TermuxConstants/FileUtilsErrno sources. Implemented explicit service execution of the fixed CLI path, allowlisted native arguments, runtime RUN_COMMAND permission, background execution and an explicit mutable one-shot PendingIntent to a non-exported receiver. No shell command construction or Termux:API dependency.
- Added bounded SAF read/decode, metadata validation, JPEG normalization and base64 stdin transfer (120 KiB image, 160 KiB ASCII stdin). Termux creates private random temporary files and the CLI remains the configuration engine. No shared storage permission, server, downloaded code or persisted URI grant.
- Added CLI protocol probe, stdin image input, distinct reload failure exit code, UI readiness/progress states, native duplicate prevention, timeout handling, setup instructions and copyable sanitized diagnostics.
- Located SDK at /home/jadis/android-sdk and installed platform 35 / build-tools 35. Initial Android resource build hit x86_64 AAPT2 on this aarch64 host; retrying with installed native Termux AAPT2 override (local command only).
- Newly confirmed product compatibility blocker: upstream TermuxPropertyConstants does not document or implement background/background.opacity/background.blur/background.animation. Configuration/reload success is not evidence of rendering. UI now states build-dependent rendering; no background-capable Termux distribution has yet been qualified. Do not claim RC without identifying and testing one.

### Build and hardening progress

- Android lint, 8 initial unit tests in both variants, debug APK and R8/resource-shrunk release APK all built successfully using native AAPT2. The SDK/toolchain blocker is resolved. Subsequent validation is rerunning after recovery/image preview hardening.
- Persisted in-flight operation state and added native recovery confirmation after process death/timeout. Malformed results cannot unlock mutations as successful operations. Missing CLI uses official Termux file-not-found error codes 150/151.
- Moved SAF validation/normalization to selection and preview the exact prepared JPEG through native-delivered data. WebView receives no content URI and filesystem/content access remains disabled. Prepared images stay in memory; revoked grants after preparation cannot invalidate the snapshot.
- Extended CLI tests for protocol probe, bounded stdin transfer, malformed base64, overflow, private temporary cleanup, whitespace in managed property keys, and reload error codes. Added package metadata/payload/executable/version/status verification.
- CI retains all prior gates and now verifies tagged APK signatures and tests the built DEB. No signing identity was generated or publication attempted.

### Supported renderer and test distribution

- Follow-up research found Termux Monet's documented wallpaper interface. Added an explicit `--backend monet` CLI adapter: installs `~/.termux/background/background.jpeg`, invalidates Monet's named orientation caches, uses `background-overlay-color` for opacity, and records an ownership marker for reset. Legacy CLI invocations remain supported and regression-tested.
- Companion always requests the fixed Monet backend. Blur/scroll have no supported Monet equivalent, so controls are disabled and requests are rejected in both native and CLI validation. Monet's Background Image preference must be enabled by the user; no private preference XML is edited. Reset may disable Monet's preference, requiring re-enabling it before reapply.
- References: https://github.com/Termux-Monet/termux-monet#wallpaper-support and app/src/main/java/com/termux/app/style/TermuxBackgroundManager.java / termux-shared/.../TermuxConstants.java in that repository, reviewed 2026-10-21. No exact Monet/device combination is yet qualified.
- Per user request, debug builds are explicitly test APKs: debuggable, application ID com.termuxbackground.test, version suffix -test, label Termux Background Test. They can coexist with production.
- User authorized staging, professional commit and direct GitHub push. SSH remote access verified. GitHub CLI has no API login, so no manual GitHub Release upload is available; pushing main triggers the existing Actions artifact pipeline. No stable release tag will be created.

### Final release audit

- Reviewed all 50 meaningful final audit matches for TODO/FIXME/HACK, credential/key names, debug output, ignored errors, process execution, JavaScript exposure and properties access. Matches are test fixtures/configuration docs, environment-only signing inputs, or the deliberately narrow bundled JavaScript interface. No raw shell/process execution, committed signing credentials, arbitrary path handoff, or ignored command failures were introduced.
- Identified and corrected a real hosted-CI portability defect: CLI regression tests now invoke the Termux-shebang script via host `bash`, allowing Ubuntu runners to execute it without `/data/data/com.termux/files/usr/bin/bash`.
- Kept the existing SDK 35/AppCompat dependency scope. Lint warnings about target/dependency freshness, launcher shape, fallback hardcoded text, fixed Termux executable path, and backup schema are recorded rather than hidden. The application-only singleton avoids an Activity context leak. No lint gate was weakened.

## Current validation and release decision (supersedes historical blockers above)

All available final checks passed on 2026-10-21:

| Check | Final result |
| --- | --- |
| `./gradlew clean` | PASSED |
| `./gradlew -Pandroid.aapt2FromMavenOverride=/data/data/com.termux/files/usr/bin/aapt2 lint` | PASSED; warnings recorded, no errors |
| Same Gradle override with `test` | PASSED; 10 tests each in debug/release, zero failures/errors/skips |
| Same Gradle override with `assembleDebug` | PASSED |
| Same Gradle override with `assembleRelease` | PASSED, R8/resource shrinking enabled; production APK unsigned without external credentials |
| Final combined `lint test assembleDebug assembleRelease` with native AAPT2 override | PASSED after final native/UI changes |
| `./tests/cli_test.sh` | PASSED: legacy, transport and Monet regressions |
| `./build-deb.sh` and `./tests/package_test.sh` | PASSED: DEB metadata, executable payload, version and status |
| `bash -n` on builder and both shell tests/CLI | PASSED |
| `node --check` on extracted bundled UI script | PASSED |
| `apksigner verify --verbose` on test APK | PASSED, APK Signature Scheme v2 |
| Native AAPT2 badging | CONFIRMED com.termuxbackground.test / 1.1.0-test / application-debuggable |
| `sha256sum --check SHA256SUMS` | PASSED for test APK and DEB |
| `git diff --check` | PASSED |

The SDK was found and platform/build-tools 35 installed. The first x86 AAPT2
attempt failed on this ARM host; the installed native AAPT2 resolved it without
weakening build gates or committing machine-specific SDK paths. Local Java is 21;
source/target and CI remain Java 17. AAPT2 emits nonfatal platform resource lookup
messages on this host; all resource packaging tasks complete successfully.

Test deliverables (ignored build outputs):

- `build/test-release/termux-background-1.1.0-test.apk` — debuggable, debug-signed, isolated test application ID.
- `build/test-release/termux-background_1.1.0_all.deb`.
- `build/test-release/SHA256SUMS`.

No emulator/physical-device execution or production signing was performed.
Remaining qualification work: run docs/DEVICE_RC_CHECKLIST.md on a specified
Monet version, verify wallpaper cache/reload/resume and Reset/reapply, permission
and callback behavior, SAF errors, accessibility and persistence. Monet's own
Background Image setting requires manual authorization; blur/scroll are explicitly
unsupported, not simulated as successful device features.

Release classification: **BETA**. The implementation and test APK are ready for
device qualification; claiming RELEASE CANDIDATE or RELEASE READY would exceed
the available end-to-end rendering evidence. CI retains CLI/lint/tests/debug/
release/DEB/checksum gates, adds DEB payload checks and tagged APK verification,
and publishes signed production releases only through the existing tag workflow.
