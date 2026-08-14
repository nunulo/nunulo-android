package com.lumokato.nunulo

import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
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
    return CheckinItem(
        id = json.getString("id"),
        userId = json.optInt("user_id"),
        authorName = author?.optString("display_name")?.takeIf(String::isNotBlank) ?: "我",
        photoIds = json.optJSONArray("photo_ids").stringItems(),
        photos = photos,
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
