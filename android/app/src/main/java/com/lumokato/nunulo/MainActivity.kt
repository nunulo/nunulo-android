package com.lumokato.nunulo

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.MapView
import com.amap.api.maps.MapsInitializer
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.LatLngBounds
import com.amap.api.maps.model.MarkerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapsInitializer.updatePrivacyShow(this, true, true)
        MapsInitializer.updatePrivacyAgree(this, true)
        setContent { NunuloApp() }
    }
}

private enum class AppTab(val title: String) {
    Feed("动态"),
    Map("地图"),
    Publish("登记"),
    Notifications("消息"),
    Me("我的"),
}

private enum class LocationRequestPurpose {
    Map,
    Draft,
    PermissionOnly,
}

internal enum class FeedScope(val apiValue: String, val label: String) {
    All("all", "全部"),
    Following("following", "关注"),
    Public("public", "公开"),
    Mine("mine", "我的"),
}

private data class AuthUser(
    val id: Int,
    val displayName: String,
    val username: String?,
    val email: String?,
    val roles: List<String>,
    val storageUsageBytes: Long,
    val storageQuotaBytes: Long,
    val avatarUrl: String?,
)

internal data class CheckinItem(
    val id: String,
    val userId: Int = 0,
    val authorName: String = "我",
    val placeName: String,
    val note: String,
    val latitude: Double,
    val longitude: Double,
    val tags: List<String>,
    val createdAt: String?,
    val takenAt: String?,
    val source: String,
    val visibility: String = "private",
    val canEdit: Boolean = true,
    val liked: Boolean = false,
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val thumbUrl: String?,
    val displayUrl: String?,
    val originalUrl: String?,
)

private data class CheckinEditDraft(
    val placeName: String,
    val latitude: String,
    val longitude: String,
    val note: String,
    val tags: String,
    val visibility: String = "private",
)

private data class CommentItem(val id: String, val displayName: String, val body: String, val createdAt: String?)
private data class NotificationItem(val id: String, val title: String, val body: String, val readAt: String?, val createdAt: String?)
private data class PersonItem(val id: Int, val displayName: String, val username: String?, val bio: String?, val following: Boolean)
private data class AlbumItem(val id: String, val title: String, val itemCount: Int, val visibility: String)
private data class ExportItem(val id: String, val status: String, val createdAt: String?, val downloadUrl: String?)
private data class AuxiliaryState(
    val tags: List<TagItem>,
    val notifications: List<NotificationItem>,
    val people: List<PersonItem>,
    val albums: List<AlbumItem>,
    val exports: List<ExportItem>,
)

private data class TagItem(
    val name: String,
    val count: Int = 0,
)

private data class AuthTokens(val accessToken: String, val refreshToken: String?)

internal data class UploadDraft(
    val photoUri: Uri? = null,
    val placeName: String = "",
    val latitude: String = "",
    val longitude: String = "",
    val locationSource: String = "manual",
    val note: String = "",
    val tags: String = "",
    val visibility: String = "private",
)

internal data class PendingUpload(
    val requestId: String,
    val draft: UploadDraft,
    val attempted: Boolean = false,
)

private data class BrowseFilters(
    val query: String = "",
    val tag: String = "",
    val place: String = "",
)

private data class DraftValidation(
    val hasPhoto: Boolean,
    val hasValidLatitude: Boolean,
    val hasValidLongitude: Boolean,
    val hasPlaceName: Boolean,
    val hasTags: Boolean,
    val ready: Boolean,
    val missingText: String,
)

private val DEFAULT_API_BASE = BuildConfig.NUNULO_API_BASE_URL
private const val AMAP_LOG_TAG = "NunuloAmapNative"
private const val APP_LOG_TAG = "NunuloApp"
private object NunuloUi {
    val Background = Color(0xFFF6FAFE)
    val Surface = Color(0xFFF6FAFE)
    val Paper = Color(0xFFFFFFFF)
    val PaperTint = Color(0xFFF0F4F8)
    val Ink = Color(0xFF171C1F)
    val Slate = Color(0xFF574143)
    val Muted = Color(0xFF574143)
    val Hairline = Color(0xFFDEBFC1)
    val Placeholder = Color(0xFFE4E9ED)
    val Coral = Color(0xFFA93349)
    val CoralSoft = Color(0xFFFFDADC)
    val Green = Color(0xFF006B5F)
    val GreenSoft = Color(0xFF62FAE3)
    val Blue = Color(0xFF0060AC)
    val BlueSoft = Color(0xFFD4E3FF)
    val Amber = Color(0xFF8A4F00)
    val AmberSoft = Color(0xFFFFDDB3)
    val Danger = Color(0xFFBA1A1A)
    val CardRadius = 8.dp
}

private class NunuloApi(private val client: OkHttpClient = defaultNunuloHttpClient()) {
    private val http = JsonHttpClient(client)
    @Volatile
    private var mediaAccessToken: String = ""

    fun setAccessToken(token: String) {
        mediaAccessToken = token
    }

    suspend fun login(apiBase: String, login: String, password: String): Pair<AuthUser, AuthTokens> = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put("login", login)
            .put("password", password)
            .put("device_name", "Android App")
            .put("device_type", "android")
            .toString()
        val request = Request.Builder()
            .url(apiUrl(apiBase, "/api/auth/login"))
            .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()
        val json = http.executeJson(request)
        val user = parseAuthUser(json.getJSONObject("user"))
        val tokens = AuthTokens(
            accessToken = json.getString("access_token"),
            refreshToken = json.getString("refresh_token"),
        )
        user to tokens
    }

    suspend fun register(apiBase: String, username: String, displayName: String, inviteCode: String, password: String): Pair<AuthUser, AuthTokens> = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put("username", username.trim())
            .put("display_name", displayName.trim())
            .put("invite_code", inviteCode.trim())
            .put("password", password)
            .put("device_name", "Android App")
            .put("device_type", "android")
            .toString()
        val request = Request.Builder()
            .url(apiUrl(apiBase, "/api/auth/register"))
            .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()
        val json = http.executeJson(request)
        parseAuthUser(json.getJSONObject("user")) to AuthTokens(json.getString("access_token"), json.getString("refresh_token"))
    }

    suspend fun me(apiBase: String, token: String): AuthUser = withContext(Dispatchers.IO) {
        val request = http.authorizedBuilder(apiBase, "/api/auth/me", token).get().build()
        parseAuthUser(http.executeJson(request).getJSONObject("user"))
    }

    suspend fun refreshAccessToken(apiBase: String, refreshToken: String): String = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put("refresh_token", refreshToken)
            .toString()
        val request = Request.Builder()
            .url(apiUrl(apiBase, "/api/auth/refresh"))
            .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()
        http.executeJson(request).getString("access_token")
    }

    suspend fun listCheckins(apiBase: String, token: String, scope: FeedScope = FeedScope.All): List<CheckinItem> = withContext(Dispatchers.IO) {
        val request = authorizedBuilder(apiBase, checkinFeedPath(scope), token).get().build()
        val items = executeJson(request).getJSONArray("items")
        buildList {
            for (index in 0 until items.length()) {
                add(parseCheckin(items.getJSONObject(index)))
            }
        }
    }

    suspend fun listTags(apiBase: String, token: String): List<TagItem> = withContext(Dispatchers.IO) {
        val request = authorizedBuilder(apiBase, "/api/tags", token).get().build()
        val items = executeJson(request).getJSONArray("items")
        buildList {
            for (index in 0 until items.length()) add(parseTagItem(items.getJSONObject(index)))
        }
    }

    suspend fun uploadCheckin(
        context: Context,
        apiBase: String,
        token: String,
        draft: UploadDraft,
        requestId: String,
        onProgress: (Int) -> Unit = {},
    ): CheckinItem = withContext(Dispatchers.IO) {
        val uri = draft.photoUri ?: throw IllegalArgumentException("请先拍照或选择图片")
        val media = prepareUploadMedia(context.contentResolver, uri) { written, total ->
            if (total != null && total > 0L) onProgress(((written * 100L) / total).toInt().coerceIn(0, 100))
        }
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("client_request_id", requestId)
            .addFormDataPart("place_name", draft.placeName.ifBlank { "未命名地点" })
            .addFormDataPart("latitude", draft.latitude.trim())
            .addFormDataPart("longitude", draft.longitude.trim())
            .addFormDataPart("note", draft.note)
            .addFormDataPart("tags", draft.tags)
            .addFormDataPart("source", "android_capture")
            .addFormDataPart("location_source", draft.locationSource)
            .addFormDataPart("visibility", draft.visibility)
            .addFormDataPart("photo", media.filename, media.requestBody)
            .build()
        val request = authorizedBuilder(apiBase, "/api/checkins", token).post(body).build()
        parseCheckin(executeJson(request))
    }

    suspend fun deleteCheckin(apiBase: String, token: String, checkinId: String): Boolean = withContext(Dispatchers.IO) {
        val request = authorizedBuilder(apiBase, "/api/checkins/$checkinId", token).delete().build()
        executeJson(request).optBoolean("deleted", false)
    }

    suspend fun getCheckin(apiBase: String, token: String, checkinId: String): CheckinItem = withContext(Dispatchers.IO) {
        val request = authorizedBuilder(apiBase, "/api/checkins/$checkinId", token).get().build()
        parseCheckin(executeJson(request))
    }

    suspend fun updateCheckin(apiBase: String, token: String, record: CheckinItem, draft: CheckinEditDraft): CheckinItem = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put("place_name", draft.placeName.trim())
            .put("latitude", draft.latitude.toDouble())
            .put("longitude", draft.longitude.toDouble())
            .put("taken_at", record.takenAt)
            .put("note", draft.note.trim())
            .put("tags", draft.tags.trim())
            .put("source", record.source.ifBlank { "android_capture" })
            .put("location_source", "manual")
            .put("visibility", draft.visibility)
            .toString()
        val request = authorizedBuilder(apiBase, "/api/checkins/${record.id}", token)
            .patch(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()
        parseCheckin(executeJson(request))
    }

    suspend fun setLike(apiBase: String, token: String, record: CheckinItem): CheckinItem = withContext(Dispatchers.IO) {
        val requestBuilder = authorizedBuilder(apiBase, "/api/checkins/${record.id}/like", token)
        val request = if (record.liked) requestBuilder.delete().build() else requestBuilder.post(ByteArray(0).toRequestBody(null)).build()
        val interaction = executeJson(request)
        record.copy(
            liked = interaction.optBoolean("liked"),
            likeCount = interaction.optInt("like_count"),
            commentCount = interaction.optInt("comment_count"),
        )
    }

    suspend fun listComments(apiBase: String, token: String, checkinId: String): List<CommentItem> = withContext(Dispatchers.IO) {
        val items = executeJson(authorizedBuilder(apiBase, "/api/checkins/$checkinId/comments", token).get().build()).getJSONArray("items")
        buildList { for (index in 0 until items.length()) add(parseComment(items.getJSONObject(index))) }
    }

    suspend fun addComment(apiBase: String, token: String, checkinId: String, body: String): CommentItem = withContext(Dispatchers.IO) {
        val payload = JSONObject().put("body", body.trim()).toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        parseComment(executeJson(authorizedBuilder(apiBase, "/api/checkins/$checkinId/comments", token).post(payload).build()))
    }

    suspend fun listNotifications(apiBase: String, token: String): List<NotificationItem> = withContext(Dispatchers.IO) {
        val items = executeJson(authorizedBuilder(apiBase, "/api/notifications?limit=100", token).get().build()).getJSONArray("items")
        buildList { for (index in 0 until items.length()) add(parseNotification(items.getJSONObject(index))) }
    }

    suspend fun markNotificationsRead(apiBase: String, token: String) = withContext(Dispatchers.IO) {
        executeJson(authorizedBuilder(apiBase, "/api/notifications/read-all", token).post(ByteArray(0).toRequestBody(null)).build())
    }

    suspend fun listPeople(apiBase: String, token: String): List<PersonItem> = withContext(Dispatchers.IO) {
        val items = executeJson(authorizedBuilder(apiBase, "/api/users?limit=100", token).get().build()).getJSONArray("items")
        buildList { for (index in 0 until items.length()) add(parsePerson(items.getJSONObject(index))) }
    }

    suspend fun setFollowing(apiBase: String, token: String, person: PersonItem): PersonItem = withContext(Dispatchers.IO) {
        val builder = authorizedBuilder(apiBase, "/api/users/${person.id}/follow", token)
        val request = if (person.following) builder.delete().build() else builder.post(ByteArray(0).toRequestBody(null)).build()
        person.copy(following = executeJson(request).optBoolean("following"))
    }

    suspend fun blockPerson(apiBase: String, token: String, personId: Int) = withContext(Dispatchers.IO) {
        executeJson(authorizedBuilder(apiBase, "/api/users/$personId/block", token).post(ByteArray(0).toRequestBody(null)).build())
    }

    suspend fun listAlbums(apiBase: String, token: String): List<AlbumItem> = withContext(Dispatchers.IO) {
        val items = executeJson(authorizedBuilder(apiBase, "/api/albums", token).get().build()).getJSONArray("items")
        buildList { for (index in 0 until items.length()) add(parseAlbum(items.getJSONObject(index))) }
    }

    suspend fun listExports(apiBase: String, token: String): List<ExportItem> = withContext(Dispatchers.IO) {
        val items = executeJson(authorizedBuilder(apiBase, "/api/exports?limit=50", token).get().build()).getJSONArray("items")
        buildList { for (index in 0 until items.length()) add(parseExport(items.getJSONObject(index))) }
    }

    suspend fun createExport(apiBase: String, token: String): ExportItem = withContext(Dispatchers.IO) {
        parseExport(executeJson(authorizedBuilder(apiBase, "/api/exports", token).post(ByteArray(0).toRequestBody(null)).build()))
    }

    suspend fun downloadExport(context: Context, apiBase: String, token: String, record: ExportItem): Uri = withContext(Dispatchers.IO) {
        val request = authorizedBuilder(apiBase, record.downloadUrl ?: "/api/exports/${record.id}/download", token).get().build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw ApiException(response.code, "导出下载失败（HTTP ${response.code}）")
            val directory = File(context.cacheDir, "exports").apply { mkdirs() }
            val target = File(directory, "nunulo-export-${record.id}.zip")
            response.body.byteStream().use { input -> target.outputStream().use(input::copyTo) }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", target)
        }
    }

    suspend fun createInvite(apiBase: String, token: String): String = withContext(Dispatchers.IO) {
        val body = "{}".toRequestBody("application/json; charset=utf-8".toMediaType())
        executeJson(authorizedBuilder(apiBase, "/api/invites", token).post(body).build()).getString("code")
    }

    suspend fun reportCheckin(apiBase: String, token: String, checkinId: String, reason: String) = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("target_type", "checkin")
            .put("target_id", checkinId)
            .put("reason", reason)
            .toString()
            .toRequestBody("application/json; charset=utf-8".toMediaType())
        executeJson(authorizedBuilder(apiBase, "/api/reports", token).post(body).build())
    }

    suspend fun createAlbum(apiBase: String, token: String, title: String): AlbumItem = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("title", title.trim())
            .put("description", "")
            .put("visibility", "private")
            .toString()
            .toRequestBody("application/json; charset=utf-8".toMediaType())
        parseAlbum(executeJson(authorizedBuilder(apiBase, "/api/albums", token).post(body).build()))
    }

    suspend fun addAlbumItem(apiBase: String, token: String, albumId: String, checkinId: String): AlbumItem = withContext(Dispatchers.IO) {
        parseAlbum(executeJson(authorizedBuilder(apiBase, "/api/albums/$albumId/checkins/$checkinId", token).post(ByteArray(0).toRequestBody(null)).build()))
    }

    suspend fun uploadAvatar(context: Context, apiBase: String, token: String, uri: Uri): AuthUser = withContext(Dispatchers.IO) {
        val media = prepareUploadMedia(context.contentResolver, uri)
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("photo", media.filename, media.requestBody)
            .build()
        val request = authorizedBuilder(apiBase, "/api/users/me/avatar", token).post(body).build()
        parseAuthUser(executeJson(request).getJSONObject("user"))
    }

    suspend fun downloadBitmap(apiBase: String, url: String): Bitmap? = withContext(Dispatchers.IO) {
        val requestBuilder = Request.Builder().url(url).get()
        if (mediaAccessToken.isNotBlank() && url.startsWith(apiBase.trimEnd('/') + "/api/")) {
            requestBuilder.header("Authorization", "Bearer $mediaAccessToken")
        }
        val request = requestBuilder.build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext null
            response.body.byteStream().use(BitmapFactory::decodeStream)
        }
    }

    private fun authorizedBuilder(apiBase: String, path: String, token: String): Request.Builder =
        http.authorizedBuilder(apiBase, path, token)

    private fun executeJson(request: Request): JSONObject = http.executeJson(request)

}

@Composable
private fun NunuloApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("nunulo", Context.MODE_PRIVATE) }
    val api = remember { NunuloApi() }
    val tokenRefreshCoordinator = remember { TokenRefreshCoordinator() }
    val scope = rememberCoroutineScope()

    var activeTab by rememberSaveable { mutableStateOf(AppTab.Feed.name) }
    var apiBase by rememberSaveable { mutableStateOf(prefs.getString("apiBase", DEFAULT_API_BASE) ?: DEFAULT_API_BASE) }
    var loginName by rememberSaveable { mutableStateOf(prefs.getString("lastLogin", "lumokato") ?: "lumokato") }
    var password by rememberSaveable { mutableStateOf("") }
    var accessToken by rememberSaveable { mutableStateOf(prefs.getString("accessToken", "") ?: "") }
    var refreshToken by rememberSaveable { mutableStateOf(prefs.getString("refreshToken", "") ?: "") }
    var currentUser by remember { mutableStateOf<AuthUser?>(null) }
    var records by remember { mutableStateOf<List<CheckinItem>>(emptyList()) }
    var mineRecords by remember { mutableStateOf<List<CheckinItem>>(emptyList()) }
    var feedScope by rememberSaveable { mutableStateOf(FeedScope.All.name) }
    var notifications by remember { mutableStateOf<List<NotificationItem>>(emptyList()) }
    var people by remember { mutableStateOf<List<PersonItem>>(emptyList()) }
    var albums by remember { mutableStateOf<List<AlbumItem>>(emptyList()) }
    var exports by remember { mutableStateOf<List<ExportItem>>(emptyList()) }
    var comments by remember { mutableStateOf<List<CommentItem>>(emptyList()) }
    var inviteCode by rememberSaveable { mutableStateOf("") }
    var availableTags by remember { mutableStateOf<List<TagItem>>(emptyList()) }
    var draft by remember { mutableStateOf(UploadDraft()) }
    var currentDeviceLocation by remember { mutableStateOf<MapPoint?>(null) }
    var pendingLocationPurpose by remember { mutableStateOf<LocationRequestPurpose?>(null) }
    var avatarUri by remember { mutableStateOf<Uri?>(null) }
    var filters by remember { mutableStateOf(BrowseFilters()) }
    var pendingDeleteId by rememberSaveable { mutableStateOf("") }
    var selectedRecord by remember { mutableStateOf<CheckinItem?>(null) }
    var editingRecord by remember { mutableStateOf<CheckinItem?>(null) }
    var uploadPhase by rememberSaveable { mutableStateOf("idle") }
    var uploadProgress by rememberSaveable { mutableStateOf(0) }
    var message by rememberSaveable { mutableStateOf("准备记录今天的娃娃出行") }
    var busy by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val pending = loadPendingUpload(prefs)
        if (pending != null) {
            draft = pending.draft
            uploadPhase = if (pending.attempted) "failed" else "idle"
            message = if (pending.attempted) {
                "上次上传被中断，草稿已恢复；再次登记会安全重试"
            } else {
                "未完成草稿已恢复"
            }
            activeTab = AppTab.Publish.name
        }
    }

    LaunchedEffect(draft) {
        if (draft.photoUri != null) {
            runCatching {
                withContext(Dispatchers.IO) {
                    val pending = loadPendingUpload(prefs)?.takeIf { it.draft.photoUri == draft.photoUri }
                        ?: PendingUpload(UUID.randomUUID().toString(), draft)
                    savePendingUpload(prefs, pending.copy(draft = draft))
                }
            }.onFailure { error ->
                message = error.message ?: "无法保存上传草稿"
                Log.e(APP_LOG_TAG, "Failed to persist upload draft for ${draft.photoUri}", error)
            }
        }
    }

    LaunchedEffect(accessToken) {
        api.setAccessToken(accessToken)
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) {
            val capturedUri = draft.photoUri
            if (capturedUri != null) {
                scope.launch {
                    busy = true
                    try {
                        val cachedUri = withContext(Dispatchers.IO) { cacheSelectedMedia(context, capturedUri) }
                        runCatching { context.contentResolver.delete(capturedUri, null, null) }
                        draft = draft.copy(photoUri = cachedUri)
                        message = "照片已就绪，可以补充地点和标签"
                    } catch (error: Exception) {
                        draft = draft.copy(photoUri = null)
                        message = error.message ?: "无法读取拍摄照片"
                        Log.e(APP_LOG_TAG, "Failed to cache captured media $capturedUri", error)
                    } finally {
                        busy = false
                    }
                }
            }
        } else {
            deleteCachedMedia(context, draft.photoUri)
            draft.photoUri?.let { runCatching { context.contentResolver.delete(it, null, null) } }
            draft = draft.copy(photoUri = null)
            message = "拍照已取消"
        }
    }
    val pickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            scope.launch {
                busy = true
                try {
                    val cachedUri = withContext(Dispatchers.IO) { cacheSelectedMedia(context, uri) }
                    deleteCachedMedia(context, draft.photoUri)
                    draft = draft.copy(photoUri = cachedUri)
                    message = "已选择图片，可以上传"
                } catch (error: Exception) {
                    draft = draft.copy(photoUri = null)
                    message = error.message ?: "无法读取图片"
                    Log.e(APP_LOG_TAG, "Failed to cache selected media $uri", error)
                } finally {
                    busy = false
                }
            }
        }
    }
    val handleDeviceLocation: (LocationRequestPurpose, MapPoint?) -> Unit = { purpose, location ->
        if (location == null) {
            message = when (purpose) {
                LocationRequestPurpose.Draft -> "暂时拿不到定位，请稍后重试或手动填写坐标"
                LocationRequestPurpose.Map -> "暂时拿不到当前位置，请检查系统定位后重试"
                LocationRequestPurpose.PermissionOnly -> "定位权限已开启，但暂时拿不到当前位置"
            }
        } else {
            currentDeviceLocation = location
            when (purpose) {
                LocationRequestPurpose.Draft -> {
                    draft = draft.copy(
                        latitude = "%.6f".format(location.latitude),
                        longitude = "%.6f".format(location.longitude),
                        locationSource = "device_location",
                    )
                    message = "已填入当前位置"
                }
                LocationRequestPurpose.Map -> message = "地图已定位到当前位置"
                LocationRequestPurpose.PermissionOnly -> message = "定位权限已开启"
            }
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        val purpose = pendingLocationPurpose ?: LocationRequestPurpose.PermissionOnly
        pendingLocationPurpose = null
        val granted = permissions.values.any { it }
        if (granted) {
            scope.launch {
                val location = currentLocation(context)?.let { MapPoint(it.latitude, it.longitude) }
                handleDeviceLocation(purpose, location)
            }
        } else {
            message = if (purpose == LocationRequestPurpose.Draft) {
                "未开启定位权限，可继续手动填写坐标"
            } else {
                "未开启定位权限，地图无法显示当前位置"
            }
        }
    }

    fun persistTokens(newAccessToken: String, newRefreshToken: String = refreshToken) {
        accessToken = newAccessToken
        refreshToken = newRefreshToken
        prefs.edit()
            .putString("accessToken", accessToken)
            .putString("refreshToken", refreshToken)
            .apply()
    }

    fun clearAuthState(nextMessage: String) {
        accessToken = ""
        refreshToken = ""
        currentUser = null
        records = emptyList()
        mineRecords = emptyList()
        notifications = emptyList()
        people = emptyList()
        albums = emptyList()
        exports = emptyList()
        comments = emptyList()
        availableTags = emptyList()
        prefs.edit().remove("accessToken").remove("refreshToken").apply()
        message = nextMessage
        activeTab = AppTab.Me.name
    }

    suspend fun <T> runWithTokenRefresh(block: suspend (String) -> T): T {
        return tokenRefreshCoordinator.run(
            currentAccessToken = { accessToken },
            currentRefreshToken = { refreshToken },
            persistAccessToken = { persistTokens(it) },
            refreshAccessToken = { api.refreshAccessToken(apiBase, it) },
            block = block,
        )
    }

    suspend fun refreshLibraryState() {
        if (accessToken.isBlank()) return
        val state = runCatching {
            runWithTokenRefresh { token ->
                AuxiliaryState(
                    tags = api.listTags(apiBase, token),
                    notifications = api.listNotifications(apiBase, token),
                    people = api.listPeople(apiBase, token),
                    albums = api.listAlbums(apiBase, token),
                    exports = api.listExports(apiBase, token),
                )
            }
        }.getOrNull()
        if (state != null) {
            availableTags = state.tags
            notifications = state.notifications
            people = state.people
            albums = state.albums
            exports = state.exports
        }
    }

    val avatarPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            avatarUri = uri
            if (accessToken.isBlank()) {
                message = "请先登录后上传头像"
            } else {
                busy = true
                scope.launch {
                    try {
                        currentUser = runWithTokenRefresh { token -> api.uploadAvatar(context, apiBase, token, uri) }
                        refreshLibraryState()
                        message = "头像已上传"
                    } catch (error: Exception) {
                        message = error.message ?: "头像上传失败"
                    } finally {
                        busy = false
                    }
                }
            }
        }
    }

    fun refreshProfileAndRecords() {
        if (accessToken.isBlank()) {
            activeTab = AppTab.Me.name
            message = "请先登录"
            return
        }
        busy = true
        scope.launch {
            try {
                val (user, feedItems, ownItems) = runWithTokenRefresh { token ->
                    val selectedScope = FeedScope.valueOf(feedScope)
                    val selectedItems = api.listCheckins(apiBase, token, selectedScope)
                    Triple(
                        api.me(apiBase, token),
                        selectedItems,
                        if (selectedScope == FeedScope.Mine) selectedItems else api.listCheckins(apiBase, token, FeedScope.Mine),
                    )
                }
                currentUser = user
                records = feedItems
                mineRecords = ownItems
                refreshLibraryState()
                pendingDeleteId = ""
                message = "已同步 ${records.size} 条记录"
            } catch (error: Exception) {
                if (looksLikeExpiredToken(error)) {
                    clearAuthState("登录已过期，请重新登录")
                } else {
                    message = error.message ?: "同步失败"
                }
            } finally {
                busy = false
            }
        }
    }

    fun switchFeedScope(next: FeedScope) {
        val previous = FeedScope.valueOf(feedScope)
        feedScope = next.name
        if (accessToken.isBlank()) return
        busy = true
        scope.launch {
            try {
                records = runWithTokenRefresh { token -> api.listCheckins(apiBase, token, next) }
                if (next == FeedScope.Mine) mineRecords = records
                message = "已切换到${next.label}动态"
            } catch (error: Exception) {
                feedScope = previous.name
                message = error.message ?: "动态加载失败"
            } finally {
                busy = false
            }
        }
    }

    fun login() {
        busy = true
        scope.launch {
            try {
                val (user, tokens) = api.login(apiBase, loginName, password)
                currentUser = user
                prefs.edit()
                    .putString("apiBase", apiBase.trim().trimEnd('/'))
                    .putString("lastLogin", loginName)
                    .apply()
                persistTokens(tokens.accessToken, tokens.refreshToken.orEmpty())
                password = ""
                records = api.listCheckins(apiBase, tokens.accessToken, FeedScope.All)
                mineRecords = api.listCheckins(apiBase, tokens.accessToken, FeedScope.Mine)
                feedScope = FeedScope.All.name
                refreshLibraryState()
                pendingDeleteId = ""
                message = "欢迎回来，${user.displayName}"
                activeTab = AppTab.Feed.name
            } catch (error: Exception) {
                message = error.message ?: "登录失败"
            } finally {
                busy = false
            }
        }
    }

    fun register(username: String, displayName: String, invite: String, newPassword: String) {
        busy = true
        scope.launch {
            try {
                val (user, tokens) = api.register(apiBase, username, displayName, invite, newPassword)
                currentUser = user
                loginName = username
                persistTokens(tokens.accessToken, tokens.refreshToken.orEmpty())
                records = api.listCheckins(apiBase, tokens.accessToken, FeedScope.All)
                mineRecords = api.listCheckins(apiBase, tokens.accessToken, FeedScope.Mine)
                feedScope = FeedScope.All.name
                refreshLibraryState()
                message = "测试账号已创建，欢迎 ${user.displayName}"
                activeTab = AppTab.Feed.name
            } catch (error: Exception) {
                message = error.message ?: "注册失败"
            } finally {
                busy = false
            }
        }
    }

    fun logout() {
        clearAuthState("已退出登录")
    }

    fun requestDeviceLocation(purpose: LocationRequestPurpose) {
        pendingLocationPurpose = purpose
        if (!hasLocationPermission(context)) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                )
            )
            return
        }
        pendingLocationPurpose = null
        scope.launch {
            val location = currentLocation(context)?.let { MapPoint(it.latitude, it.longitude) }
            handleDeviceLocation(purpose, location)
        }
    }

    fun requestCorePermissions() = requestDeviceLocation(LocationRequestPurpose.PermissionOnly)

    fun useDeviceLocation() = requestDeviceLocation(LocationRequestPurpose.Draft)

    fun takePhoto() {
        val uri = createCaptureUri(context)
        deleteCachedMedia(context, draft.photoUri)
        clearPendingUpload(prefs)
        draft = draft.copy(photoUri = uri)
        cameraLauncher.launch(uri)
    }

    fun clearUploadDraft() {
        if (busy) return
        deleteCachedMedia(context, draft.photoUri)
        clearPendingUpload(prefs)
        draft = UploadDraft()
        uploadPhase = "idle"
        uploadProgress = 0
        message = "上传草稿已清除"
    }

    fun upload() {
        if (accessToken.isBlank()) {
            message = "请先登录"
            activeTab = AppTab.Me.name
            return
        }
        val uploadDraft = draft
        busy = true
        uploadPhase = "preparing"
        uploadProgress = 0
        scope.launch {
            try {
                val pendingUpload = withContext(Dispatchers.IO) {
                    val pending = loadPendingUpload(prefs)?.takeIf { it.draft.photoUri == uploadDraft.photoUri }
                        ?: PendingUpload(UUID.randomUUID().toString(), uploadDraft)
                    pending.copy(draft = uploadDraft, attempted = true).also { savePendingUpload(prefs, it) }
                }
                uploadPhase = "uploading"
                val uploaded = runWithTokenRefresh { token ->
                    api.uploadCheckin(context, apiBase, token, uploadDraft, pendingUpload.requestId) { progress ->
                        scope.launch { uploadProgress = progress }
                    }
                }
                mineRecords = listOf(uploaded) + mineRecords.filterNot { it.id == uploaded.id }
                val selectedScope = FeedScope.valueOf(feedScope)
                records = runCatching {
                    runWithTokenRefresh { token -> api.listCheckins(apiBase, token, FeedScope.valueOf(feedScope)) }
                }.getOrElse {
                    if (shouldShowOwnUpload(selectedScope, uploaded.visibility)) {
                        listOf(uploaded) + records.filterNot { it.id == uploaded.id }
                    } else {
                        records
                    }
                }
                currentUser = runCatching { runWithTokenRefresh { token -> api.me(apiBase, token) } }.getOrNull() ?: currentUser
                refreshLibraryState()
                deleteCachedMedia(context, uploadDraft.photoUri)
                clearPendingUpload(prefs)
                draft = UploadDraft()
                pendingDeleteId = ""
                uploadPhase = "done"
                uploadProgress = 100
                message = "上传成功：${uploaded.placeName}"
                activeTab = AppTab.Feed.name
            } catch (error: Exception) {
                uploadPhase = "failed"
                uploadProgress = 0
                message = error.message ?: "上传失败"
                Log.e(APP_LOG_TAG, "Upload failed for ${uploadDraft.photoUri}", error)
            } finally {
                busy = false
            }
        }
    }

    fun deleteRecord(record: CheckinItem) {
        if (accessToken.isBlank()) {
            message = "请先登录"
            activeTab = AppTab.Me.name
            return
        }
        if (pendingDeleteId != record.id) {
            pendingDeleteId = record.id
            message = "再次点击删除 ${record.placeName}"
            return
        }
        busy = true
        scope.launch {
            try {
                runWithTokenRefresh { token -> api.deleteCheckin(apiBase, token, record.id) }
                records = records.filterNot { it.id == record.id }
                mineRecords = mineRecords.filterNot { it.id == record.id }
                currentUser = runCatching { runWithTokenRefresh { token -> api.me(apiBase, token) } }.getOrNull() ?: currentUser
                refreshLibraryState()
                pendingDeleteId = ""
                message = "已删除：${record.placeName}"
            } catch (error: Exception) {
                message = error.message ?: "删除失败"
            } finally {
                busy = false
            }
        }
    }

    fun openRecord(record: CheckinItem) {
        selectedRecord = record
        scope.launch {
            val detail = runCatching { runWithTokenRefresh { token -> api.getCheckin(apiBase, token, record.id) } }.getOrDefault(record)
            selectedRecord = detail
            comments = runCatching { runWithTokenRefresh { token -> api.listComments(apiBase, token, record.id) } }.getOrDefault(emptyList())
        }
    }

    fun toggleLike(record: CheckinItem) {
        scope.launch {
            try {
                val updated = runWithTokenRefresh { token -> api.setLike(apiBase, token, record) }
                records = records.map { if (it.id == updated.id) updated else it }
                mineRecords = mineRecords.map { if (it.id == updated.id) updated else it }
                if (selectedRecord?.id == updated.id) selectedRecord = updated
            } catch (error: Exception) {
                message = error.message ?: "互动失败"
            }
        }
    }

    fun addComment(record: CheckinItem, body: String) {
        if (body.isBlank()) return
        scope.launch {
            try {
                comments = comments + runWithTokenRefresh { token -> api.addComment(apiBase, token, record.id, body) }
                val updated = record.copy(commentCount = record.commentCount + 1)
                records = records.map { if (it.id == updated.id) updated else it }
                mineRecords = mineRecords.map { if (it.id == updated.id) updated else it }
                selectedRecord = updated
            } catch (error: Exception) {
                message = error.message ?: "评论失败"
            }
        }
    }

    fun reportRecord(record: CheckinItem) {
        scope.launch {
            try {
                runWithTokenRefresh { token -> api.reportCheckin(apiBase, token, record.id, "Android 测试用户举报") }
                message = "举报已提交给管理员"
            } catch (error: Exception) {
                message = error.message ?: "举报失败"
            }
        }
    }

    fun toggleFollow(person: PersonItem) {
        scope.launch {
            try {
                val updated = runWithTokenRefresh { token -> api.setFollowing(apiBase, token, person) }
                people = people.map { if (it.id == updated.id) updated else it }
            } catch (error: Exception) {
                message = error.message ?: "关注操作失败"
            }
        }
    }

    fun blockPerson(person: PersonItem) {
        scope.launch {
            try {
                runWithTokenRefresh { token -> api.blockPerson(apiBase, token, person.id) }
                people = people.filterNot { it.id == person.id }
                records = records.filterNot { it.userId == person.id }
                if (selectedRecord?.userId == person.id) selectedRecord = null
                message = "已屏蔽 ${person.displayName}"
            } catch (error: Exception) {
                message = error.message ?: "屏蔽失败"
            }
        }
    }

    fun readAllNotifications() {
        scope.launch {
            runCatching { runWithTokenRefresh { token -> api.markNotificationsRead(apiBase, token) } }
            notifications = notifications.map { it.copy(readAt = it.readAt ?: OffsetDateTime.now().toString()) }
        }
    }

    fun createPersonalInvite() {
        scope.launch {
            try {
                inviteCode = runWithTokenRefresh { token -> api.createInvite(apiBase, token) }
                message = "个人邀请码已生成"
            } catch (error: Exception) {
                message = error.message ?: "邀请码生成失败"
            }
        }
    }

    fun createDataExport() {
        scope.launch {
            try {
                val created = runWithTokenRefresh { token -> api.createExport(apiBase, token) }
                exports = listOf(created) + exports.filterNot { it.id == created.id }
                message = "个人数据导出已生成"
            } catch (error: Exception) {
                message = error.message ?: "导出失败"
            }
        }
    }

    fun downloadDataExport(record: ExportItem) {
        scope.launch {
            busy = true
            try {
                val uri = runWithTokenRefresh { token -> api.downloadExport(context, apiBase, token, record) }
                val share = Intent(Intent.ACTION_SEND).apply {
                    type = "application/zip"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(share, "保存或分享 Nunulo 数据导出"))
                message = "导出文件已准备好"
            } catch (error: Exception) {
                message = error.message ?: "导出下载失败"
            } finally {
                busy = false
            }
        }
    }

    fun createPersonalAlbum(title: String) {
        scope.launch {
            try {
                val created = runWithTokenRefresh { token -> api.createAlbum(apiBase, token, title) }
                albums = listOf(created) + albums
                message = "合集 ${created.title} 已创建"
            } catch (error: Exception) {
                message = error.message ?: "合集创建失败"
            }
        }
    }

    fun addRecordToAlbum(record: CheckinItem, album: AlbumItem) {
        scope.launch {
            try {
                val updated = runWithTokenRefresh { token -> api.addAlbumItem(apiBase, token, album.id, record.id) }
                albums = albums.map { if (it.id == updated.id) updated else it }
                message = "已加入 ${album.title}"
            } catch (error: Exception) {
                message = error.message ?: "加入合集失败"
            }
        }
    }

    fun saveRecord(record: CheckinItem, edit: CheckinEditDraft) {
        busy = true
        scope.launch {
            try {
                val updated = runWithTokenRefresh { token -> api.updateCheckin(apiBase, token, record, edit) }
                records = records.map { if (it.id == updated.id) updated else it }
                mineRecords = listOf(updated) + mineRecords.filterNot { it.id == updated.id }
                selectedRecord = updated
                editingRecord = null
                refreshLibraryState()
                message = "记录已更新"
            } catch (error: Exception) {
                message = error.message ?: "更新失败"
            } finally {
                busy = false
            }
        }
    }

    LaunchedEffect(Unit) {
        if (accessToken.isNotBlank()) refreshProfileAndRecords()
    }

    LaunchedEffect(activeTab) {
        when (AppTab.valueOf(activeTab)) {
            AppTab.Map -> requestDeviceLocation(LocationRequestPurpose.Map)
            AppTab.Publish -> if (draft.latitude.isBlank() && draft.longitude.isBlank()) {
                requestDeviceLocation(LocationRequestPurpose.Draft)
            }
            else -> Unit
        }
    }

    DollTheme {
        Scaffold(
            bottomBar = {
                Column(Modifier.fillMaxWidth().background(Color.White).navigationBarsPadding()) {
                    Surface(color = Color.White, border = BorderStroke(0.5.dp, NunuloUi.Hairline), modifier = Modifier.fillMaxWidth().height(56.dp)) {
                        Row(Modifier.fillMaxSize()) {
                            AppTab.entries.forEach { tab ->
                                val selected = activeTab == tab.name
                                Column(Modifier.weight(1f).fillMaxSize().clickable { activeTab = tab.name }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                    Box(contentAlignment = Alignment.TopEnd) {
                                        StitchTabIcon(tab, selected)
                                        if (tab == AppTab.Notifications) {
                                            TabUnreadBadge(notifications.count { it.readAt == null })
                                        }
                                    }
                                    Spacer(Modifier.height(2.dp))
                                    Text(tab.title, fontSize = 10.sp, lineHeight = 12.sp, color = if (selected) NunuloUi.Coral else NunuloUi.Ink, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        }
                    }
                }
            },
        ) { padding ->
            Surface(color = NunuloUi.Background, modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                    when (AppTab.valueOf(activeTab)) {
                        AppTab.Feed -> FeedScreen(
                            records = filterRecords(records, filters),
                            allRecords = records,
                            scope = FeedScope.valueOf(feedScope),
                            filters = filters,
                            apiBase = apiBase,
                            api = api,
                            onScopeChange = { switchFeedScope(it) },
                            onFiltersChange = { filters = it },
                            onPublish = { activeTab = AppTab.Publish.name },
                            onOpen = { openRecord(it) },
                            onLike = { toggleLike(it) },
                        )
                        AppTab.Map -> MapScreen(
                            records = filterRecords(mineRecords, filters),
                            currentLocation = currentDeviceLocation,
                            onLocate = { requestDeviceLocation(LocationRequestPurpose.Map) },
                            onPublish = { activeTab = AppTab.Publish.name },
                            onOpen = { openRecord(it) },
                        )
                        AppTab.Publish -> PublishScreen(
                            draft = draft,
                            busy = busy,
                            uploadPhase = uploadPhase,
                            uploadProgress = uploadProgress,
                            uploadMessage = message,
                            availableTags = availableTags,
                            onDraftChange = { draft = it },
                            onPick = { pickerLauncher.launch(arrayOf("image/jpeg", "image/png", "image/webp")) },
                            onCamera = { takePhoto() },
                            onLocation = { useDeviceLocation() },
                            onClear = { clearUploadDraft() },
                            onUpload = { upload() },
                        )
                        AppTab.Notifications -> NotificationsScreen(
                            notifications = notifications,
                            people = people,
                            onReadAll = { readAllNotifications() },
                            onFollow = { toggleFollow(it) },
                            onBlock = { blockPerson(it) },
                        )
                        AppTab.Me -> MeScreen(
                            apiBase = apiBase,
                            api = api,
                            loginName = loginName,
                            password = password,
                            user = currentUser,
                            records = mineRecords,
                            albums = albums,
                            exports = exports,
                            inviteCode = inviteCode,
                            avatarUri = avatarUri,
                            busy = busy,
                            onLoginNameChange = { loginName = it },
                            onApiBaseChange = { apiBase = it },
                            onPasswordChange = { password = it },
                            onLogin = { login() },
                            onRegister = { username, displayName, invite, newPassword -> register(username, displayName, invite, newPassword) },
                            onLogout = { logout() },
                            onPermissions = { requestCorePermissions() },
                            onPickAvatar = { avatarPickerLauncher.launch("image/*") },
                            onRefresh = { refreshProfileAndRecords() },
                            onOpenRecord = { openRecord(it) },
                            onOpenPhotos = { filters = BrowseFilters(); switchFeedScope(FeedScope.Mine); activeTab = AppTab.Feed.name },
                            onOpenPlaces = { activeTab = AppTab.Map.name },
                            onOpenTags = { filters = filters.copy(tag = rankedTags(mineRecords).firstOrNull()?.first.orEmpty()); switchFeedScope(FeedScope.Mine); activeTab = AppTab.Feed.name },
                            onCreateInvite = { createPersonalInvite() },
                            onCreateAlbum = { createPersonalAlbum(it) },
                            onCreateExport = { createDataExport() },
                            onDownloadExport = { downloadDataExport(it) },
                            message = message,
                        )
                    }
                }
            }
        }
        selectedRecord?.let { record ->
            CheckinDetailDialog(
                record = record,
                apiBase = apiBase,
                api = api,
                busy = busy,
                onDismiss = { selectedRecord = null; pendingDeleteId = "" },
                onEdit = { editingRecord = record },
                onDelete = {
                    if (pendingDeleteId == record.id) {
                        deleteRecord(record)
                        selectedRecord = null
                    } else pendingDeleteId = record.id
                },
                comments = comments,
                albums = albums,
                onLike = { toggleLike(record) },
                onComment = { addComment(record, it) },
                onReport = { reportRecord(record) },
                onAddToAlbum = { addRecordToAlbum(record, it) },
                pendingDelete = pendingDeleteId == record.id,
            )
        }
        editingRecord?.let { record ->
            CheckinEditDialog(
                record = record,
                availableTags = availableTags,
                busy = busy,
                onDismiss = { editingRecord = null },
                onSave = { saveRecord(record, it) },
            )
        }
    }
}

@Composable
private fun TopStatusBar(user: AuthUser?, recordsCount: Int, message: String, busy: Boolean, onRefresh: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(NunuloUi.Background)
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(listOf(NunuloUi.CoralSoft, NunuloUi.BlueSoft))),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("N", color = NunuloUi.Coral, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Nunulo", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = NunuloUi.Ink)
                    Text(
                        user?.let { "${it.displayName} 的照片动态 · $recordsCount 张" } ?: "邀请制多人照片测试",
                        color = NunuloUi.Muted,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            TextButton(onClick = onRefresh, enabled = !busy) { Text(if (busy) "同步中" else "同步") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            TopIndexPill("照片", recordsCount.toString(), NunuloUi.CoralSoft, NunuloUi.Coral, Modifier.weight(1f))
            StatusPill(text = message, busy = busy, modifier = Modifier.weight(2f))
        }
    }
}

@Composable
private fun TopIndexPill(label: String, value: String, background: Color, foreground: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(background)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(value, color = foreground, fontWeight = FontWeight.Black, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
        Spacer(Modifier.width(5.dp))
        Text(label, color = NunuloUi.Slate, style = MaterialTheme.typography.bodySmall, maxLines = 1)
    }
}

@Composable
private fun StatusPill(text: String, busy: Boolean, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(if (busy) NunuloUi.AmberSoft else NunuloUi.GreenSoft)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (busy) NunuloUi.Amber else NunuloUi.Green),
        )
        Spacer(Modifier.width(8.dp))
        Text(text, color = NunuloUi.Slate, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun JournalCard(
    modifier: Modifier = Modifier,
    containerColor: Color = NunuloUi.Paper,
    radius: Dp = NunuloUi.CardRadius,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(radius),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, NunuloUi.Hairline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) { content() }
}

@Composable
private fun StitchTabIcon(tab: AppTab, selected: Boolean) {
    val image = when (tab) {
        AppTab.Feed -> if (selected) Icons.Filled.Home else Icons.Outlined.Home
        AppTab.Map -> if (selected) Icons.Filled.Map else Icons.Outlined.Map
        AppTab.Publish -> if (selected) Icons.Filled.AddCircle else Icons.Outlined.AddCircleOutline
        AppTab.Notifications -> if (selected) Icons.Filled.Notifications else Icons.Outlined.NotificationsNone
        AppTab.Me -> if (selected) Icons.Filled.Person else Icons.Outlined.PersonOutline
    }
    Icon(
        imageVector = image,
        contentDescription = tab.title,
        tint = if (selected) NunuloUi.Coral else NunuloUi.Ink,
        modifier = Modifier.size(24.dp),
    )
}

@Composable
private fun TabUnreadBadge(count: Int) {
    if (count <= 0) return
    Surface(color = NunuloUi.Danger, contentColor = Color.White, shape = CircleShape) {
        Text(
            if (count > 99) "99+" else count.toString(),
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
            fontSize = 9.sp,
            lineHeight = 11.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun StitchChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(modifier = Modifier.clickable(onClick = onClick), color = if (selected) NunuloUi.Coral else NunuloUi.Paper, contentColor = if (selected) Color.White else NunuloUi.Ink, shape = RoundedCornerShape(12.dp), border = if (selected) null else BorderStroke(1.dp, NunuloUi.Hairline)) {
        Text(label, modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp), fontSize = 12.sp, lineHeight = 16.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun FeedScreen(
    records: List<CheckinItem>, allRecords: List<CheckinItem>, filters: BrowseFilters,
    scope: FeedScope,
    apiBase: String, api: NunuloApi,
    onScopeChange: (FeedScope) -> Unit,
    onFiltersChange: (BrowseFilters) -> Unit, onPublish: () -> Unit, onOpen: (CheckinItem) -> Unit,
    onLike: (CheckinItem) -> Unit,
) {
    val tags = rankedTags(allRecords).map { it.first }
    val places = allRecords.map { it.placeName }.filter { it.isNotBlank() }.distinct().sorted()
    val rows = records.chunked(2)
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 8.dp)) {
        stickyHeader {
            Surface(color = NunuloUi.Surface, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("照片动态", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    TextButton(onClick = onPublish) { Text("登记") }
                }
            }
        }
        item {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(FeedScope.entries) { item ->
                        FilterChip(selected = scope == item, onClick = { onScopeChange(item) }, label = { Text(item.label) })
                    }
                }
                BrowseFilterPanel(filters = filters, tags = tags, places = places, onFiltersChange = onFiltersChange)
                RouteIndexCard(records = allRecords, visibleCount = records.size, filters = filters, placesCount = places.size, tagsCount = tags.size)
            }
        }
        if (allRecords.isEmpty()) item { Box(Modifier.padding(12.dp)) { FirstRecordCard(onPublish) } }
        else if (records.isEmpty()) item { Box(Modifier.padding(12.dp)) { EmptyState("没有匹配的照片", "调整搜索或清除筛选后再试。") } }
        items(rows, key = { it.joinToString("|") { record -> record.id } }) { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                row.forEach { record ->
                    Column(Modifier.weight(1f).background(Color.White)) {
                        Box(Modifier.fillMaxWidth().clickable { onOpen(record) }) {
                            RemoteImage(url = record.displayUrl ?: record.thumbUrl, apiBase = apiBase, api = api, aspect = 0.82f)
                            Surface(modifier = Modifier.align(Alignment.BottomStart).padding(6.dp), color = Color.Black.copy(alpha = 0.6f), shape = RoundedCornerShape(4.dp)) {
                                Text(record.authorName, color = Color.White, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp), maxLines = 1)
                            }
                        }
                        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(record.placeName, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(visibilityLabel(record.visibility), color = NunuloUi.Muted, style = MaterialTheme.typography.bodySmall)
                            }
                            TextButton(onClick = { onLike(record) }) { Text("${if (record.liked) "♥" else "♡"} ${record.likeCount}") }
                            TextButton(onClick = { onOpen(record) }) { Text("评 ${record.commentCount}") }
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(2.dp))
        }
    }
}

@Composable
private fun MapScreen(records: List<CheckinItem>, currentLocation: MapPoint?, onLocate: () -> Unit, onPublish: () -> Unit, onOpen: (CheckinItem) -> Unit) {
    Box(Modifier.fillMaxSize()) {
        AmapNativeMap(records = records, currentLocation = currentLocation, modifier = Modifier.fillMaxSize(), onOpen = onOpen)
        Surface(modifier = Modifier.align(Alignment.TopCenter).padding(12.dp), color = Color.White.copy(alpha = 0.96f), shape = RoundedCornerShape(4.dp), border = BorderStroke(1.dp, NunuloUi.Hairline)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.padding(start = 14.dp, top = 9.dp, bottom = 9.dp)) {
                    Text("我的照片地点", fontWeight = FontWeight.Bold)
                    Text(currentLocation?.let { "当前位置 ${formatCoordinate(it.latitude, it.longitude)}" } ?: "正在获取当前位置", color = NunuloUi.Muted, style = MaterialTheme.typography.bodySmall)
                }
                TextButton(onClick = onLocate) { Text("重新定位") }
            }
        }
        Surface(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(), color = Color.White.copy(alpha = 0.97f), shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp), border = BorderStroke(0.5.dp, NunuloUi.Hairline)) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("个人照片地图", fontWeight = FontWeight.Bold)
                    Text("${records.size} 个记录地点", color = NunuloUi.Muted, style = MaterialTheme.typography.bodySmall)
                }
                Surface(color = NunuloUi.Coral, shape = RoundedCornerShape(4.dp), modifier = Modifier.clickable(onClick = onPublish)) {
                    Text("在这里登记", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp))
                }
            }
        }
    }
}

@Composable
private fun NotificationsScreen(
    notifications: List<NotificationItem>,
    people: List<PersonItem>,
    onReadAll: () -> Unit,
    onFollow: (PersonItem) -> Unit,
    onBlock: (PersonItem) -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            SectionHeader("消息", "真实互动与测试成员", onReadAll, "全部已读")
        }
        if (notifications.isEmpty()) item { EmptyState("还没有消息", "点赞、评论和新的关注会显示在这里。") }
        items(notifications, key = { it.id }) { notice ->
            JournalCard(containerColor = if (notice.readAt == null) NunuloUi.CoralSoft else NunuloUi.Paper) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(notice.title, fontWeight = FontWeight.Bold)
                    Text(notice.body, color = NunuloUi.Slate)
                    Text(shortDate(notice.createdAt), color = NunuloUi.Muted, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item { Text("测试成员", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp)) }
        if (people.isEmpty()) item { EmptyState("还没有其他成员", "邀请好友注册后，可以在这里关注。") }
        items(people, key = { it.id }) { person ->
            JournalCard {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(42.dp).clip(CircleShape).background(NunuloUi.GreenSoft), contentAlignment = Alignment.Center) { Text(person.displayName.take(1), color = NunuloUi.Green, fontWeight = FontWeight.Black) }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(person.displayName, fontWeight = FontWeight.Bold)
                        Text(person.bio ?: person.username?.let { "@$it" } ?: "测试成员", color = NunuloUi.Muted, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Button(onClick = { onFollow(person) }) { Text(if (person.following) "已关注" else "关注") }
                        TextButton(onClick = { onBlock(person) }) { Text("屏蔽", color = NunuloUi.Danger) }
                    }
                }
            }
        }
    }
}

@Composable
private fun PublishScreen(draft: UploadDraft, busy: Boolean, uploadPhase: String, uploadProgress: Int, uploadMessage: String, availableTags: List<TagItem>, onDraftChange: (UploadDraft) -> Unit, onPick: () -> Unit, onCamera: () -> Unit, onLocation: () -> Unit, onClear: () -> Unit, onUpload: () -> Unit) {
    val validation = validateDraft(draft)
    fun toggle(tag: String) = onDraftChange(draft.copy(tags = toggleTag(draft.tags, tag)))
    LazyColumn(Modifier.fillMaxSize().background(Color.White), contentPadding = PaddingValues(bottom = 20.dp)) {
        item { PhotoPickerCard(uri = draft.photoUri, onPick = onPick, onCamera = onCamera) }
        item {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("登记", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = NunuloUi.Ink)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(draft.placeName, { onDraftChange(draft.copy(placeName = it)) }, label = { Text("地点") }, modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(4.dp))
                    Spacer(Modifier.width(8.dp))
                    Surface(shape = RoundedCornerShape(4.dp), border = BorderStroke(1.dp, NunuloUi.Hairline), color = Color.White, modifier = Modifier.height(56.dp).clickable(onClick = onLocation)) {
                        Box(Modifier.padding(horizontal = 14.dp), contentAlignment = Alignment.Center) { Text("定位", color = NunuloUi.Coral, fontWeight = FontWeight.Bold) }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = draft.latitude,
                        onValueChange = { onDraftChange(draft.copy(latitude = it, locationSource = "manual")) },
                        label = { Text("纬度") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(4.dp),
                    )
                    OutlinedTextField(
                        value = draft.longitude,
                        onValueChange = { onDraftChange(draft.copy(longitude = it, locationSource = "manual")) },
                        label = { Text("经度") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(4.dp),
                    )
                }
                Text(
                    if (draft.locationSource == "device_location") "坐标来自设备定位，可手工修正" else "可直接填写坐标，或点击定位自动获取",
                    color = NunuloUi.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
                PublishTagSelector(tags = availableTags, value = draft.tags, onValueChange = { onDraftChange(draft.copy(tags = it)) }, onToggle = ::toggle)
                OutlinedTextField(draft.note, { onDraftChange(draft.copy(note = it)) }, label = { Text("备注") }, modifier = Modifier.fillMaxWidth().heightIn(min = 84.dp), shape = RoundedCornerShape(4.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("谁可以看到", color = NunuloUi.Muted, style = MaterialTheme.typography.bodySmall)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf("private", "followers", "public")) { value ->
                            FilterChip(
                                selected = draft.visibility == value,
                                onClick = { onDraftChange(draft.copy(visibility = value)) },
                                label = { Text(visibilityLabel(value)) },
                            )
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("非本人查看时位置会降低精度", color = NunuloUi.Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    TextButton(onClick = onClear, enabled = !busy && draft.photoUri != null) { Text("清除草稿") }
                }
                PublishReadinessCard(validation)
                if (busy || uploadPhase == "failed") UploadPhaseBar(uploadPhase, busy, uploadProgress, uploadMessage)
                Surface(color = if (!busy && validation.ready) NunuloUi.Coral else NunuloUi.Placeholder, shape = RoundedCornerShape(4.dp), modifier = Modifier.fillMaxWidth().height(48.dp).clickable(enabled = !busy && validation.ready, onClick = onUpload)) {
                    Box(contentAlignment = Alignment.Center) { Text(if (busy) "上传中" else "登记", color = if (!busy && validation.ready) Color.White else NunuloUi.Muted, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

@Composable
private fun AvatarView(uri: Uri?, imageUrl: String?, apiBase: String, api: NunuloApi, label: String, onClick: () -> Unit) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(initialValue = null, uri) {
        value = if (uri == null) null else withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.use { input -> BitmapFactory.decodeStream(input) }
        }
    }
    val remoteBitmap by produceState<Bitmap?>(initialValue = null, imageUrl, apiBase) {
        value = imageUrl?.let { runCatching { api.downloadBitmap(apiBase, resolveAssetUrl(apiBase, it)) }.getOrNull() }
    }
    val displayBitmap = bitmap ?: remoteBitmap
    Box(
        Modifier.size(80.dp).clip(CircleShape).background(Brush.linearGradient(listOf(NunuloUi.CoralSoft, NunuloUi.BlueSoft))).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (displayBitmap == null) {
            Text(label, color = NunuloUi.Coral, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge)
        } else {
            Image(bitmap = displayBitmap.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun ProfileStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, color = NunuloUi.Ink, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
        Text(label, color = NunuloUi.Muted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun MeScreen(loginName: String, password: String, user: AuthUser?, records: List<CheckinItem>, albums: List<AlbumItem>, exports: List<ExportItem>, inviteCode: String, avatarUri: Uri?, apiBase: String, api: NunuloApi, busy: Boolean, onLoginNameChange: (String) -> Unit, onApiBaseChange: (String) -> Unit, onPasswordChange: (String) -> Unit, onLogin: () -> Unit, onRegister: (String, String, String, String) -> Unit, onLogout: () -> Unit, onPermissions: () -> Unit, onPickAvatar: () -> Unit, onRefresh: () -> Unit, onOpenRecord: (CheckinItem) -> Unit, onOpenPhotos: () -> Unit, onOpenPlaces: () -> Unit, onOpenTags: () -> Unit, onCreateInvite: () -> Unit, onCreateAlbum: (String) -> Unit, onCreateExport: () -> Unit, onDownloadExport: (ExportItem) -> Unit, message: String) {
    val places = records.map { it.placeName }.filter { it.isNotBlank() }.distinct().size
    val tags = records.flatMap { it.tags }.distinct().size
    if (user == null) {
        var registerMode by rememberSaveable { mutableStateOf(false) }
        var registerName by rememberSaveable { mutableStateOf("") }
        var displayName by rememberSaveable { mutableStateOf("") }
        var registerInvite by rememberSaveable { mutableStateOf("") }
        var registerPassword by rememberSaveable { mutableStateOf("") }
        Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.Center) {
            Text(if (registerMode) "邀请注册" else "登录", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            if (registerMode) {
                OutlinedTextField(registerInvite, { registerInvite = it }, label = { Text("邀请码") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(registerName, { registerName = it }, label = { Text("用户名") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(displayName, { displayName = it }, label = { Text("显示名") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            } else {
                OutlinedTextField(loginName, onLoginNameChange, label = { Text("邮箱或用户名") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            }
            Spacer(Modifier.height(10.dp))
            if (BuildConfig.DEBUG) {
                OutlinedTextField(apiBase, onApiBaseChange, label = { Text("调试 API 地址") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(Modifier.height(10.dp))
            }
            OutlinedTextField(if (registerMode) registerPassword else password, if (registerMode) ({ registerPassword = it }) else onPasswordChange, label = { Text("密码") }, modifier = Modifier.fillMaxWidth(), singleLine = true, visualTransformation = PasswordVisualTransformation())
            Spacer(Modifier.height(14.dp))
            Button(onClick = { if (registerMode) onRegister(registerName, displayName, registerInvite, registerPassword) else onLogin() }, enabled = !busy && if (registerMode) registerName.isNotBlank() && registerInvite.isNotBlank() && registerPassword.length >= 8 else loginName.isNotBlank() && password.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text(if (registerMode) "创建测试账号" else "登录") }
            TextButton(onClick = { registerMode = !registerMode }, modifier = Modifier.fillMaxWidth()) { Text(if (registerMode) "已有账号，返回登录" else "使用邀请码注册") }
        }
        return
    }
    var albumTitle by rememberSaveable { mutableStateOf("") }
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Column(Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                AvatarView(uri = avatarUri, imageUrl = user.avatarUrl, apiBase = apiBase, api = api, label = user.displayName.take(1), onClick = onPickAvatar)
                Spacer(Modifier.height(8.dp))
                Text(user.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(user.username ?: "记录和收藏我的娃娃足迹", color = NunuloUi.Muted, style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = onPickAvatar) { Text("编辑资料") }
                Text(
                    "已使用 ${formatBytes(user.storageUsageBytes)} / ${formatBytes(user.storageQuotaBytes)}",
                    color = NunuloUi.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
                Row(Modifier.fillMaxWidth().padding(horizontal = 36.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    ProfileStat("照片", records.size.toString(), Modifier.weight(1f).clickable(onClick = onOpenPhotos))
                    ProfileStat("地点", places.toString(), Modifier.weight(1f).clickable(onClick = onOpenPlaces))
                    ProfileStat("标签", tags.toString(), Modifier.weight(1f).clickable(onClick = onOpenTags))
                }
            }
        }
        item {
            Column(Modifier.fillMaxWidth()) {
                ProfileMenuRow("个人地图", places.toString(), onOpenPlaces)
                ProfileMenuRow("我的照片", records.size.toString(), onOpenPhotos)
                ProfileMenuRow("标签浏览", tags.toString(), onOpenTags)
                ProfileMenuRow("邀请测试成员", if (inviteCode.isBlank()) "生成" else inviteCode, onCreateInvite)
                ProfileMenuRow("重新同步数据", "同步", onRefresh)
                ProfileMenuRow("检查定位权限", "检查", onPermissions)
            }
        }
        item {
            Text(message, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), color = NunuloUi.Muted, style = MaterialTheme.typography.bodySmall)
        }
        item {
            Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("我的合集", fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = albumTitle,
                        onValueChange = { albumTitle = it.take(160) },
                        label = { Text("新合集名称") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    Button(
                        onClick = {
                            val title = albumTitle.trim()
                            if (title.isNotEmpty()) {
                                onCreateAlbum(title)
                                albumTitle = ""
                            }
                        },
                        enabled = !busy && albumTitle.isNotBlank(),
                    ) { Text("创建") }
                }
                if (albums.isEmpty()) {
                    Text("还没有合集，可创建后在照片详情中加入。", color = NunuloUi.Muted, style = MaterialTheme.typography.bodySmall)
                } else {
                    albums.forEach { album ->
                        JournalCard {
                            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(album.title, fontWeight = FontWeight.Bold)
                                    Text(visibilityLabel(album.visibility), color = NunuloUi.Muted, style = MaterialTheme.typography.bodySmall)
                                }
                                Text("${album.itemCount} 张", color = NunuloUi.Coral, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
        item {
            Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("个人数据导出", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                    TextButton(onClick = onCreateExport, enabled = !busy) { Text("生成新导出") }
                }
                Text("导出包含记录清单与原图，用于个人备份和迁移。", color = NunuloUi.Muted, style = MaterialTheme.typography.bodySmall)
                if (exports.isEmpty()) {
                    Text("还没有可下载的导出。", color = NunuloUi.Muted, style = MaterialTheme.typography.bodySmall)
                } else {
                    exports.forEach { record ->
                        JournalCard(modifier = Modifier.fillMaxWidth().clickable(enabled = !busy) { onDownloadExport(record) }) {
                            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(shortDate(record.createdAt), fontWeight = FontWeight.Bold)
                                    Text(record.status, color = NunuloUi.Muted, style = MaterialTheme.typography.bodySmall)
                                }
                                Text("下载", color = NunuloUi.Coral, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
        item {
            Text("最近记录", modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp), fontWeight = FontWeight.Bold)
            records.chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    row.forEach { record -> Box(Modifier.weight(1f).clickable { onOpenRecord(record) }) { RemoteImage(record.thumbUrl ?: record.displayUrl, apiBase, api, aspect = 1f) } }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
                Spacer(Modifier.height(2.dp))
            }
        }
        item { TextButton(onClick = onLogout, modifier = Modifier.fillMaxWidth().padding(12.dp)) { Text("退出登录", color = NunuloUi.Muted) } }
    }
}

@Composable
private fun ProfileMenuRow(title: String, value: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(52.dp).clickable(onClick = onClick).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, modifier = Modifier.weight(1f))
        Text(value, color = NunuloUi.Muted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String, onAction: () -> Unit, actionLabel: String = "登记") {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = NunuloUi.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text(subtitle, color = NunuloUi.Muted, style = MaterialTheme.typography.bodySmall)
        }
        TextButton(onClick = onAction) { Text(actionLabel) }
    }
}

@Composable
private fun EmptyState(title: String, subtitle: String) {
    JournalCard(containerColor = NunuloUi.Surface) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, color = NunuloUi.Ink, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, color = NunuloUi.Muted)
        }
    }
}

@Composable
private fun FirstRecordCard(onPublish: () -> Unit) {
    JournalCard(containerColor = NunuloUi.CoralSoft) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onPublish).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("记录第一张照片", color = NunuloUi.Ink, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
                Text("先选照片，再补地点和标签。", color = NunuloUi.Slate, style = MaterialTheme.typography.bodySmall)
            }
            Button(onClick = onPublish) { Text("开始") }
        }
    }
}

@Composable
private fun RouteIndexCard(records: List<CheckinItem>, visibleCount: Int, filters: BrowseFilters, placesCount: Int, tagsCount: Int) {
    val latest = records.maxByOrNull { it.takenAt ?: it.createdAt.orEmpty() }
    val active = listOfNotNull(
        filters.query.takeIf { it.isNotBlank() }?.let { "关键词 $it" },
        filters.place.takeIf { it.isNotBlank() }?.let { "地点 $it" },
        filters.tag.takeIf { it.isNotBlank() }?.let { "标签 $it" },
    )
    JournalCard(containerColor = NunuloUi.Surface) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text("相册索引", color = NunuloUi.Ink, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                    Text(latest?.let { "最近 ${shortDate(it.takenAt ?: it.createdAt)} · ${it.placeName}" } ?: "还没有照片记录", color = NunuloUi.Muted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text("${visibleCount}/${records.size}", color = NunuloUi.Coral, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                IndexCell("地点", placesCount.toString(), NunuloUi.GreenSoft, NunuloUi.Green, Modifier.weight(1f))
                IndexCell("标签", tagsCount.toString(), NunuloUi.BlueSoft, NunuloUi.Blue, Modifier.weight(1f))
                IndexCell("待整理", records.count { it.placeName == "未命名地点" || it.tags.isEmpty() }.toString(), NunuloUi.AmberSoft, NunuloUi.Amber, Modifier.weight(1f))
            }
            if (active.isNotEmpty()) {
                Text("当前：${active.joinToString(" · ")}", color = NunuloUi.Slate, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun IndexCell(label: String, value: String, background: Color, foreground: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .padding(horizontal = 10.dp, vertical = 10.dp),
    ) {
        Text(value, color = foreground, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, maxLines = 1)
        Text(label, color = NunuloUi.Slate, style = MaterialTheme.typography.bodySmall, maxLines = 1)
    }
}

@Composable
private fun BrowseFilterPanel(
    filters: BrowseFilters,
    tags: List<String>,
    places: List<String>,
    onFiltersChange: (BrowseFilters) -> Unit,
) {
    JournalCard(containerColor = NunuloUi.Surface) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = filters.query,
                onValueChange = { onFiltersChange(filters.copy(query = it)) },
                label = { Text("搜索地点、备注、标签") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(tags.take(12)) { tag ->
                    AssistChip(onClick = { onFiltersChange(filters.copy(tag = tag)) }, label = { Text(tag, maxLines = 1, overflow = TextOverflow.Ellipsis) })
                }
                items(places.take(8)) { place ->
                    AssistChip(onClick = { onFiltersChange(filters.copy(place = place)) }, label = { Text(place, maxLines = 1, overflow = TextOverflow.Ellipsis) })
                }
                if (tags.isNotEmpty() || places.isNotEmpty()) {
                    item {
                        TextButton(onClick = { onFiltersChange(BrowseFilters()) }) { Text("清除") }
                    }
                }
            }
            if (tags.isEmpty() && places.isEmpty()) {
                TextButton(onClick = { onFiltersChange(BrowseFilters()) }) { Text("清除筛选") }
            }
        }
    }
}

@Composable
private fun PublishReadinessCard(validation: DraftValidation) {
    JournalCard(containerColor = if (validation.ready) NunuloUi.GreenSoft else NunuloUi.Surface) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text("登记检查", color = NunuloUi.Ink, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
                    Text(if (validation.ready) "信息完整，可以登记" else validation.missingText, color = NunuloUi.Muted, style = MaterialTheme.typography.bodySmall)
                }
                Text("仅自己可见", color = NunuloUi.Blue, fontWeight = FontWeight.Bold)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                ReadinessStep("照片", validation.hasPhoto, Modifier.weight(1f))
                ReadinessStep("坐标", validation.hasValidLatitude && validation.hasValidLongitude, Modifier.weight(1f))
                ReadinessStep("标签", validation.hasTags, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PublishTagSelector(tags: List<TagItem>, value: String, onValueChange: (String) -> Unit, onToggle: (String) -> Unit) {
    val selectedTags = parseDraftTags(value)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(value = value, onValueChange = onValueChange, label = { Text("标签，使用逗号分隔") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        if (tags.isNotEmpty()) {
            Text("历史标签", color = NunuloUi.Muted, style = MaterialTheme.typography.bodySmall)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(tags) { tag -> StitchChip(tag.name, tag.name in selectedTags) { onToggle(tag.name) } }
            }
        }
    }
}

@Composable
private fun ReadinessStep(label: String, done: Boolean, modifier: Modifier = Modifier) {
    val background = if (done) NunuloUi.GreenSoft else NunuloUi.AmberSoft
    val foreground = if (done) NunuloUi.Green else NunuloUi.Amber
    Row(
        modifier = modifier.clip(RoundedCornerShape(999.dp)).background(background).padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(foreground))
        Spacer(Modifier.width(6.dp))
        Text(label, color = foreground, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun UploadPhaseBar(uploadPhase: String, busy: Boolean, uploadProgress: Int, uploadMessage: String) {
    val text = when (uploadPhase) {
        "preparing" -> "正在准备图片"
        "uploading" -> "正在登记照片"
        "done" -> "最近一次上传已完成"
        "failed" -> "最近一次上传失败，可调整后重试"
        else -> "选择图片后继续填写地点和标签"
    }
    val progress = when (uploadPhase) {
        "preparing" -> 0.28f
        "uploading" -> 0.72f
        "done" -> 1f
        "failed" -> 0.42f
        else -> 0f
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        LinearProgressIndicator(
            progress = { if (uploadPhase == "uploading" && uploadProgress > 0) uploadProgress / 100f else progress },
            modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(99.dp)),
        )
        Text(if (uploadPhase == "uploading" && uploadProgress > 0) "$text $uploadProgress%" else text, color = if (uploadPhase == "failed") NunuloUi.Danger else NunuloUi.Muted, style = MaterialTheme.typography.bodySmall)
        if (uploadPhase == "failed" && uploadMessage.isNotBlank()) {
            Text(uploadMessage, color = NunuloUi.Danger, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun UploadQueueCard(uploadPhase: String, validation: DraftValidation) {
    val queueText = when (uploadPhase) {
        "preparing" -> "正在处理图片"
        "uploading" -> "登记中"
        "done" -> "已登记"
        "failed" -> "登记失败"
        else -> if (validation.hasPhoto) "照片已选择" else "等待选择照片"
    }
    val detail = when (uploadPhase) {
        "failed" -> validation.missingText.takeIf { !validation.ready } ?: "检查网络后再试。"
        "done" -> "照片已进入首页。"
        "uploading" -> "保持当前页面，完成后会回到首页。"
        else -> "登记前确认照片、地点和标签。"
    }
    Column(
        modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(NunuloUi.PaperTint).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(queueText, fontWeight = FontWeight.Black, color = NunuloUi.Slate)
        Text(detail, color = NunuloUi.Muted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun AmapNativeMap(records: List<CheckinItem>, currentLocation: MapPoint?, modifier: Modifier = Modifier, onOpen: (CheckinItem) -> Unit) {
    val mapRecords = remember(records) { records.filter { it.latitude in -90.0..90.0 && it.longitude in -180.0..180.0 && !(it.latitude == 0.0 && it.longitude == 0.0) } }
    val validCurrentLocation = currentLocation?.takeIf { it.latitude in -90.0..90.0 && it.longitude in -180.0..180.0 }
    val hasAmapConfig = BuildConfig.AMAP_ANDROID_KEY.isNotBlank()
    val primaryAbi = Build.SUPPORTED_ABIS.firstOrNull().orEmpty()
    val supportsAmapNative = primaryAbi == "arm64-v8a" || primaryAbi == "armeabi-v7a"
    if (!hasAmapConfig || !supportsAmapNative) {
        val reason = if (!hasAmapConfig) "高德 Android Key 未配置" else "当前模拟器 ABI 不支持高德原生地图"
        StaticCoordinateFallback(records = mapRecords, currentLocation = validCurrentLocation, modifier = modifier, reason = reason)
        return
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val mapView = remember(context) {
        MapView(context).apply { onCreate(Bundle()) }
    }
    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onPause()
            mapView.onDestroy()
        }
    }
    LaunchedEffect(mapView, mapRecords, validCurrentLocation) {
        configureAmap(mapView.map, mapRecords, validCurrentLocation, onOpen)
    }
    Box(modifier.clip(RoundedCornerShape(12.dp)).background(NunuloUi.Placeholder)) {
        AndroidView(
            factory = { mapView },
            update = { view -> configureAmap(view.map, mapRecords, validCurrentLocation, onOpen) },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun StaticCoordinateFallback(records: List<CheckinItem>, currentLocation: MapPoint?, modifier: Modifier, reason: String) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.linearGradient(listOf(NunuloUi.BlueSoft, NunuloUi.GreenSoft, NunuloUi.CoralSoft)))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(reason, fontWeight = FontWeight.Black, color = NunuloUi.Ink)
        Text("这里仅显示真实坐标，不绘制伪造地图或随机点。请在 ARM Android 设备上验收高德原生地图。", color = NunuloUi.Slate)
        Surface(color = Color.White.copy(alpha = 0.88f), shape = RoundedCornerShape(4.dp)) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("当前位置", fontWeight = FontWeight.Bold)
                Text(currentLocation?.let { formatCoordinate(it.latitude, it.longitude) } ?: "尚未取得定位", color = NunuloUi.Muted)
            }
        }
        Text("本人记录坐标（${records.size}）", fontWeight = FontWeight.Bold)
        records.take(8).forEach { record ->
            Text("${record.placeName.ifBlank { "未命名地点" }} · ${formatCoordinate(record.latitude, record.longitude)}", color = NunuloUi.Ink)
        }
        if (records.size > 8) Text("另有 ${records.size - 8} 个记录坐标", color = NunuloUi.Muted)
    }
}

private fun configureAmap(amap: AMap, records: List<CheckinItem>, currentLocation: MapPoint?, onOpen: (CheckinItem) -> Unit) {
    amap.uiSettings.isZoomControlsEnabled = false
    amap.uiSettings.isScaleControlsEnabled = true
    amap.uiSettings.isCompassEnabled = false
    amap.mapType = AMap.MAP_TYPE_NORMAL
    amap.clear()
    val points = records.take(160).map { record ->
        val point = toAmapPoint(record.latitude, record.longitude)
        record to LatLng(point.latitude, point.longitude)
    }
    val markerRecords = mutableMapOf<String, CheckinItem>()
    points.forEach { (record, point) ->
        val marker = amap.addMarker(
            MarkerOptions()
                .position(point)
                .title(record.placeName.ifBlank { "未命名地点" })
                .snippet(shortDate(record.takenAt ?: record.createdAt)),
        )
        markerRecords[marker.id] = record
    }
    val currentPoint = currentLocation?.let { toAmapPoint(it.latitude, it.longitude) }?.let { LatLng(it.latitude, it.longitude) }
    if (currentPoint != null) {
        amap.addMarker(
            MarkerOptions()
                .position(currentPoint)
                .title("当前位置")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)),
        )
    }
    amap.setOnMarkerClickListener { marker -> markerRecords[marker.id]?.let(onOpen); true }
    val visiblePoints = points.map { it.second } + listOfNotNull(currentPoint)
    if (visiblePoints.size == 1) {
        amap.moveCamera(CameraUpdateFactory.newLatLngZoom(visiblePoints.first(), 14f))
    } else if (visiblePoints.size > 1) {
        val bounds = LatLngBounds.builder().apply { visiblePoints.forEach { include(it) } }.build()
        amap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 54))
    }
    Log.d(AMAP_LOG_TAG, "native-map-ready mode=personal records=${points.size} current=${currentPoint != null}")
}

@Composable
private fun CheckinDetailDialog(
    record: CheckinItem,
    apiBase: String,
    api: NunuloApi,
    busy: Boolean,
    pendingDelete: Boolean,
    comments: List<CommentItem>,
    albums: List<AlbumItem>,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onLike: () -> Unit,
    onComment: (String) -> Unit,
    onReport: () -> Unit,
    onAddToAlbum: (AlbumItem) -> Unit,
) {
    var commentBody by rememberSaveable(record.id) { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(record.placeName.ifBlank { "未命名地点" }, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { RemoteImage(record.displayUrl ?: record.thumbUrl ?: record.originalUrl, apiBase, api, aspect = 0.82f) }
                item {
                    Text(
                        "${record.authorName} · ${shortDate(record.takenAt ?: record.createdAt)} · ${visibilityLabel(record.visibility)}",
                        color = NunuloUi.Muted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (record.note.isNotBlank()) item { Text(record.note, color = NunuloUi.Ink) }
                if (record.tags.isNotEmpty()) item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(record.tags) { tag -> AssistChip(onClick = {}, label = { Text(tag) }) }
                    }
                }
                item { Text(formatCoordinate(record.latitude, record.longitude), color = NunuloUi.Muted, style = MaterialTheme.typography.bodySmall) }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = onLike, enabled = !busy) {
                            Text("${if (record.liked) "♥" else "♡"} ${record.likeCount}")
                        }
                        Text("${record.commentCount} 条评论", color = NunuloUi.Muted, style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (record.canEdit && albums.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("加入我的合集", color = NunuloUi.Muted, style = MaterialTheme.typography.bodySmall)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(albums, key = { it.id }) { album ->
                                    AssistChip(onClick = { onAddToAlbum(album) }, label = { Text(album.title) })
                                }
                            }
                        }
                    }
                }
                item { Text("评论", fontWeight = FontWeight.Bold) }
                if (comments.isEmpty()) {
                    item { Text("还没有评论，说点和这张照片有关的事。", color = NunuloUi.Muted, style = MaterialTheme.typography.bodySmall) }
                } else {
                    items(comments, key = { it.id }) { comment ->
                        JournalCard(containerColor = NunuloUi.Surface) {
                            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(comment.displayName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                Text(comment.body)
                                Text(shortDate(comment.createdAt), color = NunuloUi.Muted, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = commentBody,
                            onValueChange = { if (it.length <= 1000) commentBody = it },
                            label = { Text("写一条评论") },
                            modifier = Modifier.weight(1f),
                            maxLines = 3,
                        )
                        Button(
                            onClick = {
                                val text = commentBody.trim()
                                if (text.isNotEmpty()) {
                                    onComment(text)
                                    commentBody = ""
                                }
                            },
                            enabled = !busy && commentBody.isNotBlank(),
                        ) { Text("发送") }
                    }
                }
            }
        },
        confirmButton = {
            if (record.canEdit) {
                TextButton(onClick = onEdit, enabled = !busy) { Text("编辑") }
            } else {
                TextButton(onClick = onReport, enabled = !busy) { Text("举报", color = NunuloUi.Danger) }
            }
        },
        dismissButton = {
            Row {
                if (record.canEdit) {
                    TextButton(onClick = onDelete, enabled = !busy) { Text(if (pendingDelete) "确认删除" else "删除", color = NunuloUi.Danger) }
                }
                TextButton(onClick = onDismiss) { Text("关闭") }
            }
        },
    )
}

@Composable
private fun CheckinEditDialog(record: CheckinItem, availableTags: List<TagItem>, busy: Boolean, onDismiss: () -> Unit, onSave: (CheckinEditDraft) -> Unit) {
    var placeName by rememberSaveable(record.id) { mutableStateOf(record.placeName) }
    var latitude by rememberSaveable(record.id) { mutableStateOf(record.latitude.toString()) }
    var longitude by rememberSaveable(record.id) { mutableStateOf(record.longitude.toString()) }
    var note by rememberSaveable(record.id) { mutableStateOf(record.note) }
    var tagText by rememberSaveable(record.id) { mutableStateOf(record.tags.joinToString(",")) }
    var visibility by rememberSaveable(record.id) { mutableStateOf(record.visibility) }
    val selectedTags = parseDraftTags(tagText)
    val latitudeValue = latitude.toDoubleOrNull()
    val longitudeValue = longitude.toDoubleOrNull()
    val valid = placeName.isNotBlank() && latitudeValue != null && latitudeValue in -90.0..90.0 && longitudeValue != null && longitudeValue in -180.0..180.0
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑记录") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { OutlinedTextField(placeName, { placeName = it }, label = { Text("地点") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(latitude, { latitude = it }, label = { Text("纬度") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                        OutlinedTextField(longitude, { longitude = it }, label = { Text("经度") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                    }
                }
                item { OutlinedTextField(note, { note = it }, label = { Text("备注") }, minLines = 2, modifier = Modifier.fillMaxWidth()) }
                item {
                    OutlinedTextField(
                        value = tagText,
                        onValueChange = { tagText = it },
                        label = { Text("标签，使用逗号分隔") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    if (availableTags.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(availableTags) { tag ->
                                StitchChip(tag.name, tag.name in selectedTags) {
                                    tagText = toggleTag(tagText, tag.name)
                                }
                            }
                        }
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("谁可以看到", color = NunuloUi.Muted, style = MaterialTheme.typography.bodySmall)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(listOf("private", "followers", "public")) { value ->
                                FilterChip(
                                    selected = visibility == value,
                                    onClick = { visibility = value },
                                    label = { Text(visibilityLabel(value)) },
                                )
                            }
                        }
                        Text("非本人查看时位置会降低精度", color = NunuloUi.Muted, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid && !busy,
                onClick = {
                    onSave(
                        CheckinEditDraft(
                            placeName = placeName,
                            latitude = latitude,
                            longitude = longitude,
                            note = note,
                            tags = tagText,
                            visibility = visibility,
                        )
                    )
                },
            ) { Text(if (busy) "保存中" else "保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun RemoteImage(url: String?, apiBase: String, api: NunuloApi, aspect: Float = 1.25f) {
    if (url.isNullOrBlank()) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(aspect).background(NunuloUi.Placeholder),
            contentAlignment = Alignment.Center,
        ) { Text("等待图片", color = NunuloUi.Muted) }
        return
    }
    val resolved = remember(url, apiBase) { resolveAssetUrl(apiBase, url) }
    val bitmap by produceState<Bitmap?>(initialValue = null, resolved) {
        value = runCatching { api.downloadBitmap(apiBase, resolved) }.getOrNull()
    }
    if (bitmap == null) {
        Box(Modifier.fillMaxWidth().aspectRatio(aspect).background(NunuloUi.Placeholder), contentAlignment = Alignment.Center) {
            Text("加载图片", color = NunuloUi.Muted)
        }
    } else {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().aspectRatio(aspect),
        )
    }
}

@Composable
private fun PhotoPickerCard(uri: Uri?, onPick: () -> Unit, onCamera: () -> Unit) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(initialValue = null, uri) {
        value = if (uri == null) null else withContext(Dispatchers.IO) { decodeSampledBitmap(context.contentResolver, uri) }
    }
    Box(Modifier.fillMaxWidth().height(360.dp).background(NunuloUi.Placeholder).clickable(onClick = onPick), contentAlignment = Alignment.Center) {
        if (bitmap == null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(if (uri == null) "选择一张照片" else "照片已选择，预览暂不可用", color = NunuloUi.Ink, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(color = NunuloUi.Coral, shape = RoundedCornerShape(4.dp), modifier = Modifier.clickable(onClick = onPick)) { Text("相册", color = Color.White, modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp), fontWeight = FontWeight.Bold) }
                    Surface(color = Color.White, shape = RoundedCornerShape(4.dp), border = BorderStroke(1.dp, NunuloUi.Hairline), modifier = Modifier.clickable(onClick = onCamera)) { Text("拍摄", color = NunuloUi.Ink, modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp), fontWeight = FontWeight.Bold) }
                }
            }
        } else {
            Image(bitmap = bitmap!!.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            Surface(color = Color.Black.copy(alpha = 0.58f), shape = RoundedCornerShape(3.dp), modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp).clickable(onClick = onCamera)) { Text("重新拍摄", color = Color.White, modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) }
        }
    }
}

@Composable
private fun PlaceRow(record: CheckinItem) {
    JournalCard {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(CircleShape).background(NunuloUi.BlueSoft), contentAlignment = Alignment.Center) {
                Text("点", color = NunuloUi.Blue, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(record.placeName, color = NunuloUi.Ink, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(formatCoordinate(record.latitude, record.longitude), color = NunuloUi.Muted, style = MaterialTheme.typography.bodySmall)
            }
            Text(shortDate(record.takenAt ?: record.createdAt), color = NunuloUi.Muted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun LibraryEntryCard(
    title: String,
    subtitle: String,
    value: String,
    background: Color,
    foreground: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    JournalCard(modifier = modifier, containerColor = NunuloUi.Surface) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .heightIn(min = 72.dp)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .widthIn(min = 44.dp)
                    .height(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(background),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    value,
                    color = foreground,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, color = NunuloUi.Ink, fontWeight = FontWeight.Black, maxLines = 1)
                Text(subtitle, color = NunuloUi.Muted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun LibraryGroup(title: String, chips: List<String>, selected: String, onChip: (String) -> Unit) {
    JournalCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, color = NunuloUi.Ink, fontWeight = FontWeight.Black)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(chips.take(12)) { chip ->
                    FilterChip(
                        selected = selected == chip,
                        onClick = { onChip(chip) },
                        label = { Text(chip, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DollTheme(content: @Composable () -> Unit) {
    val colors = lightColorScheme(
        primary = NunuloUi.Coral,
        secondary = NunuloUi.Green,
        tertiary = NunuloUi.Blue,
        background = NunuloUi.Background,
        surface = NunuloUi.Surface,
        onPrimary = NunuloUi.Paper,
        onSecondary = NunuloUi.Paper,
        onSurface = NunuloUi.Ink,
    )
    val compactTypography = Typography(
        headlineMedium = TextStyle(fontSize = 20.sp, lineHeight = 25.sp, fontWeight = FontWeight.Bold),
        titleLarge = TextStyle(fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold),
        titleMedium = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold),
        titleSmall = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Bold),
        bodyMedium = TextStyle(fontSize = 13.sp, lineHeight = 19.sp),
        bodySmall = TextStyle(fontSize = 11.sp, lineHeight = 16.sp),
        labelLarge = TextStyle(fontSize = 12.sp, lineHeight = 17.sp, fontWeight = FontWeight.Bold),
    )
    MaterialTheme(colorScheme = colors, typography = compactTypography, content = content)
}

private fun resolveAssetUrl(apiBase: String, url: String): String = when {
    url.startsWith("http://") || url.startsWith("https://") -> url
    url.startsWith("/") -> apiBase.trim().trimEnd('/') + url
    else -> apiBase.trim().trimEnd('/') + "/" + url
}

private fun parseAuthUser(json: JSONObject): AuthUser {
    return AuthUser(
        id = json.getInt("id"),
        displayName = json.getString("display_name"),
        username = json.nullableString("username"),
        email = json.nullableString("email"),
        roles = parseStringArray(json.getJSONArray("roles")),
        storageUsageBytes = json.getLong("storage_usage_bytes"),
        storageQuotaBytes = json.getLong("storage_quota_bytes"),
        avatarUrl = json.nullableString("avatar_url"),
    )
}

private fun parseTagItem(json: JSONObject): TagItem {
    return TagItem(
        name = json.getString("name"),
        count = json.getInt("count"),
    )
}

private fun parseComment(json: JSONObject) = CommentItem(
    id = json.getString("id"),
    displayName = json.optString("display_name", "测试成员"),
    body = json.getString("body"),
    createdAt = json.nullableString("created_at"),
)

private fun parseNotification(json: JSONObject) = NotificationItem(
    id = json.getString("id"),
    title = json.getString("title"),
    body = json.getString("body"),
    readAt = json.nullableString("read_at"),
    createdAt = json.nullableString("created_at"),
)

private fun parsePerson(json: JSONObject) = PersonItem(
    id = json.getInt("id"),
    displayName = json.getString("display_name"),
    username = json.nullableString("username"),
    bio = json.nullableString("bio"),
    following = json.optBoolean("following"),
)

private fun parseAlbum(json: JSONObject) = AlbumItem(
    id = json.getString("id"),
    title = json.getString("title"),
    itemCount = json.optInt("item_count"),
    visibility = json.optString("visibility", "private"),
)

private fun parseExport(json: JSONObject) = ExportItem(
    id = json.getString("id"),
    status = json.getString("status"),
    createdAt = json.nullableString("created_at"),
    downloadUrl = json.nullableString("download_url"),
)

internal fun parseCheckin(json: JSONObject): CheckinItem {
    val assets = json.optJSONArray("assets")
    var originalUrl: String? = null
    if (assets != null) {
        for (index in 0 until assets.length()) {
            val asset = assets.optJSONObject(index) ?: continue
            if (asset.optString("variant") == "original") {
                originalUrl = asset.nullableString("url")
                break
            }
        }
    }
    val author = json.optJSONObject("author")
    val interaction = json.optJSONObject("interaction")
    return CheckinItem(
        id = json.getString("id"),
        userId = json.optInt("user_id"),
        authorName = author?.optString("display_name")?.takeIf { it.isNotBlank() } ?: "我",
        placeName = json.getString("place_name"),
        note = json.getString("note"),
        latitude = json.getDouble("latitude"),
        longitude = json.getDouble("longitude"),
        tags = parseStringArray(json.getJSONArray("tags")),
        createdAt = json.nullableString("created_at"),
        takenAt = json.nullableString("taken_at"),
        source = json.getString("source"),
        visibility = json.optString("visibility", "private"),
        canEdit = json.optBoolean("can_edit", true),
        liked = interaction?.optBoolean("liked") ?: false,
        likeCount = interaction?.optInt("like_count") ?: 0,
        commentCount = interaction?.optInt("comment_count") ?: 0,
        thumbUrl = json.nullableString("thumb_url"),
        displayUrl = json.nullableString("display_url"),
        originalUrl = originalUrl,
    )
}

private fun JSONObject.nullableString(key: String): String? {
    val value = opt(key)
    if (value == null || value == JSONObject.NULL) return null
    return value.toString().takeIf { it.isNotBlank() && it != "null" }
}

private fun parseStringArray(array: JSONArray?): List<String> {
    if (array == null) return emptyList()
    return buildList {
        for (index in 0 until array.length()) add(array.optString(index))
    }.filter { it.isNotBlank() }
}

internal fun checkinFeedPath(scope: FeedScope): String = "/api/checkins?scope=${scope.apiValue}&limit=120&offset=0"

internal fun shouldShowOwnUpload(scope: FeedScope, visibility: String): Boolean = when (scope) {
    FeedScope.All, FeedScope.Mine -> true
    FeedScope.Public -> visibility == "public"
    FeedScope.Following -> false
}

internal fun visibilityLabel(value: String): String = when (value) {
    "private" -> "仅自己"
    "followers" -> "关注者"
    "public" -> "所有测试成员"
    else -> "未知范围"
}

private fun validateDraft(draft: UploadDraft): DraftValidation {
    val latitude = draft.latitude.trim().toDoubleOrNull()
    val longitude = draft.longitude.trim().toDoubleOrNull()
    val hasPhoto = draft.photoUri != null
    val hasValidLatitude = latitude != null && latitude in -90.0..90.0
    val hasValidLongitude = longitude != null && longitude in -180.0..180.0
    val hasPlaceName = draft.placeName.trim().isNotBlank()
    val hasTags = parseDraftTags(draft.tags).isNotEmpty()
    val missing = buildList {
        if (!hasPhoto) add("照片")
        if (!hasValidLatitude || !hasValidLongitude) add("有效坐标")
        if (!hasPlaceName) add("地点名")
        if (!hasTags) add("标签")
    }
    return DraftValidation(
        hasPhoto = hasPhoto,
        hasValidLatitude = hasValidLatitude,
        hasValidLongitude = hasValidLongitude,
        hasPlaceName = hasPlaceName,
        hasTags = hasTags,
        ready = missing.isEmpty(),
        missingText = if (missing.isEmpty()) "信息完整" else "请补充${missing.joinToString("、")}",
    )
}

private fun parseDraftTags(value: String): List<String> = value.split(',', '，').map { it.trim() }.filter { it.isNotBlank() }.distinct()

private fun toggleTag(current: String, tag: String): String {
    val tags = parseDraftTags(current).toMutableList()
    if (tags.remove(tag)) return tags.joinToString(",")
    return (tags + tag).joinToString(",")
}

private fun rankedTags(records: List<CheckinItem>): List<Pair<String, Int>> {
    return records.flatMap { it.tags }
        .filter { it.isNotBlank() }
        .groupingBy { it }
        .eachCount()
        .entries
        .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
        .map { it.key to it.value }
}

private fun filterRecords(records: List<CheckinItem>, filters: BrowseFilters): List<CheckinItem> {
    val query = filters.query.trim().lowercase()
    val tag = filters.tag.trim().lowercase()
    val place = filters.place.trim().lowercase()
    return records.filter { record ->
        val searchable = buildString {
            append(record.placeName).append(' ')
            append(record.note).append(' ')
            append(record.tags.joinToString(" "))
        }.lowercase()
        (query.isBlank() || query in searchable) &&
            (tag.isBlank() || record.tags.any { it.lowercase() == tag }) &&
            (place.isBlank() || record.placeName.lowercase() == place)
    }
}

internal data class MapPoint(val latitude: Double, val longitude: Double)

internal fun toAmapPoint(latitude: Double, longitude: Double): MapPoint {
    if (isOutsideChina(latitude, longitude)) return MapPoint(latitude, longitude)
    val dLat = transformLatitude(longitude - 105.0, latitude - 35.0)
    val dLng = transformLongitude(longitude - 105.0, latitude - 35.0)
    val radLat = latitude / 180.0 * PI
    var magic = sin(radLat)
    magic = 1.0 - 0.00669342162296594323 * magic * magic
    val sqrtMagic = sqrt(magic)
    val gcjLat = latitude + (dLat * 180.0) / ((6335552.717000426 / (magic * sqrtMagic)) * PI)
    val gcjLng = longitude + (dLng * 180.0) / ((6378245.0 / sqrtMagic) * cos(radLat) * PI)
    return MapPoint(gcjLat, gcjLng)
}

private fun isOutsideChina(latitude: Double, longitude: Double): Boolean {
    return longitude < 72.004 || longitude > 137.8347 || latitude < 0.8293 || latitude > 55.8271
}

private fun transformLatitude(x: Double, y: Double): Double {
    var ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * sqrt(abs(x))
    ret += (20.0 * sin(6.0 * x * PI) + 20.0 * sin(2.0 * x * PI)) * 2.0 / 3.0
    ret += (20.0 * sin(y * PI) + 40.0 * sin(y / 3.0 * PI)) * 2.0 / 3.0
    ret += (160.0 * sin(y / 12.0 * PI) + 320.0 * sin(y * PI / 30.0)) * 2.0 / 3.0
    return ret
}

private fun transformLongitude(x: Double, y: Double): Double {
    var ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * sqrt(abs(x))
    ret += (20.0 * sin(6.0 * x * PI) + 20.0 * sin(2.0 * x * PI)) * 2.0 / 3.0
    ret += (20.0 * sin(x * PI) + 40.0 * sin(x / 3.0 * PI)) * 2.0 / 3.0
    ret += (150.0 * sin(x / 12.0 * PI) + 300.0 * sin(x / 30.0 * PI)) * 2.0 / 3.0
    return ret
}

private fun formatCoordinate(latitude: Double, longitude: Double): String = "%.5f, %.5f".format(latitude, longitude)

private fun createCaptureUri(context: Context): Uri {
    val dir = File(context.cacheDir, "capture").apply { mkdirs() }
    val file = File(dir, "nunulo-${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

private fun formatBytes(value: Long): String {
    val units = listOf("B", "KB", "MB", "GB", "TB")
    var size = value.toDouble()
    var index = 0
    while (size >= 1024 && index < units.lastIndex) {
        size /= 1024
        index += 1
    }
    return if (index == 0) "${value}B" else "%.1f%s".format(size, units[index])
}

private fun shortDate(value: String?): String {
    if (value.isNullOrBlank()) return "刚刚"
    return runCatching {
        OffsetDateTime.parse(value).format(DateTimeFormatter.ofPattern("MM-dd HH:mm"))
    }.getOrDefault(value.take(10))
}
