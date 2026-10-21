# Termux Background 1.1.0 development build

The Android companion now invokes the Termux-owned CLI through the supported
RUN_COMMAND interface, transfers validated images through bounded stdin, and
waits for command completion. Setup diagnostics check the CLI protocol,
transport, writable configuration and reload. Native validation prevents shell
command construction; timeout/interruption recovery prevents overlapping work.

The CLI retains unrelated configuration, accepts bounded base64 image input,
and distinguishes validation, transfer, write, reload and cleanup failures.
Android lint/tests/debug/release assembly and CLI/DEB checks run in CI. Tagged
releases require signing secrets and APK signature verification.

The companion now targets Termux Monet's documented wallpaper files and overlay
color through an explicit CLI backend. Existing legacy CLI behavior remains.
Enable Background Image in Monet settings before Apply (and again after Reset if
disabled). Blur and animation are unsupported by Monet and disabled/rejected.
No exact device/Monet version has passed the
[device checklist](docs/DEVICE_RC_CHECKLIST.md), so this is a test build.

The debuggable APK uses package `com.termuxbackground.test` and can coexist with
the production app. Local production release APKs are unsigned without release
credentials; they are not distributed as the test artifact.

See [README](README.md), [bridge design](docs/ANDROID_TERMUX_BRIDGE.md), and
[upgrade report](UPGRADE_REPORT.md) for setup and validation evidence.
