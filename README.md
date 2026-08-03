# nunulo-android

Nunulo Android 日常客户端，使用 Kotlin、Jetpack Compose、高德地图 SDK 和系统照片选择器。

## 当前状态

- 当前生产 API：`https://nunulo.lumokato.com`。
- 当前版本：`0.1.0` debug 个人测试版。
- 模拟器已完成登录、选图预览、北京站定位、手工坐标、上传进度、中断草稿恢复、幂等重试、私密图片读取、编辑和删除闭环。
- 当前里程碑：建立正式 release keystore，并在物理 ARM 设备验收高德原生地图、拍照、大图和可升级 release APK。
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

选择或拍摄的照片会复制到应用私有目录并立即保存草稿。应用被强停或上传中断后会恢复同一照片、地点、坐标、标签、备注和请求号；重复登记由 API 幂等保护。用户可在登记页手工修改经纬度或主动清除草稿。

产品与验收状态以 `C:/Dev/Nunulo/nunulo-docs/docs/主线/当前真实状态.md` 为准。
