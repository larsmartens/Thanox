# Pixel 7 Pro Thanox IFW Investigation - 2026-04-25

## Context

Device: Pixel 7 Pro (`cheetah`) on Android 16 (`CP1A.260405.005`).

Problem: during a broader Magisk/LSPosed crash investigation, Thanox was not the
primary `system_server` watchdog root cause, but it was repeatedly visible as a
source of framework log pressure inside `system_server`.

Raw phone logs are intentionally not committed to this repository because they
include personal app/package state and device-specific identifiers.

Local evidence roots:

- `/home/larsm/android-evidence-20260425-deep-log`
- `/home/larsm/android-evidence-20260425-post-fix`
- `/home/larsm/android-evidence-20260425-verify`
- `/home/larsm/android-evidence-20260425-final`

## Findings

Repeated post-boot logcat entries showed Thanox logging errors for broadcasts
where the Android framework had not resolved a receiver package at the IFW hook
point:

```text
Thanox-Core: checkBroadcast, receiverPkgName: null or callPkgName: android is null. return.
```

The noisy path originated from the Magisk framework patch IFW hook:

- `android/android_framework/patch-magisk/patch-framework/src/main/java/github/tornaco/thanox/android/server/patch/framework/hooks/am/IFWHooks.java`

In current Android framework behavior, implicit broadcasts can reach
`IntentFirewall.checkBroadcast` without an explicit component or package on the
`Intent`. Forwarding those intents into Thanox does not provide enough receiver
identity for a useful package-level policy decision and creates avoidable
`system_server` log churn.

## Fix

Patch committed on branch `fix/ifw-implicit-broadcast-noise`:

- Commit: `5cd2eec5 Reduce Thanox IFW implicit broadcast noise`
- GitHub Actions run: `24932589464`
- CI artifact:
  `/home/larsm/android-evidence-20260425-thanox-ci/thanox-zygisk-module/zygisk-thanox-8.6-43-5cd2eec-fix_ifw-implicit-broadcast-noise-release.zip`

The hook now returns without consulting Thanox when:

- hook arguments are malformed
- the broadcast `Intent` is null
- the broadcast is implicit and has neither `component` nor `package`

Explicit/package-targeted broadcasts still pass through the existing Thanox
policy path.

## Validation Status

The patched module built successfully through GitHub Actions.

Device-side installation and post-reboot validation are pending because the phone
stopped enumerating via both adb and fastboot before the module could be staged.
The prepared install/verify helper is local-only:

- `/home/larsm/tmp/install_patched_thanox_when_device_returns.sh`

Expected post-install checks:

- boot completes with `sys.system_server.start_count=1`
- no new `system_server_anr`, `system_server_watchdog`, or
  `system_server_pre_watchdog` dropbox entries after an idle window
- `Thanox-Core: checkBroadcast, receiverPkgName: null...` log volume is reduced
- patched module hashes match the CI artifact contents
