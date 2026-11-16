# 小米应用商店伪装

LSPosed 模块，包名 `io.github.flyrenxing.marketspoof`。只在小米应用商店（`com.xiaomi.market`）进程内，把机型伪装成 **Xiaomi / madrid / 2611FPNFAG**（小米 18 Pro Max）。

它**不会**修改全局 `getprop`。其他应用仍然看到真实机型。

模块按官方现代 Xposed API 编写：

- [使用现代 Xposed API 开发模块](https://github.com/LSPosed/LSPosed/wiki/Develop-Xposed-Modules-Using-Modern-Xposed-API)
- [libxposed API 102](https://libxposed.github.io/api/)
- 工程结构对齐 [libxposed/example](https://github.com/libxposed/example)

## 解决什么问题

小米应用商店会按机型控制下载管理器是否限速。

在部分机型上，商店会把下载速度压得很低。本仓库最初针对的是 REDMI K90 Pro Max（真实型号 `25102RKBEC`，设备代号 `myron`）：商店下载明显变慢，而把商店看到的身份改成 `Xiaomi` + `madrid` + `2611FPNFAG` 后，限速会解除。

这不是全局改机。商店会读取当前进程里的 `Build` 字段和 `ro.product.*` 系统属性，再据此决定要不要对下载管理器限速。本模块只改商店进程里的这些值，让商店把自己当成小米 18 Pro Max，从而避开按型号下发的下载限速策略。

本模块：

- 只作用于 `com.xiaomi.market`
- 不改系统全局机型，不影响其他应用
- 不改指纹、不改 MIUI 版本号
- 不碰商店服务端，也不改下载协议

## 环境要求

- 支持现代 Xposed API 102 的 LSPosed（Zygisk / KernelSU）
- Android 8.0+（`minSdk 26`）
- 推荐作用域只勾选 `com.xiaomi.market`（模块已设置 `staticScope=true`）

## 安装

1. 从 [Latest Release](https://github.com/FlyRenxing/xiaomi-market-spoof/releases/latest) 下载 APK，或自行编译。每次推送到 `main` 都会覆盖更新这个发布。
2. 安装 APK。
3. 打开 LSPosed，启用 **小米应用商店伪装**。
4. 作用域保持为 `com.xiaomi.market`。
5. 强行停止小米应用商店，再打开商店重新下载。

启用后如果限速还在，先强停商店再试；本机若还有其他模块也勾了商店，可先关掉对比。

## 伪装了什么

只在商店进程内处理：

- `android.os.Build` 字段：`BRAND`、`MANUFACTURER`、`DEVICE`、`PRODUCT`、`MODEL`
- `android.os.SystemProperties.get(String)` / `get(String, String)`，覆盖
  `ro.product.*` 的 brand / manufacturer / device / name / model，
  以及 `ro.build.product`、`ro.product.mod_device`
- 进程里如果存在 `miui.os.Build`，也同步改对应字段

不会进入 `system_server`，不会改 fingerprint，也不会改 MIUI 版本属性。

伪装目标：

| 项目 | 值 |
| --- | --- |
| 厂商 / 品牌 | `Xiaomi` |
| 设备代号 / 产品名 | `madrid` |
| 型号 | `2611FPNFAG` |

## 编译

Gradle Wrapper、AGP 和 libxposed 版本跟随官方示例：

- Android Gradle Plugin `9.2.1`
- Gradle `9.5.1`
- `compileOnly("io.github.libxposed:api:102.0.0")`
- `compileSdk` / `targetSdk` `37`，Java 21

```bash
export ANDROID_HOME=/path/to/android-sdk
./gradlew :app:assembleRelease
```

Release APK 默认用 Android debug 密钥签名，方便本机测试。对外正式发布前请更换 `signingConfig`。

### SDK 说明

Google 当前把 API 37 发成 `platforms;android-37.0`。AGP 仍然要求 `compileSdk = 37`，必要时需要做符号链接：

```text
platforms;android-37.0
build-tools;37.0.0
ln -s android-37.0 $ANDROID_HOME/platforms/android-37
```

Linux aarch64 上，Google 的 `build-tools` 原生文件仍是 x86_64。需要自行放入 arm64 的 `aapt2` / `aidl` / `zipalign`（例如 [Commit451/android-arm-build-tools](https://github.com/Commit451/android-arm-build-tools)）。AGP 9.x 会从 Maven 拉 aapt2，因此要在本机 `~/.gradle/gradle.properties` 里写：

```properties
android.aapt2FromMavenOverride=/path/to/sdk/build-tools/37.0.0/aapt2
```

不要把这条写进仓库。GitHub Actions 跑在 x86_64 上，不需要这项覆盖。

## 工程结构

```text
app/src/main/java/io/github/flyrenxing/marketspoof/MarketSpoofModule.java
app/src/main/resources/META-INF/xposed/java_init.list
app/src/main/resources/META-INF/xposed/module.prop
app/src/main/resources/META-INF/xposed/scope.list
```

本仓库用到的现代 API 约定：

- 入口类继承 `io.github.libxposed.api.XposedModule`，并提供无参构造
- `onModuleLoaded()` 之前不做初始化
- 使用 `META-INF/xposed/*`，不再使用 `assets/xposed_init` 和旧 metadata
- `minApiVersion` / `targetApiVersion` = 102
- Hook 写法：`hook(executable).setId(...).intercept(chain -> ...)`
- 目标 API 是 102，因此不会调用旧的 `de.robv.android.xposed`

## 提交到 LSPosed 模块仓库

`lsposed-repo/` 里是给 [Xposed-Modules-Repo](https://github.com/Xposed-Modules-Repo/submission) 用的说明文件，不要把源码推进那个仓库。

1. 用 GitHub 账号开 Issue，标题为 `[submission] io.github.flyrenxing.marketspoof`
2. 接受机器人邀请后，把 `SUMMARY`、`README.md`、`SCOPE`、`SOURCE_URL` 推到官方仓库
3. 在网页上填写仓库 Description 为「小米应用商店伪装」
4. 发布带 APK 的 Release，tag 形如 `5-1.1.0`

## 声明

这是个人开源的本机辅助模块，用来处理小米应用商店按机型限制下载管理器速度的问题。它不是小米官方产品。请只在自己的设备上使用。不要用来冒充他人、绕过付费，或攻击商店基础设施。

## 许可证

Apache License 2.0
