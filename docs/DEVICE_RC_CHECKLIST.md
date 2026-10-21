# Device RC qualification

Record Android version, device, WebView version, companion APK SHA-256, CLI
version, and exact Termux distribution/version. Run with debug and signed,
minified release APKs. A successful CLI callback alone does not qualify rendering.
No device/emulator run was performed in the development environment.

1. Fresh-install companion without Termux: show missing dependency; Apply/Reset disabled.
2. Install/open Termux Monet retaining RUN_COMMAND >=0.109; enable Background Image in its settings. Deny Run commands permission: clear remediation;
   no mutation. Grant with Authorize; test denial with “don't ask again” through
   Android App info → Permissions. Test a Termux distribution lacking RUN_COMMAND.
3. With CLI absent, enable `allow-external-apps=true`, reload, run diagnostics:
   report CLI missing with DEB instructions. Disable external apps and recheck:
   execution error and setup guidance, never success.
4. Install release DEB and dependencies. Run diagnostics: Termux/interface/grant,
   CLI protocol, stdin transfer, writable config and actual reload pass. Copy
   diagnostics; verify no selected URI, private file contents or credentials.
5. Save `cp ~/.termux/termux.properties ~/properties-before-test`. Select PNG,
   then JPEG; verify preview. Cancel picker: previous selection remains usable.
   Select corrupt image, unsupported file, >20 MiB image and >40 megapixel image:
   reject with no configuration change. Revoke provider access before selection finishes:
   report transfer error. Revoke after successful preparation: the prepared
   snapshot still applies successfully. Test low storage in companion and Termux.
6. Apply once, then press repeatedly: one dispatch; progress until callback,
   no premature success. Confirm image actually appears in Termux. If not,
   this Monet distribution fails qualification; do not publish an RC claim.
7. Change opacity at 0, 0.5, 1; confirm preview and actual terminal behavior.
   Confirm blur and scroll are disabled as unsupported in Monet; injected native
   requests for either must fail validation. Verify preview accessibility.
8. Restart Termux, then companion: configuration persists, controls start safely,
   diagnostics reports managed background; Apply requires selecting an image.
9. Reset: background removed in Termux. Compare properties against saved copy;
   unrelated values/comments and legacy keys remain (Monet overlay is managed).
   Enable Background Image again in Monet settings if Reset disabled it, then
   reapply and confirm portrait/landscape cache regeneration.
10. Temporarily make CLI non-executable or rename it; run diagnostics and restore.
    Substitute a failing reload command in an isolated test setup; verify reload
    failure is distinct and warns configuration may already have changed.
11. Interrupt an active Apply by killing companion/Termux; relaunch. No overlapping
    mutation permitted. Withhold callback for 60 seconds: timeout, unknown result,
    recovery instructions. Stop/reopen Termux and confirm native Recover dialog;
    recheck diagnostics before further operations. Reject stale callbacks.
12. Check narrow display, landscape, TalkBack, large font, dark theme, keyboard
    focus and progress announcements. Check navigation/external links cannot
    open arbitrary content in the privileged WebView.
