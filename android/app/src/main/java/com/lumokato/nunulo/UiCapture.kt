package com.lumokato.nunulo

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.MapView
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.MarkerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private enum class CaptureStep(val title: String, val hint: String) {
    Photos("照片", "先选好这次记录的 1–9 张照片"),
    Relations("关系与地点", "关联伙伴、作品、角色和真实地点"),
    Details("补充信息", "写下时间、说明和参与活动"),
    Confirm("发布确认", "最后检查可见范围与发布状态"),
}

@Composable
internal fun CaptureScreen(controller: NunuloController, onPick: () -> Unit, onCamera: () -> Unit) {
    val draft = controller.draft
    val validation = validateDraft(draft)
    var mapOpen by rememberSaveable { mutableStateOf(false) }
    var candidateOpen by rememberSaveable { mutableStateOf(false) }
    var groupId by rememberSaveable { mutableStateOf("") }
    var stepName by rememberSaveable { mutableStateOf(CaptureStep.Photos.name) }
    val step = CaptureStep.valueOf(stepName)
    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(step.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text(step.hint, color = NunuloColors.Muted)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    CaptureStep.entries.forEachIndexed { index, value ->
                        Surface(
                            color = if (value == step) NunuloColors.Coral else NunuloColors.Hairline,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.weight(1f).height(4.dp),
                        ) {}
                    }
                }
            }
        }
        if (step == CaptureStep.Photos) item {
            SectionCard(if (draft.editingId == null) "实时记录" else "编辑记录", "相机优先推荐 1 张；历史相册可选择 1–9 张，首图即封面。") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onCamera, enabled = !controller.busy && draft.photos.size < 9) { Text("应用内拍摄") }
                    TextButton(onClick = onPick, enabled = !controller.busy && draft.photos.size < 9) { Text("从相册选择") }
                    Spacer(Modifier.weight(1f))
                    Text("${draft.photos.size}/9", color = NunuloColors.Muted)
                }
                if (draft.photos.isEmpty()) {
                    Box(Modifier.fillMaxWidth().height(220.dp).background(NunuloColors.Placeholder, RoundedCornerShape(10.dp)).clickable(onClick = onCamera), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("拍下第一张照片", fontWeight = FontWeight.Bold)
                            Text("也可以从系统相册批量选择", color = NunuloColors.Muted, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(draft.photos, key = { it.key }) { item ->
                            DraftPhotoCard(
                                item = item,
                                index = draft.photos.indexOf(item),
                                total = draft.photos.size,
                                controller = controller,
                            )
                        }
                    }
                }
            }
        }
        if (step == CaptureStep.Details) item {
            SectionCard("拍摄时间与说明", "服务端照片元数据优先填入时间；可在历史补录时修正。") {
                OutlinedTextField(draft.takenAt, { controller.updateDraft(draft.copy(takenAt = it)) }, label = { Text("拍摄时间（ISO，可留空）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(draft.note, { controller.updateDraft(draft.copy(note = it)) }, label = { Text("这次记录") }, minLines = 3, modifier = Modifier.fillMaxWidth())
            }
        }
        if (step == CaptureStep.Relations) item {
            SectionCard("地点", "优先照片 GNSS；没有时使用手机 GPS；仍可地图补点或无地点发布。") {
                OutlinedTextField(draft.placeName, { controller.updateDraft(draft.copy(placeName = it)) }, label = { Text("地点名称（选填）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(draft.latitude, { controller.updateDraft(draft.copy(latitude = it, locationSource = "manual")) }, label = { Text("纬度") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(draft.longitude, { controller.updateDraft(draft.copy(longitude = it, locationSource = "manual")) }, label = { Text("经度") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                Text("来源：${locationSourceLabel(draft.locationSource)}", color = NunuloColors.Muted, style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { controller.requestLocation(LocationPurpose.Draft) }) { Text("使用手机定位") }
                    TextButton(onClick = { mapOpen = !mapOpen }) { Text(if (mapOpen) "收起地图" else "地图补点") }
                    TextButton(onClick = { controller.updateDraft(draft.copy(latitude = "", longitude = "", locationSource = "none", placeName = "")) }) { Text("无地点发布") }
                }
                if (mapOpen) {
                    DraftLocationMap(
                        latitude = draft.latitude.toDoubleOrNull(),
                        longitude = draft.longitude.toDoubleOrNull(),
                        onSelect = { point -> controller.updateDraft(draft.copy(latitude = point.latitude.toString(), longitude = point.longitude.toString(), locationSource = "map_picker")) },
                    )
                }
                Text("位置精度", fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("exact" to "精确", "regional" to "区域级", "hidden" to "隐藏")) { option ->
                        FilterChip(selected = draft.locationPrivacy == option.first, onClick = { controller.updateDraft(draft.copy(locationPrivacy = option.first)) }, label = { Text(option.second) })
                    }
                }
            }
        }
        if (step == CaptureStep.Relations) item {
            SectionCard("伙伴", "选择自己的伙伴可自动继承其物件类型、作品与角色；一条合照可登记多个伙伴。") {
                if (controller.partners.isEmpty()) Text("还没有伙伴，可先发布后补登记，或到伙伴页创建。", color = NunuloColors.Muted)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(controller.partners, key = { it.id }) { partner ->
                        FilterChip(
                            selected = partner.id in draft.partnerIds,
                            onClick = {
                                val selected = toggleId(draft.partnerIds, partner.id)
                                controller.updateDraft(
                                    draft.copy(
                                        partnerIds = selected,
                                        itemTypeIds = if (partner.id !in draft.partnerIds) (draft.itemTypeIds + listOfNotNull(partner.itemType?.id)).distinct() else draft.itemTypeIds,
                                        workIds = if (partner.id !in draft.partnerIds) (draft.workIds + listOfNotNull(partner.work?.id)).distinct() else draft.workIds,
                                        characterIds = if (partner.id !in draft.partnerIds) (draft.characterIds + listOfNotNull(partner.character?.id)).distinct() else draft.characterIds,
                                    )
                                )
                            },
                            label = { Text(partner.name) },
                        )
                    }
                }
            }
        }
        if (step == CaptureStep.Relations) item {
            SectionCard("类别", "没有伙伴时请选择允许的物件类型、作品或角色；不使用自由 tag。") {
                CatalogSelection("物件类型", controller.discovery.catalog["item_type"].orEmpty(), draft.itemTypeIds) { id -> controller.updateDraft(draft.copy(itemTypeIds = toggleId(draft.itemTypeIds, id))) }
                CatalogSelection("作品 / IP", controller.discovery.catalog["work"].orEmpty(), draft.workIds) { id -> controller.updateDraft(draft.copy(workIds = toggleId(draft.workIds, id))) }
                val groups = controller.discovery.catalog["group"].orEmpty().filter { it.work == null || it.work.id in draft.workIds || draft.workIds.isEmpty() }
                CatalogSelection("乐队 / 组合", groups, listOfNotNull(groupId.takeIf(String::isNotBlank))) { id -> groupId = if (groupId == id) "" else id }
                CatalogSelection("角色", controller.discovery.catalog["character"].orEmpty().filter { (it.work == null || it.work.id in draft.workIds || draft.workIds.isEmpty()) && (groupId.isBlank() || it.group?.id == groupId) }, draft.characterIds) { id -> controller.updateDraft(draft.copy(characterIds = toggleId(draft.characterIds, id))) }
                TextButton(onClick = { candidateOpen = true }) { Text("没有合适项？提交候选") }
            }
        }
        if (step == CaptureStep.Details) item {
            SectionCard("活动", "一条记录最多关联一个线下活动，可同时关联多个线上生日合集。") {
                if (controller.discovery.events.isEmpty()) Text("暂无可选活动，可在发现页创建。", color = NunuloColors.Muted)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(controller.discovery.events, key = { it.id }) { event ->
                        FilterChip(
                            selected = event.id in draft.eventIds,
                            onClick = {
                                val next = if (event.id in draft.eventIds) {
                                    draft.eventIds - event.id
                                } else if (event.eventType.startsWith("offline_")) {
                                    draft.eventIds.filter { id -> controller.discovery.events.firstOrNull { it.id == id }?.eventType?.startsWith("offline_") != true } + event.id
                                } else {
                                    draft.eventIds + event.id
                                }
                                controller.updateDraft(draft.copy(eventIds = next.distinct()))
                            },
                            label = { Text(event.name) },
                        )
                    }
                }
            }
        }
        if (step == CaptureStep.Confirm) item {
            SectionCard("可见性", "成员可见、世界发现和互联网匿名展示是三层独立选择。") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("private", "followers", "public")) { visibility ->
                        FilterChip(
                            selected = draft.visibility == visibility,
                            onClick = {
                                controller.updateDraft(
                                    draft.copy(
                                        visibility = visibility,
                                        worldVisible = if (visibility == "public") draft.worldVisible else false,
                                        publicShowcase = if (visibility == "public") draft.publicShowcase else false,
                                    )
                                )
                            },
                            label = { Text(visibilityLabel(visibility)) },
                        )
                    }
                }
                FilterChip(
                    selected = draft.worldVisible,
                    enabled = draft.visibility == "public" && draft.latitude.isNotBlank() && draft.longitude.isNotBlank(),
                    onClick = { controller.updateDraft(draft.copy(worldVisible = !draft.worldVisible, publicShowcase = if (draft.worldVisible) false else draft.publicShowcase)) },
                    label = { Text("加入登录成员世界发现") },
                )
                FilterChip(
                    selected = draft.publicShowcase,
                    enabled = draft.visibility == "public" && draft.worldVisible,
                    onClick = { controller.updateDraft(draft.copy(publicShowcase = !draft.publicShowcase)) },
                    label = { Text("互联网匿名展示") },
                )
                Text("匿名展示只提供缩略图、区域级位置和必要公开文字。", color = NunuloColors.Muted, style = MaterialTheme.typography.bodySmall)
            }
        }
        if (step == CaptureStep.Confirm) item {
            SectionCard("发布检查", validation.missingText) {
                Text("照片 ${validation.photoCount}/9 · ${if (validation.allPhotosReady) "全部已上传" else "仍有待处理"} · ${if (validation.coordinatesValid) "地点有效" else "地点无效"}")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = controller::saveDraft, enabled = validation.ready && !controller.busy, modifier = Modifier.weight(1f)) { Text(if (draft.editingId == null) "发布记录" else "保存修改") }
                    TextButton(onClick = controller::clearDraft, enabled = !controller.busy) { Text("清除") }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (step != CaptureStep.Photos) TextButton(onClick = { stepName = CaptureStep.entries[step.ordinal - 1].name }) { Text("上一步") }
                Spacer(Modifier.weight(1f))
                if (step != CaptureStep.Confirm) {
                    Button(
                        onClick = { stepName = CaptureStep.entries[step.ordinal + 1].name },
                        enabled = step != CaptureStep.Photos || draft.photos.isNotEmpty(),
                    ) { Text("下一步") }
                }
            }
        }
    }
    if (candidateOpen) CandidateDialog(controller, onDismiss = { candidateOpen = false })
}

@Composable
private fun DraftPhotoCard(item: DraftPhotoItem, index: Int, total: Int, controller: NunuloController) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.width(230.dp)) {
        Column {
            DraftPhotoPreview(item, controller)
            Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(if (index == 0) "封面" else "第 ${index + 1} 张", fontWeight = FontWeight.Bold)
                when (item.status) {
                    "ready" -> Text("已上传${if (item.photo?.contentSha256 != null) " · checksum 已校验" else ""}", color = NunuloColors.Success, style = MaterialTheme.typography.bodySmall)
                    "error" -> Text(item.error ?: "上传失败", color = NunuloColors.Danger, style = MaterialTheme.typography.bodySmall)
                    else -> LinearProgressIndicator(progress = { (item.progress.coerceIn(0, 100) / 100f) }, modifier = Modifier.fillMaxWidth())
                }
                Row {
                    TextButton(onClick = { controller.movePhoto(item.key, -1) }, enabled = index > 0) { Text("前移") }
                    TextButton(onClick = { controller.movePhoto(item.key, 1) }, enabled = index < total - 1) { Text("后移") }
                    if (item.status == "error") TextButton(onClick = { controller.retryPhoto(item.key) }) { Text("重试") }
                    TextButton(onClick = { controller.removePhoto(item.key) }) { Text("移除", color = NunuloColors.Danger) }
                }
            }
        }
    }
}

@Composable
private fun DraftPhotoPreview(item: DraftPhotoItem, controller: NunuloController) {
    val context = LocalContext.current
    if (item.localUri != null) {
        val bitmap by produceState<Bitmap?>(initialValue = null, item.localUri) {
            value = withContext(Dispatchers.IO) { decodeSampledBitmap(context.contentResolver, item.localUri, 900) }
        }
        if (bitmap == null) Box(Modifier.fillMaxWidth().aspectRatio(0.9f).background(NunuloColors.Placeholder), contentAlignment = Alignment.Center) { Text("读取图片") }
        else Image(bitmap!!.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().aspectRatio(0.9f))
    } else {
        RemoteImage(item.photo?.displayUrl ?: item.photo?.thumbUrl, controller.baseUrl, controller.mediaApi, aspect = 0.9f)
    }
}

@Composable
private fun CatalogSelection(title: String, items: List<CatalogEntityItem>, selected: List<String>, onToggle: (String) -> Unit) {
    var query by rememberSaveable(title) { mutableStateOf("") }
    val matches = remember(items, query) { items.filter { it.matchesCatalogQuery(query) } }
    Text(title, fontWeight = FontWeight.Bold)
    OutlinedTextField(
        value = query,
        onValueChange = { query = it },
        label = { Text("搜索$title") },
        placeholder = { Text("支持中文、日文、罗马字和别名") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    if (matches.isEmpty()) Text(if (query.isBlank()) "暂无候选" else "没有找到“${query.trim()}”", color = NunuloColors.Muted, style = MaterialTheme.typography.bodySmall)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(matches, key = { it.id }) { item -> FilterChip(selected = item.id in selected, onClick = { onToggle(item.id) }, label = { Text(item.canonicalName) }) }
    }
}

@Composable
private fun CandidateDialog(controller: NunuloController, onDismiss: () -> Unit) {
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
                    Text(if (type == "group") "组合必须选择所属作品" else "角色必须选择所属作品", color = NunuloColors.Muted)
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
private fun DraftLocationMap(latitude: Double?, longitude: Double?, onSelect: (MapPoint) -> Unit) {
    nativeAmapUnavailableReason()?.let { reason ->
        CoordinateFallback(
            reason = reason,
            currentLabel = if (latitude != null && longitude != null) "当前草稿 · ${formatCoordinate(latitude, longitude)}" else null,
            rows = emptyList(),
        )
        Text("仍可使用照片 GNSS、手机 GPS 或上方经纬度输入；在 ARM Android 设备上可点击地图选点。", color = NunuloColors.Muted, style = MaterialTheme.typography.bodySmall)
        return
    }
    val context = LocalContext.current
    val mapView = remember { MapView(context).apply { onCreate(null) } }
    DisposableEffect(mapView) {
        mapView.onResume()
        onDispose { mapView.onPause(); mapView.onDestroy() }
    }
    AndroidView(
        factory = { mapView },
        modifier = Modifier.fillMaxWidth().height(280.dp),
        update = { view ->
            val map = view.map
            map.setOnMapClickListener { latLng -> onSelect(fromAmapPoint(latLng.latitude, latLng.longitude)) }
            map.clear()
            if (latitude != null && longitude != null) {
                val point = toAmapPoint(latitude, longitude)
                val latLng = LatLng(point.latitude, point.longitude)
                map.addMarker(MarkerOptions().position(latLng).title("记录地点"))
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14f))
            }
        },
    )
    Text("点击地图选择地点；保存到服务端时仍使用 WGS84 坐标。", color = NunuloColors.Muted, style = MaterialTheme.typography.bodySmall)
}

private fun toggleId(values: List<String>, id: String): List<String> = if (id in values) values - id else values + id

private fun locationSourceLabel(value: String): String = when (value) {
    "photo_exif" -> "照片 GNSS"
    "device_current", "device_gps" -> "当前设备位置"
    "device_last_known" -> "较早的设备位置，请确认"
    "map", "map_picker" -> "地图选点"
    "manual" -> "手工坐标"
    else -> "未登记"
}
