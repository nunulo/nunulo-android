package com.lumokato.nunulo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest

@Composable
private fun ScreenshotFrame(tab: AppTab, content: @Composable () -> Unit) {
    NunuloTheme {
        Scaffold(topBar = { NunuloTopBar(tab, "", false, 0, {}, {}) }) { padding ->
            Surface(color = NunuloColors.Background, modifier = Modifier.padding(padding).fillMaxSize()) {
                content()
            }
        }
    }
}

@PreviewTest
@Preview(name = "capture_empty_360x800", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
fun CaptureEmptyScreenshot() = ScreenshotFrame(AppTab.Capture) {
    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("照片", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text("先选好这次记录的 1–9 张照片", color = NunuloColors.Muted)
                CaptureStepIndicator(CaptureStep.Photos)
            }
        }
        item {
            SectionCard("实时记录", "相机优先推荐 1 张；历史相册可选择 1–9 张，首图即封面。") {
                CapturePhotoEmptyState(onCamera = {})
            }
        }
    }
}

@PreviewTest
@Preview(name = "catalog_no_result_393x852", widthDp = 393, heightDp = 852, showBackground = true)
@Composable
fun CatalogNoResultScreenshot() = ScreenshotFrame(AppTab.Partners) {
    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            SectionCard("作品、组合与角色", "可搜索正式名、日文、罗马字和必要别名。") {
                CatalogSelection(
                    title = "角色",
                    items = emptyList(),
                    selected = emptyList(),
                    onToggle = {},
                    onNoResult = {},
                    initialQuery = "不存在的角色",
                )
                Text("候选会进入待审核状态，正式归并由 Web Admin 完成。", color = NunuloColors.Muted)
            }
        }
    }
}

@PreviewTest
@Preview(name = "feed_offline_360x800", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
fun FeedOfflineScreenshot() = ScreenshotFrame(AppTab.Feed) {
    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { FeedSyncFailure("无法连接到服务", hasCachedContent = false, onRetry = {}) }
    }
}

@PreviewTest
@Preview(name = "capture_partial_failure_393x852", widthDp = 393, heightDp = 852, showBackground = true)
@Composable
fun CapturePartialFailureScreenshot() = ScreenshotFrame(AppTab.Capture) {
    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            SectionCard("照片", "上传中断不会丢掉已完成的图片。") {
                DraftRecoverySummary(
                    photos = listOf(
                        DraftPhotoItem(key = "ready-1", photo = PhotoItem("photo-1", contentSha256 = "sha-1"), status = "ready", progress = 100),
                        DraftPhotoItem(key = "ready-2", photo = PhotoItem("photo-2", contentSha256 = "sha-2"), status = "ready", progress = 100),
                        DraftPhotoItem(key = "failed", status = "error", error = "网络连接已中断"),
                    ),
                    onRetryFailed = {},
                )
                Text("第 3 张 · 网络连接已中断", color = NunuloColors.Danger, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@PreviewTest
@Preview(name = "capture_location_unavailable_360x800", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
fun CaptureLocationUnavailableScreenshot() = ScreenshotFrame(AppTab.Capture) {
    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            SectionCard("地点", "照片没有 GNSS，设备定位权限也尚未开启。") {
                LocationChoiceState(
                    hasCoordinates = false,
                    mapOpen = false,
                    onDeviceLocation = {},
                    onToggleMap = {},
                    onClear = {},
                )
            }
        }
    }
}

@PreviewTest
@Preview(name = "feed_real_content_393x852", widthDp = 393, heightDp = 852, showBackground = true)
@Composable
fun FeedRealContentScreenshot() = ScreenshotFrame(AppTab.Feed) {
    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            FeedRecordCard(
                record = screenshotRecord(),
                onOpen = {},
                onLike = {},
                image = { ScreenshotMedia("3 图") },
            )
        }
    }
}

@PreviewTest
@Preview(name = "partner_long_content_393x852", widthDp = 393, heightDp = 852, showBackground = true)
@Composable
fun PartnerLongContentScreenshot() = ScreenshotFrame(AppTab.Partners) {
    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            SectionCard("我的伙伴", "长名称仍需保持可读，不挤掉稳定编号和记录状态。") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PartnerSummaryCard(
                        partner = screenshotPartner(),
                        onOpen = {},
                        image = { ScreenshotMedia("伙伴封面") },
                    )
                }
            }
        }
    }
}

@PreviewTest
@Preview(name = "profile_member_393x852", widthDp = 393, heightDp = 852, showBackground = true)
@Composable
fun ProfileMemberScreenshot() = ScreenshotFrame(AppTab.Profile) {
    Surface(color = NunuloColors.Paper, modifier = Modifier.padding(12.dp)) {
        ProfileIdentity(
            user = screenshotUser(roles = listOf("member")),
            onPickAvatar = {},
            onLogout = {},
            onOpenAdmin = {},
            avatar = { ScreenshotAvatar() },
        )
    }
}

@PreviewTest
@Preview(name = "profile_owner_393x852", widthDp = 393, heightDp = 852, showBackground = true)
@Composable
fun ProfileOwnerScreenshot() = ScreenshotFrame(AppTab.Profile) {
    Surface(color = NunuloColors.Paper, modifier = Modifier.padding(12.dp)) {
        ProfileIdentity(
            user = screenshotUser(roles = listOf("member", "owner")),
            onPickAvatar = {},
            onLogout = {},
            onOpenAdmin = {},
            avatar = { ScreenshotAvatar() },
        )
    }
}

@Composable
private fun ScreenshotAvatar() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("旅", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
    }
}

private fun screenshotUser(roles: List<String>) = AuthUser(
    id = 1,
    displayName = "旅行收藏者",
    username = "nunulo_member",
    email = null,
    roles = roles,
    storageUsageBytes = 0,
    storageQuotaBytes = 2_147_483_648,
    avatarUrl = null,
)

@Composable
private fun ScreenshotMedia(label: String) {
    Box(
        Modifier.fillMaxWidth().aspectRatio(1.04f).background(NunuloColors.Placeholder),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = NunuloColors.Muted) }
}

private fun screenshotRecord() = CheckinItem(
    id = "record-1",
    userId = 8,
    authorName = "带着伙伴去看演出的旅行收藏者",
    photos = listOf(PhotoItem("p1"), PhotoItem("p2"), PhotoItem("p3")),
    placeName = "北京工人体育场北门外场区域",
    note = "第一次带着伙伴一起看 MyGO!!!!! 的现场。散场以后还遇到了同好，补了一张很喜欢的合照。",
    latitude = 39.9305,
    longitude = 116.4469,
    createdAt = "2026-08-13T12:30:00Z",
    takenAt = "2026-08-13T12:10:00Z",
    source = "android_capture",
    visibility = "public",
    worldVisible = true,
    itemTypes = listOf(CatalogRef("cotton-doll", "棉花娃娃")),
    works = listOf(CatalogRef("bandori", "BanG Dream!")),
    characters = listOf(CatalogRef("tomori", "高松灯")),
    partners = listOf(screenshotPartner()),
    events = listOf(
        EventItem("event-1", "MyGO!!!!! 7th LIVE「こたえなんてなくても」", "offline", "public", "active", true, null, null, null, null, "", 18, false)
    ),
    liked = true,
    likeCount = 28,
    commentCount = 6,
)

private fun screenshotPartner() = PartnerItem(
    id = "partner-1",
    publicCode = "N-2026-08-000128",
    ownerUserId = 8,
    name = "去过很多次演出现场的高松灯棉花娃娃",
    visibility = "public",
    moderationStatus = "active",
    itemType = CatalogRef("cotton-doll", "棉花娃娃"),
    work = CatalogRef("bandori", "BanG Dream! 少女乐团派对！"),
    character = CatalogRef("tomori", "高松灯 / 高松 燈 / Takamatsu Tomori"),
    coverUrl = null,
    recordCount = 42,
    canEdit = true,
)
