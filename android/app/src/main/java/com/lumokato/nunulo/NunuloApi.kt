package com.lumokato.nunulo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

internal data class AuthUser(
    val id: Int,
    val displayName: String,
    val username: String?,
    val email: String?,
    val roles: List<String>,
    val storageUsageBytes: Long,
    val storageQuotaBytes: Long,
    val avatarUrl: String?,
    val bio: String? = null,
)

internal data class AuthTokens(val accessToken: String, val refreshToken: String?)

internal data class PublicConfig(
    val publicRegistrationEnabled: Boolean,
    val termsVersion: String,
    val privacyVersion: String,
    val termsUrl: String,
    val privacyUrl: String,
)

internal data class CommentItem(val id: String, val displayName: String, val body: String, val createdAt: String?)
internal data class NotificationItem(val id: String, val title: String, val body: String, val targetType: String?, val targetId: String?, val readAt: String?, val createdAt: String?)
internal data class PersonItem(
    val id: Int,
    val displayName: String,
    val username: String?,
    val bio: String?,
    val following: Boolean,
    val avatarUrl: String? = null,
    val followerCount: Int = 0,
    val followingCount: Int = 0,
)
internal data class AlbumItem(
    val id: String,
    val title: String,
    val itemCount: Int,
    val visibility: String,
    val description: String = "",
    val createdAt: String? = null,
)
internal data class AlbumContent(val album: AlbumItem, val checkinIds: List<String>)
internal data class ExportItem(val id: String, val status: String, val createdAt: String?, val downloadUrl: String?)

internal class NunuloApi(private val client: OkHttpClient = defaultNunuloHttpClient()) {
    private val http = JsonHttpClient(client)
    @Volatile private var mediaAccessToken = ""

    fun setAccessToken(token: String) {
        mediaAccessToken = token
    }

    suspend fun login(apiBase: String, login: String, password: String): Pair<AuthUser, AuthTokens> = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put("login", login.trim())
            .put("password", password)
            .put("device_name", "Android App")
            .put("device_type", "android")
        val json = executeJson(
            Request.Builder()
                .url(apiUrl(apiBase, "/api/auth/login"))
                .post(payload.jsonBody())
                .build()
        )
        parseAuthUser(json.getJSONObject("user")) to AuthTokens(json.getString("access_token"), json.optionalString("refresh_token"))
    }

    suspend fun publicConfig(apiBase: String): PublicConfig = withContext(Dispatchers.IO) {
        val json = executeJson(Request.Builder().url(apiUrl(apiBase, "/api/public/config")).get().build())
        PublicConfig(
            publicRegistrationEnabled = json.optBoolean("public_registration_enabled"),
            termsVersion = json.optString("terms_version"),
            privacyVersion = json.optString("privacy_version"),
            termsUrl = json.optString("terms_url", "/terms/"),
            privacyUrl = json.optString("privacy_url", "/privacy/"),
        )
    }

    suspend fun register(
        apiBase: String,
        username: String,
        displayName: String,
        inviteCode: String,
        password: String,
        acceptedPolicies: Boolean,
        config: PublicConfig?,
    ): Pair<AuthUser, AuthTokens> = withContext(Dispatchers.IO) {
        val currentConfig = config ?: throw IllegalStateException("注册配置尚未加载，请稍后重试")
        if (!acceptedPolicies) throw IllegalArgumentException("请先同意服务条款和隐私政策")
        if (inviteCode.isBlank() && !currentConfig.publicRegistrationEnabled) throw IllegalArgumentException("公开注册暂未开放，请填写邀请码")
        val payload = JSONObject()
            .put("username", username.trim())
            .put("display_name", displayName.trim())
            .put("password", password)
            .put("device_name", "Android App")
            .put("device_type", "android")
            .put("accepted_terms", true)
            .put("accepted_privacy", true)
            .put("terms_version", currentConfig.termsVersion)
            .put("privacy_version", currentConfig.privacyVersion)
        inviteCode.trim().takeIf(String::isNotBlank)?.let { payload.put("invite_code", it) }
        val json = executeJson(
            Request.Builder()
                .url(apiUrl(apiBase, "/api/auth/register"))
                .post(payload.jsonBody())
                .build()
        )
        parseAuthUser(json.getJSONObject("user")) to AuthTokens(json.getString("access_token"), json.optionalString("refresh_token"))
    }

    suspend fun refreshAccessToken(apiBase: String, refreshToken: String): String = withContext(Dispatchers.IO) {
        val payload = JSONObject().put("refresh_token", refreshToken)
        executeJson(Request.Builder().url(apiUrl(apiBase, "/api/auth/refresh")).post(payload.jsonBody()).build()).getString("access_token")
    }

    suspend fun me(apiBase: String, token: String): AuthUser = withContext(Dispatchers.IO) {
        parseAuthUser(executeJson(authorized(apiBase, "/api/auth/me", token).get().build()).getJSONObject("user"))
    }

    suspend fun updateProfile(apiBase: String, token: String, displayName: String, bio: String): AuthUser = withContext(Dispatchers.IO) {
        val payload = profilePayload(displayName, bio)
        parseAuthUser(
            executeJson(authorized(apiBase, "/api/users/me", token).patch(payload.jsonBody()).build()).getJSONObject("user")
        )
    }

    suspend fun listCheckins(
        apiBase: String,
        token: String,
        scope: FeedScope = FeedScope.Discover,
        order: FeedOrder = FeedOrder.Popular,
        filters: Map<String, String> = emptyMap(),
    ): List<CheckinItem> = withContext(Dispatchers.IO) {
        val path = checkinFeedPath(scope, order, filters)
        executeJson(authorized(apiBase, path, token).get().build()).getJSONArray("items").objectItems(::parseCheckin)
    }

    suspend fun getCheckin(apiBase: String, token: String, checkinId: String): CheckinItem = withContext(Dispatchers.IO) {
        parseCheckin(executeJson(authorized(apiBase, "/api/checkins/$checkinId", token).get().build()))
    }

    suspend fun uploadPhoto(
        context: Context,
        apiBase: String,
        token: String,
        uri: Uri,
        checksum: String,
        onProgress: (Int) -> Unit = {},
    ): PhotoItem = withContext(Dispatchers.IO) {
        val media = prepareUploadMedia(context.contentResolver, uri) { written, total ->
            if (total != null && total > 0L) onProgress(((written * 100L) / total).toInt().coerceIn(0, 100))
        }
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("checksum_sha256", checksum)
            .addFormDataPart("photo", media.filename, media.requestBody)
            .build()
        parsePhoto(executeJson(authorized(apiBase, "/api/photos", token).post(body).build()))
    }

    suspend fun saveCheckin(apiBase: String, token: String, draft: UploadDraft, requestId: String): CheckinItem = withContext(Dispatchers.IO) {
        val payload = checkinPayload(draft, requestId.takeUnless { draft.editingId != null })
        val builder = authorized(apiBase, draft.editingId?.let { "/api/checkins/$it" } ?: "/api/checkins", token)
        val request = if (draft.editingId == null) builder.post(payload.jsonBody()).build() else builder.patch(payload.jsonBody()).build()
        parseCheckin(executeJson(request))
    }

    suspend fun deleteCheckin(apiBase: String, token: String, checkinId: String) = withContext(Dispatchers.IO) {
        executeJson(authorized(apiBase, "/api/checkins/$checkinId", token).delete().build())
    }

    suspend fun setLike(apiBase: String, token: String, record: CheckinItem): CheckinItem = withContext(Dispatchers.IO) {
        val builder = authorized(apiBase, "/api/checkins/${record.id}/like", token)
        val request = if (record.liked) builder.delete().build() else builder.post(emptyBody()).build()
        val interaction = executeJson(request)
        record.copy(
            liked = interaction.optBoolean("liked"),
            likeCount = interaction.optInt("like_count"),
            commentCount = interaction.optInt("comment_count"),
        )
    }

    suspend fun listComments(apiBase: String, token: String, checkinId: String): List<CommentItem> = withContext(Dispatchers.IO) {
        executeJson(authorized(apiBase, "/api/checkins/$checkinId/comments", token).get().build())
            .getJSONArray("items").objectItems(::parseComment)
    }

    suspend fun addComment(apiBase: String, token: String, checkinId: String, body: String): CommentItem = withContext(Dispatchers.IO) {
        parseComment(
            executeJson(
                authorized(apiBase, "/api/checkins/$checkinId/comments", token)
                    .post(JSONObject().put("body", body.trim()).jsonBody()).build()
            )
        )
    }

    suspend fun reportCheckin(apiBase: String, token: String, checkinId: String, reason: String) = withContext(Dispatchers.IO) {
        val payload = JSONObject().put("target_type", "checkin").put("target_id", checkinId).put("reason", reason.trim())
        executeJson(authorized(apiBase, "/api/reports", token).post(payload.jsonBody()).build())
    }

    suspend fun listCatalog(apiBase: String, token: String, entityType: String): List<CatalogEntityItem> = withContext(Dispatchers.IO) {
        executeJson(authorized(apiBase, "/api/catalog/$entityType", token).get().build())
            .getJSONArray("items").objectItems(::parseCatalogEntity)
    }

    suspend fun createCatalogCandidate(apiBase: String, token: String, entityType: String, name: String, workId: String? = null): CatalogEntityItem = withContext(Dispatchers.IO) {
        val payload = JSONObject().put("name", name.trim()).put("work_id", workId)
        parseCatalogEntity(executeJson(authorized(apiBase, "/api/catalog/$entityType/candidates", token).post(payload.jsonBody()).build()))
    }

    suspend fun setCatalogFollow(apiBase: String, token: String, entity: CatalogEntityItem): CatalogEntityItem = withContext(Dispatchers.IO) {
        val builder = authorized(apiBase, "/api/catalog/${entity.entityType}/${entity.id}/follow", token)
        val request = if (entity.followed) builder.delete().build() else builder.post(emptyBody()).build()
        parseCatalogEntity(executeJson(request))
    }

    suspend fun discovery(apiBase: String, token: String): DiscoveryState = withContext(Dispatchers.IO) {
        parseDiscovery(executeJson(authorized(apiBase, "/api/discovery", token).get().build()))
    }

    suspend fun worldRegions(apiBase: String, token: String, filters: Map<String, String> = emptyMap()): List<WorldRegionItem> = withContext(Dispatchers.IO) {
        val path = queryPath("/api/world/regions", filters)
        executeJson(authorized(apiBase, path, token).get().build()).getJSONArray("items").objectItems(::parseWorldRegion)
    }

    suspend fun listPartners(apiBase: String, token: String, q: String? = null): List<PartnerItem> = withContext(Dispatchers.IO) {
        val path = queryPath("/api/partners", q?.takeIf(String::isNotBlank)?.let { mapOf("q" to it) } ?: emptyMap())
        executeJson(authorized(apiBase, path, token).get().build()).getJSONArray("items").objectItems(::parsePartner)
    }

    suspend fun getPartner(apiBase: String, token: String, partnerId: String): PartnerItem = withContext(Dispatchers.IO) {
        parsePartner(executeJson(authorized(apiBase, "/api/partners/$partnerId", token).get().build()))
    }

    suspend fun savePartner(
        apiBase: String,
        token: String,
        id: String?,
        name: String,
        itemTypeId: String,
        workId: String?,
        characterId: String?,
        visibility: String,
    ): PartnerItem = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put("name", name.trim())
            .put("item_type_id", itemTypeId)
            .put("work_id", workId)
            .put("character_id", characterId)
            .put("visibility", visibility)
        val builder = authorized(apiBase, id?.let { "/api/partners/$it" } ?: "/api/partners", token)
        val request = if (id == null) builder.post(payload.jsonBody()).build() else builder.patch(payload.jsonBody()).build()
        parsePartner(executeJson(request))
    }

    suspend fun deletePartner(apiBase: String, token: String, partnerId: String) = withContext(Dispatchers.IO) {
        executeJson(authorized(apiBase, "/api/partners/$partnerId", token).delete().build())
    }

    suspend fun listPartnerRequests(apiBase: String, token: String): List<PartnerRequestItem> = withContext(Dispatchers.IO) {
        executeJson(authorized(apiBase, "/api/partners/requests", token).get().build())
            .getJSONArray("items").objectItems(::parsePartnerRequest)
    }

    suspend fun resolvePartnerRequest(apiBase: String, token: String, request: PartnerRequestItem, approved: Boolean) = withContext(Dispatchers.IO) {
        executeJson(
            authorized(apiBase, "/api/checkins/${request.checkinId}/partners/${request.partnerId}", token)
                .patch(JSONObject().put("approved", approved).jsonBody()).build()
        )
    }

    suspend fun requestPartnerRegistration(apiBase: String, token: String, checkinId: String, partnerId: String, visibility: String = "public") = withContext(Dispatchers.IO) {
        executeJson(
            authorized(apiBase, "/api/checkins/$checkinId/partners/$partnerId", token)
                .post(JSONObject().put("visibility", visibility).jsonBody()).build()
        )
    }

    suspend fun updateCheckinPartnerVisibility(
        apiBase: String,
        token: String,
        checkinId: String,
        partnerId: String,
        visibility: String,
    ): String = withContext(Dispatchers.IO) {
        executeJson(
            authorized(apiBase, partnerRelationVisibilityPath(checkinId, partnerId), token)
                .patch(partnerRelationVisibilityPayload(visibility).jsonBody()).build()
        ).getString("visibility")
    }

    suspend fun removeCheckinPartner(apiBase: String, token: String, checkinId: String, partnerId: String) = withContext(Dispatchers.IO) {
        executeJson(authorized(apiBase, "/api/checkins/$checkinId/partners/$partnerId", token).delete().build())
    }

    suspend fun listPartnerMeetings(apiBase: String, token: String, partnerId: String): List<PartnerMeetingItem> = withContext(Dispatchers.IO) {
        executeJson(authorized(apiBase, "/api/partners/$partnerId/meetings", token).get().build())
            .getJSONArray("items").objectItems(::parsePartnerMeeting)
    }

    suspend fun listEvents(apiBase: String, token: String): List<EventItem> = withContext(Dispatchers.IO) {
        executeJson(authorized(apiBase, eventListPath(includePast = true), token).get().build())
            .getJSONArray("items").objectItems(::parseEvent)
    }

    suspend fun listEventSeries(apiBase: String, token: String): List<EventSeriesItem> = withContext(Dispatchers.IO) {
        executeJson(authorized(apiBase, "/api/events/series", token).get().build())
            .getJSONArray("items").objectItems(::parseEventSeries)
    }

    suspend fun saveEvent(
        apiBase: String,
        token: String,
        id: String?,
        name: String,
        eventType: String,
        visibility: String,
        placeId: String?,
        seriesId: String?,
        startsAt: String?,
        endsAt: String?,
        description: String,
    ): EventItem = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put("name", name.trim())
            .put("event_type", eventType)
            .put("visibility", visibility)
            .put("place_id", placeId)
            .put("series_id", seriesId)
            .put("starts_at", startsAt?.takeIf(String::isNotBlank))
            .put("ends_at", endsAt?.takeIf(String::isNotBlank))
            .put("description", description.trim())
        val builder = authorized(apiBase, id?.let { "/api/events/$it" } ?: "/api/events", token)
        val request = if (id == null) builder.post(payload.jsonBody()).build() else builder.patch(payload.jsonBody()).build()
        parseEvent(executeJson(request))
    }

    suspend fun deleteEvent(apiBase: String, token: String, eventId: String) = withContext(Dispatchers.IO) {
        executeJson(authorized(apiBase, "/api/events/$eventId", token).delete().build())
    }

    suspend fun listPlaces(apiBase: String, token: String): List<PlaceItem> = withContext(Dispatchers.IO) {
        executeJson(authorized(apiBase, "/api/places", token).get().build()).getJSONArray("items").objectItems(::parsePlace)
    }

    suspend fun createPlace(apiBase: String, token: String, name: String, latitude: Double, longitude: Double): PlaceItem = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put("name", name.trim())
            .put("latitude", latitude)
            .put("longitude", longitude)
            .put("privacy_level", "exact")
        parsePlace(executeJson(authorized(apiBase, "/api/places", token).post(payload.jsonBody()).build()))
    }

    suspend fun footprint(apiBase: String, token: String): FootprintState = withContext(Dispatchers.IO) {
        parseFootprint(executeJson(authorized(apiBase, "/api/users/me/footprint", token).get().build()))
    }

    suspend fun setHomeLocation(apiBase: String, token: String, name: String, latitude: Double, longitude: Double): HomeLocationItem = withContext(Dispatchers.IO) {
        val json = executeJson(
            authorized(apiBase, "/api/users/me/home-location", token)
                .put(JSONObject().put("name", name.trim()).put("latitude", latitude).put("longitude", longitude).jsonBody())
                .build()
        )
        parseHomeLocation(json)
    }

    suspend fun deleteHomeLocation(apiBase: String, token: String) = withContext(Dispatchers.IO) {
        executeJson(authorized(apiBase, "/api/users/me/home-location", token).delete().build())
    }

    suspend fun listNotifications(apiBase: String, token: String): List<NotificationItem> = withContext(Dispatchers.IO) {
        executeJson(authorized(apiBase, "/api/notifications?limit=100", token).get().build())
            .getJSONArray("items").objectItems(::parseNotification)
    }

    suspend fun markNotificationsRead(apiBase: String, token: String) = withContext(Dispatchers.IO) {
        executeJson(authorized(apiBase, "/api/notifications/read-all", token).post(emptyBody()).build())
    }

    suspend fun listPeople(apiBase: String, token: String): List<PersonItem> = withContext(Dispatchers.IO) {
        executeJson(authorized(apiBase, "/api/users?limit=100", token).get().build()).getJSONArray("items").objectItems(::parsePerson)
    }

    suspend fun getPerson(apiBase: String, token: String, personId: Int): PersonItem = withContext(Dispatchers.IO) {
        parsePerson(executeJson(authorized(apiBase, "/api/users/$personId", token).get().build()).getJSONObject("user"))
    }

    suspend fun setFollowing(apiBase: String, token: String, person: PersonItem): PersonItem = withContext(Dispatchers.IO) {
        val builder = authorized(apiBase, "/api/users/${person.id}/follow", token)
        val request = if (person.following) builder.delete().build() else builder.post(emptyBody()).build()
        val following = executeJson(request).optBoolean("following")
        val followerDelta = when {
            following && !person.following -> 1
            !following && person.following -> -1
            else -> 0
        }
        person.copy(
            following = following,
            followerCount = (person.followerCount + followerDelta).coerceAtLeast(0),
        )
    }

    suspend fun blockPerson(apiBase: String, token: String, personId: Int) = withContext(Dispatchers.IO) {
        executeJson(authorized(apiBase, "/api/users/$personId/block", token).post(emptyBody()).build())
    }

    suspend fun listAlbums(apiBase: String, token: String): List<AlbumItem> = withContext(Dispatchers.IO) {
        executeJson(authorized(apiBase, "/api/albums", token).get().build()).getJSONArray("items").objectItems(::parseAlbum)
    }

    suspend fun saveAlbum(apiBase: String, token: String, id: String?, title: String, description: String, visibility: String): AlbumItem = withContext(Dispatchers.IO) {
        val builder = authorized(apiBase, id?.let { "/api/albums/$it" } ?: "/api/albums", token)
        val body = albumPayload(title, description, visibility).jsonBody()
        parseAlbum(executeJson(if (id == null) builder.post(body).build() else builder.patch(body).build()))
    }

    suspend fun addAlbumItem(apiBase: String, token: String, albumId: String, checkinId: String): AlbumItem = withContext(Dispatchers.IO) {
        parseAlbum(executeJson(authorized(apiBase, "/api/albums/$albumId/checkins/$checkinId", token).post(emptyBody()).build()))
    }

    suspend fun albumContent(apiBase: String, token: String, albumId: String): AlbumContent = withContext(Dispatchers.IO) {
        val json = executeJson(authorized(apiBase, "/api/albums/$albumId/checkins", token).get().build())
        AlbumContent(parseAlbum(json.getJSONObject("album")), json.optJSONArray("checkin_ids").stringItems())
    }

    suspend fun removeAlbumItem(apiBase: String, token: String, albumId: String, checkinId: String): AlbumItem = withContext(Dispatchers.IO) {
        parseAlbum(executeJson(authorized(apiBase, "/api/albums/$albumId/checkins/$checkinId", token).delete().build()))
    }

    suspend fun deleteAlbum(apiBase: String, token: String, albumId: String) = withContext(Dispatchers.IO) {
        executeJson(authorized(apiBase, "/api/albums/$albumId", token).delete().build())
        Unit
    }

    suspend fun listExports(apiBase: String, token: String): List<ExportItem> = withContext(Dispatchers.IO) {
        executeJson(authorized(apiBase, "/api/exports?limit=50", token).get().build()).getJSONArray("items").objectItems(::parseExport)
    }

    suspend fun createExport(apiBase: String, token: String): ExportItem = withContext(Dispatchers.IO) {
        parseExport(executeJson(authorized(apiBase, "/api/exports", token).post(emptyBody()).build()))
    }

    suspend fun downloadExport(context: Context, apiBase: String, token: String, record: ExportItem): Uri = withContext(Dispatchers.IO) {
        val path = record.downloadUrl ?: "/api/exports/${record.id}/download"
        client.newCall(authorized(apiBase, path, token).get().build()).execute().use { response ->
            if (!response.isSuccessful) throw ApiException(response.code, "导出下载失败（HTTP ${response.code}）")
            val directory = File(context.cacheDir, "exports").apply(File::mkdirs)
            val target = File(directory, "nunulo-export-${record.id}.zip")
            response.body.byteStream().use { input -> target.outputStream().use(input::copyTo) }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", target)
        }
    }

    suspend fun createInvite(apiBase: String, token: String): String = withContext(Dispatchers.IO) {
        executeJson(authorized(apiBase, "/api/invites", token).post(JSONObject().jsonBody()).build()).getString("code")
    }

    suspend fun uploadAvatar(context: Context, apiBase: String, token: String, uri: Uri): AuthUser = withContext(Dispatchers.IO) {
        val media = prepareUploadMedia(context.contentResolver, uri)
        val body = MultipartBody.Builder().setType(MultipartBody.FORM).addFormDataPart("photo", media.filename, media.requestBody).build()
        parseAuthUser(executeJson(authorized(apiBase, "/api/users/me/avatar", token).post(body).build()).getJSONObject("user"))
    }

    suspend fun downloadBitmap(apiBase: String, url: String): Bitmap? = withContext(Dispatchers.IO) {
        val requestBuilder = Request.Builder().url(url).get()
        if (mediaAccessToken.isNotBlank() && url.startsWith(apiBase.trimEnd('/') + "/api/")) {
            requestBuilder.header("Authorization", "Bearer $mediaAccessToken")
        }
        client.newCall(requestBuilder.build()).execute().use { response ->
            if (!response.isSuccessful) return@withContext null
            response.body.byteStream().use(BitmapFactory::decodeStream)
        }
    }

    private fun authorized(apiBase: String, path: String, token: String): Request.Builder = http.authorizedBuilder(apiBase, path, token)
    private fun executeJson(request: Request): JSONObject = http.executeJson(request)
}

internal fun partnerRelationVisibilityPayload(visibility: String): JSONObject = JSONObject()
    .put("visibility", visibility.trim().lowercase())

internal fun partnerRelationVisibilityPath(checkinId: String, partnerId: String): String =
    "/api/checkins/$checkinId/partners/$partnerId/visibility"

internal fun eventListPath(includePast: Boolean): String = "/api/events?include_past=$includePast"

internal fun checkinFeedPath(scope: FeedScope, order: FeedOrder, filters: Map<String, String> = emptyMap()): String = queryPath(
    "/api/checkins",
    linkedMapOf("scope" to scope.apiValue, "order" to order.apiValue, "limit" to "120", "offset" to "0") + filters,
)

internal fun queryPath(path: String, parameters: Map<String, String>): String {
    val values = parameters.filterValues(String::isNotBlank)
    if (values.isEmpty()) return path
    return path + values.entries.joinToString(prefix = "?", separator = "&") { (key, value) ->
        "${encodeQuery(key)}=${encodeQuery(value)}"
    }
}

private fun encodeQuery(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.toString())
private fun JSONObject.jsonBody() = toString().toRequestBody("application/json; charset=utf-8".toMediaType())
private fun emptyBody() = ByteArray(0).toRequestBody(null)

internal fun parseAuthUser(json: JSONObject): AuthUser = AuthUser(
    id = json.getInt("id"),
    displayName = json.getString("display_name"),
    username = json.optionalString("username"),
    email = json.optionalString("email"),
    roles = json.optJSONArray("roles").stringItems(),
    storageUsageBytes = json.optLong("storage_usage_bytes"),
    storageQuotaBytes = json.optLong("storage_quota_bytes"),
    avatarUrl = json.optionalString("avatar_url"),
    bio = json.optionalString("bio"),
)

private fun parseComment(json: JSONObject) = CommentItem(
    id = json.getString("id"),
    displayName = json.optString("display_name", "Nunulo 成员"),
    body = json.getString("body"),
    createdAt = json.optionalString("created_at"),
)

private fun parseNotification(json: JSONObject) = NotificationItem(
    id = json.getString("id"),
    title = json.getString("title"),
    body = json.getString("body"),
    targetType = json.optionalString("target_type"),
    targetId = json.optionalString("target_id"),
    readAt = json.optionalString("read_at"),
    createdAt = json.optionalString("created_at"),
)

internal fun parsePerson(json: JSONObject) = PersonItem(
    id = json.getInt("id"),
    displayName = json.getString("display_name"),
    username = json.optionalString("username"),
    bio = json.optionalString("bio"),
    following = json.optBoolean("following"),
    avatarUrl = json.optionalString("avatar_url"),
    followerCount = json.optInt("follower_count"),
    followingCount = json.optInt("following_count"),
)

internal fun parseAlbum(json: JSONObject) = AlbumItem(
    id = json.getString("id"),
    title = json.getString("title"),
    itemCount = json.optInt("item_count"),
    visibility = json.optString("visibility", "private"),
    description = json.optString("description"),
    createdAt = json.optionalString("created_at"),
)

internal fun albumPayload(title: String, description: String, visibility: String) = JSONObject()
    .put("title", title.trim())
    .put("description", description.trim())
    .put("visibility", visibility.trim().lowercase())

internal fun profilePayload(displayName: String, bio: String) = JSONObject()
    .put("display_name", displayName.trim())
    .put("bio", bio.trim())

private fun parseExport(json: JSONObject) = ExportItem(
    id = json.getString("id"),
    status = json.getString("status"),
    createdAt = json.optionalString("created_at"),
    downloadUrl = json.optionalString("download_url"),
)

private fun parsePartnerMeeting(json: JSONObject) = PartnerMeetingItem(
    partner = parsePartner(json.getJSONObject("partner")),
    meetingCount = json.optInt("meeting_count"),
    firstMetAt = json.optionalString("first_met_at"),
    lastMetAt = json.optionalString("last_met_at"),
)

private fun parseTopic(json: JSONObject) = TopicItem(
    id = json.getString("id"),
    title = json.getString("title"),
    description = json.optString("description"),
    status = json.optString("status"),
    checkinIds = json.optJSONArray("checkin_ids").stringItems(),
)

private fun parseWorldRegion(json: JSONObject) = WorldRegionItem(
    key = json.getString("key"),
    name = json.getString("name"),
    countryCode = json.optionalString("country_code"),
    province = json.optionalString("province"),
    city = json.optionalString("city"),
    recordCount = json.optInt("record_count"),
    userCount = json.optInt("user_count"),
    latitude = json.getDouble("latitude"),
    longitude = json.getDouble("longitude"),
    representativeThumbUrl = json.optionalString("representative_thumb_url"),
    eligible = json.optBoolean("eligible", true),
)

private fun parseDiscovery(json: JSONObject): DiscoveryState {
    val catalogJson = json.optJSONObject("catalog") ?: JSONObject()
    val catalog = listOf("item_type", "work", "character").associateWith { type ->
        catalogJson.optJSONArray(type).objectItems(::parseCatalogEntity)
    }
    return DiscoveryState(
        catalog = catalog,
        events = json.optJSONArray("events").objectItems(::parseEvent),
        topics = json.optJSONArray("topics").objectItems(::parseTopic),
        worldRegions = json.optJSONArray("world_regions").objectItems(::parseWorldRegion),
    )
}

private fun parseHomeLocation(json: JSONObject) = HomeLocationItem(
    name = json.optString("name", "家"),
    latitude = json.getDouble("latitude"),
    longitude = json.getDouble("longitude"),
)

private fun parseFootprint(json: JSONObject) = FootprintState(
    home = json.optJSONObject("home")?.let(::parseHomeLocation),
    items = json.optJSONArray("items").objectItems { item ->
        FootprintItem(
            checkinId = item.getString("checkin_id"),
            placeId = item.getString("place_id"),
            placeName = item.getString("place_name"),
            latitude = item.getDouble("latitude"),
            longitude = item.getDouble("longitude"),
            takenAt = item.optionalString("taken_at"),
            thumbUrl = item.optionalString("thumb_url"),
        )
    },
)
