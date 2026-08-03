# nunulo-android

Nunulo Android 日常客户端，使用 Kotlin、Jetpack Compose、高德地图 SDK 和系统照片选择器。

## 当前状态

- 当前生产 API：`https://nunulo.lumokato.com`。
- 当前版本：`0.1.0` debug 工程原型。
- 当前里程碑：先在模拟器完成登录、单张照片上传、首页/详情回看、编辑和删除，再做物理设备 release 验收。
- Android namespace、applicationId 和 Kotlin 包路径统一使用 `com.lumokato.nunulo`，不保留旧包升级兼容。

## 本地构建

从 `android` 目录使用仓库固定的 Gradle Wrapper：

```powershell
.\gradlew.bat :app:assembleDebug
```

高德原生 Android Key 通过 Gradle 属性、环境变量或未提交的 `local.properties` 提供：

```properties
AMAP_ANDROID_KEY_DEBUG=<绑定 com.lumokato.nunulo 与 debug SHA-1 的 Android Key>
AMAP_ANDROID_KEY_RELEASE=<绑定 com.lumokato.nunulo 与 release SHA-1 的 Android Key>
```

`AMAP_ANDROID_KEY` 只作为 debug 的便捷别名；release 构建必须显式提供 `AMAP_ANDROID_KEY_RELEASE`。Web/JS Key 不会被读取。没有 Key 时应用使用地点名和坐标回退，不显示伪地图。真实凭据、APK、照片和设备数据不进入仓库。

产品与验收状态以 `C:/Dev/Nunulo/nunulo-docs/docs/主线/当前真实状态.md` 为准。
