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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import java.time.Instant

@Composable
private fun ScreenshotFrame(tab: AppTab, content: @Composable () -> Unit) {
    NunuloTheme {
        Scaffold(
            topBar = { NunuloTopBar(tab, false, 0, {}, {}) },
            bottomBar = { NunuloBottomBar(tab, {}) },
        ) { padding ->
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
@Preview(name = "collection_partial_failure_393x852", widthDp = 393, heightDp = 852, showBackground = true)
@Composable
fun CollectionPartialFailureScreenshot() = ScreenshotFrame(AppTab.Feed) {
    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            SectionCard("BanG Dream! 少女乐团派对！", "作品聚合") {
                Text("已加载 1 条记录", color = NunuloColors.Muted)
            }
        }
        item {
            DetailLoadState(
                title = "聚合记录没有加载完整",
                detail = "只显示当前聚合中的记录，不会混入上一页动态。",
                loading = false,
                error = "2 条记录暂时无法读取；已显示其余内容",
                onRetry = {},
            )
        }
        item {
            FeedRecordCard(
                record = screenshotRecord(),
                onOpen = {},
                onLike = {},
                image = { ScreenshotMedia("当前作品记录") },
            )
        }
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
@Preview(name = "feed_interaction_submitting_360x800", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
fun FeedInteractionSubmittingScreenshot() = ScreenshotFrame(AppTab.Feed) {
    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            FeedRecordCard(
                record = screenshotRecord(),
                onOpen = {},
                onLike = {},
                liking = true,
                image = { ScreenshotMedia("互动中的记录") },
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
@Preview(name = "partner_directory_393x852", widthDp = 393, heightDp = 852, showBackground = true)
@Composable
fun PartnerDirectoryScreenshot() = ScreenshotFrame(AppTab.Partners) {
    val partners = listOf(
        screenshotPartner().copy(name = "去过很多次演出的高松灯棉花娃娃", publicCode = "N-000042", recordCount = 18),
        screenshotPartner().copy(id = "partner-2", name = "小彩", publicCode = "N-000017", character = CatalogRef("aya", "丸山彩"), recordCount = 7),
    )
    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            PartnerDirectoryHeader(
                total = partners.size,
                visible = partners.size,
                query = "",
                visibility = PartnerVisibilityFilter.All,
                onQueryChange = {},
                onVisibilityChange = {},
                onCreate = {},
            )
        }
        items(partners, key = PartnerItem::id) { partner ->
            PartnerDirectoryCard(partner, onOpen = {}, image = { ScreenshotMedia("伙伴封面") })
        }
    }
}

@PreviewTest
@Preview(name = "partner_request_states_393x852", widthDp = 393, heightDp = 852, showBackground = true)
@Composable
fun PartnerRequestStatesScreenshot() = ScreenshotFrame(AppTab.Partners) {
    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("待确认补登记", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text("只在轮到你决定时显示操作；确认过的申请会清楚说明正在等谁。", color = NunuloColors.Muted)
            }
        }
        item {
            PartnerRequestCard(
                request = screenshotPartnerRequest(authorUserId = 7, ownerUserId = 42, authorApproved = true, ownerApproved = false),
                viewerUserId = 42,
                resolving = false,
                onApprove = {},
                onReject = {},
            )
        }
        item {
            PartnerRequestCard(
                request = screenshotPartnerRequest(authorUserId = 42, ownerUserId = 9, authorApproved = true, ownerApproved = false),
                viewerUserId = 42,
                resolving = false,
                onApprove = {},
                onReject = {},
            )
        }
    }
}

@PreviewTest
@Preview(name = "partner_relation_privacy_393x852", widthDp = 393, heightDp = 852, showBackground = true)
@Composable
fun PartnerRelationPrivacyScreenshot() = ScreenshotFrame(AppTab.Feed) {
    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            SectionCard("出镜伙伴", "每段出镜关系有独立的公开范围，不会改变伙伴资料本身的隐私。") {
                PartnerRelationCard(
                    partner = screenshotPartner().copy(relationVisibility = "public"),
                    recordAuthorUserId = 8,
                    viewerUserId = 8,
                    mutating = false,
                    onOpen = {},
                    onChangeVisibility = {},
                    onRemove = {},
                )
                PartnerRelationCard(
                    partner = screenshotMeetingPartner().copy(relationVisibility = "private"),
                    recordAuthorUserId = 8,
                    viewerUserId = 8,
                    mutating = true,
                    onOpen = {},
                    onChangeVisibility = {},
                    onRemove = {},
                )
            }
        }
    }
}

@PreviewTest
@Preview(name = "partner_detail_393x852", widthDp = 393, heightDp = 852, showBackground = true)
@Composable
fun PartnerDetailScreenshot() = NunuloTheme {
    PartnerDetailPage(
        partner = screenshotPartner(),
        meetings = listOf(
            PartnerMeetingItem(
                partner = screenshotMeetingPartner(),
                meetingCount = 3,
                firstMetAt = "2026-06-21T10:00:00Z",
                lastMetAt = "2026-08-13T12:10:00Z",
            ),
        ),
        onClose = {},
        onOpenRecords = {},
        onEdit = {},
        onDelete = {},
        onSelectMeeting = {},
        image = { ScreenshotMedia("伙伴在演出现场的封面照片") },
    )
}

@PreviewTest
@Preview(name = "partner_detail_loading_393x852", widthDp = 393, heightDp = 852, showBackground = true)
@Composable
fun PartnerDetailLoadingScreenshot() = NunuloTheme {
    PartnerDetailPage(
        partner = screenshotPartner(),
        meetings = emptyList(),
        detailLoading = true,
        meetingsLoading = true,
        onClose = {},
        onOpenRecords = {},
        onEdit = {},
        onDelete = {},
        onSelectMeeting = {},
        image = { ScreenshotMedia("伙伴摘要封面") },
    )
}

@PreviewTest
@Preview(name = "partner_detail_failure_393x852", widthDp = 393, heightDp = 852, showBackground = true)
@Composable
fun PartnerDetailFailureScreenshot() = NunuloTheme {
    PartnerDetailPage(
        partner = screenshotPartner(),
        meetings = emptyList(),
        detailError = "网络连接已中断，完整资料暂时不可用。",
        meetingsError = "相遇记录暂时无法读取。",
        onRetry = {},
        onClose = {},
        onOpenRecords = {},
        onEdit = {},
        onDelete = {},
        onSelectMeeting = {},
        image = { ScreenshotMedia("伙伴摘要封面") },
    )
}

@PreviewTest
@Preview(name = "record_comments_loading_360x800", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
fun RecordCommentsLoadingScreenshot() = ScreenshotFrame(AppTab.Feed) {
    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            SectionCard("记录回应", "完整记录仍可查看，评论独立加载。") {
                DetailLoadState(
                    title = "正在加载评论",
                    detail = "评论加载完成前不会把它误显示成空列表。",
                    loading = true,
                    error = null,
                    onRetry = {},
                )
            }
        }
    }
}

@PreviewTest
@Preview(name = "record_comments_failure_360x800", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
fun RecordCommentsFailureScreenshot() = ScreenshotFrame(AppTab.Feed) {
    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            SectionCard("记录回应", "完整记录仍可查看，评论独立加载。") {
                DetailLoadState(
                    title = "评论没有加载成功",
                    detail = "现有记录内容仍可继续查看。",
                    loading = false,
                    error = "当前网络不可用，请稍后重试。",
                    onRetry = {},
                )
            }
        }
    }
}

@PreviewTest
@Preview(name = "comment_owner_actions_360x800", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
fun CommentOwnerActionsScreenshot() = ScreenshotFrame(AppTab.Feed) {
    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("评论", style = MaterialTheme.typography.titleLarge) }
        item {
            CommentCard(
                comment = CommentItem(
                    id = "comment-own",
                    userId = 8,
                    displayName = "旅行收藏者",
                    body = "灯灯也来过这里！下次一起带伙伴去看现场。",
                    createdAt = "2026-08-15T01:00:00Z",
                ),
                canDelete = true,
                deleting = false,
                onDelete = {},
            )
        }
        item {
            CommentCard(
                comment = CommentItem(
                    id = "comment-other",
                    userId = 9,
                    displayName = "小灯的旅行册",
                    body = "约在下一场演出见。",
                    createdAt = "2026-08-14T18:30:00Z",
                ),
                canDelete = false,
                deleting = false,
                onDelete = {},
            )
        }
    }
}

@PreviewTest
@Preview(name = "comment_delete_confirm_360x800", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
fun CommentDeleteConfirmationScreenshot() = NunuloTheme {
    CommentDeleteConfirmationDialog(deleting = false, onConfirm = {}, onDismiss = {})
}

@PreviewTest
@Preview(name = "profile_member_393x852", widthDp = 393, heightDp = 852, showBackground = true)
@Composable
fun ProfileMemberScreenshot() = ScreenshotFrame(AppTab.Profile) {
    Surface(color = NunuloColors.Paper, modifier = Modifier.padding(12.dp)) {
        ProfileIdentity(
            user = screenshotUser(roles = listOf("member")),
            recordCount = 42,
            partnerCount = 3,
            placeCount = 8,
            onPickAvatar = {},
            onEditProfile = {},
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
            recordCount = 42,
            partnerCount = 3,
            placeCount = 8,
            onPickAvatar = {},
            onEditProfile = {},
            onLogout = {},
            onOpenAdmin = {},
            avatar = { ScreenshotAvatar() },
        )
    }
}

@PreviewTest
@Preview(name = "profile_editor_393x852", widthDp = 393, heightDp = 852, showBackground = true)
@Composable
fun ProfileEditorScreenshot() = NunuloTheme {
    ProfileEditorPage(
        user = screenshotUser(roles = listOf("member")).copy(
            bio = "带着高松灯棉花娃娃去看每一场 MyGO!!!!! 演出，也记录散场后的城市夜景。",
        ),
        saving = false,
        onClose = {},
        onSave = { _, _ -> },
    )
}

@PreviewTest
@Preview(name = "account_settings_393x852", widthDp = 393, heightDp = 852, showBackground = true)
@Composable
fun AccountSettingsScreenshot() = ScreenshotFrame(AppTab.Profile) {
    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ProfileSectionTabs("settings", onSelect = {}) }
        item {
            AccountSettingsSection(
                user = screenshotUser(roles = listOf("member")),
                onChangePassword = {},
                onOpenTerms = {},
                onOpenPrivacy = {},
                onLogout = {},
            )
        }
    }
}

@PreviewTest
@Preview(name = "password_editor_393x852", widthDp = 393, heightDp = 852, showBackground = true)
@Composable
fun PasswordEditorScreenshot() = NunuloTheme {
    PasswordEditorPage(saving = false, onClose = {}, onSave = { _, _ -> })
}

@PreviewTest
@Preview(name = "community_browse_393x852", widthDp = 393, heightDp = 852, showBackground = true)
@Composable
fun CommunityBrowseScreenshot() = ScreenshotFrame(AppTab.Profile) {
    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ProfileSectionTabs("community", onSelect = {}) }
        item {
            SectionCard("社区成员", "找到正在记录同一作品、角色与旅程的人。关注后，他们的新记录会进入关注动态。") {
                androidx.compose.material3.OutlinedTextField(
                    value = "演出",
                    onValueChange = {},
                    label = { Text("搜索显示名、用户名或简介") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    androidx.compose.material3.FilterChip(selected = true, onClick = {}, label = { Text("全部成员") })
                    androidx.compose.material3.FilterChip(selected = false, onClick = {}, label = { Text("已关注") })
                    androidx.compose.material3.FilterChip(selected = false, onClick = {}, label = { Text("已屏蔽") })
                }
                Text("2 位成员", color = NunuloColors.Muted, style = MaterialTheme.typography.bodySmall)
                screenshotPeople().forEach { person ->
                    MemberSummaryCard(
                        person = person,
                        following = false,
                        onOpen = {},
                        onFollow = {},
                        onBlock = {},
                        onUnblock = {},
                        unblocking = false,
                        avatar = { ScreenshotPersonAvatar() },
                    )
                }
            }
        }
    }
}

@PreviewTest
@Preview(name = "community_blocked_393x852", widthDp = 393, heightDp = 852, showBackground = true)
@Composable
fun CommunityBlockedScreenshot() = ScreenshotFrame(AppTab.Profile) {
    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ProfileSectionTabs("community", onSelect = {}) }
        item {
            SectionCard("已屏蔽成员", "你可以随时解除屏蔽；解除后不会自动恢复关注关系。") {
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    androidx.compose.material3.FilterChip(selected = false, onClick = {}, label = { Text("全部成员") })
                    androidx.compose.material3.FilterChip(selected = false, onClick = {}, label = { Text("已关注") })
                    androidx.compose.material3.FilterChip(selected = true, onClick = {}, label = { Text("已屏蔽") })
                }
                Text("屏蔽会让双方从彼此的成员列表和动态中隐藏。", color = NunuloColors.Muted, style = MaterialTheme.typography.bodySmall)
                MemberSummaryCard(
                    person = screenshotPeople().last().copy(blocked = true, following = false),
                    following = false,
                    onOpen = {},
                    onFollow = {},
                    onBlock = {},
                    onUnblock = {},
                    unblocking = false,
                    avatar = { ScreenshotPersonAvatar() },
                )
            }
        }
    }
}

@PreviewTest
@Preview(name = "member_profile_393x852", widthDp = 393, heightDp = 852, showBackground = true)
@Composable
fun MemberProfileScreenshot() = NunuloTheme {
    MemberProfilePage(
        person = screenshotPeople().first(),
        records = listOf(screenshotRecord().copy(userId = 11, authorName = "小灯的旅行册")),
        loading = false,
        error = null,
        following = false,
        onClose = {},
        onRetry = {},
        onFollow = {},
        onBlock = {},
        onOpenRecord = {},
        onLike = {},
        isLiking = { false },
        avatar = { ScreenshotPersonAvatar() },
        recordImage = { ScreenshotMedia("3 图 · MyGO!!!!! 现场") },
    )
}

@PreviewTest
@Preview(name = "profile_data_ready_393x852", widthDp = 393, heightDp = 852, showBackground = true)
@Composable
fun ProfileDataReadyScreenshot() = ScreenshotFrame(AppTab.Profile) {
    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            DataAndInviteSection(
                exports = listOf(
                    ExportItem("export-ready", "available", "2026-08-14T12:00:00Z", "/api/exports/export-ready/download"),
                    ExportItem("export-pending", "processing", "2026-08-14T11:40:00Z", null),
                ),
                inviteCode = "NUNULO-7K3M9P",
                exportCreating = false,
                exportDownloadingId = null,
                inviteCreating = false,
                onCreateExport = {},
                onDownloadExport = {},
                onCreateInvite = {},
                onCopyInvite = {},
                onShareInvite = {},
            )
        }
    }
}

@PreviewTest
@Preview(name = "auth_login_360x800", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
fun AuthLoginScreenshot() = NunuloTheme {
    AuthScreen(
        initialLogin = "",
        config = null,
        busy = false,
        message = "",
        onLogin = { _, _ -> },
        onRegister = { _, _, _, _, _ -> },
    )
}

@PreviewTest
@Preview(name = "initial_sync_loading_360x800", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
fun InitialSyncLoadingScreenshot() = NunuloTheme {
    InitialSyncScreen(busy = true, error = null, onRetry = {}, onLogout = {})
}

@PreviewTest
@Preview(name = "initial_sync_failure_360x800", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
fun InitialSyncFailureScreenshot() = NunuloTheme {
    InitialSyncScreen(
        busy = false,
        error = "当前网络不可用，无法连接到 Nunulo 服务。",
        onRetry = {},
        onLogout = {},
    )
}

@PreviewTest
@Preview(name = "notifications_393x852", widthDp = 393, heightDp = 852, showBackground = true)
@Composable
fun NotificationsScreenshot() = NunuloTheme {
    NotificationsPage(
        notifications = listOf(
            NotificationItem(
                id = "notification-1",
                title = "记录收到评论",
                body = "灯灯也来过这里！下次一起带伙伴去看现场。",
                targetType = "checkin",
                targetId = "record-1",
                readAt = null,
                createdAt = "2026-08-14T12:30:00Z",
            ),
            NotificationItem(
                id = "notification-2",
                title = "伙伴补登记待确认",
                body = "另一位成员在合照中登记了你的伙伴，请确认是否共同出现。",
                targetType = "checkin_partner",
                targetId = "partner-1",
                readAt = null,
                createdAt = "2026-08-14T10:00:00Z",
            ),
            NotificationItem(
                id = "notification-3",
                title = "账号安全提醒",
                body = "你的密码已经更新，旧会话已失效。",
                targetType = "user",
                targetId = "1",
                readAt = "2026-08-13T18:00:00Z",
                createdAt = "2026-08-13T18:00:00Z",
            ),
        ),
        onClose = {},
        onMarkAllRead = {},
        onOpen = {},
    )
}

@PreviewTest
@Preview(name = "notifications_marking_all_393x852", widthDp = 393, heightDp = 852, showBackground = true)
@Composable
fun NotificationsMarkingAllScreenshot() = NunuloTheme {
    NotificationsPage(
        notifications = listOf(
            NotificationItem(
                id = "notification-marking",
                title = "伙伴补登记待确认",
                body = "另一位成员在合照中登记了你的伙伴，请确认是否共同出现。",
                targetType = "checkin_partner",
                targetId = "record-1:partner-1",
                readAt = null,
                createdAt = "2026-08-15T00:30:00Z",
            ),
        ),
        markingAll = true,
        onClose = {},
        onMarkAllRead = {},
        onOpen = {},
    )
}

@PreviewTest
@Preview(name = "notifications_partial_failure_393x852", widthDp = 393, heightDp = 852, showBackground = true)
@Composable
fun NotificationsPartialFailureScreenshot() = NunuloTheme {
    NotificationsPage(
        notifications = listOf(
            NotificationItem(
                id = "notification-cached",
                title = "伙伴补登记已确认",
                body = "缓存中的消息仍然可以查看。",
                targetType = "checkin_partner",
                targetId = "partner-1",
                readAt = null,
                createdAt = "2026-08-14T10:00:00Z",
            ),
        ),
        error = "当前网络不可用，无法取得最新通知。",
        onClose = {},
        onMarkAllRead = {},
        onOpen = {},
        onRetry = {},
    )
}

@PreviewTest
@Preview(name = "discover_content_393x852", widthDp = 393, heightDp = 852, showBackground = true)
@Composable
fun DiscoverContentScreenshot() = ScreenshotFrame(AppTab.Discover) {
    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("从作品走进世界", style = MaterialTheme.typography.titleLarge)
                Text("沿着作品、角色、活动与地区，找到下一条真实记录。", color = NunuloColors.Muted)
            }
        }
        item {
            SectionCard("正在被记录") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CoordinateTag("BanG Dream! · 128 条")
                    CoordinateTag("MyGO!!!!! · 42 条")
                }
                Text("高松灯", style = MaterialTheme.typography.titleMedium)
                Text("12 位成员在 8 个地点留下了照片", color = NunuloColors.Muted)
            }
        }
        item {
            SectionCard("世界热门地区", "达到隐私与内容阈值后才会出现。") {
                ScreenshotMedia("北京 · 18 条 / 7 人")
            }
        }
    }
}

@PreviewTest
@Preview(name = "album_library_393x852", widthDp = 393, heightDp = 852, showBackground = true)
@Composable
fun AlbumLibraryScreenshot() = ScreenshotFrame(AppTab.Profile) {
    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ProfileSectionTabs("collection", onSelect = {}) }
        item {
            SectionCard("我的合集", "把想反复回看的记录收进一册；合集不会改变记录本身。") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("2 本合集", color = NunuloColors.Muted, modifier = Modifier.weight(1f))
                    androidx.compose.material3.Button(onClick = {}) { Text("创建合集") }
                }
                AlbumSummaryCard(
                    album = screenshotAlbum(),
                    opening = false,
                    onOpen = {},
                    onEdit = {},
                    onDelete = {},
                )
                AlbumSummaryCard(
                    album = AlbumItem("album-2", "高松灯生日记录", 12, "private", "每年留下的一张生日照片", "2026-04-14T00:00:00Z"),
                    opening = false,
                    onOpen = {},
                    onEdit = {},
                    onDelete = {},
                )
            }
        }
    }
}

@PreviewTest
@Preview(name = "album_editor_393x852", widthDp = 393, heightDp = 852, showBackground = true)
@Composable
fun AlbumEditorScreenshot() = NunuloTheme {
    AlbumEditorPage(
        initial = screenshotAlbum(),
        saving = false,
        onDismiss = {},
        onSave = { _, _, _, _ -> },
    )
}

@PreviewTest
@Preview(name = "album_collection_header_393x852", widthDp = 393, heightDp = 852, showBackground = true)
@Composable
fun AlbumCollectionHeaderScreenshot() = ScreenshotFrame(AppTab.Feed) {
    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { AlbumCollectionHeader(album = screenshotAlbum(), visibleCount = 6, onBack = {}) }
        item {
            FeedRecordCard(
                record = screenshotRecord(),
                onOpen = {},
                onLike = {},
                image = { ScreenshotMedia("合集封面记录") },
            )
        }
    }
}

@PreviewTest
@Preview(name = "event_schedule_360x800", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
fun EventScheduleScreenshot() = ScreenshotFrame(AppTab.Discover) {
    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("创建活动", style = MaterialTheme.typography.titleLarge)
                Text("不再手填时间格式，直接选择日期与时间。", color = NunuloColors.Muted)
            }
        }
        item {
            EventScheduleSection(
                startsAt = "2026-08-14T18:30:00Z",
                endsAt = "2026-08-14T21:00:00Z",
                timeError = null,
                onStartChange = {},
                onEndChange = {},
            )
        }
    }
}

@PreviewTest
@Preview(name = "event_browser_393x852", widthDp = 393, heightDp = 852, showBackground = true)
@Composable
fun EventBrowserScreenshot() = NunuloTheme {
    EventBrowserPage(
        events = listOf(
            EventItem(
                id = "event-upcoming",
                name = "MyGO!!!!! 7th LIVE「こたえなんてなくても」",
                eventType = "offline_live",
                visibility = "public",
                status = "active",
                official = true,
                place = PlaceItem("place-1", "北京工人体育场北门外场区域", latitude = 39.93, longitude = 116.44),
                series = EventSeriesItem("series-1", "MyGO!!!!! LIVE 系列", "active"),
                startsAt = "2026-08-16T10:00:00Z",
                endsAt = "2026-08-16T13:00:00Z",
                description = "带着伙伴一起看现场，关联记录即表示参与。",
                recordCount = 18,
                canEdit = false,
            ),
            EventItem(
                id = "event-past",
                name = "高松灯 2026 生日照片合集",
                eventType = "online_birthday",
                visibility = "public",
                status = "active",
                official = false,
                place = null,
                series = null,
                startsAt = "2026-04-14T00:00:00Z",
                endsAt = "2026-04-15T00:00:00Z",
                description = "往期线上生日记录仍保留在活动归档。",
                recordCount = 12,
                canEdit = true,
            ),
        ),
        onClose = {},
        onCreate = {},
        onOpen = {},
        onEdit = {},
        onDelete = {},
        now = Instant.parse("2026-08-14T12:00:00Z"),
        initialPeriod = EventPeriodFilter.All,
    )
}

@PreviewTest
@Preview(name = "catalog_browser_393x852", widthDp = 393, heightDp = 852, showBackground = true)
@Composable
fun CatalogBrowserScreenshot() = NunuloTheme {
    CatalogBrowserPage(
        catalog = screenshotCatalog(),
        onClose = {},
        onOpen = {},
        onFollow = {},
        onCreateCandidate = {},
    )
}

@PreviewTest
@Preview(name = "catalog_collection_header_393x852", widthDp = 393, heightDp = 852, showBackground = true)
@Composable
fun CatalogCollectionHeaderScreenshot() = ScreenshotFrame(AppTab.Feed) {
    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            CatalogCollectionHeader(
                entity = screenshotCatalog().getValue("character").first(),
                records = listOf(screenshotRecord(), screenshotRecord().copy(id = "record-2", userId = 11, placeName = "上海梅赛德斯奔驰文化中心")),
                onFollow = {},
                onBack = {},
            )
        }
    }
}

@PreviewTest
@Preview(name = "event_collection_header_393x852", widthDp = 393, heightDp = 852, showBackground = true)
@Composable
fun EventCollectionHeaderScreenshot() = ScreenshotFrame(AppTab.Feed) {
    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            EventCollectionHeader(
                event = EventItem(
                    id = "event-upcoming",
                    name = "MyGO!!!!! 7th LIVE「こたえなんてなくても」",
                    eventType = "offline_live",
                    visibility = "public",
                    status = "active",
                    official = true,
                    place = PlaceItem("place-1", "北京工人体育场北门外场区域", latitude = 39.93, longitude = 116.44),
                    series = EventSeriesItem("series-1", "MyGO!!!!! LIVE 系列", "active"),
                    startsAt = "2026-08-16T10:00:00Z",
                    endsAt = "2026-08-16T13:00:00Z",
                    description = "带着伙伴一起看现场，活动页只汇总真实关联的记录、伙伴与角色。",
                    recordCount = 18,
                    canEdit = false,
                ),
                records = listOf(screenshotRecord(), screenshotRecord().copy(id = "record-2", userId = 11)),
                onBack = {},
                now = Instant.parse("2026-08-14T12:00:00Z"),
            )
        }
    }
}

@PreviewTest
@Preview(name = "region_collection_header_393x852", widthDp = 393, heightDp = 852, showBackground = true)
@Composable
fun RegionCollectionHeaderScreenshot() = ScreenshotFrame(AppTab.Feed) {
    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            RegionCollectionHeader(
                region = WorldRegionItem(
                    key = "CN-Beijing",
                    name = "北京",
                    countryCode = "CN",
                    province = "北京市",
                    city = "北京",
                    recordCount = 42,
                    userCount = 8,
                    latitude = 39.9042,
                    longitude = 116.4074,
                    representativeThumbUrl = null,
                    eligible = true,
                ),
                records = listOf(
                    screenshotRecord().copy(placeId = "place-1"),
                    screenshotRecord().copy(id = "record-2", userId = 11, placeId = "place-1"),
                    screenshotRecord().copy(
                        id = "record-3",
                        userId = 12,
                        placeId = "place-2",
                        placeName = "国家体育馆南广场",
                        latitude = 39.9992,
                        longitude = 116.3907,
                    ),
                ),
                onOpenRecord = {},
                onBack = {},
            )
        }
    }
}

@PreviewTest
@Preview(name = "topic_collection_header_393x852", widthDp = 393, heightDp = 852, showBackground = true)
@Composable
fun TopicCollectionHeaderScreenshot() = ScreenshotFrame(AppTab.Feed) {
    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            TopicCollectionHeader(
                topic = TopicItem(
                    id = "topic-live-summer",
                    title = "和伙伴一起去看夏日现场",
                    description = "收录成员带着伙伴抵达演出、场外合影和散场后的真实记录。",
                    status = "published",
                    checkinIds = listOf("record-1", "record-2", "record-3"),
                ),
                records = listOf(
                    screenshotRecord(),
                    screenshotRecord().copy(id = "record-2", userId = 11, placeId = "place-2", placeName = "国家体育馆南广场"),
                    screenshotRecord().copy(id = "record-3", userId = 12, placeId = "place-3", placeName = "上海梅赛德斯奔驰文化中心"),
                ),
                onBack = {},
            )
        }
    }
}

@PreviewTest
@Preview(name = "partner_collection_header_393x852", widthDp = 393, heightDp = 852, showBackground = true)
@Composable
fun PartnerCollectionHeaderScreenshot() = ScreenshotFrame(AppTab.Feed) {
    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            PartnerCollectionHeader(
                partner = screenshotPartner(),
                records = listOf(
                    screenshotRecord().copy(placeId = "place-1"),
                    screenshotRecord().copy(
                        id = "record-2",
                        userId = 11,
                        placeId = "place-2",
                        placeName = "国家体育馆南广场",
                        partners = listOf(screenshotPartner(), screenshotMeetingPartner()),
                    ),
                ),
                onBack = {},
            )
        }
    }
}

@PreviewTest
@Preview(name = "capture_relations_393x852", widthDp = 393, heightDp = 852, showBackground = true)
@Composable
fun CaptureRelationsScreenshot() = ScreenshotFrame(AppTab.Capture) {
    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RecordStamp("2")
                Column(Modifier.weight(1f)) {
                    Text("关系与地点", style = MaterialTheme.typography.titleLarge)
                    Text("这张照片里有谁，又发生在哪里", color = NunuloColors.Muted)
                }
                Text("2/4", color = NunuloColors.MapBlue, fontWeight = FontWeight.Bold)
            }
        }
        item {
            SectionCard("伙伴", "选择后会带入它的作品与角色。") {
                CapturePartnerSelection(
                    partners = listOf(
                        screenshotPartner(),
                        screenshotPartner().copy(id = "partner-2", name = "小彩", publicCode = "N-000017", character = CatalogRef("aya", "丸山彩")),
                    ),
                    selected = listOf("partner-1"),
                    onToggle = {},
                )
            }
        }
        item {
            SectionCard("地点", "照片 GNSS 优先；没有时可以使用设备位置或地图补点。") {
                CoordinateTag("北京工人体育场北门外场区域")
                Text("39.93050, 116.44690 · 照片 GNSS", color = NunuloColors.Muted)
            }
        }
    }
}

@PreviewTest
@Preview(name = "footprint_content_393x852", widthDp = 393, heightDp = 852, showBackground = true)
@Composable
fun FootprintContentScreenshot() = ScreenshotFrame(AppTab.Profile) {
    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ProfileSectionTabs("footprint", onSelect = {}) }
        item {
            SectionCard("个人足迹", "精确位置只对自己可见。") {
                ScreenshotMedia("8 个地点 · 42 条记录")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CoordinateTag("北京 · 18")
                    CoordinateTag("上海 · 9")
                    CoordinateTag("广州 · 6")
                }
            }
        }
    }
}

@PreviewTest
@Preview(name = "auth_register_narrow_320x720", widthDp = 320, heightDp = 720, showBackground = true)
@Composable
fun AuthRegisterNarrowScreenshot() = NunuloTheme {
    AuthScreen(
        initialLogin = "",
        config = null,
        busy = false,
        message = "注册失败时会在这里说明具体原因，并保留已经填写的内容。",
        onLogin = { _, _ -> },
        onRegister = { _, _, _, _, _ -> },
        initialRegisterMode = true,
    )
}

@PreviewTest
@Preview(name = "capture_confirm_360x800", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
fun CaptureConfirmScreenshot() = ScreenshotFrame(AppTab.Capture) {
    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RecordStamp("4")
                Column(Modifier.weight(1f)) {
                    Text("发布确认", style = MaterialTheme.typography.titleLarge)
                    Text("确认谁能看到，以及是否进入世界发现", color = NunuloColors.Muted)
                }
                Text("4/4", color = NunuloColors.MapBlue, fontWeight = FontWeight.Bold)
            }
        }
        item {
            SectionCard("这一刻已准备好") {
                Text("3 张照片 · 全部安全上传", color = NunuloColors.Success, fontWeight = FontWeight.Bold)
                Text("高松灯棉花娃娃 · MyGO!!!!! · 北京工人体育场北门外场区域", color = NunuloColors.Muted)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CoordinateTag("所有注册成员")
                    CoordinateTag("进入世界发现")
                }
                androidx.compose.material3.Button(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("发布记录") }
            }
        }
    }
}

@PreviewTest
@Preview(name = "clear_draft_confirmation_360x800", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
fun ClearDraftConfirmationScreenshot() = NunuloTheme {
    Surface(color = NunuloColors.Background, modifier = Modifier.fillMaxSize()) {
        ConfirmActionDialog(
            title = "清除这条草稿？",
            body = "草稿里的照片副本、上传状态、伙伴、活动和地点信息都会从本机删除。已经发布的记录不受影响。",
            confirmLabel = "清除草稿",
            onConfirm = {},
            onDismiss = {},
        )
    }
}

@PreviewTest
@Preview(name = "edit_draft_conflict_360x800", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
fun EditDraftConflictScreenshot() = NunuloTheme {
    Surface(color = NunuloColors.Background, modifier = Modifier.fillMaxSize()) {
        EditDraftConflictDialog(photoCount = 3, onConfirm = {}, onDismiss = {})
    }
}

@PreviewTest
@Preview(name = "record_delete_confirmation_360x800", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
fun RecordDeleteConfirmationScreenshot() = NunuloTheme {
    Surface(color = NunuloColors.Background, modifier = Modifier.fillMaxSize()) {
        RecordDeleteConfirmationDialog(onConfirm = {}, onDismiss = {})
    }
}

@PreviewTest
@Preview(name = "capture_details_360x800", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
fun CaptureDetailsScreenshot() = ScreenshotFrame(AppTab.Capture) {
    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RecordStamp("3")
                Column(Modifier.weight(1f)) {
                    Text("补充信息", style = MaterialTheme.typography.titleLarge)
                    Text("写下这次同行的故事", color = NunuloColors.Muted)
                }
                Text("3/4", color = NunuloColors.MapBlue, fontWeight = FontWeight.Bold)
            }
        }
        item {
            SectionCard("拍摄时间与说明", "照片元数据会优先给出时间，也可以手动修正。") {
                Text("拍摄时间", fontWeight = FontWeight.Bold)
                Text("2026年8月13日 20:30", color = NunuloColors.Muted)
                androidx.compose.material3.TextButton(onClick = {}) { Text("修改时间") }
                Text("第一次带着伙伴一起看 MyGO!!!!! 的现场。", style = MaterialTheme.typography.bodyLarge)
            }
        }
        item {
            SectionCard("活动", "线下活动最多一个，线上生日合集可以同时选择。") {
                CoordinateTag("MyGO!!!!! 7th LIVE")
            }
        }
    }
}

@PreviewTest
@Preview(name = "record_photo_gallery_393x852", widthDp = 393, heightDp = 852, showBackground = true)
@Composable
fun RecordPhotoGalleryScreenshot() = ScreenshotFrame(AppTab.Feed) {
    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            RecordPhotoGallery(listOf(PhotoItem("photo-1"), PhotoItem("photo-2"), PhotoItem("photo-3"))) { photo, modifier ->
                Box(
                    modifier.aspectRatio(0.84f).background(NunuloColors.Placeholder),
                    contentAlignment = Alignment.Center,
                ) { Text("演出现场照片 ${photo.id.takeLast(1)}", color = NunuloColors.Muted) }
            }
        }
        item {
            SectionCard("这一刻", "相册下方继续承接记录正文与关系。") {
                Text("第一次带着伙伴一起看 MyGO!!!!! 的现场。")
            }
        }
    }
}

@PreviewTest
@Preview(name = "record_interactions_393x852", widthDp = 393, heightDp = 852, showBackground = true)
@Composable
fun RecordInteractionsScreenshot() = ScreenshotFrame(AppTab.Feed) {
    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ScreenshotMedia("3 张照片") }
        item {
            SectionCard("第一次带伙伴一起看现场") {
                Text("带着伙伴去看演出的旅行收藏者 · 2026-08-13", color = NunuloColors.Muted)
                CoordinateTag("北京工人体育场北门外场区域")
                Text("散场以后遇到了同好，也补了一张很喜欢的合照。")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("♥ 28", color = NunuloColors.Coral, fontWeight = FontWeight.Bold)
                    Text("6 条评论", color = NunuloColors.Muted)
                }
            }
        }
        item {
            SectionCard("评论") {
                Text("灯灯也来过这里！", fontWeight = FontWeight.Bold)
                Text("@mygo_friend · 刚刚", color = NunuloColors.Muted, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ScreenshotAvatar() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("旅", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun ScreenshotPersonAvatar() {
    Surface(color = NunuloColors.Soft, shape = androidx.compose.foundation.shape.CircleShape, modifier = Modifier.size(54.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Text("旅", color = NunuloColors.CoralDeep, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
        }
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

private fun screenshotPeople() = listOf(
    PersonItem(
        id = 11,
        displayName = "小灯的旅行册",
        username = "tomori_trip",
        bio = "带着高松灯棉花娃娃去看每一场 MyGO!!!!! 演出。",
        following = true,
        followerCount = 28,
        followingCount = 16,
    ),
    PersonItem(
        id = 12,
        displayName = "爱音今天也出门",
        username = "anon_go",
        bio = "记录演出、面基和散场后的城市夜景。",
        following = false,
        followerCount = 12,
        followingCount = 21,
    ),
)

private fun screenshotPartnerRequest(
    authorUserId: Int,
    ownerUserId: Int,
    authorApproved: Boolean,
    ownerApproved: Boolean,
) = PartnerRequestItem(
    checkinId = "record-request",
    partnerId = "partner-request-$ownerUserId",
    partnerName = "高松灯棉花娃娃",
    partnerCode = "N-2026-08-000128",
    recordAuthorUserId = authorUserId,
    recordAuthorDisplayName = "旅行收藏者",
    partnerOwnerUserId = ownerUserId,
    partnerOwnerDisplayName = "小灯的主人",
    authorApproved = authorApproved,
    ownerApproved = ownerApproved,
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

private fun screenshotMeetingPartner() = PartnerItem(
    id = "partner-2",
    publicCode = "N-2026-07-000091",
    ownerUserId = 11,
    name = "一起看过三次现场的千早爱音玩偶",
    visibility = "public",
    moderationStatus = "active",
    itemType = CatalogRef("plush", "毛绒玩偶"),
    work = CatalogRef("bandori", "BanG Dream!"),
    character = CatalogRef("anon", "千早爱音"),
    coverUrl = null,
    recordCount = 18,
    canEdit = false,
)

private fun screenshotAlbum() = AlbumItem(
    id = "album-1",
    title = "MyGO!!!!! 现场同行记录",
    itemCount = 6,
    visibility = "followers",
    description = "把带着伙伴看过的 Live 和散场合照收在一起。",
    createdAt = "2026-08-14T18:00:00Z",
)

private fun screenshotCatalog() = mapOf(
    "work" to listOf(
        CatalogEntityItem(
            id = "bandori",
            entityType = "work",
            canonicalName = "BanG Dream!",
            aliases = listOf("BanG Dream! 少女乐团派对！", "バンドリ！"),
            followed = true,
            recordCount = 128,
        ),
    ),
    "group" to listOf(
        CatalogEntityItem(
            id = "mygo",
            entityType = "group",
            canonicalName = "MyGO!!!!!",
            aliases = listOf("迷子", "マイゴ"),
            recordCount = 42,
            work = CatalogRef("bandori", "BanG Dream!"),
        ),
    ),
    "character" to listOf(
        CatalogEntityItem(
            id = "tomori",
            entityType = "character",
            canonicalName = "高松灯",
            aliases = listOf("高松 燈", "たかまつ ともり", "Takamatsu Tomori"),
            followed = true,
            recordCount = 18,
            work = CatalogRef("bandori", "BanG Dream!"),
            group = CatalogRef("mygo", "MyGO!!!!!"),
        ),
        CatalogEntityItem(
            id = "anon",
            entityType = "character",
            canonicalName = "千早爱音",
            aliases = listOf("千早 愛音", "Chihaya Anon"),
            status = "pending",
            recordCount = 9,
            work = CatalogRef("bandori", "BanG Dream!"),
            group = CatalogRef("mygo", "MyGO!!!!!"),
        ),
    ),
)
