# nunulo-android

Nunulo Android 日常客户端，使用 Kotlin、Jetpack Compose、高德地图 SDK 和系统照片选择器。

## 当前状态

- Nunulo 邀请制服务已上线，当前稳定版本为 `v0.2.1`；历史 APK 不代表当前版本。
- `v0.2.0-test.1` 的地图空状态错误写死上海，且必须在登记页手动点击定位；该版本已被当前修复取代，不能继续作为地图与定位验收基线。
- 构建默认使用 `https://nunulo.lumokato.com`；需要切换本地或其他测试环境时，通过 Gradle 属性、环境变量或未提交的 `local.properties` 显式提供 `NUNULO_API_BASE_URL`。
- 当前版本身份为 `0.2.1`（`versionCode=6`）；物理设备上的原生地图、拍照和断网恢复仍需持续回归。
- 当前里程碑：真实 PostGIS CI、外部四角色多人链路、并发、R2 数据链与 Android 模拟器闭环已通过；继续完善物理设备拍照、定位、断网与进程恢复回归。
- Android namespace、applicationId 和 Kotlin 包路径统一使用 `com.lumokato.nunulo`，不保留旧包升级兼容。

## 本地构建

从 `android` 目录使用仓库固定的 Gradle Wrapper：

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

网络请求、HTTP 状态错误和 refresh token 协调已从 Compose 页面中提取为可单测边界；401 使用明确状态码判断，并发失效请求只执行一次 refresh，非鉴权失败不会被误重试。

Android 现有五个入口：动态、地图、登记、消息、我的。动态支持全部、关注、公开、我的四种范围；登记和编辑支持仅自己、关注者、所有成员三档可见性；详情支持点赞、评论、举报、本人编辑删除和加入合集。消息显示未读数量和成员，个人页提供存储额度、合集、数据导出下载分享和个人邀请码。地图始终只显示本人记录。

进入地图页会自动请求定位并显示当前位置；进入登记页且草稿坐标为空时会自动填入设备坐标，定位按钮保留为重新定位入口。空记录地图不再移动到任何写死城市。x86_64 模拟器不支持当前高德原生 SDK 时只显示真实坐标列表，不绘制伪造底图或随机点；高德原生地图仍必须在受支持的 ARM Android 设备验收。

高德原生 Android Key 通过 Gradle 属性、环境变量或未提交的 `local.properties` 提供：

```properties
AMAP_ANDROID_KEY_DEBUG=<绑定 com.lumokato.nunulo 与 debug SHA-1 的 Android Key>
AMAP_ANDROID_KEY_RELEASE=<绑定 com.lumokato.nunulo 与 release SHA-1 的 Android Key>
NUNULO_API_BASE_URL=<API 基址>
```

debug 与 release 构建必须分别显式提供对应的 Android Key。Web/JS Key 不会被读取。没有 Key 时应用显示明确的地图不可用状态，不伪造地图。真实凭据、APK、照片和设备数据不进入仓库。

个人 release 使用稳定的项目外 keystore，通过以下 Gradle 属性或环境变量注入：

```properties
NUNULO_RELEASE_KEYSTORE_PATH=<keystore 路径>
NUNULO_RELEASE_STORE_PASSWORD=<store password>
NUNULO_RELEASE_KEY_ALIAS=<alias>
NUNULO_RELEASE_KEY_PASSWORD=<key password>
```

当前稳定版不承担早期测试 APK 的原位升级兼容义务。release 必须使用独立、稳定的项目签名，并为该签名创建对应的高德 Android Key；不得为了覆盖历史 debug 安装而复用 debug 证书。

推送与应用版本一致的 `v*` tag 时，GitHub Actions 在测试、签名、包名、版本号和地图 Key 校验通过后创建正式 Release，并上传固定名称 `nunulo-android.apk` 与 `nunulo-android.sha256`。普通分支推送只保留 CI artifact，不会自动创建公开下载。

选择或拍摄的照片会复制到应用私有目录并立即保存草稿。应用被强停或上传中断后会恢复同一照片、地点、坐标、标签、备注和请求号；重复登记由 API 幂等保护。用户可在登记页手工修改经纬度或主动清除草稿。

产品与验收状态以 `C:/Dev/Nunulo/nunulo-docs/docs/主线/当前真实状态.md` 为准。
