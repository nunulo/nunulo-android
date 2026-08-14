package com.lumokato.nunulo

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
internal fun ProfileScreen(controller: NunuloController, onPickAvatar: () -> Unit) {
    val user = controller.currentUser
    val uriHandler = LocalUriHandler.current
    var homeName by rememberSaveable { mutableStateOf(controller.footprint.home?.name ?: "家") }
    var section by rememberSaveable { mutableStateOf("footprint") }
    var homeDeleteConfirm by rememberSaveable { mutableStateOf(false) }
    var blockingPersonId by rememberSaveable { mutableStateOf<Int?>(null) }
    var albumEditorOpen by rememberSaveable { mutableStateOf(false) }
    var editingAlbumId by rememberSaveable { mutableStateOf<String?>(null) }
    var deletingAlbumId by rememberSaveable { mutableStateOf<String?>(null) }
    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Surface(color = NunuloColors.Paper, shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
              Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ProfileIdentity(
                    user = user,
                    recordCount = controller.mineItems.size,
                    partnerCount = controller.partners.size,
                    placeCount = controller.footprint.items.size,
                    onPickAvatar = onPickAvatar,
                    onLogout = controller::logout,
                    onOpenAdmin = { uriHandler.openUri(resolveAssetUrl(controller.baseUrl, "/admin/")) },
                    avatar = {
                        if (user?.avatarUrl != null) RemoteImage(user.avatarUrl, controller.baseUrl, controller.mediaApi, aspect = 1f)
                        else Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Text(user?.displayName?.take(1) ?: "N", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black) }
                    },
                )
                val used = user?.storageUsageBytes ?: 0
                val quota = user?.storageQuotaBytes ?: 0
                androidx.compose.material3.LinearProgressIndicator(progress = { if (quota > 0) (used.toFloat() / quota).coerceIn(0f, 1f) else 0f }, modifier = Modifier.fillMaxWidth(), color = NunuloColors.Coral, trackColor = NunuloColors.Hairline)
                Text("照片存储 ${formatBytes(used)} / ${formatBytes(quota)}", color = NunuloColors.Muted, style = MaterialTheme.typography.bodySmall)
              }
            }
        }
        controller.screenError(AppTab.Profile)?.let { error ->
            item {
                DetailLoadState(
                    title = "个人内容没有同步完整",
                    detail = "已经加载的足迹、合集与导出会继续保留。",
                    loading = false,
                    error = error,
                    onRetry = controller::refreshAll,
                )
            }
        }
        item {
            ProfileSectionTabs(section, onSelect = { section = it })
        }
        if (section == "footprint") item {
            SectionCard("个人足迹", "家位置与精确坐标只对你本人可见。") {
                FootprintMap(
                    home = controller.footprint.home,
                    items = controller.footprint.items,
                    onOpen = { item -> controller.mineItems.firstOrNull { it.id == item.checkinId }?.let(controller::openRecord) },
                )
                controller.footprint.home?.let { home ->
                    Row {
                        Text("${home.name} · ${formatCoordinate(home.latitude, home.longitude)}", modifier = Modifier.weight(1f))
                        TextButton(onClick = { homeDeleteConfirm = true }) { Text("删除家位置", color = NunuloColors.Danger) }
                    }
                } ?: run {
                    controller.currentLocation?.let { location ->
                        OutlinedTextField(homeName, { homeName = it }, label = { Text("家位置名称") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        TextButton(onClick = { controller.setHome(homeName, location.latitude, location.longitude) }) { Text("将当前位置登记为家") }
                    } ?: TextButton(onClick = { controller.requestLocation(LocationPurpose.Home) }) { Text("获取当前位置并登记家位置") }
                }
                CoordinateTag("${controller.footprint.items.size} 个有坐标的记录地点")
            }
        }
        if (section == "collection") item {
            SectionCard("我的记录", "Android 仍保留历史查看和编辑能力。") {
                if (controller.mineItems.isEmpty()) Text("还没有记录", color = NunuloColors.Muted)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(controller.mineItems.take(20), key = { it.id }) { record ->
                        Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.size(width = 180.dp, height = 230.dp).clickable { controller.openRecord(record) }) {
                            Column {
                                RemoteImage(record.thumbUrl ?: record.displayUrl, controller.baseUrl, controller.mediaApi, aspect = 1f)
                                Column(Modifier.padding(8.dp)) {
                                    Text(record.note.ifBlank { record.placeName.ifBlank { "照片记录" } }, fontWeight = FontWeight.Bold, maxLines = 2)
                                    Text(shortDate(record.takenAt ?: record.createdAt), color = NunuloColors.Muted, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
        if (section == "collection") item {
            SectionCard("我的合集", "把想反复回看的记录收进一册；合集不会改变记录本身。") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${controller.albums.size} 本合集", color = NunuloColors.Muted, modifier = Modifier.weight(1f))
                    Button(onClick = { editingAlbumId = null; albumEditorOpen = true }) { Text("创建合集") }
                }
                if (controller.albums.isEmpty()) {
                    Text("还没有合集。可以先建立一本，再从记录详情加入照片记录。", color = NunuloColors.Muted)
                }
                controller.albums.forEach { album ->
                    AlbumSummaryCard(
                        album = album,
                        opening = controller.albumOpeningId == album.id,
                        onOpen = { controller.openAlbum(album) },
                        onEdit = { editingAlbumId = album.id; albumEditorOpen = true },
                        onDelete = { deletingAlbumId = album.id },
                    )
                }
            }
        }
        if (section == "community") item {
            SectionCard("成员与关注", "关注成员和类别共同构成关注动态。") {
                if (controller.people.isEmpty()) Text("暂无其他成员", color = NunuloColors.Muted)
                controller.people.forEach { person ->
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(person.displayName, fontWeight = FontWeight.Bold)
                                Text(person.username?.let { "@$it" } ?: person.bio.orEmpty(), color = NunuloColors.Muted, style = MaterialTheme.typography.bodySmall)
                            }
                            TextButton(onClick = { controller.toggleFollow(person) }) { Text(if (person.following) "已关注" else "关注") }
                            TextButton(onClick = { blockingPersonId = person.id }) { Text("屏蔽", color = NunuloColors.Danger) }
                        }
                    }
                }
            }
        }
        if (section == "data") item {
            DataAndInviteSection(
                exports = controller.exports,
                inviteCode = controller.inviteCode,
                exportCreating = controller.exportCreating,
                exportDownloadingId = controller.exportDownloadingId,
                inviteCreating = controller.inviteCreating,
                onCreateExport = controller::createExport,
                onDownloadExport = controller::downloadExport,
                onCreateInvite = controller::createInvite,
                onCopyInvite = controller::copyInvite,
                onShareInvite = controller::shareInvite,
            )
        }
    }
    if (homeDeleteConfirm) {
        ConfirmActionDialog(
            title = "删除家位置？",
            body = "家位置名称和精确坐标会从账号中删除，既有照片记录和足迹不会被删除。",
            confirmLabel = "删除家位置",
            onConfirm = {
                homeDeleteConfirm = false
                controller.deleteHome()
            },
            onDismiss = { homeDeleteConfirm = false },
        )
    }
    blockingPersonId?.let { id ->
        controller.people.firstOrNull { it.id == id }?.let { person ->
            ConfirmActionDialog(
                title = "屏蔽 ${person.displayName}？",
                body = "屏蔽后，你们不会再出现在彼此的动态和成员列表中。这个操作不会删除已经发布的公共记录。",
                confirmLabel = "确认屏蔽",
                onConfirm = {
                    blockingPersonId = null
                    controller.blockPerson(person)
                },
                onDismiss = { blockingPersonId = null },
            )
        }
    }
    if (albumEditorOpen) {
        AlbumEditorDialog(
            controller = controller,
            initial = editingAlbumId?.let { id -> controller.albums.firstOrNull { it.id == id } },
            onDismiss = { albumEditorOpen = false },
        )
    }
    deletingAlbumId?.let { id ->
        controller.albums.firstOrNull { it.id == id }?.let { album ->
            ConfirmActionDialog(
                title = "删除合集？",
                body = "“${album.title}”和它的收录关系会被删除，原有记录、照片、评论和互动全部保留。",
                confirmLabel = if (controller.albumDeletingId == album.id) "删除中" else "删除合集",
                onConfirm = {
                    if (controller.albumDeletingId == null) controller.deleteAlbum(album) { deletingAlbumId = null }
                },
                onDismiss = { if (controller.albumDeletingId == null) deletingAlbumId = null },
            )
        }
    }
}

@Composable
internal fun AlbumSummaryCard(
    album: AlbumItem,
    opening: Boolean,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        color = NunuloColors.Lilac,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth().clickable(enabled = !opening, onClick = onOpen),
    ) {
        Column(Modifier.padding(horizontal = 13.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RecordStamp("集", NunuloColors.MapBlue)
                Column(Modifier.weight(1f)) {
                    Text(album.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(
                        album.description.ifBlank { "一本由你整理的照片记录册" },
                        color = NunuloColors.Muted,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                CoordinateTag("${album.itemCount} 条记录")
                Spacer(Modifier.weight(1f))
                CoordinateTag(visibilityLabel(album.visibility))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(enabled = !opening, onClick = onOpen) { Text(if (opening) "正在打开" else "打开合集") }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onEdit) { Text("编辑") }
                TextButton(onClick = onDelete) { Text("删除", color = NunuloColors.Danger) }
            }
        }
    }
}

@Composable
internal fun AlbumEditorDialog(
    controller: NunuloController,
    initial: AlbumItem?,
    onDismiss: () -> Unit,
) {
    val saving = controller.albumSavingId != null
    Dialog(onDismissRequest = { if (!saving) onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        AlbumEditorPage(
            initial = initial,
            saving = saving,
            onDismiss = onDismiss,
            onSave = { id, title, description, visibility -> controller.saveAlbum(id, title, description, visibility, onSuccess = onDismiss) },
        )
    }
}

@Composable
internal fun AlbumEditorPage(
    initial: AlbumItem?,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (String?, String, String, String) -> Unit,
) {
    var title by rememberSaveable(initial?.id) { mutableStateOf(initial?.title.orEmpty()) }
    var description by rememberSaveable(initial?.id) { mutableStateOf(initial?.description.orEmpty()) }
    var visibility by rememberSaveable(initial?.id) { mutableStateOf(initial?.visibility ?: "private") }
    Surface(color = NunuloColors.Background, modifier = Modifier.fillMaxSize()) {
        Column {
            Surface(color = NunuloColors.Paper, shadowElevation = 2.dp) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RecordStamp("集", NunuloColors.MapBlue)
                    Column(Modifier.padding(start = 10.dp).weight(1f)) {
                        Text(if (initial == null) "创建合集" else "编辑合集", style = MaterialTheme.typography.titleMedium)
                        Text("整理记录，不改变原内容", color = NunuloColors.Muted, style = MaterialTheme.typography.labelSmall)
                    }
                    TextButton(enabled = !saving, onClick = onDismiss) { Text("关闭") }
                }
            }
            LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f).fillMaxWidth().widthIn(max = 720.dp).align(Alignment.CenterHorizontally),
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("给这一册一个清楚的主题", style = MaterialTheme.typography.titleLarge)
                        Text("演出、旅行或角色照片都可以独立整理，之后还能继续增减记录。", color = NunuloColors.Muted)
                    }
                }
                item {
                    SectionCard("合集资料") {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("合集名称") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("说明（可选）") },
                            minLines = 3,
                            maxLines = 5,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                item {
                    SectionCard("谁能看到", "合集可见性不会放宽其中记录原本的权限。") {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            items(listOf("private" to "仅自己", "followers" to "关注者", "public" to "所有成员")) { option ->
                                FilterChip(selected = visibility == option.first, onClick = { visibility = option.first }, label = { Text(option.second) })
                            }
                        }
                    }
                }
                item {
                    Button(
                        enabled = title.isNotBlank() && !saving,
                        onClick = { onSave(initial?.id, title, description, visibility) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(if (saving) "正在保存" else if (initial == null) "创建合集" else "保存修改") }
                }
            }
        }
    }
}

@Composable
internal fun DataAndInviteSection(
    exports: List<ExportItem>,
    inviteCode: String,
    exportCreating: Boolean,
    exportDownloadingId: String?,
    inviteCreating: Boolean,
    onCreateExport: () -> Unit,
    onDownloadExport: (ExportItem) -> Unit,
    onCreateInvite: () -> Unit,
    onCopyInvite: () -> Unit,
    onShareInvite: () -> Unit,
) {
    SectionCard("数据与邀请", "导出包含照片资产、记录照片关系、伙伴、类别、活动和地点关系。") {
        Button(enabled = !exportCreating, onClick = onCreateExport, modifier = Modifier.fillMaxWidth()) {
            Text(if (exportCreating) "正在生成导出" else "生成新的数据导出")
        }
        if (exports.isEmpty()) {
            Text("还没有导出文件。生成后可以保存到本机或分享给自己的备份工具。", color = NunuloColors.Muted, style = MaterialTheme.typography.bodySmall)
        }
        exports.forEach { export ->
            Surface(color = NunuloColors.Lilac, shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("导出 ${exportCreatedLabel(export)}", fontWeight = FontWeight.Bold)
                        Text(exportStatusLabel(export.status), color = NunuloColors.Muted, style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(
                        enabled = export.canDownload() && exportDownloadingId == null,
                        onClick = { onDownloadExport(export) },
                    ) {
                        Text(
                            when {
                                exportDownloadingId == export.id -> "准备中"
                                export.canDownload() -> "保存或分享"
                                else -> "暂不可用"
                            }
                        )
                    }
                }
            }
        }
        androidx.compose.material3.HorizontalDivider(color = NunuloColors.Hairline)
        Text("邀请成员", fontWeight = FontWeight.Bold)
        Text("邀请码只发给你认识的人；生成新邀请码不会自动公开。", color = NunuloColors.Muted, style = MaterialTheme.typography.bodySmall)
        if (inviteCode.isBlank()) {
            TextButton(enabled = !inviteCreating, onClick = onCreateInvite, modifier = Modifier.fillMaxWidth()) {
                Text(if (inviteCreating) "正在生成邀请码" else "生成邀请码")
            }
        } else {
            Surface(color = NunuloColors.Soft, shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("本次邀请码", color = NunuloColors.Muted, style = MaterialTheme.typography.labelSmall)
                    Text(inviteCode, color = NunuloColors.CoralDeep, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TextButton(onClick = onCopyInvite) { Text("复制") }
                        TextButton(onClick = onShareInvite) { Text("分享") }
                        TextButton(enabled = !inviteCreating, onClick = onCreateInvite) { Text(if (inviteCreating) "生成中" else "再生成一个") }
                    }
                }
            }
        }
    }
}

internal fun ExportItem.canDownload(): Boolean = status.lowercase() in setOf("available", "ready", "completed")

internal fun exportStatusLabel(status: String): String = when (status.lowercase()) {
    "available", "ready", "completed" -> "可以保存或分享"
    "queued", "pending", "processing", "generating" -> "正在生成"
    "failed", "error" -> "生成失败，请重新生成"
    "expired" -> "已过期，请重新生成"
    else -> status.ifBlank { "状态未知" }
}

internal fun exportCreatedLabel(export: ExportItem): String =
    export.createdAt?.take(16)?.replace('T', ' ')?.takeIf(String::isNotBlank) ?: export.id.take(8)

@Composable
internal fun ProfileSectionTabs(selected: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    LazyRow(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        items(listOf("footprint" to "足迹", "collection" to "收藏", "community" to "社区", "data" to "数据")) { option ->
            FilterChip(selected = selected == option.first, onClick = { onSelect(option.first) }, label = { Text(option.second) })
        }
    }
}

@Composable
internal fun ProfileIdentity(
    user: AuthUser?,
    recordCount: Int,
    partnerCount: Int,
    placeCount: Int,
    onPickAvatar: () -> Unit,
    onLogout: () -> Unit,
    onOpenAdmin: () -> Unit,
    avatar: @Composable () -> Unit,
) {
    val adminRole = user?.roles.orEmpty().firstOrNull { it == "owner" || it == "admin" }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(shape = CircleShape, color = NunuloColors.Soft, modifier = Modifier.size(72.dp).clickable(onClick = onPickAvatar)) {
                avatar()
            }
            Column(Modifier.weight(1f)) {
                Text(user?.displayName ?: "Nunulo 成员", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text(user?.username?.let { "@$it" } ?: user?.email.orEmpty(), color = NunuloColors.Muted)
                if (adminRole != null) Text(if (adminRole == "owner") "站点所有者" else "管理员", color = NunuloColors.Coral, fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = onLogout) { Text("退出") }
        }
        Surface(
            color = NunuloColors.Lilac,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(Modifier.padding(horizontal = 8.dp, vertical = 10.dp)) {
                ProfileMetric("记录", recordCount, Modifier.weight(1f))
                ProfileMetric("伙伴", partnerCount, Modifier.weight(1f))
                ProfileMetric("地点", placeCount, Modifier.weight(1f))
            }
        }
        if (adminRole != null) {
            TextButton(onClick = onOpenAdmin) { Text("打开 Web 管理台") }
            Text("Android 只显示身份和入口；目录、举报、活动与存储治理仍在独立 Web Admin。", color = NunuloColors.Muted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ProfileMetric(label: String, value: Int, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(value.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
        Text(label, color = NunuloColors.Muted, style = MaterialTheme.typography.labelMedium)
    }
}
