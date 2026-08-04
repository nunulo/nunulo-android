# nunulo-android

Nunulo Android 日常客户端，使用 Kotlin、Jetpack Compose、高德地图 SDK 和系统照片选择器。

## 当前状态

- 当前生产 API：`https://nunulo.lumokato.com`。
- 当前版本：`0.1.0-personal.2` 个人签名测试版，`versionCode=2`。
- 模拟器已完成登录、选图预览、北京站定位、手工坐标、上传进度、中断草稿恢复、幂等重试、私密图片读取、编辑和删除闭环。
- 当前里程碑：在物理 ARM 设备验收高德原生地图、拍照、大图和可升级 release APK。
- Android namespace、applicationId 和 Kotlin 包路径统一使用 `com.lumokato.nunulo`，不保留旧包升级兼容。

## 本地构建

从 `android` 目录使用仓库固定的 Gradle Wrapper：

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

网络请求、HTTP 状态错误和 refresh token 协调已从 Compose 页面中提取为可单测边界；401 使用明确状态码判断，并发失效请求只执行一次 refresh，非鉴权失败不会被误重试。

高德原生 Android Key 通过 Gradle 属性、环境变量或未提交的 `local.properties` 提供：

```properties
AMAP_ANDROID_KEY_DEBUG=<绑定 com.lumokato.nunulo 与 debug SHA-1 的 Android Key>
AMAP_ANDROID_KEY_RELEASE=<绑定 com.lumokato.nunulo 与 release SHA-1 的 Android Key>
```

`AMAP_ANDROID_KEY` 只作为 debug 的便捷别名；release 构建必须显式提供 `AMAP_ANDROID_KEY_RELEASE`。Web/JS Key 不会被读取。没有 Key 时应用使用地点名和坐标回退，不显示伪地图。真实凭据、APK、照片和设备数据不进入仓库。

个人 release 使用稳定的项目外 keystore，通过以下 Gradle 属性或环境变量注入：

```properties
NUNULO_RELEASE_KEYSTORE_PATH=<keystore 路径>
NUNULO_RELEASE_STORE_PASSWORD=<store password>
NUNULO_RELEASE_KEY_ALIAS=<alias>
NUNULO_RELEASE_KEY_PASSWORD=<key password>
```

当前个人 release 沿用既有 debug 证书，SHA-1 为 `CE:D4:20:65:D8:19:9C:19:AD:84:C9:70:A4:BB:FD:96:DB:37:7C:10`，用于保证已安装 debug 包可直接升级，并匹配当前高德 Android Key。以后如切换新的正式发布证书，必须同时创建新的高德 Android Key，不能静默替换签名身份。

选择或拍摄的照片会复制到应用私有目录并立即保存草稿。应用被强停或上传中断后会恢复同一照片、地点、坐标、标签、备注和请求号；重复登记由 API 幂等保护。用户可在登记页手工修改经纬度或主动清除草稿。

产品与验收状态以 `C:/Dev/Nunulo/nunulo-docs/docs/主线/当前真实状态.md` 为准。
