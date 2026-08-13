# nunulo-android

Nunulo Android 日常客户端，使用 Kotlin、Jetpack Compose、高德地图 SDK 和系统照片选择器。

## 当前状态

- Nunulo 公开稳定服务已上线，当前正式版本为 `v0.2.5`；历史 APK 不代表当前版本。
- `main` 正在形成第一期 `v0.3.0` 候选客户端；尚未创建正式 tag 或 Release，线上 APK 仍是 `v0.2.5`。
- `v0.2.0-test.1` 的地图空状态错误写死上海，且必须在登记页手动点击定位；该版本已被当前修复取代，不能继续作为地图与定位验收基线。
- 构建默认使用 `https://nunulo.lumokato.com`；需要切换本地或其他测试环境时，通过 Gradle 属性、环境变量或未提交的 `local.properties` 显式提供 `NUNULO_API_BASE_URL`。
- 当前候选版本身份为 `0.3.0`（`versionCode=11`）；首次安装或会话失效时直接进入登录页，支持公开注册或邀请码注册。Linux 自动测试、构建与界面回归是本轮门禁；物理设备上的原生地图、拍照和断网恢复保留为后续 L4 证据。
- 当前里程碑：真实 PostGIS CI、外部四角色多人链路、并发、R2 数据链与 Android 模拟器闭环已通过；继续完善物理设备拍照、定位、断网与进程恢复回归。
- Android namespace、applicationId 和 Kotlin 包路径统一使用 `com.lumokato.nunulo`，不保留旧包升级兼容。

## 本地构建

从 `android` 目录使用仓库固定的 Gradle Wrapper：

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

网络请求、HTTP 状态错误和 refresh token 协调已从 Compose 页面中提取为可单测边界；401 使用明确状态码判断，并发失效请求只执行一次 refresh，非鉴权失败不会被误重试。

第一期本地客户端使用动态、发现、记录、伙伴、我的五个入口，通知位于顶栏。动态提供热门 / 最新与关注范围；发现聚合物件类型、作品、角色、活动、专题和达到阈值的世界地区，并按需打开地区地图；伙伴页支持稳定编号、跨用户补登记确认和相遇关系。详情继续支持点赞、评论、举报、本人编辑删除和加入合集。

记录支持应用内相机和系统相册 1–9 图，实时拍摄默认推荐 1 张；每张图先计算 SHA-256 并独立上传，失败可逐图重试，成功图片可重排封面。照片 EXIF/GNSS 优先于设备 GPS；仍无位置时可地图补点或无地点发布。记录可关联多个伙伴、物件类型、作品、角色、可选活动和单一地点，不保留旧单图 / 自由 tag 协议。

个人足迹显示本人家位置和去过的地点；世界入口默认先展示地区卡片，只有用户主动打开时才加载地图。x86_64 或缺少高德 Android Key 时，足迹、世界地区和地点补录都只显示真实坐标与明确说明，不初始化不受支持的原生地图，也不绘制伪造底图或随机点。

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

选择或拍摄的照片会复制到应用私有目录并立即保存草稿。应用被强停或上传中断后会恢复照片顺序、成功/失败状态、地点、坐标、类别、伙伴、活动、备注和请求号；已成功照片不会重传，中断中的照片会变为可重试状态，不可恢复的本地空照片不会残留。重复登记由 API 幂等与照片 checksum 复用共同保护。用户可在登记页手工修改经纬度或主动清除草稿。

Linux 界面回归直接渲染生产 Compose 组件，覆盖记录空态、部分上传失败、无定位、目录无结果、动态离线、真实长内容动态、长伙伴资料，以及普通成员和 Owner 角色边界：

```powershell
.\gradlew.bat :app:validateDebugScreenshotTest
```

产品与验收状态以 `C:/Dev/Nunulo/nunulo-docs/docs/主线/当前真实状态.md` 为准。
