package com.lumokato.nunulo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
internal fun DiscoveryScreen(controller: NunuloController) {
    var candidateOpen by rememberSaveable { mutableStateOf(false) }
    var eventEditor by rememberSaveable { mutableStateOf(false) }
    var editingEventId by rememberSaveable { mutableStateOf<String?>(null) }
    var worldMapOpen by rememberSaveable { mutableStateOf(false) }
    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Column(Modifier.padding(horizontal = 4.dp, vertical = 2.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("从作品走进世界", style = MaterialTheme.typography.titleLarge)
                Text("沿着作品、角色、活动与地区，找到下一条真实记录。", color = NunuloColors.Muted)
            }
        }
        item {
            SectionCard("正在被记录", "关注作品、组合或角色后，它们会进入你的关注动态。") {
                listOf("item_type" to "物件类型", "work" to "作品 / IP", "group" to "乐队 / 组合", "character" to "角色").forEach { (type, title) ->
                    Text(title, fontWeight = FontWeight.Bold)
                    val values = controller.discovery.catalog[type].orEmpty()
                    if (values.isEmpty()) Text("暂无正式数据", color = NunuloColors.Muted, style = MaterialTheme.typography.bodySmall)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(values, key = { it.id }) { entity ->
                            Card(colors = CardDefaults.cardColors(containerColor = if (entity.followed) NunuloColors.Soft else Color.White), modifier = Modifier.width(190.dp)) {
                                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(entity.canonicalName, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    if (entity.work != null) Text(listOfNotNull(entity.work.name, entity.group?.name).joinToString(" · "), color = NunuloColors.Muted, style = MaterialTheme.typography.bodySmall)
                                    Text("${entity.recordCount} 条记录 · ${if (entity.status == "pending") "待审核" else "正式"}", color = NunuloColors.Muted, style = MaterialTheme.typography.bodySmall)
                                    Row {
                                        TextButton(onClick = { controller.openCatalog(entity) }) { Text("查看") }
                                        TextButton(onClick = { controller.toggleCatalogFollow(entity) }) { Text(if (entity.followed) "取消关注" else "关注") }
                                    }
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
                Row {
                    Button(onClick = { editingEventId = null; eventEditor = true }) { Text("创建活动") }
                    Spacer(Modifier.weight(1f))
                    Text("${controller.discovery.events.size} 个", color = NunuloColors.Muted)
                }
                if (controller.discovery.events.isEmpty()) Text("暂无活动，首批 Live 可由热心用户建立，再由管理员转为官方。", color = NunuloColors.Muted)
                controller.discovery.events.forEach { event ->
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth().clickable { controller.openEvent(event) }) {
                        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row {
                                Column(Modifier.weight(1f)) {
                                    Text(event.name, fontWeight = FontWeight.Bold)
                                    Text("${eventTypeLabel(event.eventType)} · ${if (event.official) "官方" else "共建"} · ${event.recordCount} 条记录", color = NunuloColors.Muted, style = MaterialTheme.typography.bodySmall)
                                    event.place?.let { Text(it.name, color = NunuloColors.Muted, style = MaterialTheme.typography.bodySmall) }
                                }
                                if (event.canEdit) {
                                    TextButton(onClick = { editingEventId = event.id; eventEditor = true }) { Text("编辑") }
                                    TextButton(onClick = { controller.deleteEvent(event) }) { Text("删除", color = NunuloColors.Danger) }
                                }
                            }
                            if (event.description.isNotBlank()) Text(event.description, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
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
    if (eventEditor) {
        EventEditorDialog(
            controller = controller,
            initial = editingEventId?.let { id -> controller.discovery.events.firstOrNull { it.id == id } },
            onDismiss = { eventEditor = false },
        )
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
            TextButton(enabled = name.isNotBlank() && (type !in setOf("group", "character") || workId.isNotBlank()), onClick = {
                controller.createCatalogCandidate(type, name, workId.takeIf(String::isNotBlank))
                onDismiss()
            }) { Text("提交") }
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "创建活动" else "编辑活动") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { OutlinedTextField(name, { name = it }, label = { Text("活动名称") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item {
                    Text("活动类型", fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listOf("offline_live" to "线下 Live", "offline_meetup" to "小团体面基", "online_birthday" to "线上生日合集")) { option ->
                            FilterChip(selected = type == option.first, onClick = { type = option.first; if (!type.startsWith("offline_")) placeId = "" }, label = { Text(option.second) })
                        }
                    }
                }
                item {
                    Text("可见性", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(selected = visibility == "public", onClick = { visibility = "public" }, label = { Text("公开") })
                        FilterChip(selected = visibility == "private", onClick = { visibility = "private" }, label = { Text("私人小团体") })
                    }
                }
                if (offline) {
                    item {
                        Text("单一地点", fontWeight = FontWeight.Bold)
                        if (controller.places.isEmpty()) Text("尚无地点，可用当前位置创建。", color = NunuloColors.Muted)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(controller.places, key = { it.id }) { place -> FilterChip(selected = placeId == place.id, onClick = { placeId = place.id }, label = { Text(place.name) }) }
                        }
                        controller.currentLocation?.let { location ->
                            OutlinedTextField(placeName, { placeName = it }, label = { Text("当前位置名称") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            TextButton(enabled = placeName.isNotBlank(), onClick = { controller.createPlace(placeName, location.latitude, location.longitude) }) { Text("保存当前位置为活动地点") }
                        } ?: TextButton(onClick = { controller.requestLocation(LocationPurpose.Place) }) { Text("获取当前位置") }
                    }
                }
                item {
                    Text("活动系列（选填）", fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item { FilterChip(selected = seriesId.isBlank(), onClick = { seriesId = "" }, label = { Text("无系列") }) }
                        items(controller.eventSeries, key = { it.id }) { series -> FilterChip(selected = seriesId == series.id, onClick = { seriesId = series.id }, label = { Text(series.canonicalName) }) }
                    }
                }
                item { OutlinedTextField(startsAt, { startsAt = it }, label = { Text("开始时间 ISO（选填）") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(endsAt, { endsAt = it }, label = { Text("结束时间 ISO（选填）") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(description, { description = it }, label = { Text("说明") }, minLines = 3, modifier = Modifier.fillMaxWidth()) }
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank() && (!offline || placeId.isNotBlank()) && !controller.busy, onClick = {
                controller.saveEvent(initial?.id, name, type, visibility, placeId.takeIf(String::isNotBlank), seriesId.takeIf(String::isNotBlank), startsAt.takeIf(String::isNotBlank), endsAt.takeIf(String::isNotBlank), description)
                onDismiss()
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}
