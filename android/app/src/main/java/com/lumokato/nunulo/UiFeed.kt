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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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

@Composable
internal fun FeedScreen(controller: NunuloController) {
    LazyColumn(contentPadding = PaddingValues(bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                controller.collection?.let { target ->
                    target.event?.let { event ->
                        EventCollectionHeader(event, controller.feedItems, onBack = controller::loadFeed)
                    } ?: target.album?.let { album ->
                        AlbumCollectionHeader(album, controller.feedItems.size, onBack = controller::loadFeed)
                    } ?: target.catalogEntity?.let { entity ->
                        CatalogCollectionHeader(
                            entity = entity,
                            records = controller.feedItems,
                            onFollow = { controller.toggleCatalogFollow(entity) },
                            onBack = controller::loadFeed,
                        )
                    } ?: target.region?.let { region ->
                        RegionCollectionHeader(
                            region = region,
                            records = controller.feedItems,
                            loading = controller.collectionLoading,
                            onOpenRecord = controller::openRecord,
                            onBack = controller::loadFeed,
                        )
                    } ?: target.topic?.let { topic ->
                        TopicCollectionHeader(
                            topic = topic,
                            records = controller.feedItems,
                            loading = controller.collectionLoading,
                            onBack = controller::loadFeed,
                        )
                    } ?: target.partner?.let { partner ->
                        PartnerCollectionHeader(
                            partner = partner,
                            records = controller.feedItems,
                            loading = controller.collectionLoading,
                            onBack = { controller.returnToPartner(partner) },
                        )
                    } ?: SectionCard(target.title, target.subtitle) {
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
                        Text(if (controller.collection == null) "今天，伙伴去了哪里？" else "这里留下的记录", style = MaterialTheme.typography.titleLarge)
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
                item {
                    Box(Modifier.padding(horizontal = 12.dp)) {
                        EmptyState(
                            if (controller.collection == null) "这里还没有内容" else "还没有关联记录",
                            if (controller.collection == null) "先发布第一条照片记录，或关注作品、角色和伙伴。" else "发布记录时选择这个目录项，真实动态就会收进这里。",
                        )
                    }
                }
            }
        }
        items(controller.feedItems, key = CheckinItem::id) { record ->
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                FeedCard(record, controller, Modifier.fillMaxWidth().padding(horizontal = 12.dp))
                controller.collection?.album?.let { album ->
                    Surface(
                        color = NunuloColors.Lilac,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    ) {
                        Row(Modifier.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("已收录在 ${album.title}", color = NunuloColors.Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            TextButton(
                                enabled = record.id !in controller.albumRemovingRecordIds,
                                onClick = { controller.removeFromAlbum(record) },
                            ) { Text(if (record.id in controller.albumRemovingRecordIds) "移除中" else "移出合集") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun PartnerCollectionHeader(
    partner: PartnerItem,
    records: List<CheckinItem>,
    loading: Boolean = false,
    onBack: () -> Unit,
) {
    val stats = partnerCollectionStats(partner.id, records)
    Surface(color = NunuloColors.Soft, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                RecordStamp("伴", NunuloColors.MapBlue)
                Column(Modifier.weight(1f)) {
                    Text(partner.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(partner.publicCode, color = NunuloColors.MapBlue, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
                EventHeaderTag(visibilityLabel(partner.visibility))
            }
            val identity = listOfNotNull(partner.itemType?.name, partner.work?.name, partner.character?.name)
            if (identity.isNotEmpty()) {
                Text(identity.joinToString(" · "), color = NunuloColors.Muted, style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                EventHeaderTag("${partner.recordCount} 条记录")
                EventHeaderTag(if (loading && records.isEmpty()) "正在读取" else "当前可见 ${records.size} 条")
            }
            Surface(color = NunuloColors.Paper, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 9.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("这位伙伴走过的地方", fontWeight = FontWeight.Bold)
                    Text(
                        if (loading && records.isEmpty()) "正在整理成员、相遇伙伴、地点与活动…" else "${stats.memberCount} 位成员 · 见过 ${stats.meetingPartnerCount} 位伙伴 · ${stats.placeCount} 个地点 · ${stats.eventCount} 个活动",
                        color = NunuloColors.Muted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            Text("这里只显示你当前有权查看的关联记录；私人出镜关系不会因此公开。", color = NunuloColors.Muted, style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = onBack, contentPadding = PaddingValues(horizontal = 4.dp)) { Text("返回伙伴主页") }
        }
    }
}

@Composable
internal fun TopicCollectionHeader(
    topic: TopicItem,
    records: List<CheckinItem>,
    loading: Boolean = false,
    onBack: () -> Unit,
) {
    val stats = catalogCollectionStats(records)
    Surface(color = NunuloColors.Lilac, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                RecordStamp("题", NunuloColors.Coral)
                Column(Modifier.weight(1f)) {
                    Text(topic.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text("Nunulo 专题收录", color = NunuloColors.Muted, style = MaterialTheme.typography.bodySmall)
                }
                EventHeaderTag(if (topic.status in setOf("active", "published")) "已发布" else "整理中")
            }
            if (topic.description.isNotBlank()) {
                Text(topic.description, maxLines = 4, overflow = TextOverflow.Ellipsis)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                EventHeaderTag("${topic.checkinIds.size} 条收录")
                EventHeaderTag(if (loading && records.isEmpty()) "正在读取" else "当前可见 ${records.size} 条")
            }
            Surface(color = NunuloColors.Paper, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 9.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("专题里的真实足迹", fontWeight = FontWeight.Bold)
                    Text(
                        if (loading && records.isEmpty()) "正在整理成员、伙伴与地点…" else "${stats.memberCount} 位成员 · ${stats.partnerCount} 位伙伴 · ${stats.placeCount} 个地点",
                        color = NunuloColors.Muted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            Text("专题只负责收录，原记录、照片、作者和互动都保持不变。", color = NunuloColors.Muted, style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = onBack, contentPadding = PaddingValues(horizontal = 4.dp)) { Text("返回动态") }
        }
    }
}

@Composable
internal fun RegionCollectionHeader(
    region: WorldRegionItem,
    records: List<CheckinItem>,
    loading: Boolean = false,
    onOpenRecord: (CheckinItem) -> Unit,
    onBack: () -> Unit,
) {
    var mapOpen by rememberSaveable(region.key) { mutableStateOf(false) }
    val places = regionCollectionPlaces(records)
    Surface(color = NunuloColors.Soft, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                RecordStamp("地", NunuloColors.MapBlue)
                Column(Modifier.weight(1f)) {
                    Text(region.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(
                        listOfNotNull(region.city, region.province, region.countryCode).distinct().joinToString(" · ").ifBlank { "世界发现地区" },
                        color = NunuloColors.Muted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                EventHeaderTag("世界发现")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                EventHeaderTag("${region.recordCount} 条记录")
                EventHeaderTag("${region.userCount} 位成员")
                EventHeaderTag(if (loading && places.isEmpty()) "地点同步中" else "${places.size} 个地点")
            }
            Text("这里只汇总明确加入世界发现且达到公开阈值的记录。", color = NunuloColors.Muted, style = MaterialTheme.typography.bodySmall)
            if (loading && places.isEmpty()) {
                Text("正在整理这个地区的地点与记录…", color = NunuloColors.Muted)
            } else if (places.isEmpty()) {
                Text("当前加载的记录还没有可显示地点。", color = NunuloColors.Muted)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("去过的地点", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    TextButton(onClick = { mapOpen = !mapOpen }) { Text(if (mapOpen) "收起地图" else "查看地图") }
                }
                if (mapOpen) {
                    FootprintMap(
                        home = null,
                        items = places.map { place ->
                            FootprintItem(
                                checkinId = place.representativeRecordId,
                                placeId = place.id,
                                placeName = place.name,
                                latitude = place.latitude,
                                longitude = place.longitude,
                                takenAt = null,
                                thumbUrl = place.representativeThumbUrl,
                            )
                        },
                        onOpen = { item -> records.firstOrNull { it.id == item.checkinId }?.let(onOpenRecord) },
                    )
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(places, key = RegionPlaceSummary::id) { place ->
                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.width(210.dp).clickable {
                                records.firstOrNull { it.id == place.representativeRecordId }?.let(onOpenRecord)
                            },
                        ) {
                            Column(Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(place.name, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text("${place.recordCount} 条记录 · ${formatCoordinate(place.latitude, place.longitude)}", color = NunuloColors.Muted, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
            TextButton(onClick = onBack, contentPadding = PaddingValues(horizontal = 4.dp)) { Text("返回动态") }
        }
    }
}

@Composable
internal fun AlbumCollectionHeader(
    album: AlbumItem,
    visibleCount: Int,
    onBack: () -> Unit,
) {
    Surface(color = NunuloColors.Lilac, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                RecordStamp("集", NunuloColors.MapBlue)
                Column(Modifier.weight(1f)) {
                    Text(album.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text("我的照片记录册", color = NunuloColors.Muted, style = MaterialTheme.typography.bodySmall)
                }
                EventHeaderTag(visibilityLabel(album.visibility))
            }
            if (album.description.isNotBlank()) {
                Text(album.description, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                EventHeaderTag("${album.itemCount} 条收录")
                EventHeaderTag("当前可见 $visibleCount 条")
            }
            Text("从这里移除只会取消收录，原记录、照片和互动都会保留。", color = NunuloColors.Muted, style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = onBack, contentPadding = PaddingValues(horizontal = 4.dp)) { Text("返回动态") }
        }
    }
}

@Composable
internal fun CatalogCollectionHeader(
    entity: CatalogEntityItem,
    records: List<CheckinItem>,
    onFollow: () -> Unit,
    onBack: () -> Unit,
) {
    val stats = catalogCollectionStats(records)
    val type = CatalogTypeFilter.entries.firstOrNull { it.key == entity.entityType }
    val hierarchy = listOfNotNull(entity.work?.name, entity.group?.name).distinct()
    Surface(color = if (entity.followed) NunuloColors.Soft else NunuloColors.Lilac, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                RecordStamp(type?.label?.take(1) ?: "录", if (entity.followed) NunuloColors.Coral else NunuloColors.MapBlue)
                Column(Modifier.weight(1f)) {
                    Text(entity.canonicalName, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(
                        listOfNotNull(type?.label, hierarchy.takeIf { it.isNotEmpty() }?.joinToString(" · ")).joinToString(" · "),
                        color = NunuloColors.Muted,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                EventHeaderTag(if (entity.status == "active") "正式" else "待审核")
            }
            if (entity.aliases.isNotEmpty()) {
                Text("别名 · ${entity.aliases.take(3).joinToString(" / ")}", color = NunuloColors.Muted, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                EventHeaderTag("${entity.recordCount} 条记录")
                EventHeaderTag(if (entity.followed) "已关注" else "未关注")
            }
            Surface(color = NunuloColors.Paper, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 9.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("当前可见记录", fontWeight = FontWeight.Bold)
                    Text(
                        "${stats.memberCount} 位成员 · ${stats.partnerCount} 位伙伴 · ${stats.placeCount} 个地点 · ${stats.eventCount} 个活动",
                        color = NunuloColors.Muted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack, contentPadding = PaddingValues(horizontal = 4.dp)) { Text("返回动态") }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onFollow) { Text(if (entity.followed) "取消关注" else "关注") }
            }
        }
    }
}

@Composable
internal fun EventCollectionHeader(
    event: EventItem,
    records: List<CheckinItem>,
    onBack: () -> Unit,
    now: Instant = Instant.now(),
) {
    val stats = eventCollectionStats(records)
    Surface(color = NunuloColors.Lilac, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                RecordStamp("活", NunuloColors.Leaf)
                Column(Modifier.weight(1f)) {
                    Text(event.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(
                        listOf(eventTypeLabel(event.eventType), event.series?.canonicalName).filterNotNull().joinToString(" · "),
                        color = NunuloColors.Muted,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                EventHeaderTag(if (event.official) "官方" else "共建")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                EventHeaderTag(event.periodLabel(now))
                EventHeaderTag("${event.recordCount} 条记录")
            }
            val schedule = listOfNotNull(event.startsAt?.let(::shortDate), event.endsAt?.let(::shortDate)).distinct().joinToString("—")
            if (schedule.isNotBlank()) Text(schedule, color = NunuloColors.MapBlue, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
            event.place?.let { place ->
                Surface(color = NunuloColors.Paper, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
                        Text(place.name, fontWeight = FontWeight.Bold)
                        Text("活动地点 · ${formatCoordinate(place.latitude, place.longitude)}", color = NunuloColors.Muted, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            if (event.description.isNotBlank()) Text(event.description, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Text(
                "当前可见记录中 · ${stats.memberCount} 位成员 · ${stats.partnerCount} 位伙伴 · ${stats.characterCount} 个角色",
                color = NunuloColors.Muted,
                style = MaterialTheme.typography.bodySmall,
            )
            TextButton(onClick = onBack, contentPadding = PaddingValues(horizontal = 4.dp)) { Text("返回动态") }
        }
    }
}

@Composable
private fun EventHeaderTag(label: String) {
    Surface(color = NunuloColors.Paper, shape = RoundedCornerShape(50)) {
        Text(
            label,
            color = NunuloColors.MapBlue,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
        )
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
        liking = controller.isLiking(record),
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
    liking: Boolean = false,
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
                    TextButton(enabled = !liking, onClick = onLike, contentPadding = PaddingValues(horizontal = 4.dp)) {
                        Text(if (liking) "更新中" else "${if (record.liked) "♥" else "♡"} ${record.likeCount}", color = if (record.liked) NunuloColors.Coral else NunuloColors.Muted)
                    }
                    Text("评 ${record.commentCount}", color = NunuloColors.Muted, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
internal fun RecordPhotoGallery(
    photos: List<PhotoItem>,
    image: @Composable (PhotoItem, Modifier) -> Unit,
) {
    if (photos.isEmpty()) {
        Surface(color = NunuloColors.Placeholder, shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth().height(260.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("照片暂时没有加载出来", fontWeight = FontWeight.Bold)
                Text("可以继续查看文字与关系，或稍后重新加载详情。", color = NunuloColors.Muted, style = MaterialTheme.typography.bodySmall)
            }
        }
        return
    }
    val listState = rememberLazyListState()
    val currentIndex by remember { derivedStateOf { listState.firstVisibleItemIndex.coerceIn(0, photos.lastIndex) } }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LazyRow(state = listState, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(photos, key = { _, photo -> photo.id }) { index, photo ->
                Box(Modifier.width(310.dp)) {
                    image(photo, Modifier.fillMaxWidth())
                    Surface(
                        color = NunuloColors.Ink.copy(alpha = 0.78f),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.align(Alignment.TopEnd).padding(10.dp),
                    ) {
                        Text("${index + 1} / ${photos.size}", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp))
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${photos.size} 张照片 · 左右滑动查看", color = NunuloColors.Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                photos.indices.forEach { index ->
                    Box(
                        Modifier.size(if (index == currentIndex) 8.dp else 6.dp)
                            .background(if (index == currentIndex) NunuloColors.Coral else NunuloColors.Hairline, RoundedCornerShape(50))
                    )
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
    var editDraftConflictOpen by rememberSaveable(record.id) { mutableStateOf(false) }
    var deleteConfirmOpen by rememberSaveable(record.id) { mutableStateOf(false) }
    var removingPartnerId by rememberSaveable(record.id) { mutableStateOf<String?>(null) }
    var deletingCommentId by rememberSaveable(record.id) { mutableStateOf<String?>(null) }
    var partnerCode by rememberSaveable(record.id) { mutableStateOf("") }
    if (reportOpen) {
        AlertDialog(
            onDismissRequest = { reportOpen = false },
            title = { Text("投诉这条记录") },
            text = { OutlinedTextField(reportReason, { if (it.length <= 120) reportReason = it }, label = { Text("原因") }, minLines = 3, modifier = Modifier.fillMaxWidth()) },
            confirmButton = {
                TextButton(enabled = reportReason.isNotBlank() && !controller.isReporting(record), onClick = {
                    controller.reportRecord(record, reportReason) {
                        reportReason = ""
                        reportOpen = false
                    }
                }) { Text(if (controller.isReporting(record)) "提交中" else "提交", color = NunuloColors.Danger) }
            },
            dismissButton = { TextButton(onClick = { reportOpen = false }) { Text("取消") } },
        )
        return
    }
    if (editDraftConflictOpen) {
        EditDraftConflictDialog(
            photoCount = controller.draft.photos.size,
            onConfirm = {
                editDraftConflictOpen = false
                controller.editRecord(record, replaceConflictingDraft = true)
            },
            onDismiss = { editDraftConflictOpen = false },
        )
        return
    }
    if (deleteConfirmOpen) {
        RecordDeleteConfirmationDialog(
            onConfirm = {
                deleteConfirmOpen = false
                controller.deleteRecord(record)
            },
            onDismiss = { deleteConfirmOpen = false },
        )
        return
    }
    removingPartnerId?.let { partnerId ->
        val partner = record.partners.firstOrNull { it.id == partnerId }
        if (partner != null) {
            ConfirmActionDialog(
                title = "移除出镜伙伴？",
                body = "${partner.name} 会从这条记录和由此推导的相遇统计中移除；伙伴资料、照片和其他记录不会删除。",
                confirmLabel = "移除伙伴关系",
                onConfirm = {
                    removingPartnerId = null
                    controller.removePartnerFromRecord(record, partner)
                },
                onDismiss = { removingPartnerId = null },
            )
            return
        }
        removingPartnerId = null
    }
    deletingCommentId?.let { commentId ->
        val commentToDelete = controller.comments.firstOrNull { it.id == commentId }
        if (commentToDelete != null) {
            CommentDeleteConfirmationDialog(
                deleting = controller.isDeletingComment(commentToDelete),
                onConfirm = {
                    if (!controller.isDeletingComment(commentToDelete)) {
                        controller.deleteComment(record, commentToDelete) { deletingCommentId = null }
                    }
                },
                onDismiss = { if (!controller.isDeletingComment(commentToDelete)) deletingCommentId = null },
            )
            return
        }
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
                    RecordPhotoGallery(record.photos) { photo, modifier ->
                        RemoteImage(photo.displayUrl ?: photo.thumbUrl ?: photo.originalUrl, controller.baseUrl, controller.mediaApi, modifier = modifier, aspect = 0.84f)
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
                                items(record.itemTypes) { item -> AssistChip(onClick = { controller.openCatalogRef("item_type", item) }, label = { Text(item.name) }) }
                                items(record.works) { item -> AssistChip(onClick = { controller.openCatalogRef("work", item) }, label = { Text(item.name) }) }
                                items(record.characters) { item -> AssistChip(onClick = { controller.openCatalogRef("character", item) }, label = { Text(item.name) }) }
                            }
                        }
                    }
                }
                if (record.partners.isNotEmpty()) {
                    item {
                        SectionCard("出镜伙伴", "每段出镜关系有独立的公开范围，不会改变伙伴资料本身的隐私。") {
                            record.partners.forEach { partner ->
                                PartnerRelationCard(
                                    partner = partner,
                                    recordAuthorUserId = record.userId,
                                    viewerUserId = controller.currentUser?.id,
                                    mutating = controller.isMutatingPartnerRelation(record, partner),
                                    onOpen = { controller.closeRecord(); controller.selectPartner(partner); controller.selectTab(AppTab.Partners) },
                                    onChangeVisibility = { controller.setPartnerRelationVisibility(record, partner, it) },
                                    onRemove = { removingPartnerId = partner.id },
                                )
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
                            Button(enabled = !controller.isLiking(record), onClick = { controller.toggleLike(record) }) { Text(if (controller.isLiking(record)) "更新中" else "${if (record.liked) "♥" else "♡"} ${record.likeCount}") }
                            Text("${record.commentCount} 条评论", color = NunuloColors.Muted, modifier = Modifier.padding(start = 10.dp))
                            Spacer(Modifier.weight(1f))
                            if (!record.canEdit) TextButton(onClick = { reportOpen = true }) { Text("投诉", color = NunuloColors.Danger) }
                        }
                    }
                }
                item {
                    SectionCard("补登记伙伴", "合照中的其他伙伴会在双方确认后进入相遇记录。") {
                        OutlinedTextField(partnerCode, { partnerCode = it }, label = { Text("伙伴编号，例如 N-...") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        TextButton(
                            enabled = partnerCode.isNotBlank() && !controller.isRequestingPartner(record),
                            onClick = { controller.requestPartnerForRecord(record, partnerCode) { partnerCode = "" } },
                        ) { Text(if (controller.isRequestingPartner(record)) "提交中" else "提交补登记") }
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
                        CommentCard(
                            comment = item,
                            canDelete = controller.canDeleteComment(record, item),
                            deleting = controller.isDeletingComment(item),
                            onDelete = { deletingCommentId = item.id },
                        )
                    }
                }
                item {
                    SectionCard("写下回应") {
                        OutlinedTextField(comment, { comment = it }, label = { Text("评论") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                        Button(
                            enabled = comment.isNotBlank() && !controller.isCommenting(record),
                            onClick = { controller.addComment(record, comment) { comment = "" } },
                        ) { Text(if (controller.isCommenting(record)) "发布中" else "发布评论") }
                    }
                }
                if (record.canEdit) {
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(
                                enabled = !controller.isDeletingRecord(record),
                                onClick = { deleteConfirmOpen = true },
                            ) { Text(if (controller.isDeletingRecord(record)) "删除中" else "删除", color = NunuloColors.Danger) }
                            Button(onClick = {
                                if (controller.hasConflictingDraft(record)) editDraftConflictOpen = true
                                else controller.editRecord(record)
                            }) { Text("编辑记录") }
                        }
                    }
                }
            }
            }
        }
    }
}

@Composable
internal fun CommentCard(
    comment: CommentItem,
    canDelete: Boolean,
    deleting: Boolean,
    onDelete: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().background(NunuloColors.Paper, RoundedCornerShape(18.dp)).padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(comment.displayName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            if (canDelete) {
                TextButton(enabled = !deleting, onClick = onDelete) {
                    Text(if (deleting) "删除中" else "删除", color = NunuloColors.Danger)
                }
            }
        }
        Text(comment.body)
        Text(shortDate(comment.createdAt), color = NunuloColors.Muted, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
internal fun CommentDeleteConfirmationDialog(deleting: Boolean, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    ConfirmActionDialog(
        title = "删除这条评论？",
        body = "评论会从这条记录中移除，记录、照片、点赞和其他评论都会保留。",
        confirmLabel = if (deleting) "删除中" else "删除评论",
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}

@Composable
internal fun PartnerRelationCard(
    partner: PartnerItem,
    recordAuthorUserId: Int,
    viewerUserId: Int?,
    mutating: Boolean,
    onOpen: () -> Unit,
    onChangeVisibility: (String) -> Unit,
    onRemove: () -> Unit,
) {
    val state = partner.relationUiState(recordAuthorUserId, viewerUserId)
    Surface(
        color = if (partner.relationVisibility == "public") NunuloColors.Lilac else NunuloColors.Placeholder,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 13.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                RecordStamp(partner.name.take(1).ifBlank { "伴" }, if (partner.relationVisibility == "public") NunuloColors.MapBlue else NunuloColors.Muted)
                Column(Modifier.weight(1f)) {
                    Text(partner.name, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(partner.publicCode, color = NunuloColors.MapBlue, style = MaterialTheme.typography.labelSmall)
                }
                CoordinateTag(state.confirmationLabel)
            }
            Text(state.visibilityLabel, color = if (partner.relationVisibility == "public") NunuloColors.Leaf else NunuloColors.Ink, fontWeight = FontWeight.Bold)
            Text(state.visibilityDetail, color = NunuloColors.Muted, style = MaterialTheme.typography.bodySmall)
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onOpen, contentPadding = PaddingValues(horizontal = 4.dp)) { Text("查看伙伴") }
                Spacer(Modifier.weight(1f))
                if (state.canManage) {
                    TextButton(enabled = !mutating, onClick = { onChangeVisibility(state.nextVisibility) }) {
                        Text(if (mutating) "更新中" else state.visibilityActionLabel, color = NunuloColors.MapBlue)
                    }
                    TextButton(enabled = !mutating, onClick = onRemove) { Text("移除", color = NunuloColors.Danger) }
                }
            }
        }
    }
}

@Composable
internal fun EditDraftConflictDialog(photoCount: Int, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    ConfirmActionDialog(
        title = "替换当前草稿？",
        body = "你还有一条包含 $photoCount 张照片的未完成草稿。继续编辑会移除这条本机草稿和它的照片副本；已经发布或上传的照片资产不会删除。",
        confirmLabel = "替换并编辑",
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}

@Composable
internal fun RecordDeleteConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    ConfirmActionDialog(
        title = "删除这条记录？",
        body = "记录会从动态、足迹和相关聚合中移除，评论与伙伴关系也不再显示。照片资产仍保留在你的个人媒体库中。",
        confirmLabel = "删除记录",
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}

@Composable
internal fun NotificationsDialog(controller: NunuloController) {
    Dialog(onDismissRequest = { controller.notificationsOpen = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        NotificationsPage(
            notifications = controller.notifications,
            error = controller.notificationsError,
            markingAll = controller.notificationsMarkingAll,
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
    markingAll: Boolean = false,
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
                            TextButton(enabled = unreadCount > 0 && !markingAll, onClick = onMarkAllRead) {
                                Text(if (markingAll) "同步中" else "全部已读")
                            }
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
