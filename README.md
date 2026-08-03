# nunulo-android

Nunulo Android 日常客户端，使用 Kotlin、Jetpack Compose、高德地图 SDK 和系统照片选择器。

## 当前状态

- 当前生产 API：`https://nunulo.lumokato.com`。
- 当前版本：`0.1.0` debug 工程原型。
- 当前里程碑：在物理设备完成登录、单张照片上传、首页/详情回看、编辑和删除。
- `com.lumokato.dollcheckin` 暂作为兼容 applicationId 和包路径保留；正式发布前另行执行受控改名，不在普通整理中破坏升级路径。

## 本地构建

从 `android` 目录使用仓库固定的 Gradle Wrapper：

```powershell
.\gradlew.bat :app:assembleDebug
```

高德 Key 通过 Gradle 属性、环境变量或未提交的 `local.properties` 提供。真实凭据、APK、照片和设备数据不进入仓库。

产品与验收状态以 `C:/Dev/Nunulo/nunulo-docs/docs/主线/当前真实状态.md` 为准。
