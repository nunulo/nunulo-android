package com.lumokato.nunulo

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
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

internal enum class CommunityFollowFilter(val label: String) {
    All("全部成员"),
    Following("已关注"),
}

internal fun communityBrowseItems(
    people: List<PersonItem>,
    query: String,
    followFilter: CommunityFollowFilter,
): List<PersonItem> {
    val term = query.trim().lowercase()
    return people.asSequence()
        .filter { followFilter == CommunityFollowFilter.All || it.following }
        .filter { person ->
            term.isBlank() || listOf(person.displayName, person.username.orEmpty(), person.bio.orEmpty())
                .any { value -> value.lowercase().contains(term) }
        }
        .sortedWith(compareByDescending<PersonItem> { it.following }.thenByDescending { it.followerCount }.thenBy { it.displayName })
        .toList()
}

internal fun memberRecordsFor(personId: Int, records: List<CheckinItem>): List<CheckinItem> =
    records.filter { it.userId == personId }

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
    var communityQuery by rememberSaveable { mutableStateOf("") }
    var communityFollowFilterName by rememberSaveable { mutableStateOf(CommunityFollowFilter.All.name) }
    var profileEditorOpen by rememberSaveable { mutableStateOf(false) }
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
                    onEditProfile = { profileEditorOpen = true },
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
            val followFilter = runCatching { CommunityFollowFilter.valueOf(communityFollowFilterName) }.getOrDefault(CommunityFollowFilter.All)
            val visiblePeople = communityBrowseItems(controller.people, communityQuery, followFilter)
            SectionCard("社区成员", "找到正在记录同一作品、角色与旅程的人。关注后，他们的新记录会进入关注动态。") {
                OutlinedTextField(
                    value = communityQuery,
                    onValueChange = { communityQuery = it },
                    label = { Text("搜索显示名、用户名或简介") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    items(CommunityFollowFilter.entries) { option ->
                        FilterChip(
                            selected = followFilter == option,
                            onClick = { communityFollowFilterName = option.name },
                            label = { Text(option.label) },
                        )
                    }
                }
                Text("${visiblePeople.size} 位成员", color = NunuloColors.Muted, style = MaterialTheme.typography.bodySmall)
                if (controller.people.isEmpty()) {
                    Text("暂无其他成员。新成员加入后会出现在这里。", color = NunuloColors.Muted)
                } else if (visiblePeople.isEmpty()) {
                    Text("没有匹配的成员。可以换个名字、用户名或简介关键词。", color = NunuloColors.Muted)
                }
                visiblePeople.forEach { person ->
                    MemberSummaryCard(
                        person = person,
                        following = person.id in controller.followingPersonIds,
                        onOpen = { controller.selectPerson(person) },
                        onFollow = { controller.toggleFollow(person) },
                        onBlock = { blockingPersonId = person.id },
                        avatar = { PersonAvatar(person, controller.baseUrl, controller.mediaApi) },
                    )
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
    if (profileEditorOpen) {
        Dialog(onDismissRequest = { if (!controller.profileSaving) profileEditorOpen = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            ProfileEditorPage(
                user = controller.currentUser,
                saving = controller.profileSaving,
                onClose = { profileEditorOpen = false },
                onSave = { displayName, bio -> controller.saveProfile(displayName, bio) { profileEditorOpen = false } },
            )
        }
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
    controller.selectedPerson?.let { person ->
        Dialog(onDismissRequest = controller::closePersonProfile, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            MemberProfilePage(
                person = person,
                records = controller.selectedPersonRecords,
                loading = controller.personProfileLoading,
                error = controller.personProfileError,
                following = person.id in controller.followingPersonIds,
                onClose = controller::closePersonProfile,
                onRetry = controller::reloadPersonProfile,
                onFollow = { controller.toggleFollow(person) },
                onBlock = {
                    controller.closePersonProfile()
                    blockingPersonId = person.id
                },
                onOpenRecord = controller::openRecord,
                onLike = controller::toggleLike,
                isLiking = controller::isLiking,
                avatar = { PersonAvatar(person, controller.baseUrl, controller.mediaApi) },
                recordImage = { record -> RemoteImage(record.thumbUrl ?: record.displayUrl, controller.baseUrl, controller.mediaApi, aspect = 1.04f) },
            )
        }
    }
}

@Composable
internal fun PersonAvatar(person: PersonItem, apiBase: String, api: NunuloApi, modifier: Modifier = Modifier) {
    Surface(shape = CircleShape, color = NunuloColors.Soft, modifier = modifier.size(54.dp)) {
        if (person.avatarUrl.isNullOrBlank()) {
            Box(contentAlignment = Alignment.Center) {
                Text(person.displayName.take(1).ifBlank { "N" }, color = NunuloColors.CoralDeep, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
            }
        } else {
            RemoteImage(person.avatarUrl, apiBase, api, aspect = 1f)
        }
    }
}

@Composable
internal fun MemberSummaryCard(
    person: PersonItem,
    following: Boolean,
    onOpen: () -> Unit,
    onFollow: () -> Unit,
    onBlock: () -> Unit,
    avatar: @Composable () -> Unit,
) {
    Surface(
        color = if (person.following) NunuloColors.Soft else Color.White,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, NunuloColors.Hairline),
        modifier = Modifier.fillMaxWidth().clickable(enabled = !following, onClick = onOpen),
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 11.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                avatar()
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(person.displayName, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    person.username?.let { Text("@$it", color = NunuloColors.MapBlue, style = MaterialTheme.typography.bodySmall) }
                    Text(person.bio?.takeIf(String::isNotBlank) ?: "还没有填写简介", color = NunuloColors.Muted, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                CoordinateTag("${person.followerCount} 位关注者")
                Spacer(Modifier.weight(1f))
                TextButton(enabled = !following, onClick = onFollow) { Text(if (following) "处理中" else if (person.following) "取消关注" else "关注") }
                TextButton(enabled = !following, onClick = onBlock) { Text("屏蔽", color = NunuloColors.Danger) }
            }
        }
    }
}

@Composable
internal fun MemberProfilePage(
    person: PersonItem,
    records: List<CheckinItem>,
    loading: Boolean,
    error: String?,
    following: Boolean,
    onClose: () -> Unit,
    onRetry: () -> Unit,
    onFollow: () -> Unit,
    onBlock: () -> Unit,
    onOpenRecord: (CheckinItem) -> Unit,
    onLike: (CheckinItem) -> Unit,
    isLiking: (CheckinItem) -> Boolean,
    avatar: @Composable () -> Unit,
    recordImage: @Composable (CheckinItem) -> Unit,
) {
    Surface(color = NunuloColors.Background, modifier = Modifier.fillMaxSize()) {
        Column {
            Surface(color = NunuloColors.Paper, shadowElevation = 2.dp) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    RecordStamp("人", NunuloColors.MapBlue)
                    Column(Modifier.padding(start = 10.dp).weight(1f)) {
                        Text("成员主页", style = MaterialTheme.typography.titleMedium)
                        Text("这位成员允许你看到的记录与联系", color = NunuloColors.Muted, style = MaterialTheme.typography.labelSmall)
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
                    Surface(color = NunuloColors.Lilac, shape = androidx.compose.foundation.shape.RoundedCornerShape(26.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                avatar()
                                Column(Modifier.weight(1f)) {
                                    Text(person.displayName, style = MaterialTheme.typography.titleLarge)
                                    person.username?.let { Text("@$it", color = NunuloColors.MapBlue, fontWeight = FontWeight.Bold) }
                                }
                            }
                            Text(person.bio?.takeIf(String::isNotBlank) ?: "这位成员还没有填写简介。", color = NunuloColors.Muted)
                            Row(Modifier.fillMaxWidth()) {
                                ProfileMetric("关注者", person.followerCount, Modifier.weight(1f))
                                ProfileMetric("正在关注", person.followingCount, Modifier.weight(1f))
                                ProfileMetric("可见记录", records.size, Modifier.weight(1f))
                            }
                            Button(enabled = !following, onClick = onFollow, modifier = Modifier.fillMaxWidth().height(46.dp)) {
                                Text(if (following) "正在更新" else if (person.following) "取消关注" else "关注这位成员")
                            }
                            TextButton(enabled = !following, onClick = onBlock, modifier = Modifier.fillMaxWidth()) { Text("屏蔽这位成员", color = NunuloColors.Danger) }
                        }
                    }
                }
                if (loading || error != null) {
                    item {
                        DetailLoadState(
                            title = if (loading) "正在整理成员主页" else "成员主页没有加载完整",
                            detail = "已加载的资料与记录会继续保留。",
                            loading = loading,
                            error = error,
                            onRetry = onRetry,
                        )
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("这位成员的旅行记录", style = MaterialTheme.typography.titleLarge)
                        Text("这里只显示你当前有权查看的内容。", color = NunuloColors.Muted, style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (!loading && error == null && records.isEmpty()) {
                    item { EmptyState("还没有可见记录", "对方可能还没有发布，或记录只向自己和关注者开放。") }
                }
                items(records, key = { it.id }) { record ->
                    FeedRecordCard(
                        record = record,
                        onOpen = { onOpenRecord(record) },
                        onLike = { onLike(record) },
                        liking = isLiking(record),
                        image = { recordImage(record) },
                    )
                }
            }
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
    onEditProfile: () -> Unit,
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
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TextButton(onClick = onEditProfile) { Text("编辑个人资料") }
            TextButton(onClick = onPickAvatar) { Text("更换头像") }
        }
        if (adminRole != null) {
            TextButton(onClick = onOpenAdmin) { Text("打开 Web 管理台") }
            Text("Android 只显示身份和入口；目录、举报、活动与存储治理仍在独立 Web Admin。", color = NunuloColors.Muted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
internal fun ProfileEditorPage(
    user: AuthUser?,
    saving: Boolean,
    onClose: () -> Unit,
    onSave: (String, String) -> Unit,
) {
    var displayName by rememberSaveable(user?.id) { mutableStateOf(user?.displayName.orEmpty()) }
    var bio by rememberSaveable(user?.id) { mutableStateOf(user?.bio.orEmpty()) }
    val valid = displayName.isNotBlank() && displayName.length <= 120 && bio.length <= 280
    Surface(color = NunuloColors.Background, modifier = Modifier.fillMaxSize()) {
        Column {
            Surface(color = NunuloColors.Paper, shadowElevation = 2.dp) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    RecordStamp("我", NunuloColors.Coral)
                    Column(Modifier.padding(start = 10.dp).weight(1f)) {
                        Text("编辑个人资料", style = MaterialTheme.typography.titleMedium)
                        Text("成员目录与记录作者信息会使用这里的资料", color = NunuloColors.Muted, style = MaterialTheme.typography.labelSmall)
                    }
                    TextButton(enabled = !saving, onClick = onClose) { Text("关闭") }
                }
            }
            LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f).fillMaxWidth().widthIn(max = 720.dp).align(Alignment.CenterHorizontally),
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("让别人认出你", style = MaterialTheme.typography.titleLarge)
                        Text("显示名用于记录和互动；简介可以写常带出门的伙伴、喜欢的作品或活动。", color = NunuloColors.Muted)
                    }
                }
                item {
                    SectionCard("公开资料", "用户名不能在这里修改，登录方式不会改变。") {
                        OutlinedTextField(
                            value = displayName,
                            onValueChange = { if (it.length <= 120) displayName = it },
                            label = { Text("显示名") },
                            supportingText = { Text("${displayName.length}/120") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = bio,
                            onValueChange = { if (it.length <= 280) bio = it },
                            label = { Text("个人简介（可选）") },
                            supportingText = { Text("${bio.length}/280") },
                            minLines = 4,
                            maxLines = 7,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        user?.username?.let { CoordinateTag("用户名 @$it") }
                    }
                }
                item {
                    Button(enabled = valid && !saving, onClick = { onSave(displayName, bio) }, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                        Text(if (saving) "正在保存" else "保存个人资料")
                    }
                }
            }
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
