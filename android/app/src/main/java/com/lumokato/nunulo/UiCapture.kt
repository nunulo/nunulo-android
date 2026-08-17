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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.MapView
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.MarkerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant

internal enum class CaptureStep(val title: String, val hint: String) {
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
    var candidateRequest by remember { mutableStateOf<CatalogCandidateRequest?>(null) }
    var groupId by rememberSaveable { mutableStateOf("") }
    var stepName by rememberSaveable { mutableStateOf(CaptureStep.Photos.name) }
    var clearDraftConfirm by rememberSaveable { mutableStateOf(false) }
    val step = CaptureStep.valueOf(stepName)
    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Column(Modifier.padding(horizontal = 4.dp, vertical = 2.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    RecordStamp("${step.ordinal + 1}")
                    Column(Modifier.weight(1f)) {
                        Text(step.title, style = MaterialTheme.typography.titleLarge)
                        Text(step.hint, color = NunuloColors.Muted, style = MaterialTheme.typography.bodySmall)
                    }
                    Text("${step.ordinal + 1}/4", color = NunuloColors.MapBlue, fontWeight = FontWeight.Bold)
                }
                CaptureStepIndicator(step)
            }
        }
        controller.screenError(AppTab.Capture)?.let { error ->
            item {
                DetailLoadState(
                    title = "关系目录没有同步完整",
                    detail = "照片与本机草稿不受影响；重新同步后再选择伙伴、作品或活动。",
                    loading = false,
                    error = error,
                    onRetry = controller::refreshAll,
                )
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
                if (draft.photos.any { it.status == "error" || it.status == "uploading" || it.status == "validating" }) {
                    DraftRecoverySummary(
                        photos = draft.photos,
                        onRetryFailed = { draft.photos.filter { it.status == "error" }.forEach { controller.retryPhoto(it.key) } },
                    )
                }
                if (draft.photos.isEmpty()) {
                    CapturePhotoEmptyState(onCamera = onCamera)
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
                DateTimeField(
                    label = "拍摄时间",
                    value = draft.takenAt,
                    emptyLabel = "使用照片时间；也可以手动选择",
                    clearLabel = "使用照片时间",
                    onChange = { controller.updateDraft(draft.copy(takenAt = it)) },
                )
                OutlinedTextField(draft.note, { controller.updateDraft(draft.copy(note = it)) }, label = { Text("这次记录") }, minLines = 3, modifier = Modifier.fillMaxWidth())
            }
        }
        if (step == CaptureStep.Relations) item {
            SectionCard("地点", "优先照片 GNSS；没有时使用手机 GPS；仍可地图补点或无地点发布。") {
                CapturePlaceSelection(
                    places = controller.places,
                    selectedPlaceId = draft.placeId,
                    latitude = draft.latitude.toDoubleOrNull(),
                    longitude = draft.longitude.toDoubleOrNull(),
                    creating = controller.placeCreating,
                    onSelect = { place ->
                        controller.updateDraft(
                            draft.copy(
                                placeId = place.id,
                                placeName = place.name,
                                latitude = place.latitude.toString(),
                                longitude = place.longitude.toString(),
                                locationSource = "place_search",
                                locationProvider = null,
                                locationCapturedAtMillis = null,
                                locationAccuracyMeters = null,
                                locationPrivacy = place.privacyLevel,
                            )
                        )
                    },
                    onClearSelection = { controller.updateDraft(draft.copy(placeId = null, locationSource = "manual")) },
                    onCreate = { name ->
                        val latitude = draft.latitude.toDoubleOrNull()
                        val longitude = draft.longitude.toDoubleOrNull()
                        if (latitude != null && longitude != null) {
                            controller.createPlace(name, latitude, longitude) { saved ->
                                controller.updateDraft(
                                    controller.draft.copy(
                                        placeId = saved.id,
                                        placeName = saved.name,
                                        latitude = saved.latitude.toString(),
                                        longitude = saved.longitude.toString(),
                                        locationSource = "place_search",
                                        locationPrivacy = saved.privacyLevel,
                                    )
                                )
                            }
                        }
                    },
                )
                OutlinedTextField(draft.placeName, { controller.updateDraft(draft.copy(placeId = null, placeName = it, locationSource = "manual")) }, label = { Text("地点名称（选填）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(draft.latitude, { controller.updateDraft(draft.copy(placeId = null, latitude = it, locationSource = "manual")) }, label = { Text("纬度") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(draft.longitude, { controller.updateDraft(draft.copy(placeId = null, longitude = it, locationSource = "manual")) }, label = { Text("经度") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                Text("来源：${locationSourceLabel(draft.locationSource)}", color = NunuloColors.Muted, style = MaterialTheme.typography.bodySmall)
                LocationChoiceState(
                    hasCoordinates = draft.latitude.isNotBlank() && draft.longitude.isNotBlank(),
                    mapOpen = mapOpen,
                    onDeviceLocation = { controller.requestLocation(LocationPurpose.Draft) },
                    onToggleMap = { mapOpen = !mapOpen },
                    onClear = { controller.updateDraft(draft.copy(placeId = null, latitude = "", longitude = "", locationSource = "none", placeName = "")) },
                )
                if (mapOpen) {
                    DraftLocationMap(
                        latitude = draft.latitude.toDoubleOrNull(),
                        longitude = draft.longitude.toDoubleOrNull(),
                        onSelect = { point -> controller.updateDraft(draft.copy(placeId = null, latitude = point.latitude.toString(), longitude = point.longitude.toString(), locationSource = "map_picker")) },
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
                CapturePartnerSelection(
                    partners = controller.partners,
                    selected = draft.partnerIds,
                    onToggle = { partner ->
                        val adding = partner.id !in draft.partnerIds
                        controller.updateDraft(
                            draft.copy(
                                partnerIds = toggleId(draft.partnerIds, partner.id),
                                itemTypeIds = if (adding) (draft.itemTypeIds + listOfNotNull(partner.itemType?.id)).distinct() else draft.itemTypeIds,
                                workIds = if (adding) (draft.workIds + listOfNotNull(partner.work?.id)).distinct() else draft.workIds,
                                characterIds = if (adding) (draft.characterIds + listOfNotNull(partner.character?.id)).distinct() else draft.characterIds,
                            )
                        )
                    },
                )
            }
        }
        if (step == CaptureStep.Relations) item {
            SectionCard("类别", "没有伙伴时请选择允许的物件类型、作品或角色；不使用自由 tag。") {
                CatalogSelection("物件类型", controller.discovery.catalog["item_type"].orEmpty(), draft.itemTypeIds, onToggle = { id -> controller.updateDraft(draft.copy(itemTypeIds = toggleId(draft.itemTypeIds, id))) }, onNoResult = { query -> candidateRequest = CatalogCandidateRequest("item_type", query) })
                CatalogSelection("作品 / IP", controller.discovery.catalog["work"].orEmpty(), draft.workIds, onToggle = { id -> controller.updateDraft(draft.copy(workIds = toggleId(draft.workIds, id))) }, onNoResult = { query -> candidateRequest = CatalogCandidateRequest("work", query) })
                val groups = controller.discovery.catalog["group"].orEmpty().filter { it.work == null || it.work.id in draft.workIds || draft.workIds.isEmpty() }
                CatalogSelection("乐队 / 组合", groups, listOfNotNull(groupId.takeIf(String::isNotBlank)), onToggle = { id -> groupId = if (groupId == id) "" else id }, onNoResult = { query -> candidateRequest = CatalogCandidateRequest("group", query, draft.workIds.singleOrNull()) }, candidateEnabled = draft.workIds.size == 1, candidateDisabledHint = "先只选择一个所属作品，再提交组合候选")
                CatalogSelection("角色", controller.discovery.catalog["character"].orEmpty().filter { (it.work == null || it.work.id in draft.workIds || draft.workIds.isEmpty()) && (groupId.isBlank() || it.group?.id == groupId) }, draft.characterIds, onToggle = { id -> controller.updateDraft(draft.copy(characterIds = toggleId(draft.characterIds, id))) }, onNoResult = { query -> candidateRequest = CatalogCandidateRequest("character", query, draft.workIds.singleOrNull()) }, candidateEnabled = draft.workIds.size == 1, candidateDisabledHint = "先只选择一个所属作品，再提交角色候选")
            }
        }
        if (step == CaptureStep.Details) item {
            SectionCard("活动", "一条记录最多关联一个线下活动，可同时关联多个线上生日合集。") {
                CaptureEventSelection(
                    events = controller.discovery.events,
                    selected = draft.eventIds,
                    onToggle = { event ->
                        val next = if (event.id in draft.eventIds) {
                            draft.eventIds - event.id
                        } else if (event.eventType.startsWith("offline_")) {
                            draft.eventIds.filter { id -> controller.discovery.events.firstOrNull { it.id == id }?.eventType?.startsWith("offline_") != true } + event.id
                        } else {
                            draft.eventIds + event.id
                        }
                        controller.updateDraft(draft.copy(eventIds = next.distinct()))
                    },
                )
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
                    TextButton(onClick = { clearDraftConfirm = true }, enabled = !controller.busy) { Text("清除草稿", color = NunuloColors.Danger) }
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
    candidateRequest?.let { request -> CandidateDialog(controller, request, onDismiss = { candidateRequest = null }) }
    if (clearDraftConfirm) {
        ConfirmActionDialog(
            title = "清除这条草稿？",
            body = "草稿里的照片副本、上传状态、伙伴、活动和地点信息都会从本机删除。已经发布的记录不受影响。",
            confirmLabel = "清除草稿",
            onConfirm = {
                clearDraftConfirm = false
                controller.clearDraft()
            },
            onDismiss = { clearDraftConfirm = false },
        )
    }
}

internal data class CatalogCandidateRequest(val type: String, val name: String, val workId: String? = null)

@Composable
internal fun CaptureStepIndicator(step: CaptureStep) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        CaptureStep.entries.forEach { value ->
            Surface(
                color = if (value == step) NunuloColors.Coral else NunuloColors.Hairline,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.weight(1f).height(4.dp),
            ) {}
        }
    }
}

@Composable
internal fun CapturePhotoEmptyState(onCamera: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(184.dp).background(NunuloColors.Lilac, RoundedCornerShape(18.dp)).clickable(onClick = onCamera), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            RecordStamp("＋", NunuloColors.MapBlue)
            Text("拍下第一张照片", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text("也可以从系统相册批量选择", color = NunuloColors.Muted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
internal fun DraftRecoverySummary(photos: List<DraftPhotoItem>, onRetryFailed: () -> Unit, modifier: Modifier = Modifier) {
    val ready = photos.count { it.status == "ready" }
    val failed = photos.count { it.status == "error" }
    val pending = photos.size - ready - failed
    Surface(color = NunuloColors.Soft, shape = RoundedCornerShape(14.dp), modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(if (failed > 0) "有照片需要重试" else "正在处理照片", fontWeight = FontWeight.Bold)
            Text(
                buildList {
                    if (ready > 0) add("$ready 张已安全上传")
                    if (pending > 0) add("$pending 张处理中")
                    if (failed > 0) add("$failed 张失败")
                }.joinToString(" · "),
                color = NunuloColors.Muted,
                style = MaterialTheme.typography.bodySmall,
            )
            Text("已成功的照片不会重传；草稿、顺序和请求号会保留。", color = NunuloColors.Muted, style = MaterialTheme.typography.bodySmall)
            if (failed > 0) TextButton(onClick = onRetryFailed, contentPadding = PaddingValues(0.dp)) { Text("重试失败照片") }
        }
    }
}

@Composable
internal fun LocationChoiceState(
    hasCoordinates: Boolean,
    mapOpen: Boolean,
    onDeviceLocation: () -> Unit,
    onToggleMap: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        if (!hasCoordinates) {
            Surface(color = NunuloColors.Soft, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("还没有地点", fontWeight = FontWeight.Bold)
                    Text("定位权限被拒绝或暂时无定位时，仍可读取照片 GNSS、地图补点或直接无地点发布。", color = NunuloColors.Muted, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onDeviceLocation) { Text(if (hasCoordinates) "重新定位" else "使用手机定位") }
            TextButton(onClick = onToggleMap) { Text(if (mapOpen) "收起地图" else "地图补点") }
            if (hasCoordinates) TextButton(onClick = onClear) { Text("清除地点") }
            else TextButton(onClick = onClear) { Text("无地点发布") }
        }
    }
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
internal fun CatalogSelection(
    title: String,
    items: List<CatalogEntityItem>,
    selected: List<String>,
    onToggle: (String) -> Unit,
    onNoResult: ((String) -> Unit)? = null,
    candidateEnabled: Boolean = true,
    candidateDisabledHint: String = "",
    initialQuery: String = "",
) {
    var query by rememberSaveable(title) { mutableStateOf(initialQuery) }
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
    if (matches.isEmpty()) {
        Text(if (query.isBlank()) "暂无候选" else "没有找到“${query.trim()}”", color = NunuloColors.Muted, style = MaterialTheme.typography.bodySmall)
        if (query.isNotBlank() && onNoResult != null) {
            TextButton(enabled = candidateEnabled, onClick = { onNoResult(query.trim()) }) { Text("提交“${query.trim()}”候选") }
            if (!candidateEnabled && candidateDisabledHint.isNotBlank()) Text(candidateDisabledHint, color = NunuloColors.Muted, style = MaterialTheme.typography.bodySmall)
        }
    }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(matches, key = { it.id }) { item -> FilterChip(selected = item.id in selected, onClick = { onToggle(item.id) }, label = { Text(item.canonicalName) }) }
    }
}

@Composable
internal fun CapturePartnerSelection(
    partners: List<PartnerItem>,
    selected: List<String>,
    onToggle: (PartnerItem) -> Unit,
    initialQuery: String = "",
) {
    var query by rememberSaveable("capture-partner-query") { mutableStateOf(initialQuery) }
    val matches = remember(partners, query, selected) {
        filterPartners(partners, query, PartnerVisibilityFilter.All)
            .sortedBy { it.id !in selected }
    }
    if (partners.isEmpty()) {
        Text("还没有伙伴，可先发布后补登记，或到伙伴页创建。", color = NunuloColors.Muted)
        return
    }
    OutlinedTextField(
        value = query,
        onValueChange = { query = it },
        label = { Text("查找要出镜的伙伴") },
        placeholder = { Text("名称、编号、作品或角色") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    Text(
        if (selected.isEmpty()) "尚未选择伙伴" else "已选择 ${selected.size} 位伙伴",
        color = NunuloColors.Muted,
        style = MaterialTheme.typography.bodySmall,
    )
    if (matches.isEmpty()) {
        Text("没有找到“${query.trim()}”；可以到伙伴页登记，或发布后用稳定编号补登记。", color = NunuloColors.Muted, style = MaterialTheme.typography.bodySmall)
    } else {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(matches, key = PartnerItem::id) { partner ->
                FilterChip(
                    selected = partner.id in selected,
                    onClick = { onToggle(partner) },
                    label = { Text(partner.name, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 180.dp)) },
                )
            }
        }
    }
    if (selected.isNotEmpty()) {
        Text("移除伙伴不会自动删除已经带入的作品与角色，仍可在下方类别中调整。", color = NunuloColors.Muted, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
internal fun CapturePlaceSelection(
    places: List<PlaceItem>,
    selectedPlaceId: String?,
    latitude: Double?,
    longitude: Double?,
    creating: Boolean,
    onSelect: (PlaceItem) -> Unit,
    onClearSelection: () -> Unit,
    onCreate: (String) -> Unit,
    initialQuery: String = "",
) {
    var query by rememberSaveable("capture-place-query") { mutableStateOf(initialQuery) }
    val matches = remember(places, query, selectedPlaceId) {
        placeSelectionItems(places, query).sortedBy { it.id != selectedPlaceId }
    }
    Text("复用我的地点", fontWeight = FontWeight.Bold)
    OutlinedTextField(
        value = query,
        onValueChange = { query = it },
        label = { Text("搜索或保存地点") },
        placeholder = { Text("名称、城市、区域或地址") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    if (selectedPlaceId != null) {
        val selected = places.firstOrNull { it.id == selectedPlaceId }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("正在复用：${selected?.name ?: "已选地点"}", color = NunuloColors.MapBlue, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            TextButton(onClick = onClearSelection) { Text("改为本次地点") }
        }
    }
    if (matches.isNotEmpty()) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(matches, key = PlaceItem::id) { place ->
                FilterChip(
                    selected = place.id == selectedPlaceId,
                    onClick = { onSelect(place) },
                    label = { Text(place.name, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 180.dp)) },
                )
            }
        }
    } else if (query.isNotBlank()) {
        Text("我的地点中没有“${query.trim()}”", color = NunuloColors.Muted, style = MaterialTheme.typography.bodySmall)
    } else if (places.isEmpty()) {
        Text("还没有保存过地点；取得坐标后可在这里建立第一个。", color = NunuloColors.Muted, style = MaterialTheme.typography.bodySmall)
    }
    if (query.isNotBlank() && matches.isEmpty()) {
        TextButton(
            enabled = latitude != null && longitude != null && !creating,
            onClick = { onCreate(query.trim()) },
        ) { Text(if (creating) "保存中" else "把当前坐标保存为“${query.trim()}”") }
        if (latitude == null || longitude == null) Text("先使用照片、设备定位或地图取得坐标，再保存地点。", color = NunuloColors.Muted, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
internal fun CaptureEventSelection(
    events: List<EventItem>,
    selected: List<String>,
    onToggle: (EventItem) -> Unit,
    now: Instant = Instant.now(),
    initialQuery: String = "",
) {
    var query by rememberSaveable("capture-event-query") { mutableStateOf(initialQuery) }
    var period by rememberSaveable("capture-event-period") { mutableStateOf(EventPeriodFilter.Upcoming) }
    var kind by rememberSaveable("capture-event-kind") { mutableStateOf(EventKindFilter.All) }
    if (events.isEmpty()) {
        Text("暂无可选活动，可在发现页创建。", color = NunuloColors.Muted)
        return
    }
    val selectedEvents = events.filter { it.id in selected }
    val matches = remember(events, query, period, kind, selected, now) {
        (selectedEvents + eventBrowseItems(events, query, period, kind, now)).distinctBy(EventItem::id)
    }
    OutlinedTextField(
        value = query,
        onValueChange = { query = it },
        label = { Text("查找活动") },
        placeholder = { Text("活动名、地点或系列") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(EventPeriodFilter.entries) { filter ->
            FilterChip(selected = period == filter, onClick = { period = filter }, label = { Text(filter.label) })
        }
    }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(EventKindFilter.entries) { filter ->
            FilterChip(selected = kind == filter, onClick = { kind = filter }, label = { Text(filter.label) })
        }
    }
    Text(
        if (selected.isEmpty()) "尚未关联活动" else "已关联 ${selected.size} 个活动；已选项始终排在前面",
        color = NunuloColors.Muted,
        style = MaterialTheme.typography.bodySmall,
    )
    if (matches.isEmpty()) {
        Text("没有找到“${query.trim()}”；可切换往期或活动类型，也可以到发现页共建活动。", color = NunuloColors.Muted, style = MaterialTheme.typography.bodySmall)
    } else {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(matches, key = EventItem::id) { event ->
                FilterChip(
                    selected = event.id in selected,
                    onClick = { onToggle(event) },
                    label = { Text(event.name, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 210.dp)) },
                )
            }
        }
    }
    selectedEvents.forEach { event ->
        Text(
            "${event.name} · ${event.periodLabel(now)} · ${eventTypeLabel(event.eventType)}",
            color = NunuloColors.MapBlue,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CandidateDialog(controller: NunuloController, request: CatalogCandidateRequest, onDismiss: () -> Unit) {
    var type by rememberSaveable(request.type) { mutableStateOf(request.type) }
    var name by rememberSaveable(request.name) { mutableStateOf(request.name) }
    var workId by rememberSaveable(request.workId) { mutableStateOf(request.workId.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("提交类别候选") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf("item_type" to "物件类型", "work" to "作品", "group" to "乐队 / 组合", "character" to "角色")) { option -> FilterChip(selected = type == option.first, onClick = { type = option.first }, label = { Text(option.second) }) }
                }
                OutlinedTextField(name, { name = it }, label = { Text("候选名称") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                if (type == "group" || type == "character") {
                    Text(if (type == "group") "组合必须选择所属作品" else "角色必须选择所属作品", color = NunuloColors.Muted)
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
