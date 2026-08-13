package com.lumokato.nunulo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
internal fun FeedScreen(controller: NunuloController) {
    LazyColumn(contentPadding = PaddingValues(bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                controller.collection?.let { target ->
                    SectionCard(target.title, target.subtitle) {
                        TextButton(onClick = { controller.loadFeed() }) { Text("返回动态") }
                    }
                } ?: run {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(FeedScope.entries) { scope ->
                            FilterChip(selected = controller.feedScope == scope, onClick = { controller.loadFeed(scope, controller.feedOrder) }, label = { Text(scope.label) })
                        }
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(FeedOrder.entries) { order ->
                            FilterChip(selected = controller.feedOrder == order, onClick = { controller.loadFeed(controller.feedScope, order) }, label = { Text(order.label) })
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(if (controller.collection == null) "照片动态" else "聚合记录", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                        Text("${controller.feedItems.size} 条记录", color = NunuloColors.Muted, style = MaterialTheme.typography.bodySmall)
                    }
                    Button(onClick = { controller.selectTab(AppTab.Capture) }) { Text("记录") }
                }
            }
        }
        controller.syncError?.let { error ->
            item {
                FeedSyncFailure(
                    detail = error,
                    hasCachedContent = controller.feedItems.isNotEmpty(),
                    onRetry = controller::refreshAll,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
            }
        }
        if (controller.feedItems.isEmpty()) {
            if (controller.syncError == null) {
                item { Box(Modifier.padding(horizontal = 12.dp)) { EmptyState("这里还没有内容", "先发布第一条照片记录，或关注作品、角色和伙伴。") } }
            }
        }
        items(controller.feedItems, key = CheckinItem::id) { record ->
            FeedCard(record, controller, Modifier.fillMaxWidth().padding(horizontal = 12.dp))
        }
    }
}

@Composable
internal fun FeedSyncFailure(detail: String, hasCachedContent: Boolean, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Surface(color = NunuloColors.Soft, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("暂时无法同步", fontWeight = FontWeight.Bold)
            Text(
                if (hasCachedContent) "当前显示上次同步的内容；本地草稿不会丢失。" else "当前网络不可用，本地草稿不会丢失。",
                color = NunuloColors.Muted,
                style = MaterialTheme.typography.bodySmall,
            )
            if (detail.isNotBlank() && detail !in setOf("同步失败", "动态加载失败")) {
                Text(detail, color = NunuloColors.Muted, style = MaterialTheme.typography.labelSmall, maxLines = 2)
            }
            TextButton(onClick = onRetry, contentPadding = PaddingValues(0.dp)) { Text("重新同步") }
        }
    }
}

@Composable
private fun FeedCard(record: CheckinItem, controller: NunuloController, modifier: Modifier) {
    FeedRecordCard(
        record = record,
        modifier = modifier,
        onOpen = { controller.openRecord(record) },
        onLike = { controller.toggleLike(record) },
        image = {
            RemoteImage(record.displayUrl ?: record.thumbUrl, controller.baseUrl, controller.mediaApi, aspect = 1.04f)
        },
    )
}

@Composable
internal fun FeedRecordCard(
    record: CheckinItem,
    onOpen: () -> Unit,
    onLike: () -> Unit,
    modifier: Modifier = Modifier,
    image: @Composable () -> Unit,
) {
    Surface(modifier, color = Color.White, shape = RoundedCornerShape(20.dp)) {
        Column {
            Box(Modifier.fillMaxWidth().clickable(onClick = onOpen)) {
                image()
                if (record.photos.size > 1) {
                    Surface(color = Color.Black.copy(alpha = 0.62f), shape = RoundedCornerShape(12.dp), modifier = Modifier.align(Alignment.TopEnd).padding(7.dp)) {
                        Text("${record.photos.size} 图", color = Color.White, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp))
                    }
                }
            }
            Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = NunuloColors.Soft, shape = RoundedCornerShape(50)) {
                        Text(record.authorName.take(1), color = NunuloColors.Coral, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp))
                    }
                    Column(Modifier.padding(start = 8.dp).weight(1f)) {
                        Text(record.authorName, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(shortDate(record.takenAt ?: record.createdAt), color = NunuloColors.Muted, style = MaterialTheme.typography.labelSmall)
                    }
                }
                Text(record.note.ifBlank { record.placeName.ifBlank { "照片记录" } }, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, maxLines = 3, overflow = TextOverflow.Ellipsis)
                val summary = buildList {
                    addAll(record.partners.take(2).map(PartnerItem::name))
                    addAll(record.characters.take(2).map(CatalogRef::name))
                    record.events.firstOrNull()?.name?.let(::add)
                    record.placeName.takeIf(String::isNotBlank)?.let(::add)
                }.distinct().take(3)
                if (summary.isNotEmpty()) Text(summary.joinToString(" · "), color = NunuloColors.Muted, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(record.placeName, color = NunuloColors.Muted, style = MaterialTheme.typography.labelSmall, maxLines = 1, modifier = Modifier.weight(1f))
                    TextButton(onClick = onLike, contentPadding = PaddingValues(horizontal = 4.dp)) {
                        Text("${if (record.liked) "♥" else "♡"} ${record.likeCount}")
                    }
                    Text("评 ${record.commentCount}", color = NunuloColors.Muted, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
internal fun RecordDetailDialog(controller: NunuloController, record: CheckinItem) {
    var comment by rememberSaveable(record.id) { mutableStateOf("") }
    var reportReason by rememberSaveable(record.id) { mutableStateOf("") }
    var reportOpen by rememberSaveable(record.id) { mutableStateOf(false) }
    var partnerCode by rememberSaveable(record.id) { mutableStateOf("") }
    if (reportOpen) {
        AlertDialog(
            onDismissRequest = { reportOpen = false },
            title = { Text("投诉这条记录") },
            text = { OutlinedTextField(reportReason, { if (it.length <= 120) reportReason = it }, label = { Text("原因") }, minLines = 3, modifier = Modifier.fillMaxWidth()) },
            confirmButton = {
                TextButton(enabled = reportReason.isNotBlank(), onClick = {
                    controller.reportRecord(record, reportReason)
                    reportReason = ""
                    reportOpen = false
                }) { Text("提交", color = NunuloColors.Danger) }
            },
            dismissButton = { TextButton(onClick = { reportOpen = false }) { Text("取消") } },
        )
        return
    }
    AlertDialog(
        onDismissRequest = controller::closeRecord,
        title = { Text(record.note.ifBlank { record.placeName.ifBlank { "记录详情" } }, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.widthIn(max = 560.dp)) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(record.photos, key = { it.id }) { photo ->
                            RemoteImage(photo.displayUrl ?: photo.thumbUrl ?: photo.originalUrl, controller.baseUrl, controller.mediaApi, modifier = Modifier.width(270.dp), aspect = 0.84f)
                        }
                    }
                }
                item {
                    Text("${record.authorName} · ${shortDate(record.takenAt ?: record.createdAt)} · ${visibilityLabel(record.visibility)}", color = NunuloColors.Muted, style = MaterialTheme.typography.bodySmall)
                    Text("${if (record.worldVisible) "进入世界发现" else "不进入世界发现"}${if (record.publicShowcase) " · 匿名展示" else ""}", color = NunuloColors.Muted, style = MaterialTheme.typography.bodySmall)
                }
                if (record.note.isNotBlank()) item { Text(record.note) }
                if (record.itemTypes.isNotEmpty() || record.works.isNotEmpty() || record.characters.isNotEmpty()) {
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(record.itemTypes) { item -> AssistChip(onClick = { controller.openCollection(RecordCollection(item.name, "物件类型", mapOf("item_type_id" to item.id))) }, label = { Text(item.name) }) }
                            items(record.works) { item -> AssistChip(onClick = { controller.openCollection(RecordCollection(item.name, "作品", mapOf("work_id" to item.id))) }, label = { Text(item.name) }) }
                            items(record.characters) { item -> AssistChip(onClick = { controller.openCollection(RecordCollection(item.name, "角色", mapOf("character_id" to item.id))) }, label = { Text(item.name) }) }
                        }
                    }
                }
                if (record.partners.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("出镜伙伴", fontWeight = FontWeight.Bold)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(record.partners, key = { it.id }) { partner -> AssistChip(onClick = { controller.closeRecord(); controller.selectPartner(partner); controller.selectTab(AppTab.Partners) }, label = { Text(partner.name) }) }
                            }
                        }
                    }
                }
                if (record.events.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("活动", fontWeight = FontWeight.Bold)
                            record.events.forEach { event -> TextButton(onClick = { controller.closeRecord(); controller.openEvent(event) }) { Text("${eventTypeLabel(event.eventType)} · ${event.name}") } }
                        }
                    }
                }
                item {
                    Text(record.placeName.ifBlank { "未登记地点" }, fontWeight = FontWeight.Bold)
                    Text(formatCoordinate(record.latitude, record.longitude), color = NunuloColors.Muted, style = MaterialTheme.typography.bodySmall)
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { controller.toggleLike(record) }) { Text("${if (record.liked) "♥" else "♡"} ${record.likeCount}") }
                        Text("${record.commentCount} 条评论", color = NunuloColors.Muted)
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = { reportOpen = true }) { Text("投诉", color = NunuloColors.Danger) }
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("补登记伙伴", fontWeight = FontWeight.Bold)
                        OutlinedTextField(partnerCode, { partnerCode = it }, label = { Text("伙伴编号，例如 N-...") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        TextButton(enabled = partnerCode.isNotBlank(), onClick = { controller.requestPartnerForRecord(record, partnerCode); partnerCode = "" }) { Text("提交补登记") }
                    }
                }
                if (controller.albums.isNotEmpty()) {
                    item {
                        Text("加入合集", fontWeight = FontWeight.Bold)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(controller.albums, key = { it.id }) { album -> AssistChip(onClick = { controller.addToAlbum(record, album) }, label = { Text(album.title) }) }
                        }
                    }
                }
                item { Text("评论", fontWeight = FontWeight.Bold) }
                items(controller.comments, key = { it.id }) { item ->
                    Column(Modifier.fillMaxWidth().background(NunuloColors.Background, RoundedCornerShape(8.dp)).padding(8.dp)) {
                        Text(item.displayName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        Text(item.body)
                    }
                }
                item {
                    OutlinedTextField(comment, { comment = it }, label = { Text("写评论") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                    TextButton(enabled = comment.isNotBlank(), onClick = { controller.addComment(record, comment); comment = "" }) { Text("发布评论") }
                }
            }
        },
        confirmButton = {
            if (record.canEdit) TextButton(onClick = { controller.editRecord(record) }) { Text("编辑") }
        },
        dismissButton = {
            Row {
                if (record.canEdit) TextButton(onClick = { controller.deleteRecord(record) }) { Text(if (controller.isDeletePending(record)) "确认删除" else "删除", color = NunuloColors.Danger) }
                TextButton(onClick = controller::closeRecord) { Text("关闭") }
            }
        },
    )
}

@Composable
internal fun NotificationsDialog(controller: NunuloController) {
    AlertDialog(
        onDismissRequest = { controller.notificationsOpen = false },
        title = { Text("通知") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.widthIn(max = 520.dp)) {
                if (controller.notifications.isEmpty()) item { EmptyState("暂无通知", "伙伴补登记、互动和治理结果会出现在这里。") }
                items(controller.notifications, key = { it.id }) { notification ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = if (notification.readAt == null) NunuloColors.Soft else Color.White),
                        modifier = Modifier.fillMaxWidth().clickable { controller.openNotification(notification) },
                    ) {
                        Column(Modifier.padding(10.dp)) {
                            Text(notification.title, fontWeight = FontWeight.Bold)
                            Text(notification.body, color = NunuloColors.Muted, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = controller::markNotificationsRead) { Text("全部已读") } },
        dismissButton = { TextButton(onClick = { controller.notificationsOpen = false }) { Text("关闭") } },
    )
}
