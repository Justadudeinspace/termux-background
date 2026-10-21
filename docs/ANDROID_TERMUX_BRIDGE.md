# Android → Termux protocol

The companion sends an explicit `com.termux.RUN_COMMAND` intent to
`com.termux/com.termux.app.RunCommandService`. The executable is always
`/data/data/com.termux/files/usr/bin/termux-background`. Native code adds the fixed `--backend monet` prefix and maps APPLY,
RESET, STATUS, PROBE and RELOAD to literal argument arrays. It never invokes a
shell interpreter, accepts an executable/path from JavaScript, or constructs a
shell command. Termux owns every configuration write.

Requires Termux >= 0.109, initialized by opening it at least once, the runtime
`com.termux.permission.RUN_COMMAND` grant, and a user-configured
`allow-external-apps=true` in Termux properties. This permission allows execution
with Termux's privileges; grant it only to trusted apps. Termux:API adds no
needed transport capability and is not required. Termux Monet must retain the official package/service/API.

References reviewed on 2026-10-21:

- [Official RUN_COMMAND guide](https://github.com/termux/termux-app/wiki/RUN_COMMAND-Intent)
- [RunCommandService](https://github.com/termux/termux-app/blob/master/app/src/main/java/com/termux/app/RunCommandService.java)
- [TermuxConstants](https://github.com/termux/termux-app/blob/master/termux-shared/src/main/java/com/termux/shared/termux/TermuxConstants.java)
- [FileUtilsErrno](https://github.com/termux/termux-app/blob/master/termux-shared/src/main/java/com/termux/shared/file/FileUtilsErrno.java)
- [Upstream property definitions](https://github.com/termux/termux-app/blob/master/termux-shared/src/main/java/com/termux/shared/termux/settings/properties/TermuxPropertyConstants.java)

## Transfer

SAF supplies a native-selected content URI, never a frontend-provided path. Native
code reads at most 20 MiB, accepts decoded PNG/JPEG metadata only, rejects images
above 40 megapixels, decodes with subsampling to at most 1600 pixels per side,
and compresses to JPEG at descending quality until at most 120 KiB. A complex
image that cannot meet the limit is rejected; choose a smaller image. Transparency
is flattened by JPEG encoding. Selection prepares an in-memory snapshot; preview
uses exactly those JPEG bytes. Apply uses that snapshot, so later URI revocation
does not affect an already prepared image. An unreadable URI is rejected during
selection. WebView never receives the content URI or filesystem read access.

Base64 is sent through `RUN_COMMAND_STDIN` (at most 163840 ASCII characters,
roughly 328 KiB in Android's UTF-16 parcel). This stays below Termux's documented
roughly 500 KB intent guideline and does not consume command argument space.
The CLI reads at most 163841 characters, rejects overflow, decodes into an
unpredictable mode-600 private temporary file, checks image type, and uses the
existing set implementation. EXIT/signal cleanup removes temporary files;
cleanup errors are reported. A forcible OS kill can leave private temporary files,
but no externally readable transfer file exists. No URI grants to Termux, shared
storage, broad storage permission, local network service or persistent URI
permissions are used. Revoked/unreadable URI, interrupted read, corrupt/unsupported
image, and compression/storage failures produce a transfer error.

## Results and recovery

A unique explicit mutable one-shot broadcast PendingIntent targets a non-exported
receiver. It is mutable only so Termux can fill in the documented result bundle;
random per-operation URI identity correlates it. The bridge validates the active
operation and bundle and cancels the capability on completion. No raw stdout,
stderr, URI or error stack is copied into diagnostics.

Dispatch is not success. `err=-1` and `exitCode=0` mean CLI completion. Exit codes
2/3/4/5/6/7/8 identify validation/reload-unavailable/transfer/config/reload/cleanup/engine-dependency failures.
Termux file-not-found errors 150/151 and command exit 127 identify a missing CLI.
Other Termux internal errors remain execution errors with authorization/setup
remediation; localized internal errors are not guessed from text.

Only one operation runs at a time. A 60-second deadline covers preparation and
execution. Timeout means unknown completion, not cancellation. A durable in-flight
marker detects process death. After timeout, invalid result, or process death,
mutations remain disabled until the user force-stops/reopens Termux and confirms
the native Recover dialog. Late callbacks are ignored. Android cannot cancel a
RUN_COMMAND task reliably through this API.

PROBE protocol 1 checks an actual stdin transfer into a private temporary file,
configuration directory write access and reload execution without changing
background properties. Only a complete successful probe enables mutations.
Configuration presence is reported separately. The dependency check does not
silently bootstrap anything: users install the release DEB and dependencies in
Termux themselves. The probe transfers test bytes, not a selected image; image
decoding/transfer is checked when Apply runs.

## Rendering backend and qualification

Upstream Termux does not implement this project's legacy background properties.
The companion therefore always requests the explicit `--backend monet` CLI
backend. Existing legacy CLI calls retain their behavior for compatible forks.

The Monet backend follows [documented wallpaper support](https://github.com/Termux-Monet/termux-monet#wallpaper-support),
[current constants](https://github.com/Termux-Monet/termux-monet/blob/master/termux-shared/src/main/java/com/termux/shared/termux/TermuxConstants.java)
and [TermuxBackgroundManager](https://github.com/Termux-Monet/termux-monet/blob/master/app/src/main/java/com/termux/app/style/TermuxBackgroundManager.java).
It installs `.termux/background/background.jpeg`, invalidates only the two named
orientation caches (which Monet regenerates), and maps opacity to a black
`background-overlay-color=#AARRGGBB` overlay. An ownership marker limits reset to
wallpapers managed through this backend. Reset removes the wallpaper, its caches
and the overlay key while preserving unrelated legacy keys and properties.

Background Image must be enabled manually in Monet's own settings. No supported
command API controls that preference; this app does not edit private preference
XML. After Reset Monet may disable the preference on encountering a missing
wallpaper; enable it again before the next Apply. Blur and scroll animation have
no documented equivalent and are rejected natively and by the Monet CLI backend;
the UI disables them. A black overlay approximates opacity, not alpha compositing
against arbitrary terminal content.

This is a source-backed adapter, not device qualification. Returning to Monet
may be needed for its activity to reload while previously in the background.
An exact compatible Monet version still needs the device checklist, especially
cache regeneration, reload/resume behavior, opacity and Reset/reapply.
