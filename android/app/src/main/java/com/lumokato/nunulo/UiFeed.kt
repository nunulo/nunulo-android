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
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

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
                        Text(if (controller.collection == null) "今天，伙伴去了哪里？" else "聚合记录", style = MaterialTheme.typography.titleLarge)
                        Text("${controller.feedItems.size} 条照片记录，沿着伙伴、角色与地点继续发现", color = NunuloColors.Muted, style = MaterialTheme.typography.bodySmall)
                    }
                    Button(onClick = { controller.selectTab(AppTab.Capture) }) { Text("记一刻") }
                }
            }
        }
        controller.screenError(AppTab.Feed)?.let { error ->
            item {
                FeedSyncFailure(
                    detail = error,
                    hasCachedContent = controller.feedItems.isNotEmpty(),
                    onRetry = controller::refreshAll,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
            }
        }
        if (controller.collectionLoading || controller.collectionError != null) {
            item {
                DetailLoadState(
                    title = if (controller.collectionLoading) "正在整理聚合记录" else "聚合记录没有加载完整",
                    detail = "只显示当前聚合中的记录，不会混入上一页动态。",
                    loading = controller.collectionLoading,
                    error = controller.collectionError,
                    onRetry = controller::reloadCollection,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
            }
        }
        if (controller.feedItems.isEmpty()) {
            if (controller.screenError(AppTab.Feed) == null && !controller.collectionLoading && controller.collectionError == null) {
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
    Surface(modifier, color = NunuloColors.Paper, shape = RoundedCornerShape(26.dp), shadowElevation = 2.dp) {
        Column {
            Box(Modifier.fillMaxWidth().clickable(onClick = onOpen)) {
                image()
                if (record.photos.size > 1) {
                    Surface(color = Color.Black.copy(alpha = 0.62f), shape = RoundedCornerShape(12.dp), modifier = Modifier.align(Alignment.TopEnd).padding(7.dp)) {
                        Text("${record.photos.size} 图", color = Color.White, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp))
                    }
                }
            }
            Column(Modifier.padding(horizontal = 15.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RecordStamp(record.authorName.take(1), NunuloColors.Coral)
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
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (record.placeName.isNotBlank()) CoordinateTag(record.placeName, Modifier.weight(1f)) else Spacer(Modifier.weight(1f))
                    TextButton(onClick = onLike, contentPadding = PaddingValues(horizontal = 4.dp)) {
                        Text("${if (record.liked) "♥" else "♡"} ${record.likeCount}", color = if (record.liked) NunuloColors.Coral else NunuloColors.Muted)
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
    Dialog(onDismissRequest = controller::closeRecord, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(color = NunuloColors.Background, modifier = Modifier.fillMaxSize()) {
            Column {
                Surface(color = NunuloColors.Paper, shadowElevation = 2.dp) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        RecordStamp("记")
                        Column(Modifier.padding(start = 10.dp).weight(1f)) {
                            Text(record.note.ifBlank { record.placeName.ifBlank { "记录详情" } }, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${record.authorName} · ${shortDate(record.takenAt ?: record.createdAt)}", color = NunuloColors.Muted, style = MaterialTheme.typography.labelSmall)
                        }
                        TextButton(onClick = controller::closeRecord) { Text("关闭") }
                    }
                }
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(12.dp),
                    modifier = Modifier.weight(1f).widthIn(max = 720.dp).fillMaxWidth().align(Alignment.CenterHorizontally),
                ) {
                if (controller.recordDetailLoading || controller.recordDetailError != null) {
                    item {
                        DetailLoadState(
                            title = if (controller.recordDetailLoading) "正在加载完整内容" else "完整内容没有加载成功",
                            detail = "先显示动态里的摘要，照片与关系加载完成后会自动更新。",
                            loading = controller.recordDetailLoading,
                            error = controller.recordDetailError,
                            onRetry = controller::reloadRecordDetails,
                        )
                    }
                }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(record.photos, key = { it.id }) { photo ->
                            RemoteImage(photo.displayUrl ?: photo.thumbUrl ?: photo.originalUrl, controller.baseUrl, controller.mediaApi, modifier = Modifier.width(310.dp), aspect = 0.84f)
                        }
                    }
                }
                item {
                    SectionCard("这一刻") {
                        if (record.note.isNotBlank()) Text(record.note, style = MaterialTheme.typography.bodyLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            CoordinateTag(visibilityLabel(record.visibility))
                            if (record.worldVisible) CoordinateTag("世界发现")
                            if (record.publicShowcase) CoordinateTag("匿名展示")
                        }
                        if (record.placeName.isNotBlank()) CoordinateTag(record.placeName)
                        if (record.latitude != null && record.longitude != null) Text(formatCoordinate(record.latitude, record.longitude), color = NunuloColors.Muted, style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (record.itemTypes.isNotEmpty() || record.works.isNotEmpty() || record.characters.isNotEmpty()) {
                    item {
                        SectionCard("作品与角色") {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(record.itemTypes) { item -> AssistChip(onClick = { controller.openCollection(RecordCollection(item.name, "物件类型", mapOf("item_type_id" to item.id))) }, label = { Text(item.name) }) }
                                items(record.works) { item -> AssistChip(onClick = { controller.openCollection(RecordCollection(item.name, "作品", mapOf("work_id" to item.id))) }, label = { Text(item.name) }) }
                                items(record.characters) { item -> AssistChip(onClick = { controller.openCollection(RecordCollection(item.name, "角色", mapOf("character_id" to item.id))) }, label = { Text(item.name) }) }
                            }
                        }
                    }
                }
                if (record.partners.isNotEmpty()) {
                    item {
                        SectionCard("出镜伙伴", "从记录继续进入伙伴自己的相册与足迹。") {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(record.partners, key = { it.id }) { partner -> AssistChip(onClick = { controller.closeRecord(); controller.selectPartner(partner); controller.selectTab(AppTab.Partners) }, label = { Text(partner.name) }) }
                            }
                        }
                    }
                }
                if (record.events.isNotEmpty()) {
                    item {
                        SectionCard("活动") {
                            record.events.forEach { event -> TextButton(onClick = { controller.closeRecord(); controller.openEvent(event) }) { Text("${eventTypeLabel(event.eventType)} · ${event.name}") } }
                        }
                    }
                }
                item {
                    SectionCard("回应") {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Button(onClick = { controller.toggleLike(record) }) { Text("${if (record.liked) "♥" else "♡"} ${record.likeCount}") }
                            Text("${record.commentCount} 条评论", color = NunuloColors.Muted, modifier = Modifier.padding(start = 10.dp))
                            Spacer(Modifier.weight(1f))
                            if (!record.canEdit) TextButton(onClick = { reportOpen = true }) { Text("投诉", color = NunuloColors.Danger) }
                        }
                    }
                }
                item {
                    SectionCard("补登记伙伴", "合照中的其他伙伴会在双方确认后进入相遇记录。") {
                        OutlinedTextField(partnerCode, { partnerCode = it }, label = { Text("伙伴编号，例如 N-...") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        TextButton(enabled = partnerCode.isNotBlank(), onClick = { controller.requestPartnerForRecord(record, partnerCode); partnerCode = "" }) { Text("提交补登记") }
                    }
                }
                if (controller.albums.isNotEmpty()) {
                    item {
                        SectionCard("加入合集") {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(controller.albums, key = { it.id }) { album -> AssistChip(onClick = { controller.addToAlbum(record, album) }, label = { Text(album.title) }) }
                            }
                        }
                    }
                }
                item { Text("评论", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 4.dp)) }
                when {
                    controller.commentsLoading -> item {
                        DetailLoadState(
                            title = "正在加载评论",
                            detail = "评论加载完成前不会把它误显示成空列表。",
                            loading = true,
                            error = null,
                            onRetry = controller::reloadRecordDetails,
                        )
                    }
                    controller.commentsError != null -> item {
                        DetailLoadState(
                            title = "评论没有加载成功",
                            detail = "现有记录内容仍可继续查看。",
                            loading = false,
                            error = controller.commentsError,
                            onRetry = controller::reloadRecordDetails,
                        )
                    }
                    controller.comments.isEmpty() -> item {
                        Text("还没有评论，写下第一条回应吧。", color = NunuloColors.Muted, modifier = Modifier.padding(horizontal = 4.dp))
                    }
                    else -> items(controller.comments, key = { it.id }) { item ->
                        Column(Modifier.fillMaxWidth().background(NunuloColors.Paper, RoundedCornerShape(18.dp)).padding(12.dp)) {
                            Text(item.displayName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                            Text(item.body)
                        }
                    }
                }
                item {
                    SectionCard("写下回应") {
                        OutlinedTextField(comment, { comment = it }, label = { Text("评论") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                        Button(enabled = comment.isNotBlank(), onClick = { controller.addComment(record, comment); comment = "" }) { Text("发布评论") }
                    }
                }
                if (record.canEdit) {
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { controller.deleteRecord(record) }) { Text(if (controller.isDeletePending(record)) "确认删除" else "删除", color = NunuloColors.Danger) }
                            Button(onClick = { controller.editRecord(record) }) { Text("编辑记录") }
                        }
                    }
                }
            }
            }
        }
    }
}

@Composable
internal fun NotificationsDialog(controller: NunuloController) {
    Dialog(onDismissRequest = { controller.notificationsOpen = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        NotificationsPage(
            notifications = controller.notifications,
            error = controller.notificationsError,
            onClose = { controller.notificationsOpen = false },
            onMarkAllRead = controller::markNotificationsRead,
            onOpen = controller::openNotification,
            onRetry = controller::refreshAll,
        )
    }
}

@Composable
internal fun NotificationsPage(
    notifications: List<NotificationItem>,
    error: String? = null,
    onClose: () -> Unit,
    onMarkAllRead: () -> Unit,
    onOpen: (NotificationItem) -> Unit,
    onRetry: () -> Unit = {},
) {
    val unreadCount = notifications.count { it.readAt == null }
    Surface(color = NunuloColors.Background, modifier = Modifier.fillMaxSize()) {
        Column {
            Surface(color = NunuloColors.Paper, shadowElevation = 2.dp) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RecordStamp("信", NunuloColors.MapBlue)
                    Column(Modifier.padding(start = 10.dp).weight(1f)) {
                        Text("通知", style = MaterialTheme.typography.titleMedium)
                        Text(if (unreadCount == 0) "没有未读消息" else "$unreadCount 条未读消息", color = NunuloColors.Muted, style = MaterialTheme.typography.labelSmall)
                    }
                    TextButton(onClick = onClose) { Text("关闭") }
                }
            }
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(12.dp),
                modifier = Modifier.weight(1f).fillMaxWidth().widthIn(max = 720.dp).align(Alignment.CenterHorizontally),
            ) {
                if (error != null) {
                    item {
                        DetailLoadState(
                            title = "通知没有同步完整",
                            detail = "已经加载的通知会继续保留。",
                            loading = false,
                            error = error,
                            onRetry = onRetry,
                        )
                    }
                }
                if (notifications.isEmpty() && error == null) {
                    item { EmptyState("还没有通知", "伙伴补登记、喜欢、评论和治理结果会出现在这里。") }
                } else if (notifications.isNotEmpty()) {
                    item {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(if (unreadCount == 0) "都看完了" else "先看未读消息", style = MaterialTheme.typography.titleLarge)
                                Text("点击通知会直接打开对应记录或功能页。", color = NunuloColors.Muted, style = MaterialTheme.typography.bodySmall)
                            }
                            TextButton(enabled = unreadCount > 0, onClick = onMarkAllRead) { Text("全部已读") }
                        }
                    }
                    items(notifications.sortedWith(compareBy<NotificationItem> { it.readAt != null }.thenByDescending { it.createdAt.orEmpty() }), key = { it.id }) { notification ->
                        Surface(
                            color = if (notification.readAt == null) NunuloColors.Soft else NunuloColors.Paper,
                            shape = RoundedCornerShape(20.dp),
                            shadowElevation = if (notification.readAt == null) 2.dp else 0.dp,
                            modifier = Modifier.fillMaxWidth().clickable { onOpen(notification) },
                        ) {
                            Column(Modifier.padding(horizontal = 15.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(notification.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                                    Text(if (notification.readAt == null) "未读" else "已读", color = if (notification.readAt == null) NunuloColors.Coral else NunuloColors.Muted, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                                Text(notification.body, color = NunuloColors.Muted)
                                Text(
                                    listOf(notificationKindLabel(notification.targetType), shortDate(notification.createdAt)).filter(String::isNotBlank).joinToString(" · "),
                                    color = NunuloColors.MapBlue,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

internal fun notificationKindLabel(targetType: String?): String = when (targetType) {
    "checkin" -> "记录互动"
    "checkin_partner" -> "伙伴关系"
    "invite_code" -> "邀请"
    "photo" -> "照片与存储"
    "user" -> "账号"
    else -> "系统消息"
}
