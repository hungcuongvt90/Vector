# Manager stops with `LayoutNode … not found in RectList`

## Evidence

The reported standalone manager was official `ddeed8ca` (2.2 / 3112), on
Android 17 / SDK 37. The exception is thrown by Compose UI's spatial rectangle
bookkeeping during layout, not a daemon/Binder failure. The official release
APK's R8 map ID matches the report; inspecting its `ek2` class identifies
`d` as `RectManager.indexInRectList` and `g` as `recalculateRectIfDirty`.
Without that release's mapping file, the entire obfuscated trace cannot be
retraced or the particular screen identified reliably.

The old catalog used stable BOM `2026.09.00` with an explicit Material 3
`1.5.0-alpha29` override. Gradle's resolved manager release classpath selected
Compose UI/runtime/foundation `1.13.0-alpha01`, not the BOM's UI `1.12.1`.
A regular `platform` supplies constraints, not a forced version: a newer
transitive request wins.

AndroidX fixed this exact exception when alignment lines (text baselines) are
queried during measurement, including reused lazy-list items:

- [Issue b/549552303](https://issuetracker.google.com/issues/549552303)
- [Change Icec9e](https://android-review.googlesource.com/q/Icec9eff72dbc6e77ee168c26c36f37ca7b244bee)
- [Compose UI 1.13.0-beta01 release notes](https://developer.android.com/jetpack/androidx/releases/compose-ui#1.13.0-beta01)

`RectListRegressionTest` adapts the upstream reproducer. On an Android 16
(API 36) emulator, its lazy-item-reuse case **failed before the dependency
change** with `IllegalArgumentException: LayoutNode 25 not found in RectList`.
Thus the failure is reproducible without Android 17 or a daemon. Reinstalling
or restarting clears the live layout tree and may temporarily hide it; it
does not repair an APK that still contains the affected library.

## Fix

Both manager modules now use `compose-bom-beta:2026.10.01` consistently:
Compose UI/runtime/foundation/animation `1.13.0-beta02`, Material 3 and the
adaptive navigation suite `1.5.0-beta02`. Remove the individual Material 3
pins so they follow the same BOM. The beta BOM retains Expressive APIs the
manager already uses and includes the upstream fix instead of swallowing a
layout exception or disabling accessibility/rectangle tracking.

These libraries require compile SDK **37.1**, set only for `:manager` and
`:manager-ui`; min SDK 27 and target SDK 37 are unchanged. Native injection,
daemon lifecycle, scopes, permissions, and saved settings are not modified.
Test-runner libraries are confined to instrumentation/debug configurations.

## Verify

```sh
./gradlew :manager-ui:connectedDebugAndroidTest
./gradlew :manager:assembleDebug :manager:assembleRelease
./gradlew :manager:dependencyInsight \
  --configuration releaseRuntimeClasspath --dependency androidx.compose.ui:ui-android
python3 -m unittest discover -s tests -v
```

All three instrumentation cases pass after the change: nested baseline layouts,
padded nested layouts, and baseline queries while lazy-list slots are reused.
The Python suite also passes (six existing uninstall/workflow-policy checks).
The release APK is R8-minified, locally debug-key-signed, and is at
`manager/build/outputs/apk/release/manager-release.apk`. It was installed on the
same API 36 emulator: cold start, two rounds of Home/Modules/Store/Logs tab
switching, and three background/restore cycles completed without a crash. The
emulator had no daemon, so this does not verify module/scope/log integration.

For a phone check: switch tabs repeatedly; scroll modules, scope lists, store,
and logs; open/close sheets and settings; rotate and background/restore the app.
Check the manager build stamp so an older installed or bundled manager is not
mistaken for this build. Emulator checks do not establish long-term stability
on the reported Android 17 phone, nor rule out unrelated crashes.

## Installation

The standalone APK and module-bundled manager are separate copies. Installing
this APK updates only the standalone copy; the bundled manager gains this fix
when a module is built from this tree and updated in place. Do not uninstall
or clear the root module to fix this UI crash.

A locally signed APK cannot replace an official-signature standalone app with
`adb install -r`; use a build signed with the same key, or uninstall only the
standalone app before installing this one. Uninstalling the standalone app
resets its local preferences. The root daemon's module/scope configuration is
separate. Reinstalling an old APK can still allow this crash to recur.
