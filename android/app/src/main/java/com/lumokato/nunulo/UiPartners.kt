package com.lumokato.nunulo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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

@Composable
internal fun PartnersScreen(controller: NunuloController) {
    var editorOpen by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var searchCode by rememberSaveable { mutableStateOf("") }
    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            SectionCard("我的伙伴", "伙伴是你拥有、会重复出镜的具体物件；稳定编号用于站内识别，不是 NFT 或物权证明。") {
                Row {
                    Button(onClick = { editingId = null; editorOpen = true }) { Text("登记伙伴") }
                    Spacer(Modifier.weight(1f))
                    Text("${controller.partners.size} 个", color = NunuloColors.Muted)
                }
                if (controller.partners.isEmpty()) Text("先登记常拍的娃娃，之后记录时无需重复选择类别。", color = NunuloColors.Muted)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(controller.partners, key = { it.id }) { partner ->
                        Surface(color = NunuloColors.Background, shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp), modifier = Modifier.width(220.dp).clickable { controller.selectPartner(partner) }) {
                            Column {
                                RemoteImage(partner.coverUrl, controller.baseUrl, controller.mediaApi, aspect = 1.2f)
                                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Text(partner.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Surface(color = NunuloColors.Soft, shape = androidx.compose.foundation.shape.RoundedCornerShape(5.dp)) {
                                        Text(partner.publicCode, color = NunuloColors.Coral, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp))
                                    }
                                    Text(listOfNotNull(partner.itemType?.name, partner.work?.name, partner.character?.name).joinToString(" · "), color = NunuloColors.Muted, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                                    Text("${partner.recordCount} 条记录 · ${visibilityLabel(partner.visibility)}", color = NunuloColors.Muted, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
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
        controller.selectedPartner?.let { partner ->
            item {
                SectionCard("伙伴主页", "${partner.publicCode} · ${visibilityLabel(partner.visibility)}") {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.width(150.dp)) { RemoteImage(partner.coverUrl, controller.baseUrl, controller.mediaApi, aspect = 1f) }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(partner.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                            Text(listOfNotNull(partner.itemType?.name, partner.work?.name, partner.character?.name).joinToString(" · "), color = NunuloColors.Muted)
                            Text("${partner.recordCount} 条记录", color = NunuloColors.Muted)
                            Row {
                                TextButton(onClick = { controller.openPartnerRecords(partner) }) { Text("查看动态") }
                                if (partner.canEdit) TextButton(onClick = { editingId = partner.id; editorOpen = true }) { Text("编辑") }
                                if (partner.canEdit) TextButton(onClick = { controller.deletePartner(partner) }) { Text("删除", color = NunuloColors.Danger) }
                                TextButton(onClick = controller::clearSelectedPartner) { Text("收起") }
                            }
                        }
                    }
                    Text("见过的伙伴", fontWeight = FontWeight.Bold)
                    if (controller.selectedPartnerMeetings.isEmpty()) Text("还没有经双方确认的跨主人相遇记录。", color = NunuloColors.Muted)
                    controller.selectedPartnerMeetings.forEach { meeting ->
                        Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth().clickable { controller.selectPartner(meeting.partner) }) {
                            Row(Modifier.padding(10.dp)) {
                                Column(Modifier.weight(1f)) {
                                    Text(meeting.partner.name, fontWeight = FontWeight.Bold)
                                    Text(meeting.partner.publicCode, color = NunuloColors.Coral, style = MaterialTheme.typography.bodySmall)
                                }
                                Text("共同出现 ${meeting.meetingCount} 次\n${shortDate(meeting.firstMetAt)}—${shortDate(meeting.lastMetAt)}", color = NunuloColors.Muted, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "登记伙伴" else "编辑伙伴") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { OutlinedTextField(name, { name = it }, label = { Text("伙伴名称") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item {
                    Text("物件类型（必选）", fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(controller.discovery.catalog["item_type"].orEmpty(), key = { it.id }) { entity -> FilterChip(selected = itemTypeId == entity.id, onClick = { itemTypeId = entity.id }, label = { Text(entity.canonicalName) }) }
                    }
                }
                item {
                    Text("作品 / IP", fontWeight = FontWeight.Bold)
                    OutlinedTextField(workQuery, { workQuery = it }, label = { Text("搜索作品 / IP") }, placeholder = { Text("中文、日文、罗马字或别名") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item { FilterChip(selected = workId.isBlank(), onClick = { workId = ""; characterId = "" }, label = { Text("其他 / 无作品") }) }
                        items(works, key = { it.id }) { entity -> FilterChip(selected = workId == entity.id, onClick = { workId = entity.id; characterId = "" }, label = { Text(entity.canonicalName) }) }
                    }
                    if (workQuery.isNotBlank() && works.isEmpty()) TextButton(onClick = { candidateType = "work" }) { Text("没有“${workQuery.trim()}”？提交作品候选") }
                }
                item {
                    Text("乐队 / 组合", fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item { FilterChip(selected = groupId.isBlank(), onClick = { groupId = "" }, label = { Text("全部组合") }) }
                        items(groups, key = { it.id }) { entity -> FilterChip(selected = groupId == entity.id, onClick = { groupId = entity.id }, label = { Text(entity.canonicalName) }) }
                    }
                }
                item {
                    Text("角色", fontWeight = FontWeight.Bold)
                    OutlinedTextField(characterQuery, { characterQuery = it }, label = { Text("搜索角色") }, placeholder = { Text("中文、日文、罗马字或别名") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item { FilterChip(selected = characterId.isBlank(), onClick = { characterId = "" }, label = { Text("无角色") }) }
                        items(characters, key = { it.id }) { entity -> FilterChip(selected = characterId == entity.id, onClick = { characterId = entity.id; entity.work?.id?.let { workId = it } }, label = { Text(entity.canonicalName) }) }
                    }
                    if (characterQuery.isNotBlank() && characters.isEmpty()) TextButton(enabled = workId.isNotBlank(), onClick = { candidateType = "character" }) { Text(if (workId.isBlank()) "先选择角色所属作品" else "没有“${characterQuery.trim()}”？提交角色候选") }
                }
                item {
                    Text("伙伴主页可见性", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("private", "followers", "public").forEach { value -> FilterChip(selected = visibility == value, onClick = { visibility = value }, label = { Text(visibilityLabel(value)) }) }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank() && itemTypeId.isNotBlank() && !controller.busy, onClick = {
                controller.savePartner(initial?.id, name, itemTypeId, workId.takeIf(String::isNotBlank), characterId.takeIf(String::isNotBlank), visibility)
                onDismiss()
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
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
