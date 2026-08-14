package com.lumokato.nunulo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
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
import java.time.Instant
import java.time.OffsetDateTime

@Composable
internal fun DiscoveryScreen(controller: NunuloController) {
    var candidateOpen by rememberSaveable { mutableStateOf(false) }
    var eventEditor by rememberSaveable { mutableStateOf(false) }
    var eventBrowserOpen by rememberSaveable { mutableStateOf(false) }
    var editingEventId by rememberSaveable { mutableStateOf<String?>(null) }
    var deletingEventId by rememberSaveable { mutableStateOf<String?>(null) }
    var worldMapOpen by rememberSaveable { mutableStateOf(false) }
    var catalogType by rememberSaveable { mutableStateOf("work") }
    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Column(Modifier.padding(horizontal = 4.dp, vertical = 2.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("从作品走进世界", style = MaterialTheme.typography.titleLarge)
                Text("沿着作品、角色、活动与地区，找到下一条真实记录。", color = NunuloColors.Muted)
            }
        }
        controller.screenError(AppTab.Discover)?.let { error ->
            item {
                DetailLoadState(
                    title = "发现内容没有同步完整",
                    detail = "已经加载的作品、活动与地区会继续保留。",
                    loading = false,
                    error = error,
                    onRetry = controller::refreshAll,
                )
            }
        }
        item {
            SectionCard("正在被记录", "关注作品、组合或角色后，它们会进入你的关注动态。") {
                val catalogTypes = listOf("item_type" to "物件", "work" to "作品", "group" to "组合", "character" to "角色")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(catalogTypes) { option -> FilterChip(selected = catalogType == option.first, onClick = { catalogType = option.first }, label = { Text(option.second) }) }
                }
                val values = controller.discovery.catalog[catalogType].orEmpty()
                if (values.isEmpty()) Text("这一类还没有正式内容", color = NunuloColors.Muted, style = MaterialTheme.typography.bodySmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(values, key = { it.id }) { entity ->
                        Card(colors = CardDefaults.cardColors(containerColor = if (entity.followed) NunuloColors.Soft else NunuloColors.Paper), modifier = Modifier.width(190.dp)) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(entity.canonicalName, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                if (entity.work != null) Text(listOfNotNull(entity.work.name, entity.group?.name).joinToString(" · "), color = NunuloColors.Muted, style = MaterialTheme.typography.bodySmall)
                                CoordinateTag("${entity.recordCount} 条记录")
                                Row {
                                    TextButton(onClick = { controller.openCatalog(entity) }) { Text("查看") }
                                    TextButton(onClick = { controller.toggleCatalogFollow(entity) }) { Text(if (entity.followed) "已关注" else "关注") }
                                }
                            }
                        }
                    }
                }
                TextButton(onClick = { candidateOpen = true }) { Text("没有找到？提交目录候选") }
            }
        }
        item {
            SectionCard("活动", "线下 Live / 小团体面基使用单一地点；线上生日合集没有物理地点。") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = { editingEventId = null; eventEditor = true }) { Text("创建活动") }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { eventBrowserOpen = true }) { Text("查看全部 ${controller.discovery.events.size} 个") }
                }
                if (controller.discovery.events.isEmpty()) Text("暂无活动，首批 Live 可由热心用户建立，再由管理员转为官方。", color = NunuloColors.Muted)
                val now = Instant.now()
                val upcoming = eventBrowseItems(controller.discovery.events, "", EventPeriodFilter.Upcoming, EventKindFilter.All, now)
                val featured = upcoming.take(2).ifEmpty { eventBrowseItems(controller.discovery.events, "", EventPeriodFilter.Past, EventKindFilter.All, now).take(2) }
                featured.forEach { event ->
                    EventSummaryCard(
                        event = event,
                        now = now,
                        onOpen = { controller.openEvent(event) },
                        onEdit = if (event.canEdit) ({ editingEventId = event.id; eventEditor = true }) else null,
                        onDelete = if (event.canEdit) ({ deletingEventId = event.id }) else null,
                    )
                }
            }
        }
        item {
            SectionCard("专题", "专题由平台维护，聚合同一批真实记录。") {
                if (controller.discovery.topics.isEmpty()) Text("暂无已发布专题", color = NunuloColors.Muted)
                controller.discovery.topics.forEach { topic ->
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth().clickable { controller.openTopic(topic) }) {
                        Column(Modifier.padding(10.dp)) {
                            Text(topic.title, fontWeight = FontWeight.Bold)
                            Text(topic.description.ifBlank { "${topic.checkinIds.size} 条精选记录" }, color = NunuloColors.Muted, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
        item {
            SectionCard("世界热门地区", "达到隐私与内容阈值后才会出现；进入地区后再看照片记录。") {
                if (controller.discovery.worldRegions.isEmpty()) Text("当前还没有达到 3 条 / 2 人阈值的地区。", color = NunuloColors.Muted)
                if (controller.discovery.worldRegions.isNotEmpty()) {
                    TextButton(onClick = { worldMapOpen = !worldMapOpen }) { Text(if (worldMapOpen) "收起地区地图" else "打开地区地图") }
                    if (worldMapOpen) WorldRegionMap(controller.discovery.worldRegions, onOpen = controller::openRegion)
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(controller.discovery.worldRegions, key = { it.key }) { region ->
                        Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.width(220.dp).clickable { controller.openRegion(region) }) {
                            Column {
                                RemoteImage(region.representativeThumbUrl, controller.baseUrl, controller.mediaApi, aspect = 1.5f)
                                Column(Modifier.padding(10.dp)) {
                                    Text(region.name, fontWeight = FontWeight.Bold)
                                    Text("${region.recordCount} 条 · ${region.userCount} 人", color = NunuloColors.Muted, style = MaterialTheme.typography.bodySmall)
                                    Text("${region.province.orEmpty()} ${region.countryCode.orEmpty()}".trim(), color = NunuloColors.Muted, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    if (candidateOpen) DiscoveryCandidateDialog(controller, onDismiss = { candidateOpen = false })
    if (eventBrowserOpen) {
        Dialog(onDismissRequest = { eventBrowserOpen = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            EventBrowserPage(
                events = controller.discovery.events,
                onClose = { eventBrowserOpen = false },
                onCreate = { editingEventId = null; eventEditor = true },
                onOpen = controller::openEvent,
                onEdit = { event -> editingEventId = event.id; eventEditor = true },
                onDelete = { event -> deletingEventId = event.id },
            )
        }
    }
    if (eventEditor) {
        EventEditorDialog(
            controller = controller,
            initial = editingEventId?.let { id -> controller.discovery.events.firstOrNull { it.id == id } },
            onDismiss = { eventEditor = false },
        )
    }
    deletingEventId?.let { id ->
        controller.discovery.events.firstOrNull { it.id == id }?.let { event ->
            ConfirmActionDialog(
                title = "删除活动？",
                body = "“${event.name}”会从活动目录中删除，已经发布的记录和照片仍会保留活动名称快照。",
                confirmLabel = "删除活动",
                onConfirm = {
                    deletingEventId = null
                    controller.deleteEvent(event)
                },
                onDismiss = { deletingEventId = null },
            )
        }
    }
}

@Composable
internal fun EventBrowserPage(
    events: List<EventItem>,
    onClose: () -> Unit,
    onCreate: () -> Unit,
    onOpen: (EventItem) -> Unit,
    onEdit: (EventItem) -> Unit,
    onDelete: (EventItem) -> Unit,
    now: Instant = Instant.now(),
    initialPeriod: EventPeriodFilter = EventPeriodFilter.Upcoming,
    initialKind: EventKindFilter = EventKindFilter.All,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var periodName by rememberSaveable { mutableStateOf(initialPeriod.name) }
    var kindName by rememberSaveable { mutableStateOf(initialKind.name) }
    val period = runCatching { EventPeriodFilter.valueOf(periodName) }.getOrDefault(EventPeriodFilter.Upcoming)
    val kind = runCatching { EventKindFilter.valueOf(kindName) }.getOrDefault(EventKindFilter.All)
    val visible = eventBrowseItems(events, query, period, kind, now)
    Surface(color = NunuloColors.Background, modifier = Modifier.fillMaxSize()) {
        Column {
            Surface(color = NunuloColors.Paper, shadowElevation = 2.dp) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RecordStamp("活", NunuloColors.Leaf)
                    Column(Modifier.padding(start = 10.dp).weight(1f)) {
                        Text("活动", style = MaterialTheme.typography.titleMedium)
                        Text("近期与往期 · ${events.size} 个", color = NunuloColors.Muted, style = MaterialTheme.typography.labelSmall)
                    }
                    TextButton(onClick = onClose) { Text("关闭") }
                }
            }
            LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f).fillMaxWidth().widthIn(max = 720.dp).align(Alignment.CenterHorizontally),
            ) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("把同一场活动收进一页", style = MaterialTheme.typography.titleLarge)
                            Text("搜索活动、地点或系列；往期记录会留在归档中。", color = NunuloColors.Muted)
                        }
                        Button(onClick = onCreate) { Text("创建") }
                    }
                }
                item {
                    SectionCard("查找活动") {
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            label = { Text("活动、地点或系列") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(EventPeriodFilter.entries) { option ->
                                FilterChip(selected = period == option, onClick = { periodName = option.name }, label = { Text(option.label) })
                            }
                        }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(EventKindFilter.entries) { option ->
                                FilterChip(selected = kind == option, onClick = { kindName = option.name }, label = { Text(option.label) })
                            }
                        }
                    }
                }
                if (visible.isEmpty()) {
                    item { EmptyState("没有符合条件的活动", "换一个关键词或筛选条件；也可以创建新的共建活动。") }
                } else {
                    items(visible, key = EventItem::id) { event ->
                        EventSummaryCard(
                            event = event,
                            now = now,
                            onOpen = { onOpen(event) },
                            onEdit = if (event.canEdit) ({ onEdit(event) }) else null,
                            onDelete = if (event.canEdit) ({ onDelete(event) }) else null,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun EventSummaryCard(
    event: EventItem,
    now: Instant,
    onOpen: () -> Unit,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = if (event.isPast(now)) NunuloColors.Placeholder else NunuloColors.Paper),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
    ) {
        Column(Modifier.padding(horizontal = 13.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(event.name, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(
                        listOf(eventTypeLabel(event.eventType), event.series?.canonicalName).filterNotNull().joinToString(" · "),
                        color = NunuloColors.Muted,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (event.official) CoordinateTag("官方") else CoordinateTag("共建")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CoordinateTag(event.periodLabel(now))
                CoordinateTag("${event.recordCount} 条记录")
            }
            val schedule = listOfNotNull(event.startsAt?.let(::shortDate), event.endsAt?.let(::shortDate)).distinct().joinToString("—")
            if (schedule.isNotBlank()) Text(schedule, color = NunuloColors.MapBlue, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            event.place?.let { Text(it.name, color = NunuloColors.Muted, style = MaterialTheme.typography.bodySmall) }
            if (event.description.isNotBlank()) Text(event.description, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onOpen, contentPadding = PaddingValues(horizontal = 4.dp)) { Text("查看活动记录") }
                Spacer(Modifier.weight(1f))
                onEdit?.let { TextButton(onClick = it) { Text("编辑") } }
                onDelete?.let { TextButton(onClick = it) { Text("删除", color = NunuloColors.Danger) } }
            }
        }
    }
}

@Composable
private fun DiscoveryCandidateDialog(controller: NunuloController, onDismiss: () -> Unit) {
    var type by rememberSaveable { mutableStateOf("item_type") }
    var name by rememberSaveable { mutableStateOf("") }
    var workId by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("提交类别候选") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf("item_type" to "物件类型", "work" to "作品", "group" to "乐队 / 组合", "character" to "角色")) { option -> FilterChip(selected = type == option.first, onClick = { type = option.first }, label = { Text(option.second) }) }
                }
                OutlinedTextField(name, { name = it }, label = { Text("中文候选名") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                if (type == "group" || type == "character") {
                    Text(if (type == "group") "组合必须属于一个作品" else "角色只属于一个作品", color = NunuloColors.Muted)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(controller.discovery.catalog["work"].orEmpty(), key = { it.id }) { work -> FilterChip(selected = workId == work.id, onClick = { workId = work.id }, label = { Text(work.canonicalName) }) }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && (type !in setOf("group", "character") || workId.isNotBlank()) && !controller.catalogCandidateCreating,
                onClick = { controller.createCatalogCandidate(type, name, workId.takeIf(String::isNotBlank), onSuccess = onDismiss) },
            ) { Text(if (controller.catalogCandidateCreating) "提交中" else "提交") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun EventEditorDialog(controller: NunuloController, initial: EventItem?, onDismiss: () -> Unit) {
    var name by rememberSaveable(initial?.id) { mutableStateOf(initial?.name.orEmpty()) }
    var type by rememberSaveable(initial?.id) { mutableStateOf(initial?.eventType ?: "offline_live") }
    var visibility by rememberSaveable(initial?.id) { mutableStateOf(initial?.visibility ?: "public") }
    var placeId by rememberSaveable(initial?.id) { mutableStateOf(initial?.place?.id.orEmpty()) }
    var seriesId by rememberSaveable(initial?.id) { mutableStateOf(initial?.series?.id.orEmpty()) }
    var startsAt by rememberSaveable(initial?.id) { mutableStateOf(initial?.startsAt.orEmpty()) }
    var endsAt by rememberSaveable(initial?.id) { mutableStateOf(initial?.endsAt.orEmpty()) }
    var description by rememberSaveable(initial?.id) { mutableStateOf(initial?.description.orEmpty()) }
    var placeName by rememberSaveable { mutableStateOf("") }
    val offline = type.startsWith("offline_")
    val timeError = eventTimeRangeError(startsAt, endsAt)
    val canSave = name.isNotBlank() && (!offline || placeId.isNotBlank()) && timeError == null && !controller.busy
    val save = {
        controller.saveEvent(initial?.id, name, type, visibility, placeId.takeIf(String::isNotBlank), seriesId.takeIf(String::isNotBlank), startsAt.takeIf(String::isNotBlank), endsAt.takeIf(String::isNotBlank), description, onSuccess = onDismiss)
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(color = NunuloColors.Background, modifier = Modifier.fillMaxSize()) {
            Column {
                Surface(color = NunuloColors.Paper, shadowElevation = 2.dp) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    ) {
                        TextButton(onClick = onDismiss) { Text("取消") }
                        Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                            Text(if (initial == null) "创建活动" else "编辑活动", style = MaterialTheme.typography.titleMedium)
                            Text("活动把同一时间与地点的记录收在一起", color = NunuloColors.Muted, style = MaterialTheme.typography.labelSmall)
                        }
                        Button(enabled = canSave, onClick = save) { Text(if (controller.busy) "保存中" else "保存") }
                    }
                }
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(12.dp),
                    modifier = Modifier.weight(1f).fillMaxWidth().widthIn(max = 720.dp).align(androidx.compose.ui.Alignment.CenterHorizontally),
                ) {
                    item {
                        Column(Modifier.padding(horizontal = 4.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(if (initial == null) "这次一起去哪里？" else "更新活动信息", style = MaterialTheme.typography.titleLarge)
                            Text("先说明活动类型，再补时间和地点；发布记录时就能直接关联。", color = NunuloColors.Muted)
                        }
                    }
                    item {
                        SectionCard("基本信息", "名称、类型和可见范围会显示在活动页。") {
                            OutlinedTextField(name, { name = it }, label = { Text("活动名称") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            Text("活动类型", fontWeight = FontWeight.Bold)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(listOf("offline_live" to "线下 Live", "offline_meetup" to "小团体面基", "online_birthday" to "线上生日合集")) { option ->
                                    FilterChip(
                                        selected = type == option.first,
                                        onClick = {
                                            type = option.first
                                            if (!type.startsWith("offline_")) placeId = ""
                                        },
                                        label = { Text(option.second) },
                                    )
                                }
                            }
                            Text("谁能看到", fontWeight = FontWeight.Bold)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                item { FilterChip(selected = visibility == "public", onClick = { visibility = "public" }, label = { Text("所有成员") }) }
                                item { FilterChip(selected = visibility == "private", onClick = { visibility = "private" }, label = { Text("私人小团体") }) }
                            }
                        }
                    }
                    if (offline) {
                        item {
                            SectionCard("活动地点", "线下活动只关联一个地点；照片仍可保留各自的拍摄坐标。") {
                                if (controller.places.isEmpty()) Text("还没有保存过地点。先获取当前位置并给它起一个容易辨认的名称。", color = NunuloColors.Muted)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(controller.places, key = { it.id }) { place ->
                                        FilterChip(selected = placeId == place.id, onClick = { placeId = place.id }, label = { Text(place.name) })
                                    }
                                }
                                controller.currentLocation?.let { location ->
                                    CoordinateTag(formatCoordinate(location.latitude, location.longitude))
                                    OutlinedTextField(placeName, { placeName = it }, label = { Text("当前位置名称") }, placeholder = { Text("例如：北京工人体育场北门") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                    Button(
                                        enabled = placeName.isNotBlank() && !controller.placeCreating,
                                        onClick = {
                                            controller.createPlace(placeName, location.latitude, location.longitude) { saved ->
                                                placeId = saved.id
                                                placeName = ""
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                    ) { Text(if (controller.placeCreating) "保存中" else "保存并选择此地点") }
                                } ?: Button(onClick = { controller.requestLocation(LocationPurpose.Place) }, modifier = Modifier.fillMaxWidth()) { Text("获取当前位置") }
                                if (placeId.isBlank()) Text("选择或创建一个地点后才能保存线下活动。", color = NunuloColors.Danger, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    item {
                        EventScheduleSection(startsAt, endsAt, timeError, onStartChange = { startsAt = it }, onEndChange = { endsAt = it })
                    }
                    item {
                        SectionCard("系列与说明", "系列用于把同一巡演或长期企划串联起来。") {
                            Text("活动系列（选填）", fontWeight = FontWeight.Bold)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                item { FilterChip(selected = seriesId.isBlank(), onClick = { seriesId = "" }, label = { Text("无系列") }) }
                                items(controller.eventSeries, key = { it.id }) { series ->
                                    FilterChip(selected = seriesId == series.id, onClick = { seriesId = series.id }, label = { Text(series.canonicalName) })
                                }
                            }
                            OutlinedTextField(description, { description = it }, label = { Text("活动说明") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun EventScheduleSection(
    startsAt: String,
    endsAt: String,
    timeError: String?,
    onStartChange: (String) -> Unit,
    onEndChange: (String) -> Unit,
) {
    SectionCard("活动时间", "开始和结束时间都可以不填；填写结束时间时必须晚于开始时间。") {
        DateTimeField("开始时间", startsAt, "尚未设置开始时间", "清除开始时间", onChange = onStartChange)
        DateTimeField("结束时间", endsAt, "尚未设置结束时间", "清除结束时间", onChange = onEndChange)
        timeError?.let { Text(it, color = NunuloColors.Danger, style = MaterialTheme.typography.bodySmall) }
    }
}

internal fun eventTimeRangeError(startsAt: String, endsAt: String): String? {
    if (startsAt.isBlank() || endsAt.isBlank()) return null
    val start = runCatching { OffsetDateTime.parse(startsAt).toInstant() }.getOrNull() ?: return "开始时间格式无效，请重新选择。"
    val end = runCatching { OffsetDateTime.parse(endsAt).toInstant() }.getOrNull() ?: return "结束时间格式无效，请重新选择。"
    return if (!end.isAfter(start)) "结束时间必须晚于开始时间。" else null
}
