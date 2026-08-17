package com.lumokato.nunulo

import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.OffsetDateTime
import java.util.UUID

internal data class PhotoItem(
    val id: String,
    val contentSha256: String? = null,
    val takenAt: String? = null,
    val exifTakenAt: String? = null,
    val exifLatitude: Double? = null,
    val exifLongitude: Double? = null,
    val capturedAtSource: String = "unknown",
    val gpsSource: String = "none",
    val metadataStatus: String = "unknown",
    val metadataWarnings: List<String> = emptyList(),
    val thumbUrl: String? = null,
    val displayUrl: String? = null,
    val originalUrl: String? = null,
)

internal data class CatalogRef(
    val id: String,
    val name: String,
)

internal data class CatalogEntityItem(
    val id: String,
    val entityType: String,
    val canonicalName: String,
    val aliases: List<String> = emptyList(),
    val status: String = "active",
    val followed: Boolean = false,
    val recordCount: Int = 0,
    val work: CatalogRef? = null,
    val group: CatalogRef? = null,
)

internal fun CatalogEntityItem.matchesCatalogQuery(query: String): Boolean {
    val needle = query.catalogSearchKey()
    if (needle.isBlank()) return true
    return sequenceOf(canonicalName, work?.name.orEmpty(), group?.name.orEmpty())
        .plus(aliases.asSequence())
        .any { it.catalogSearchKey().contains(needle) }
}

private fun String.catalogSearchKey(): String = lowercase().filter(Char::isLetterOrDigit)

internal enum class CatalogTypeFilter(val key: String?, val label: String) {
    All(null, "全部"),
    ItemType("item_type", "物件"),
    Work("work", "作品"),
    Group("group", "组合"),
    Character("character", "角色"),
}

internal enum class CatalogFollowFilter(val label: String) {
    All("全部目录"),
    Followed("我的关注"),
}

internal fun catalogBrowseItems(
    catalog: Map<String, List<CatalogEntityItem>>,
    query: String,
    type: CatalogTypeFilter,
    follow: CatalogFollowFilter,
): List<CatalogEntityItem> = catalog.values
    .flatten()
    .distinctBy { "${it.entityType}:${it.id}" }
    .filter { entity -> type.key == null || entity.entityType == type.key }
    .filter { entity -> follow == CatalogFollowFilter.All || entity.followed }
    .filter { entity -> entity.matchesCatalogQuery(query) }
    .sortedWith(
        compareByDescending<CatalogEntityItem> { it.followed }
            .thenByDescending(CatalogEntityItem::recordCount)
            .thenBy(CatalogEntityItem::canonicalName),
    )

internal data class PartnerItem(
    val id: String,
    val publicCode: String,
    val ownerUserId: Int,
    val name: String,
    val visibility: String,
    val moderationStatus: String,
    val itemType: CatalogRef?,
    val work: CatalogRef?,
    val character: CatalogRef?,
    val coverUrl: String?,
    val recordCount: Int,
    val canEdit: Boolean,
    val authorApproved: Boolean = true,
    val ownerApproved: Boolean = true,
    val relationStatus: String = "approved",
    val relationVisibility: String = "public",
)

internal enum class PartnerVisibilityFilter(val key: String?, val label: String) {
    All(null, "全部"),
    Public("public", "全部成员"),
    Followers("followers", "关注者"),
    Private("private", "仅自己"),
}

internal fun filterPartners(
    partners: List<PartnerItem>,
    query: String,
    visibility: PartnerVisibilityFilter,
): List<PartnerItem> {
    val needle = query.catalogSearchKey()
    return partners
        .asSequence()
        .filter { partner -> visibility.key == null || partner.visibility == visibility.key }
        .filter { partner ->
            needle.isBlank() || sequenceOf(
                partner.name,
                partner.publicCode,
                partner.itemType?.name.orEmpty(),
                partner.work?.name.orEmpty(),
                partner.character?.name.orEmpty(),
            ).any { it.catalogSearchKey().contains(needle) }
        }
        .sortedWith(compareByDescending<PartnerItem> { it.recordCount }.thenBy(PartnerItem::name))
        .toList()
}

internal data class PartnerRelationUiState(
    val confirmationLabel: String,
    val visibilityLabel: String,
    val visibilityDetail: String,
    val canManage: Boolean,
    val nextVisibility: String,
    val visibilityActionLabel: String,
)

internal fun PartnerItem.relationUiState(recordAuthorUserId: Int, viewerUserId: Int?): PartnerRelationUiState {
    val isPublic = relationVisibility == "public"
    return PartnerRelationUiState(
        confirmationLabel = if (relationStatus == "approved") "双方已确认" else "等待确认",
        visibilityLabel = if (isPublic) "公开显示" else "仅相关成员",
        visibilityDetail = if (isPublic) {
            "确认后会出现在伙伴主页与相遇统计中。"
        } else {
            "只向记录作者和伙伴所有者显示这段关系。"
        },
        canManage = viewerUserId != null && viewerUserId in setOf(recordAuthorUserId, ownerUserId),
        nextVisibility = if (isPublic) "private" else "public",
        visibilityActionLabel = if (isPublic) "改为仅相关成员" else "改为公开显示",
    )
}

internal data class PartnerRequestItem(
    val checkinId: String,
    val partnerId: String,
    val partnerName: String,
    val partnerCode: String,
    val recordAuthorUserId: Int,
    val recordAuthorDisplayName: String,
    val partnerOwnerUserId: Int,
    val partnerOwnerDisplayName: String,
    val authorApproved: Boolean,
    val ownerApproved: Boolean,
)

internal data class PartnerRequestUiState(
    val description: String,
    val statusLabel: String,
    val needsDecision: Boolean,
)

internal val PartnerRequestItem.requestKey: String get() = "$checkinId:$partnerId"

internal fun PartnerRequestItem.uiState(viewerUserId: Int?): PartnerRequestUiState {
    val viewerIsAuthor = viewerUserId != null && viewerUserId == recordAuthorUserId
    val viewerIsOwner = viewerUserId != null && viewerUserId == partnerOwnerUserId
    val myApproved = when {
        viewerIsAuthor -> authorApproved
        viewerIsOwner -> ownerApproved
        else -> true
    }
    val otherApproved = when {
        viewerIsAuthor -> ownerApproved
        viewerIsOwner -> authorApproved
        else -> authorApproved && ownerApproved
    }
    val counterpart = when {
        viewerIsAuthor -> partnerOwnerDisplayName
        viewerIsOwner -> recordAuthorDisplayName
        else -> "相关成员"
    }
    val description = when {
        viewerIsAuthor && !authorApproved -> "$partnerOwnerDisplayName 希望把 $partnerName 补登记到你发布的记录。"
        viewerIsOwner && !ownerApproved -> "$recordAuthorDisplayName 希望在记录中登记你的伙伴 $partnerName。"
        viewerIsAuthor -> "你已确认把 $partnerName 登记到这条记录，正在等待 $counterpart 确认。"
        viewerIsOwner -> "你已确认 $partnerName 出现在这条记录中，正在等待 $counterpart 确认。"
        else -> "$partnerName 的补登记正在由相关成员确认。"
    }
    return PartnerRequestUiState(
        description = description,
        statusLabel = when {
            !viewerIsAuthor && !viewerIsOwner -> "等待相关成员确认"
            !myApproved -> "需要你确认"
            !otherApproved -> "等待对方确认"
            else -> "双方已确认"
        },
        needsDecision = (viewerIsAuthor || viewerIsOwner) && !myApproved,
    )
}

internal data class PartnerMeetingItem(
    val partner: PartnerItem,
    val meetingCount: Int,
    val firstMetAt: String?,
    val lastMetAt: String?,
)

internal data class PlaceItem(
    val id: String,
    val name: String,
    val countryCode: String? = null,
    val province: String? = null,
    val city: String? = null,
    val district: String? = null,
    val latitude: Double,
    val longitude: Double,
    val address: String? = null,
    val privacyLevel: String = "exact",
)

internal data class EventSeriesItem(
    val id: String,
    val canonicalName: String,
    val status: String,
)

internal data class EventItem(
    val id: String,
    val name: String,
    val eventType: String,
    val visibility: String,
    val status: String,
    val official: Boolean,
    val place: PlaceItem?,
    val series: EventSeriesItem?,
    val startsAt: String?,
    val endsAt: String?,
    val description: String,
    val recordCount: Int,
    val canEdit: Boolean,
)

internal enum class EventPeriodFilter(val label: String) {
    Upcoming("近期"),
    Past("往期"),
    All("全部"),
}

internal enum class EventKindFilter(val label: String) {
    All("全部类型"),
    Offline("线下"),
    Online("线上"),
}

private fun String?.eventInstant(): Instant? = this?.let { value ->
    runCatching { OffsetDateTime.parse(value).toInstant() }
        .recoverCatching { Instant.parse(value) }
        .getOrNull()
}

internal fun EventItem.isPast(now: Instant): Boolean = (endsAt.eventInstant() ?: startsAt.eventInstant())?.isBefore(now) == true

internal fun EventItem.periodLabel(now: Instant): String {
    val start = startsAt.eventInstant()
    val end = endsAt.eventInstant()
    return when {
        (end ?: start)?.isBefore(now) == true -> "往期"
        start != null && !start.isAfter(now) && (end == null || !end.isBefore(now)) -> "进行中"
        start != null -> "即将开始"
        else -> "日期待定"
    }
}

internal fun eventBrowseItems(
    events: List<EventItem>,
    query: String,
    period: EventPeriodFilter,
    kind: EventKindFilter,
    now: Instant,
): List<EventItem> {
    val needle = query.trim().lowercase()
    val filtered = events.filter { event ->
        val matchesQuery = needle.isBlank() || listOf(
            event.name,
            event.description,
            event.place?.name.orEmpty(),
            event.series?.canonicalName.orEmpty(),
        ).any { it.lowercase().contains(needle) }
        val matchesPeriod = when (period) {
            EventPeriodFilter.Upcoming -> !event.isPast(now)
            EventPeriodFilter.Past -> event.isPast(now)
            EventPeriodFilter.All -> true
        }
        val matchesKind = when (kind) {
            EventKindFilter.All -> true
            EventKindFilter.Offline -> event.eventType.startsWith("offline_")
            EventKindFilter.Online -> event.eventType.startsWith("online_")
        }
        matchesQuery && matchesPeriod && matchesKind
    }
    val (upcoming, past) = filtered.partition { !it.isPast(now) }
    val orderedUpcoming = upcoming.sortedBy { it.startsAt.eventInstant() ?: Instant.MAX }
    val orderedPast = past.sortedByDescending { it.endsAt.eventInstant() ?: it.startsAt.eventInstant() ?: Instant.MIN }
    return when (period) {
        EventPeriodFilter.Upcoming -> orderedUpcoming
        EventPeriodFilter.Past -> orderedPast
        EventPeriodFilter.All -> orderedUpcoming + orderedPast
    }
}

internal data class EventCollectionStats(
    val partnerCount: Int,
    val characterCount: Int,
    val memberCount: Int,
)

internal fun eventCollectionStats(records: List<CheckinItem>): EventCollectionStats = EventCollectionStats(
    partnerCount = records.flatMap(CheckinItem::partners).map(PartnerItem::id).distinct().size,
    characterCount = records.flatMap(CheckinItem::characters).map(CatalogRef::id).distinct().size,
    memberCount = records.map(CheckinItem::userId).filter { it > 0 }.distinct().size,
)

internal data class CatalogCollectionStats(
    val memberCount: Int,
    val partnerCount: Int,
    val placeCount: Int,
    val eventCount: Int,
)

internal fun catalogCollectionStats(records: List<CheckinItem>): CatalogCollectionStats = CatalogCollectionStats(
    memberCount = records.map(CheckinItem::userId).filter { it > 0 }.distinct().size,
    partnerCount = records.flatMap(CheckinItem::partners).map(PartnerItem::id).distinct().size,
    placeCount = records.mapNotNull { record ->
        when {
            record.latitude != null && record.longitude != null -> "${record.placeName.trim().lowercase()}@${record.latitude},${record.longitude}"
            record.placeName.isNotBlank() -> record.placeName.trim().lowercase()
            else -> null
        }
    }.distinct().size,
    eventCount = records.flatMap(CheckinItem::events).map(EventItem::id).distinct().size,
)

internal data class RegionPlaceSummary(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val recordCount: Int,
    val representativeRecordId: String,
    val representativeThumbUrl: String?,
)

internal fun regionCollectionPlaces(records: List<CheckinItem>): List<RegionPlaceSummary> = records
    .filter { it.latitude != null && it.longitude != null }
    .groupBy { record ->
        record.placeId?.takeIf(String::isNotBlank)
            ?: "${record.placeName.trim().lowercase()}@${record.latitude},${record.longitude}"
    }
    .map { (id, groupedRecords) ->
        val representative = groupedRecords.first()
        RegionPlaceSummary(
            id = id,
            name = representative.placeName.ifBlank { "未命名地点" },
            latitude = representative.latitude!!,
            longitude = representative.longitude!!,
            recordCount = groupedRecords.size,
            representativeRecordId = representative.id,
            representativeThumbUrl = representative.thumbUrl ?: representative.displayUrl,
        )
    }
    .sortedWith(compareByDescending<RegionPlaceSummary>(RegionPlaceSummary::recordCount).thenBy(RegionPlaceSummary::name))

internal data class PartnerCollectionStats(
    val memberCount: Int,
    val meetingPartnerCount: Int,
    val placeCount: Int,
    val eventCount: Int,
)

internal fun partnerCollectionStats(partnerId: String, records: List<CheckinItem>): PartnerCollectionStats = PartnerCollectionStats(
    memberCount = records.map(CheckinItem::userId).filter { it > 0 }.distinct().size,
    meetingPartnerCount = records.flatMap(CheckinItem::partners).filterNot { it.id == partnerId }.map(PartnerItem::id).distinct().size,
    placeCount = records.mapNotNull { record ->
        record.placeId?.takeIf(String::isNotBlank) ?: when {
            record.latitude != null && record.longitude != null -> "${record.placeName}@${record.latitude},${record.longitude}"
            record.placeName.isNotBlank() -> record.placeName.trim().lowercase()
            else -> null
        }
    }.distinct().size,
    eventCount = records.flatMap(CheckinItem::events).map(EventItem::id).distinct().size,
)

internal data class TopicItem(
    val id: String,
    val title: String,
    val description: String,
    val status: String,
    val checkinIds: List<String>,
)

internal data class WorldRegionItem(
    val key: String,
    val name: String,
    val countryCode: String?,
    val province: String?,
    val city: String?,
    val recordCount: Int,
    val userCount: Int,
    val latitude: Double,
    val longitude: Double,
    val representativeThumbUrl: String?,
    val eligible: Boolean,
)

internal data class HomeLocationItem(
    val name: String,
    val latitude: Double,
    val longitude: Double,
)

internal data class FootprintItem(
    val checkinId: String,
    val placeId: String,
    val placeName: String,
    val latitude: Double,
    val longitude: Double,
    val takenAt: String?,
    val thumbUrl: String?,
)

internal data class FootprintState(
    val home: HomeLocationItem?,
    val items: List<FootprintItem>,
)

internal data class DiscoveryState(
    val catalog: Map<String, List<CatalogEntityItem>> = emptyMap(),
    val events: List<EventItem> = emptyList(),
    val topics: List<TopicItem> = emptyList(),
    val worldRegions: List<WorldRegionItem> = emptyList(),
)

internal data class CheckinItem(
    val id: String,
    val userId: Int = 0,
    val authorName: String = "我",
    val photoIds: List<String> = emptyList(),
    val photos: List<PhotoItem> = emptyList(),
    val placeId: String? = null,
    val placeName: String,
    val note: String,
    val latitude: Double?,
    val longitude: Double?,
    val createdAt: String?,
    val takenAt: String?,
    val source: String,
    val visibility: String = "private",
    val worldVisible: Boolean = false,
    val publicShowcase: Boolean = false,
    val locationSource: String = "none",
    val locationPrivacy: String = "exact",
    val itemTypes: List<CatalogRef> = emptyList(),
    val works: List<CatalogRef> = emptyList(),
    val characters: List<CatalogRef> = emptyList(),
    val partners: List<PartnerItem> = emptyList(),
    val events: List<EventItem> = emptyList(),
    val canEdit: Boolean = true,
    val liked: Boolean = false,
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val thumbUrl: String? = null,
    val displayUrl: String? = null,
) {
    val taxonomyNames: List<String>
        get() = (itemTypes + works + characters).map(CatalogRef::name).distinct()
}

internal data class DraftPhotoItem(
    val key: String = UUID.randomUUID().toString(),
    val localUri: Uri? = null,
    val photo: PhotoItem? = null,
    val status: String = "queued",
    val checksum: String? = null,
    val progress: Int = 0,
    val error: String? = null,
    val captureSource: String = "gallery",
)

internal data class UploadDraft(
    val editingId: String? = null,
    val photos: List<DraftPhotoItem> = emptyList(),
    val placeName: String = "",
    val latitude: String = "",
    val longitude: String = "",
    val locationSource: String = "none",
    val locationProvider: String? = null,
    val locationCapturedAtMillis: Long? = null,
    val locationAccuracyMeters: Float? = null,
    val locationPrivacy: String = "exact",
    val note: String = "",
    val takenAt: String = "",
    val source: String = "android_capture",
    val visibility: String = "private",
    val worldVisible: Boolean = false,
    val publicShowcase: Boolean = false,
    val partnerIds: List<String> = emptyList(),
    val itemTypeIds: List<String> = emptyList(),
    val workIds: List<String> = emptyList(),
    val characterIds: List<String> = emptyList(),
    val eventIds: List<String> = emptyList(),
)

internal data class PendingUpload(
    val requestId: String,
    val draft: UploadDraft,
    val attempted: Boolean = false,
)

internal fun parsePhoto(json: JSONObject): PhotoItem {
    val assets = json.optJSONArray("assets")
    var originalUrl: String? = null
    if (assets != null) {
        for (index in 0 until assets.length()) {
            val asset = assets.optJSONObject(index) ?: continue
            if (asset.optString("variant") == "original") {
                originalUrl = asset.optionalString("url")
                break
            }
        }
    }
    return PhotoItem(
        id = json.getString("id"),
        contentSha256 = json.optionalString("content_sha256"),
        takenAt = json.optionalString("taken_at"),
        exifTakenAt = json.optionalString("exif_taken_at"),
        exifLatitude = json.optionalDouble("exif_latitude"),
        exifLongitude = json.optionalDouble("exif_longitude"),
        capturedAtSource = json.optString("captured_at_source", "unknown"),
        gpsSource = json.optString("gps_source", "none"),
        metadataStatus = json.optString("metadata_status", "unknown"),
        metadataWarnings = json.optJSONArray("metadata_warnings").stringItems(),
        thumbUrl = json.optionalString("thumb_url"),
        displayUrl = json.optionalString("display_url"),
        originalUrl = originalUrl,
    )
}

internal fun parseCatalogEntity(json: JSONObject): CatalogEntityItem = CatalogEntityItem(
    id = json.getString("id"),
    entityType = json.optString("entity_type"),
    canonicalName = json.getString("canonical_name"),
    aliases = json.optJSONArray("aliases").stringItems(),
    status = json.optString("status", "active"),
    followed = json.optBoolean("followed"),
    recordCount = json.optInt("record_count"),
    work = json.optJSONObject("work")?.let(::parseCatalogRef),
    group = json.optJSONObject("group")?.let(::parseCatalogRef),
)

internal fun parsePartner(json: JSONObject): PartnerItem = PartnerItem(
    id = json.getString("id"),
    publicCode = json.optString("public_code"),
    ownerUserId = json.optInt("owner_user_id"),
    name = json.getString("name"),
    visibility = json.optString("visibility", "private"),
    moderationStatus = json.optString("moderation_status", "active"),
    itemType = json.optJSONObject("item_type")?.let(::parseCatalogRef),
    work = json.optJSONObject("work")?.let(::parseCatalogRef),
    character = json.optJSONObject("character")?.let(::parseCatalogRef),
    coverUrl = json.optionalString("cover_url"),
    recordCount = json.optInt("record_count"),
    canEdit = json.optBoolean("can_edit"),
    authorApproved = json.optBoolean("author_approved", true),
    ownerApproved = json.optBoolean("owner_approved", true),
    relationStatus = json.optString("relation_status", "approved"),
    relationVisibility = json.optString("relation_visibility", "public"),
)

internal fun CheckinItem.withPartnerRelationVisibility(partnerId: String, visibility: String): CheckinItem = copy(
    partners = partners.map { partner ->
        if (partner.id == partnerId) partner.copy(relationVisibility = visibility) else partner
    },
)

internal fun CheckinItem.withoutPartnerRelation(partnerId: String): CheckinItem = copy(
    partners = partners.filterNot { it.id == partnerId },
)

internal fun parsePartnerRequest(json: JSONObject): PartnerRequestItem = PartnerRequestItem(
    checkinId = json.getString("checkin_id"),
    partnerId = json.getString("partner_id"),
    partnerName = json.getString("partner_name"),
    partnerCode = json.getString("partner_code"),
    recordAuthorUserId = json.getInt("record_author_user_id"),
    recordAuthorDisplayName = json.optString("record_author_display_name", "记录作者"),
    partnerOwnerUserId = json.getInt("partner_owner_user_id"),
    partnerOwnerDisplayName = json.optString("partner_owner_display_name", "伙伴主人"),
    authorApproved = json.optBoolean("author_approved"),
    ownerApproved = json.optBoolean("owner_approved"),
)

internal fun parsePlace(json: JSONObject): PlaceItem = PlaceItem(
    id = json.getString("id"),
    name = json.getString("name"),
    countryCode = json.optionalString("country_code"),
    province = json.optionalString("province"),
    city = json.optionalString("city"),
    district = json.optionalString("district"),
    latitude = json.getDouble("latitude"),
    longitude = json.getDouble("longitude"),
    address = json.optionalString("address"),
    privacyLevel = json.optString("privacy_level", "exact"),
)

internal fun parseEventSeries(json: JSONObject): EventSeriesItem = EventSeriesItem(
    id = json.getString("id"),
    canonicalName = json.getString("canonical_name"),
    status = json.optString("status", "active"),
)

internal fun parseEvent(json: JSONObject): EventItem = EventItem(
    id = json.getString("id"),
    name = json.getString("name"),
    eventType = json.optString("event_type"),
    visibility = json.optString("visibility", "public"),
    status = json.optString("status", "active"),
    official = json.optBoolean("official"),
    place = json.optJSONObject("place")?.let(::parsePlace),
    series = json.optJSONObject("series")?.let(::parseEventSeries),
    startsAt = json.optionalString("starts_at"),
    endsAt = json.optionalString("ends_at"),
    description = json.optString("description"),
    recordCount = json.optInt("record_count"),
    canEdit = json.optBoolean("can_edit"),
)

internal fun parseCheckin(json: JSONObject): CheckinItem {
    val photos = json.optJSONArray("photos").objectItems(::parsePhoto)
    val author = json.optJSONObject("author")
    val interaction = json.optJSONObject("interaction")
    val catalog = json.optJSONObject("catalog")
    val place = json.optJSONObject("place")
    return CheckinItem(
        id = json.getString("id"),
        userId = json.optInt("user_id"),
        authorName = author?.optString("display_name")?.takeIf(String::isNotBlank) ?: "我",
        photoIds = json.optJSONArray("photo_ids").stringItems(),
        photos = photos,
        placeId = place?.optionalString("id"),
        placeName = json.optString("place_name"),
        note = json.optString("note"),
        latitude = json.optionalDouble("latitude"),
        longitude = json.optionalDouble("longitude"),
        createdAt = json.optionalString("created_at"),
        takenAt = json.optionalString("taken_at"),
        source = json.optString("source", "android_capture"),
        visibility = json.optString("visibility", "private"),
        worldVisible = json.optBoolean("world_visible"),
        publicShowcase = json.optBoolean("public_showcase"),
        locationSource = json.optString("location_source", "none"),
        locationPrivacy = json.optString("location_privacy", "exact"),
        itemTypes = catalog?.optJSONArray("item_types").catalogRefs(),
        works = catalog?.optJSONArray("works").catalogRefs(),
        characters = catalog?.optJSONArray("characters").catalogRefs(),
        partners = json.optJSONArray("partners").objectItems(::parsePartner),
        events = json.optJSONArray("events").objectItems(::parseEvent),
        canEdit = json.optBoolean("can_edit", true),
        liked = interaction?.optBoolean("liked") ?: false,
        likeCount = interaction?.optInt("like_count") ?: 0,
        commentCount = interaction?.optInt("comment_count") ?: 0,
        thumbUrl = json.optionalString("thumb_url") ?: photos.firstOrNull()?.thumbUrl,
        displayUrl = json.optionalString("display_url") ?: photos.firstOrNull()?.displayUrl,
    )
}

internal fun draftFromCheckin(record: CheckinItem): UploadDraft = UploadDraft(
    editingId = record.id,
    photos = record.photos.map { photo -> DraftPhotoItem(key = photo.id, photo = photo, status = "ready") },
    placeName = record.placeName,
    latitude = record.latitude?.toString().orEmpty(),
    longitude = record.longitude?.toString().orEmpty(),
    locationSource = record.locationSource,
    locationPrivacy = record.locationPrivacy,
    note = record.note,
    takenAt = record.takenAt.orEmpty(),
    source = record.source,
    visibility = record.visibility,
    worldVisible = record.worldVisible,
    publicShowcase = record.publicShowcase,
    partnerIds = record.partners.map(PartnerItem::id),
    itemTypeIds = record.itemTypes.map(CatalogRef::id),
    workIds = record.works.map(CatalogRef::id),
    characterIds = record.characters.map(CatalogRef::id),
    eventIds = record.events.map(EventItem::id),
)

internal fun UploadDraft.conflictsWithRecordEdit(recordId: String): Boolean =
    photos.isNotEmpty() && editingId != recordId

internal fun checkinPayload(draft: UploadDraft, requestId: String?): JSONObject {
    val latitude = draft.latitude.trim().takeIf(String::isNotBlank)?.toDoubleOrNull()
    val longitude = draft.longitude.trim().takeIf(String::isNotBlank)?.toDoubleOrNull()
    return JSONObject()
        .put("photo_ids", JSONArray(draft.photos.mapNotNull { it.photo?.id }))
        .put("client_request_id", requestId)
        .put("taken_at", draft.takenAt.trim().ifBlank { null })
        .put("note", draft.note.trim())
        .put("source", draft.source)
        .put("visibility", draft.visibility)
        .put("world_visible", draft.worldVisible)
        .put("public_showcase", draft.publicShowcase)
        .put("place_name", draft.placeName.trim())
        .put("latitude", latitude)
        .put("longitude", longitude)
        .put("location_source", if (latitude == null || longitude == null) "none" else draft.locationSource)
        .put("location_provider", draft.locationProvider)
        .put("location_captured_at", draft.locationCapturedAtMillis?.let { java.time.Instant.ofEpochMilli(it).toString() })
        .put("location_accuracy_meters", draft.locationAccuracyMeters)
        .put("location_privacy", draft.locationPrivacy)
        .put("partner_ids", JSONArray(draft.partnerIds))
        .put("item_type_ids", JSONArray(draft.itemTypeIds))
        .put("work_ids", JSONArray(draft.workIds))
        .put("character_ids", JSONArray(draft.characterIds))
        .put("event_ids", JSONArray(draft.eventIds))
}

internal fun photoToJson(photo: PhotoItem): JSONObject = JSONObject()
    .put("id", photo.id)
    .put("content_sha256", photo.contentSha256)
    .put("taken_at", photo.takenAt)
    .put("exif_taken_at", photo.exifTakenAt)
    .put("exif_latitude", photo.exifLatitude)
    .put("exif_longitude", photo.exifLongitude)
    .put("captured_at_source", photo.capturedAtSource)
    .put("gps_source", photo.gpsSource)
    .put("metadata_status", photo.metadataStatus)
    .put("metadata_warnings", JSONArray(photo.metadataWarnings))
    .put("thumb_url", photo.thumbUrl)
    .put("display_url", photo.displayUrl)
    .put("original_url", photo.originalUrl)

internal fun parseStoredPhoto(json: JSONObject): PhotoItem = PhotoItem(
    id = json.getString("id"),
    contentSha256 = json.optionalString("content_sha256"),
    takenAt = json.optionalString("taken_at"),
    exifTakenAt = json.optionalString("exif_taken_at"),
    exifLatitude = json.optionalDouble("exif_latitude"),
    exifLongitude = json.optionalDouble("exif_longitude"),
    capturedAtSource = json.optString("captured_at_source", "unknown"),
    gpsSource = json.optString("gps_source", "none"),
    metadataStatus = json.optString("metadata_status", "unknown"),
    metadataWarnings = json.optJSONArray("metadata_warnings").stringItems(),
    thumbUrl = json.optionalString("thumb_url"),
    displayUrl = json.optionalString("display_url"),
    originalUrl = json.optionalString("original_url"),
)

internal fun JSONObject.optionalString(key: String): String? = if (!has(key) || isNull(key)) null else optString(key).takeIf(String::isNotBlank)

internal fun JSONObject.optionalDouble(key: String): Double? = if (!has(key) || isNull(key)) null else optDouble(key).takeIf(Double::isFinite)

internal fun JSONArray?.stringItems(): List<String> = buildList {
    val source = this@stringItems ?: return@buildList
    for (index in 0 until source.length()) source.optString(index).takeIf(String::isNotBlank)?.let(::add)
}

internal fun <T> JSONArray?.objectItems(parser: (JSONObject) -> T): List<T> = buildList {
    val source = this@objectItems ?: return@buildList
    for (index in 0 until source.length()) source.optJSONObject(index)?.let(parser)?.let(::add)
}

private fun parseCatalogRef(json: JSONObject): CatalogRef = CatalogRef(
    id = json.getString("id"),
    name = json.optString("canonical_name", json.optString("name")),
)

private fun JSONArray?.catalogRefs(): List<CatalogRef> = objectItems(::parseCatalogRef)
