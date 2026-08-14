package com.lumokato.nunulo

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.unit.dp

@Composable
internal fun ProfileScreen(controller: NunuloController, onPickAvatar: () -> Unit) {
    val user = controller.currentUser
    val uriHandler = LocalUriHandler.current
    var homeName by rememberSaveable { mutableStateOf(controller.footprint.home?.name ?: "家") }
    var albumTitle by rememberSaveable { mutableStateOf("") }
    var section by rememberSaveable { mutableStateOf("footprint") }
    var homeDeleteConfirm by rememberSaveable { mutableStateOf(false) }
    var blockingPersonId by rememberSaveable { mutableStateOf<Int?>(null) }
    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Surface(color = NunuloColors.Paper, shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
              Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ProfileIdentity(
                    user = user,
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
            SectionCard("合集", "合集聚合记录，不承担分类权威。") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(albumTitle, { albumTitle = it }, label = { Text("新合集名称") }, singleLine = true, modifier = Modifier.weight(1f))
                    Button(enabled = albumTitle.isNotBlank(), onClick = { controller.createAlbum(albumTitle); albumTitle = "" }) { Text("创建") }
                }
                controller.albums.forEach { album ->
                    Row(Modifier.fillMaxWidth()) {
                        Text(album.title, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text("${album.itemCount} 条 · ${visibilityLabel(album.visibility)}", color = NunuloColors.Muted)
                    }
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
        if (adminRole != null) {
            TextButton(onClick = onOpenAdmin) { Text("打开 Web 管理台") }
            Text("Android 只显示身份和入口；目录、举报、活动与存储治理仍在独立 Web Admin。", color = NunuloColors.Muted, style = MaterialTheme.typography.bodySmall)
        }
    }
}
