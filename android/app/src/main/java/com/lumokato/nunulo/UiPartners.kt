package com.lumokato.nunulo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
internal fun PartnersScreen(controller: NunuloController) {
    var editorOpen by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var searchCode by rememberSaveable { mutableStateOf("") }
    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        controller.screenError(AppTab.Partners)?.let { error ->
            item {
                DetailLoadState(
                    title = "伙伴内容没有同步完整",
                    detail = "已经加载的伙伴和待确认关系会继续保留。",
                    loading = false,
                    error = error,
                    onRetry = controller::refreshAll,
                )
            }
        }
        item {
            Column(Modifier.padding(horizontal = 4.dp, vertical = 2.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("常伴出镜的伙伴", style = MaterialTheme.typography.titleLarge)
                        Text("${controller.partners.size} 位伙伴 · 记录会自动带上它的作品与角色", color = NunuloColors.Muted, style = MaterialTheme.typography.bodySmall)
                    }
                    Button(onClick = { editingId = null; editorOpen = true }) { Text("登记") }
                }
                if (controller.partners.isEmpty()) EmptyState("从第一位伙伴开始", "登记后，每次记录都能直接选择，不必反复填写作品和角色。")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(controller.partners, key = { it.id }) { partner ->
                        PartnerSummaryCard(partner = partner, onOpen = { controller.selectPartner(partner) }, image = { RemoteImage(partner.coverUrl, controller.baseUrl, controller.mediaApi, aspect = 1.2f) })
                    }
                }
            }
        }
        item {
            SectionCard("按稳定编号查找", "可查找其他用户允许展示的伙伴，用于合照补登记。") {
                OutlinedTextField(searchCode, { searchCode = it }, label = { Text("伙伴编号 N-...") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                TextButton(enabled = searchCode.isNotBlank(), onClick = { controller.searchPartner(searchCode) }) { Text("查找伙伴") }
            }
        }
        item {
            SectionCard("待确认补登记", "跨用户伙伴关系需要相关双方确认，确认后才进入伙伴主页和相遇统计。") {
                if (controller.partnerRequests.isEmpty()) Text("暂无待确认关系", color = NunuloColors.Muted)
                controller.partnerRequests.forEach { request ->
                    Surface(color = NunuloColors.Soft, shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("${request.partnerName} · ${request.partnerCode}", fontWeight = FontWeight.Bold)
                            Text("记录作者 ${request.recordAuthorUserId} / 伙伴主人 ${request.partnerOwnerUserId}", color = NunuloColors.Muted, style = MaterialTheme.typography.bodySmall)
                            Text("作者 ${if (request.authorApproved) "已确认" else "待确认"} · 主人 ${if (request.ownerApproved) "已确认" else "待确认"}", color = NunuloColors.Muted, style = MaterialTheme.typography.bodySmall)
                            Row {
                                Button(onClick = { controller.resolvePartnerRequest(request, true) }) { Text("确认") }
                                TextButton(onClick = { controller.resolvePartnerRequest(request, false) }) { Text("拒绝", color = NunuloColors.Danger) }
                            }
                        }
                    }
                }
            }
        }
    }
    controller.selectedPartner?.let { partner ->
        PartnerDetailDialog(
            controller = controller,
            partner = partner,
            onEdit = {
                editingId = partner.id
                controller.clearSelectedPartner()
                editorOpen = true
            },
        )
    }
    if (editorOpen) {
        PartnerEditorDialog(
            controller = controller,
            initial = editingId?.let { id -> controller.partners.firstOrNull { it.id == id } },
            onDismiss = { editorOpen = false },
        )
    }
}

@Composable
private fun PartnerDetailDialog(controller: NunuloController, partner: PartnerItem, onEdit: () -> Unit) {
    Dialog(onDismissRequest = controller::clearSelectedPartner, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        PartnerDetailPage(
            partner = partner,
            meetings = controller.selectedPartnerMeetings,
            detailLoading = controller.partnerDetailLoading,
            detailError = controller.partnerDetailError,
            meetingsLoading = controller.partnerMeetingsLoading,
            meetingsError = controller.partnerMeetingsError,
            onRetry = controller::reloadPartnerDetails,
            onClose = controller::clearSelectedPartner,
            onOpenRecords = { controller.openPartnerRecords(partner) },
            onEdit = onEdit,
            onDelete = {
                controller.deletePartner(partner)
                controller.clearSelectedPartner()
            },
            onSelectMeeting = controller::selectPartner,
            image = { RemoteImage(partner.coverUrl, controller.baseUrl, controller.mediaApi, aspect = 1.25f) },
        )
    }
}

@Composable
internal fun PartnerDetailPage(
    partner: PartnerItem,
    meetings: List<PartnerMeetingItem>,
    detailLoading: Boolean = false,
    detailError: String? = null,
    meetingsLoading: Boolean = false,
    meetingsError: String? = null,
    onRetry: () -> Unit = {},
    onClose: () -> Unit,
    onOpenRecords: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSelectMeeting: (PartnerItem) -> Unit,
    image: @Composable () -> Unit,
) {
    var deletePending by rememberSaveable(partner.id) { mutableStateOf(false) }
    Surface(color = NunuloColors.Background, modifier = Modifier.fillMaxSize()) {
        Column {
            Surface(color = NunuloColors.Paper, shadowElevation = 2.dp) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    RecordStamp("伴", NunuloColors.MapBlue)
                    Column(Modifier.padding(start = 10.dp).weight(1f)) {
                        Text("伙伴主页", style = MaterialTheme.typography.titleMedium)
                        Text(partner.publicCode, color = NunuloColors.Muted, style = MaterialTheme.typography.labelSmall)
                    }
                    TextButton(onClick = onClose) { Text("关闭") }
                }
            }
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(12.dp),
                modifier = Modifier.weight(1f).fillMaxWidth().widthIn(max = 720.dp).align(androidx.compose.ui.Alignment.CenterHorizontally),
            ) {
                if (detailLoading || detailError != null) {
                    item {
                        DetailLoadState(
                            title = if (detailLoading) "正在加载完整资料" else "完整资料没有加载成功",
                            detail = "先显示伙伴列表里的摘要，完整资料加载后会自动更新。",
                            loading = detailLoading,
                            error = detailError,
                            onRetry = onRetry,
                        )
                    }
                }
                item {
                    Surface(color = NunuloColors.Paper, shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp), shadowElevation = 2.dp) {
                        Column {
                            image()
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(partner.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                                CoordinateTag(partner.publicCode)
                                val relation = listOfNotNull(partner.itemType?.name, partner.work?.name, partner.character?.name)
                                if (relation.isNotEmpty()) Text(relation.joinToString(" · "), color = NunuloColors.Muted)
                                Text("${partner.recordCount} 条记录 · ${visibilityLabel(partner.visibility)}", color = NunuloColors.Muted, style = MaterialTheme.typography.bodySmall)
                                Button(onClick = onOpenRecords, modifier = Modifier.fillMaxWidth()) { Text("查看它的记录") }
                            }
                        }
                    }
                }
                item {
                    SectionCard("见过的伙伴", "只有双方确认过的合照关系才会出现在这里。") {
                        when {
                            meetingsLoading -> DetailLoadState(
                                title = "正在加载相遇记录",
                                detail = "加载完成后才会判断这里是否为空。",
                                loading = true,
                                error = null,
                                onRetry = onRetry,
                            )
                            meetingsError != null -> DetailLoadState(
                                title = "相遇记录没有加载成功",
                                detail = "伙伴资料仍可继续查看。",
                                loading = false,
                                error = meetingsError,
                                onRetry = onRetry,
                            )
                            meetings.isEmpty() -> Text("还没有确认过的相遇记录。以后在合照里补登记伙伴，就会慢慢形成同行关系。", color = NunuloColors.Muted)
                        }
                        if (!meetingsLoading && meetingsError == null) meetings.forEach { meeting ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                modifier = Modifier.fillMaxWidth().clickable { onSelectMeeting(meeting.partner) },
                            ) {
                                Row(Modifier.padding(12.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Text(meeting.partner.name, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                        Text(meeting.partner.publicCode, color = NunuloColors.Coral, style = MaterialTheme.typography.bodySmall)
                                    }
                                    Text(
                                        "共同出现 ${meeting.meetingCount} 次\n${shortDate(meeting.firstMetAt)}—${shortDate(meeting.lastMetAt)}",
                                        color = NunuloColors.Muted,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                        }
                    }
                }
                if (partner.canEdit) {
                    item {
                        SectionCard("维护伙伴", "编辑名称、作品角色和主页可见性；删除不会影响已经发布的照片。") {
                            Button(onClick = onEdit, modifier = Modifier.fillMaxWidth()) { Text("编辑伙伴资料") }
                            TextButton(
                                onClick = {
                                    if (deletePending) onDelete() else deletePending = true
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(if (deletePending) "再次点击，确认删除伙伴" else "删除伙伴", color = NunuloColors.Danger)
                            }
                            if (deletePending) {
                                Text("伙伴关系会删除，既有记录和照片仍会保留。离开本页可取消删除。", color = NunuloColors.Danger, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun PartnerSummaryCard(
    partner: PartnerItem,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    image: @Composable () -> Unit,
) {
    Surface(
        color = NunuloColors.Paper,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        shadowElevation = 2.dp,
        modifier = modifier.width(220.dp).clickable(onClick = onOpen),
    ) {
        Column {
            image()
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(partner.name, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                CoordinateTag(partner.publicCode)
                Text(listOfNotNull(partner.itemType?.name, partner.work?.name, partner.character?.name).joinToString(" · "), color = NunuloColors.Muted, style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
                Text("${partner.recordCount} 条记录 · ${visibilityLabel(partner.visibility)}", color = NunuloColors.Muted, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun PartnerEditorDialog(controller: NunuloController, initial: PartnerItem?, onDismiss: () -> Unit) {
    var name by rememberSaveable(initial?.id) { mutableStateOf(initial?.name.orEmpty()) }
    var itemTypeId by rememberSaveable(initial?.id) { mutableStateOf(initial?.itemType?.id.orEmpty()) }
    var workId by rememberSaveable(initial?.id) { mutableStateOf(initial?.work?.id.orEmpty()) }
    var characterId by rememberSaveable(initial?.id) { mutableStateOf(initial?.character?.id.orEmpty()) }
    var visibility by rememberSaveable(initial?.id) { mutableStateOf(initial?.visibility ?: "private") }
    var workQuery by rememberSaveable(initial?.id) { mutableStateOf("") }
    var groupId by rememberSaveable(initial?.id) { mutableStateOf("") }
    var characterQuery by rememberSaveable(initial?.id) { mutableStateOf("") }
    var candidateType by rememberSaveable(initial?.id) { mutableStateOf<String?>(null) }
    val works = controller.discovery.catalog["work"].orEmpty().filter { it.matchesCatalogQuery(workQuery) }
    val groups = controller.discovery.catalog["group"].orEmpty().filter { it.work == null || workId.isBlank() || it.work.id == workId }
    val characters = controller.discovery.catalog["character"].orEmpty()
        .filter { it.work == null || workId.isBlank() || it.work.id == workId }
        .filter { groupId.isBlank() || it.group?.id == groupId }
        .filter { it.matchesCatalogQuery(characterQuery) }
    val canSave = name.isNotBlank() && itemTypeId.isNotBlank() && !controller.busy
    val save = {
        controller.savePartner(initial?.id, name, itemTypeId, workId.takeIf(String::isNotBlank), characterId.takeIf(String::isNotBlank), visibility)
        onDismiss()
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
                            Text(if (initial == null) "登记伙伴" else "编辑伙伴", style = MaterialTheme.typography.titleMedium)
                            Text("名称、作品与角色会在以后记录时自动带入", color = NunuloColors.Muted, style = MaterialTheme.typography.labelSmall)
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
                            Text(if (initial == null) "先确认它是谁" else "更新伙伴资料", style = MaterialTheme.typography.titleLarge)
                            Text("作品和角色可以搜索正式名、日文名、罗马字或别名；找不到时可在这里直接提交候选。", color = NunuloColors.Muted)
                        }
                    }
                    item {
                        SectionCard("基本信息", "伙伴名称会显示在记录、相遇关系和伙伴主页。") {
                            OutlinedTextField(name, { name = it }, label = { Text("伙伴名称") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            Text("物件类型（必选）", fontWeight = FontWeight.Bold)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(controller.discovery.catalog["item_type"].orEmpty(), key = { it.id }) { entity ->
                                    FilterChip(selected = itemTypeId == entity.id, onClick = { itemTypeId = entity.id }, label = { Text(entity.canonicalName) })
                                }
                            }
                            if (itemTypeId.isBlank()) Text("请选择一个物件类型后才能保存。", color = NunuloColors.Danger, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    item {
                        SectionCard("作品与角色", "先选作品，再按组合缩小角色范围；角色不是必填。") {
                            Text("作品 / IP", fontWeight = FontWeight.Bold)
                            OutlinedTextField(workQuery, { workQuery = it }, label = { Text("搜索作品 / IP") }, placeholder = { Text("中文、日文、罗马字或别名") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                item { FilterChip(selected = workId.isBlank(), onClick = { workId = ""; groupId = ""; characterId = "" }, label = { Text("其他 / 无作品") }) }
                                items(works, key = { it.id }) { entity ->
                                    FilterChip(selected = workId == entity.id, onClick = { workId = entity.id; groupId = ""; characterId = "" }, label = { Text(entity.canonicalName) })
                                }
                            }
                            if (workQuery.isNotBlank() && works.isEmpty()) {
                                Button(onClick = { candidateType = "work" }, modifier = Modifier.fillMaxWidth()) { Text("提交“${workQuery.trim()}”为作品候选") }
                            }

                            Text("乐队 / 组合", fontWeight = FontWeight.Bold)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                item { FilterChip(selected = groupId.isBlank(), onClick = { groupId = "" }, label = { Text("全部组合") }) }
                                items(groups, key = { it.id }) { entity ->
                                    FilterChip(selected = groupId == entity.id, onClick = { groupId = entity.id; characterId = "" }, label = { Text(entity.canonicalName) })
                                }
                            }

                            Text("角色", fontWeight = FontWeight.Bold)
                            OutlinedTextField(characterQuery, { characterQuery = it }, label = { Text("搜索角色") }, placeholder = { Text("中文、日文、罗马字或别名") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                item { FilterChip(selected = characterId.isBlank(), onClick = { characterId = "" }, label = { Text("无角色") }) }
                                items(characters, key = { it.id }) { entity ->
                                    FilterChip(
                                        selected = characterId == entity.id,
                                        onClick = {
                                            characterId = entity.id
                                            entity.work?.id?.let { workId = it }
                                            entity.group?.id?.let { groupId = it }
                                        },
                                        label = { Text(entity.canonicalName) },
                                    )
                                }
                            }
                            if (characterQuery.isNotBlank() && characters.isEmpty()) {
                                Button(enabled = workId.isNotBlank(), onClick = { candidateType = "character" }, modifier = Modifier.fillMaxWidth()) {
                                    Text(if (workId.isBlank()) "先选择角色所属作品" else "提交“${characterQuery.trim()}”为角色候选")
                                }
                            }
                        }
                    }
                    item {
                        SectionCard("伙伴主页", "控制其他成员能否通过伙伴主页看到它的记录和相遇关系。") {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(listOf("private", "followers", "public")) { value ->
                                    FilterChip(selected = visibility == value, onClick = { visibility = value }, label = { Text(visibilityLabel(value)) })
                                }
                            }
                            Text(
                                when (visibility) {
                                    "public" -> "所有登录成员都能查看伙伴主页。"
                                    "followers" -> "只有关注你的成员能查看伙伴主页。"
                                    else -> "只有你自己能查看伙伴主页。"
                                },
                                color = NunuloColors.Muted,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }
    }
    candidateType?.let { type ->
        PartnerCatalogCandidateDialog(
            controller = controller,
            type = type,
            initialName = if (type == "work") workQuery else characterQuery,
            workId = workId.takeIf(String::isNotBlank),
            onDismiss = { candidateType = null },
        )
    }
}

@Composable
private fun PartnerCatalogCandidateDialog(controller: NunuloController, type: String, initialName: String, workId: String?, onDismiss: () -> Unit) {
    var name by rememberSaveable(type, initialName) { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (type == "work") "提交作品候选" else "提交角色候选") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("候选会进入待审核状态；正式归并和别名治理由 Web Admin 完成。", color = NunuloColors.Muted)
                OutlinedTextField(name, { name = it }, label = { Text("候选名称") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { TextButton(enabled = name.isNotBlank() && (type != "character" || workId != null), onClick = { controller.createCatalogCandidate(type, name, workId); onDismiss() }) { Text("提交") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}
