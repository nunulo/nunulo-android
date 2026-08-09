# nunulo-android

Nunulo Android 日常客户端，使用 Kotlin、Jetpack Compose、高德地图 SDK 和系统照片选择器。

## 当前状态

- 邀请制多人测试环境已上线，当前 Android 预发布版本为 `v0.2.0-test.1`；尚未公开正式发布，历史 APK 不代表当前版本。
- Release APK 已重新下载并核对 SHA-256；当前没有连接的物理 Android 设备，拍照、定位、断网与杀进程验收仍未完成。
- 构建默认使用 `https://nunulo.lumokato.com`；需要切换本地或其他测试环境时，通过 Gradle 属性、环境变量或未提交的 `local.properties` 显式提供 `NUNULO_API_BASE_URL`。
- 当前版本身份为 `0.2.0-test.1`（`versionCode=3`），目标是邀请制多人测试；历史模拟器闭环不再证明本轮重构后的运行结果。
- 当前里程碑：真实 PostGIS CI、外部四角色多人链路、并发与 R2 数据链已通过；剩余重点是物理设备拍照、定位、断网与进程恢复验收。
- Android namespace、applicationId 和 Kotlin 包路径统一使用 `com.lumokato.nunulo`，不保留旧包升级兼容。

## 本地构建

从 `android` 目录使用仓库固定的 Gradle Wrapper：

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

网络请求、HTTP 状态错误和 refresh token 协调已从 Compose 页面中提取为可单测边界；401 使用明确状态码判断，并发失效请求只执行一次 refresh，非鉴权失败不会被误重试。

Android 现有五个入口：动态、地图、登记、消息、我的。动态支持全部、关注、公开、我的四种范围；登记和编辑支持仅自己、关注者、所有测试成员三档可见性；详情支持点赞、评论、举报、本人编辑删除和加入合集。消息显示未读数量和测试成员，个人页提供存储额度、合集、数据导出下载分享和个人邀请码。地图始终只显示本人记录。

高德原生 Android Key 通过 Gradle 属性、环境变量或未提交的 `local.properties` 提供：

```properties
AMAP_ANDROID_KEY_DEBUG=<绑定 com.lumokato.nunulo 与 debug SHA-1 的 Android Key>
AMAP_ANDROID_KEY_RELEASE=<绑定 com.lumokato.nunulo 与 release SHA-1 的 Android Key>
NUNULO_API_BASE_URL=<当前测试 API 基址>
```

debug 与 release 构建必须分别显式提供对应的 Android Key。Web/JS Key 不会被读取。没有 Key 时应用显示明确的地图不可用状态，不伪造地图。真实凭据、APK、照片和设备数据不进入仓库。

个人 release 使用稳定的项目外 keystore，通过以下 Gradle 属性或环境变量注入：

```properties
NUNULO_RELEASE_KEYSTORE_PATH=<keystore 路径>
NUNULO_RELEASE_STORE_PASSWORD=<store password>
NUNULO_RELEASE_KEY_ALIAS=<alias>
NUNULO_RELEASE_KEY_PASSWORD=<key password>
```

测试包不承担向后升级兼容义务。release 必须使用独立、稳定的项目签名，并为该签名创建对应的高德 Android Key；不得为了覆盖历史 debug 安装而复用 debug 证书。

推送 `v*` tag 时，GitHub Actions 在测试、签名、包名、版本号和地图 Key 校验通过后创建预发布 Release，并上传固定名称 `nunulo-android.apk` 与 `nunulo-android.sha256`。普通分支推送只保留 CI artifact，不会自动创建公开下载。

选择或拍摄的照片会复制到应用私有目录并立即保存草稿。应用被强停或上传中断后会恢复同一照片、地点、坐标、标签、备注和请求号；重复登记由 API 幂等保护。用户可在登记页手工修改经纬度或主动清除草稿。

产品与验收状态以 `C:/Dev/Nunulo/nunulo-docs/docs/主线/当前真实状态.md` 为准。
