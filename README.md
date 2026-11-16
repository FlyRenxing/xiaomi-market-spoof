# Xiaomi Market Spoof

LSPosed module that spoofs Xiaomi GetApps (`com.xiaomi.market`) as
**Xiaomi / madrid / 2611FPNFAG** (Xiaomi 18 Pro Max).

It does **not** change global `getprop` values. Other apps keep seeing the real
device.

The module follows the official modern Xposed API:

- [Develop Xposed Modules Using Modern Xposed API](https://github.com/LSPosed/LSPosed/wiki/Develop-Xposed-Modules-Using-Modern-Xposed-API)
- [libxposed API 102](https://libxposed.github.io/api/)
- Project layout matches [libxposed/example](https://github.com/libxposed/example)

## Why

Xiaomi GetApps applies model-based download policy. On REDMI K90 Pro Max
(`25102RKBEC` / `myron`) the store can throttle downloads. Switching the
store-visible identity to `Xiaomi` + `madrid` + `2611FPNFAG` lifted that
throttle in a real-device test.

This module reproduces that identity **only inside GetApps**.

## Requirements

- LSPosed with modern Xposed API 102 (Zygisk / KSU)
- Android 8.0+ (`minSdk 26`)
- Recommended scope: `com.xiaomi.market` only (`staticScope=true`)

## Install

1. Build or grab a release APK.
2. Install the APK.
3. Open LSPosed → enable **Xiaomi Market Spoof**.
4. Keep scope as `com.xiaomi.market`.
5. Force-stop Xiaomi GetApps, then open it and retry a download.

## What it hooks

Inside the GetApps process only:

- `android.os.Build` fields: `BRAND`, `MANUFACTURER`, `DEVICE`, `PRODUCT`, `MODEL`
- `android.os.SystemProperties.get(String)` / `get(String, String)` for
  `ro.product.*` brand / manufacturer / device / name / model,
  `ro.build.product`, and `ro.product.mod_device`
- `miui.os.Build` fields when that class exists in the process

It does not hook system_server, does not rewrite fingerprints, and does not
change MIUI version properties.

## Build

The Gradle Wrapper, AGP and libxposed versions follow the official example:

- Android Gradle Plugin `9.2.1`
- Gradle `9.5.1`
- `compileOnly("io.github.libxposed:api:102.0.0")`
- `compileSdk` / `targetSdk` `37`, Java 21

```bash
export ANDROID_HOME=/path/to/android-sdk
./gradlew :app:assembleRelease
```

The release APK is signed with the Android debug key by default so it can be
sideloaded for testing. Replace `signingConfig` before publishing a public
release.

### SDK packages

Google currently publishes API 37 as `platforms;android-37.0`. AGP still
expects `compileSdk = 37`, so create a symlink if needed:

```text
platforms;android-37.0
build-tools;37.0.0
ln -s android-37.0 $ANDROID_HOME/platforms/android-37
```

On Linux aarch64, Google's `build-tools` natives are still x86_64. Drop in
arm64 `aapt2` / `aidl` / `zipalign` (for example from
[Commit451/android-arm-build-tools](https://github.com/Commit451/android-arm-build-tools))
and, because AGP 9.x fetches aapt2 from Maven, set:

```properties
android.aapt2FromMavenOverride=/path/to/sdk/build-tools/37.0.0/aapt2
```

in `~/.gradle/gradle.properties`, not in this repository. GitHub Actions
runs on x86_64 and does not need that override.

## Project layout

```text
app/src/main/java/io/github/xiaomimarket/spoof/MarketSpoofModule.java
app/src/main/resources/META-INF/xposed/java_init.list
app/src/main/resources/META-INF/xposed/module.prop
app/src/main/resources/META-INF/xposed/scope.list
```

Modern API notes used here:

- Entry class extends `io.github.libxposed.api.XposedModule` with a no-arg constructor
- No work before `onModuleLoaded()`
- `META-INF/xposed/*` instead of `assets/xposed_init` and old metadata
- `minApiVersion` / `targetApiVersion` = 102
- Hook model: `hook(executable).setId(...).intercept(chain -> ...)`
- Targeting API 102, so the module does not call legacy `de.robv.android.xposed`

## Disclaimer

This is a personal, open-source helper for GetApps download policy on specific
Xiaomi models. It is not an official Xiaomi product. Use it on devices you own.
Do not use it to impersonate another person, bypass payments, or attack store
infrastructure.

## License

Apache License 2.0
