package com.lumokato.nunulo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
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
