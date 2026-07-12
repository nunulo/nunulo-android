package com.lumokato.dollcheckin

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.MapView
import com.amap.api.maps.MapsInitializer
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
import java.util.concurrent.TimeUnit
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
        setContent { DollCheckinApp() }
    }
}

private enum class AppTab(val title: String) {
    Feed("首页"),
    Map("地图"),
    Publish("登记"),
    Library("消息"),
    Me("我的"),
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

private data class CheckinItem(
    val id: String,
    val placeName: String,
    val note: String,
    val latitude: Double,
    val longitude: Double,
    val tags: List<String>,
    val createdAt: String?,
    val takenAt: String?,
    val source: String,
    val visibility: String,
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
    val visibility: String,
)

private data class MessageItem(
    val id: String,
    val kind: String,
    val title: String,
    val body: String,
    val priority: String,
    val createdAt: String?,
)

private data class TagItem(
    val name: String,
    val count: Int = 0,
    val source: String = "default",
    val groupCode: String = "custom",
    val slug: String? = null,
)

private data class TagGroupItem(
    val id: String,
    val title: String,
    val description: String = "",
    val sortOrder: Int = 0,
    val tags: List<TagItem> = emptyList(),
)

private data class TagCatalog(
    val groups: List<TagGroupItem> = emptyList(),
    val total: Int = 0,
)

private data class AlbumItem(
    val id: String,
    val type: String,
    val title: String,
    val description: String,
    val itemCount: Int,
)

private data class MapCellItem(
    val id: String,
    val scope: String,
    val photoCount: Int,
    val checkinCount: Int,
)

private data class LibrarySummary(
    val albums: List<AlbumItem> = emptyList(),
    val personalCells: Int = 0,
    val queuedJobs: Int = 0,
    val storageUsageBytes: Long = 0L,
    val storageProvider: String = ""
)

private data class AuthTokens(val accessToken: String, val refreshToken: String?)

private data class UploadDraft(
    val photoUri: Uri? = null,
    val placeName: String = "未命名地点",
    val latitude: String = "31.230416",
    val longitude: String = "121.473701",
    val note: String = "",
    val tags: String = "娃娃",
    val visibility: String = "private",
)

private data class BrowseFilters(
    val query: String = "",
    val tag: String = "",
    val place: String = "",
    val source: String = "",
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

private const val DEFAULT_API_BASE = "https://doll-checkin.u.120224.xyz"
private const val AMAP_LOG_TAG = "DollAmapNative"
private val FALLBACK_TAG_GROUPS = listOf(
    TagGroupItem(
        id = "type",
        title = "类型",
        description = "娃娃、努努、立牌、手办、周边等物品类型",
        sortOrder = 10,
        tags = listOf("娃娃", "努努", "立牌", "棉花娃娃", "手办", "周边").map { TagItem(name = it, groupCode = "type") },
    ),
    TagGroupItem(
        id = "ip",
        title = "角色/IP",
        description = "作品、团体、角色和 IP 归属",
        sortOrder = 20,
        tags = listOf("BanG Dream!", "MyGO!!!!!", "Ave Mujica", "Roselia", "Poppin'Party", "Pastel＊Palettes")
            .map { TagItem(name = it, groupCode = "ip") },
    ),
    TagGroupItem(
        id = "scene",
        title = "场景",
        description = "出行、活动、展会、咖啡店等拍摄场景",
        sortOrder = 30,
        tags = listOf("出行", "活动", "咖啡店", "展会", "家里").map { TagItem(name = it, groupCode = "scene") },
    ),
    TagGroupItem(
        id = "status",
        title = "整理",
        description = "补地点、补标签、待整理等维护状态",
        sortOrder = 40,
        tags = listOf("待整理", "补地点", "补标签").map { TagItem(name = it, groupCode = "status") },
    ),
)

private object DollUi {
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

private class DollApi(private val client: OkHttpClient = defaultClient) {
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
        val json = executeJson(request)
        val user = parseAuthUser(json.getJSONObject("user"), fallbackUsage = 0L, fallbackQuota = 0L)
        val tokens = AuthTokens(
            accessToken = json.getString("access_token"),
            refreshToken = json.optString("refresh_token").ifBlank { null },
        )
        user to tokens
    }

    suspend fun me(apiBase: String, token: String): AuthUser = withContext(Dispatchers.IO) {
        val request = authorizedBuilder(apiBase, "/api/auth/me", token).get().build()
        parseAuthUser(executeJson(request).getJSONObject("user"))
    }

    suspend fun refreshAccessToken(apiBase: String, refreshToken: String): String = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put("refresh_token", refreshToken)
            .toString()
        val request = Request.Builder()
            .url(apiUrl(apiBase, "/api/auth/refresh"))
            .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()
        executeJson(request).getString("access_token")
    }

    suspend fun listCheckins(apiBase: String, token: String): List<CheckinItem> = withContext(Dispatchers.IO) {
        val request = authorizedBuilder(apiBase, "/api/checkins?limit=120&offset=0", token).get().build()
        val items = executeJson(request).getJSONArray("items")
        buildList {
            for (index in 0 until items.length()) {
                add(parseCheckin(items.getJSONObject(index)))
            }
        }
    }

    suspend fun listMessages(apiBase: String, token: String): List<MessageItem> = withContext(Dispatchers.IO) {
        val request = authorizedBuilder(apiBase, "/api/messages?limit=20", token).get().build()
        val items = executeJson(request).getJSONArray("items")
        buildList {
            for (index in 0 until items.length()) add(parseMessage(items.getJSONObject(index)))
        }
    }

    suspend fun tagCatalog(apiBase: String, token: String): TagCatalog = withContext(Dispatchers.IO) {
        val request = authorizedBuilder(apiBase, "/api/tags/catalog", token).get().build()
        parseTagCatalog(executeJson(request))
    }

    suspend fun listAlbums(apiBase: String, token: String): List<AlbumItem> = withContext(Dispatchers.IO) {
        val request = authorizedBuilder(apiBase, "/api/albums", token).get().build()
        val items = executeJson(request).getJSONArray("items")
        buildList {
            for (index in 0 until items.length()) add(parseAlbum(items.getJSONObject(index)))
        }
    }

    suspend fun listMapCells(apiBase: String, token: String, scope: String): List<MapCellItem> = withContext(Dispatchers.IO) {
        val request = authorizedBuilder(apiBase, "/api/map/cells?scope=$scope", token).get().build()
        val json = executeJson(request)
        val items = json.getJSONArray("items")
        val responseScope = json.optString("scope", scope)
        buildList {
            for (index in 0 until items.length()) add(parseMapCell(items.getJSONObject(index), responseScope))
        }
    }

    suspend fun librarySummary(apiBase: String, token: String): LibrarySummary = withContext(Dispatchers.IO) {
        val request = authorizedBuilder(apiBase, "/api/library/summary", token).get().build()
        parseLibrarySummary(executeJson(request))
    }

    suspend fun uploadCheckin(
        context: Context,
        apiBase: String,
        token: String,
        draft: UploadDraft,
    ): CheckinItem = withContext(Dispatchers.IO) {
        val uri = draft.photoUri ?: throw IllegalArgumentException("请先拍照或选择图片")
        val photoBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalArgumentException("无法读取图片")
        val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
        val filename = if (mimeType.contains("png")) "android-upload.png" else "android-upload.jpg"
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("place_name", draft.placeName.ifBlank { "未命名地点" })
            .addFormDataPart("latitude", draft.latitude.trim())
            .addFormDataPart("longitude", draft.longitude.trim())
            .addFormDataPart("note", draft.note)
            .addFormDataPart("tags", draft.tags)
            .addFormDataPart("source", "android_capture")
            .addFormDataPart("visibility", draft.visibility)
            .addFormDataPart("location_source", "device_location")
            .addFormDataPart("photo", filename, photoBytes.toRequestBody(mimeType.toMediaType()))
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
            .put("visibility", draft.visibility)
            .put("location_source", "manual")
            .toString()
        val request = authorizedBuilder(apiBase, "/api/checkins/${record.id}", token)
            .patch(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()
        parseCheckin(executeJson(request))
    }

    suspend fun uploadAvatar(context: Context, apiBase: String, token: String, uri: Uri): AuthUser = withContext(Dispatchers.IO) {
        val photoBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalArgumentException("无法读取头像图片")
        val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
        val filename = if (mimeType.contains("png")) "avatar.png" else "avatar.jpg"
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("photo", filename, photoBytes.toRequestBody(mimeType.toMediaType()))
            .build()
        val request = authorizedBuilder(apiBase, "/api/users/me/avatar", token).post(body).build()
        parseAuthUser(executeJson(request).getJSONObject("user"))
    }

    suspend fun downloadBitmap(url: String): Bitmap? = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).get().build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext null
            val bytes = response.body.bytes()
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }
    }

    private fun authorizedBuilder(apiBase: String, path: String, token: String): Request.Builder = Request.Builder()
        .url(apiUrl(apiBase, path))
        .header("Authorization", "Bearer $token")

    private fun executeJson(request: Request): JSONObject {
        client.newCall(request).execute().use { response ->
            val text = response.body.string()
            if (!response.isSuccessful) {
                val detail = runCatching { JSONObject(text).optString("detail") }.getOrNull()
                throw IllegalStateException(detail?.ifBlank { null } ?: "HTTP ${response.code}")
            }
            return JSONObject(text)
        }
    }

    companion object {
        private val defaultClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(90, TimeUnit.SECONDS)
            .build()
    }
}

@Composable
private fun DollCheckinApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("doll-checkin", Context.MODE_PRIVATE) }
    val api = remember { DollApi() }
    val scope = rememberCoroutineScope()

    var activeTab by rememberSaveable { mutableStateOf(AppTab.Feed.name) }
    var apiBase by rememberSaveable { mutableStateOf(prefs.getString("apiBase", DEFAULT_API_BASE) ?: DEFAULT_API_BASE) }
    var loginName by rememberSaveable { mutableStateOf(prefs.getString("lastLogin", "lumokato") ?: "lumokato") }
    var password by rememberSaveable { mutableStateOf("") }
    var accessToken by rememberSaveable { mutableStateOf(prefs.getString("accessToken", "") ?: "") }
    var refreshToken by rememberSaveable { mutableStateOf(prefs.getString("refreshToken", "") ?: "") }
    var currentUser by remember { mutableStateOf<AuthUser?>(null) }
    var records by remember { mutableStateOf<List<CheckinItem>>(emptyList()) }
    var messages by remember { mutableStateOf<List<MessageItem>>(emptyList()) }
    var tagCatalog by remember { mutableStateOf(TagCatalog(groups = FALLBACK_TAG_GROUPS)) }
    var albums by remember { mutableStateOf<List<AlbumItem>>(emptyList()) }
    var personalMapCells by remember { mutableStateOf<List<MapCellItem>>(emptyList()) }
    var worldMapCells by remember { mutableStateOf<List<MapCellItem>>(emptyList()) }
    var librarySummary by remember { mutableStateOf(LibrarySummary()) }
    var draft by remember { mutableStateOf(UploadDraft()) }
    var avatarUri by remember { mutableStateOf<Uri?>(null) }
    var filters by remember { mutableStateOf(BrowseFilters()) }
    var pendingDeleteId by rememberSaveable { mutableStateOf("") }
    var selectedRecord by remember { mutableStateOf<CheckinItem?>(null) }
    var editingRecord by remember { mutableStateOf<CheckinItem?>(null) }
    var uploadPhase by rememberSaveable { mutableStateOf("idle") }
    var message by rememberSaveable { mutableStateOf("准备记录今天的娃娃出行") }
    var busy by rememberSaveable { mutableStateOf(false) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) {
            message = "照片已就绪，可以补充地点和标签"
        } else {
            draft = draft.copy(photoUri = null)
            message = "拍照已取消"
        }
    }
    val pickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            draft = draft.copy(photoUri = uri)
            message = "已选择图片，可以上传"
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        val granted = permissions.values.any { it }
        message = if (granted) "权限已更新" else "部分权限未开启，可继续手动填写位置"
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
        messages = emptyList()
        tagCatalog = TagCatalog(groups = FALLBACK_TAG_GROUPS)
        albums = emptyList()
        personalMapCells = emptyList()
        worldMapCells = emptyList()
        librarySummary = LibrarySummary()
        prefs.edit().remove("accessToken").remove("refreshToken").apply()
        message = nextMessage
        activeTab = AppTab.Me.name
    }

    suspend fun <T> runWithTokenRefresh(block: suspend (String) -> T): T {
        if (accessToken.isBlank()) throw IllegalStateException("请先登录")
        return try {
            block(accessToken)
        } catch (firstError: Exception) {
            if (refreshToken.isBlank() || !looksLikeExpiredToken(firstError)) throw firstError
            val refreshed = api.refreshAccessToken(apiBase, refreshToken)
            persistTokens(refreshed)
            block(refreshed)
        }
    }

    suspend fun refreshLibraryState() {
        if (accessToken.isBlank()) return
        tagCatalog = runCatching { runWithTokenRefresh { token -> api.tagCatalog(apiBase, token) } }
            .getOrDefault(tagCatalog)
        runCatching {
            val nextSummary = runWithTokenRefresh { token -> api.librarySummary(apiBase, token) }
            librarySummary = nextSummary
            albums = nextSummary.albums
        }
        personalMapCells = runCatching { runWithTokenRefresh { token -> api.listMapCells(apiBase, token, "personal") } }.getOrDefault(personalMapCells)
        worldMapCells = runCatching { runWithTokenRefresh { token -> api.listMapCells(apiBase, token, "world") } }.getOrDefault(worldMapCells)
        if (albums.isEmpty()) {
            albums = runCatching { runWithTokenRefresh { token -> api.listAlbums(apiBase, token) } }.getOrDefault(albums)
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
                val (user, items) = runWithTokenRefresh { token ->
                    api.me(apiBase, token) to api.listCheckins(apiBase, token)
                }
                currentUser = user
                records = items
                messages = runCatching { runWithTokenRefresh { token -> api.listMessages(apiBase, token) } }.getOrDefault(emptyList())
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
                records = api.listCheckins(apiBase, tokens.accessToken)
                messages = runCatching { api.listMessages(apiBase, tokens.accessToken) }.getOrDefault(emptyList())
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

    fun logout() {
        clearAuthState("已退出登录")
    }

    fun requestCorePermissions() {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.CAMERA,
                Manifest.permission.READ_MEDIA_IMAGES,
            )
        )
    }

    fun useDeviceLocation() {
        val location = lastKnownLocation(context)
        if (location == null) {
            message = "未拿到系统定位，已保留可编辑坐标"
        } else {
            draft = draft.copy(
                latitude = "%.6f".format(location.latitude),
                longitude = "%.6f".format(location.longitude),
            )
            message = "已填入当前位置"
        }
    }

    fun takePhoto() {
        requestCorePermissions()
        val uri = createCaptureUri(context)
        draft = draft.copy(photoUri = uri)
        cameraLauncher.launch(uri)
    }

    fun upload() {
        if (accessToken.isBlank()) {
            message = "请先登录"
            activeTab = AppTab.Me.name
            return
        }
        busy = true
        uploadPhase = "preparing"
        scope.launch {
            try {
                uploadPhase = "uploading"
                val uploaded = runWithTokenRefresh { token -> api.uploadCheckin(context, apiBase, token, draft) }
                records = listOf(uploaded) + records.filterNot { it.id == uploaded.id }
                currentUser = runCatching { runWithTokenRefresh { token -> api.me(apiBase, token) } }.getOrNull() ?: currentUser
                messages = runCatching { runWithTokenRefresh { token -> api.listMessages(apiBase, token) } }.getOrDefault(messages)
                refreshLibraryState()
                draft = UploadDraft()
                pendingDeleteId = ""
                uploadPhase = "done"
                message = "上传成功：${uploaded.placeName}"
                activeTab = AppTab.Feed.name
            } catch (error: Exception) {
                uploadPhase = "failed"
                message = error.message ?: "上传失败"
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
                currentUser = runCatching { runWithTokenRefresh { token -> api.me(apiBase, token) } }.getOrNull() ?: currentUser
                messages = runCatching { runWithTokenRefresh { token -> api.listMessages(apiBase, token) } }.getOrDefault(messages)
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
            selectedRecord = runCatching { runWithTokenRefresh { token -> api.getCheckin(apiBase, token, record.id) } }.getOrDefault(record)
        }
    }

    fun saveRecord(record: CheckinItem, edit: CheckinEditDraft) {
        busy = true
        scope.launch {
            try {
                val updated = runWithTokenRefresh { token -> api.updateCheckin(apiBase, token, record, edit) }
                records = records.map { if (it.id == updated.id) updated else it }
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

    DollTheme {
        Scaffold(
            bottomBar = {
                Column(Modifier.fillMaxWidth().background(Color.White).navigationBarsPadding()) {
                    Surface(color = Color.White, border = BorderStroke(0.5.dp, DollUi.Hairline), modifier = Modifier.fillMaxWidth().height(56.dp)) {
                        Row(Modifier.fillMaxSize()) {
                            AppTab.entries.forEach { tab ->
                                val selected = activeTab == tab.name
                                Column(Modifier.weight(1f).fillMaxSize().clickable { activeTab = tab.name }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                    StitchTabIcon(tab, selected)
                                    Spacer(Modifier.height(2.dp))
                                    Text(tab.title, fontSize = 10.sp, lineHeight = 12.sp, color = if (selected) DollUi.Coral else DollUi.Ink, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        }
                    }
                }
            },
        ) { padding ->
            Surface(color = DollUi.Background, modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                    when (AppTab.valueOf(activeTab)) {
                        AppTab.Feed -> FeedScreen(
                            records = filterRecords(records, filters),
                            allRecords = records,
                            filters = filters,
                            apiBase = apiBase,
                            api = api,
                            pendingDeleteId = pendingDeleteId,
                            onFiltersChange = { filters = it },
                            onPublish = { activeTab = AppTab.Publish.name },
                            onDelete = { deleteRecord(it) },
                            onOpen = { openRecord(it) },
                        )
                        AppTab.Map -> MapScreen(
                            records = filterRecords(records, filters),
                            personalMapCells = personalMapCells,
                            worldMapCells = worldMapCells,
                            onPublish = { activeTab = AppTab.Publish.name },
                            onOpen = { openRecord(it) },
                        )
                        AppTab.Publish -> PublishScreen(
                            draft = draft,
                            busy = busy,
                            uploadPhase = uploadPhase,
                            tagGroups = tagCatalog.groups.ifEmpty { FALLBACK_TAG_GROUPS },
                            onDraftChange = { draft = it },
                            onPick = { pickerLauncher.launch("image/*") },
                            onCamera = { takePhoto() },
                            onLocation = { useDeviceLocation() },
                            onUpload = { upload() },
                        )
                        AppTab.Library -> LibraryScreen(
                            messages = messages,
                            records = records,
                            albums = albums,
                            summary = librarySummary,
                            personalMapCells = personalMapCells,
                            worldMapCells = worldMapCells,
                            onOpenPublish = { activeTab = AppTab.Publish.name },
                        )
                        AppTab.Me -> MeScreen(
                            apiBase = apiBase,
                            api = api,
                            loginName = loginName,
                            password = password,
                            user = currentUser,
                            records = records,
                            avatarUri = avatarUri,
                            busy = busy,
                            onLoginNameChange = { loginName = it },
                            onPasswordChange = { password = it },
                            onLogin = { login() },
                            onLogout = { logout() },
                            onPermissions = { requestCorePermissions() },
                            onPickAvatar = { avatarPickerLauncher.launch("image/*") },
                            onRefresh = { refreshProfileAndRecords() },
                            onOpenRecord = { openRecord(it) },
                            onOpenPhotos = { filters = BrowseFilters(); activeTab = AppTab.Feed.name },
                            onOpenPlaces = { activeTab = AppTab.Map.name },
                            onOpenTags = { filters = filters.copy(tag = rankedTags(records).firstOrNull()?.first.orEmpty()); activeTab = AppTab.Feed.name },
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
                pendingDelete = pendingDeleteId == record.id,
            )
        }
        editingRecord?.let { record ->
            CheckinEditDialog(
                record = record,
                tagGroups = tagCatalog.groups.ifEmpty { FALLBACK_TAG_GROUPS },
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
            .background(DollUi.Background)
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(listOf(DollUi.CoralSoft, DollUi.BlueSoft))),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("D", color = DollUi.Coral, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("娃娃打卡", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = DollUi.Ink)
                    Text(
                        user?.let { "${it.displayName} 的行程索引 · $recordsCount 张" } ?: "私人照片地图日志",
                        color = DollUi.Muted,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            TextButton(onClick = onRefresh, enabled = !busy) { Text(if (busy) "同步中" else "同步") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            TopIndexPill("照片", recordsCount.toString(), DollUi.CoralSoft, DollUi.Coral, Modifier.weight(1f))
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
        Text(label, color = DollUi.Slate, style = MaterialTheme.typography.bodySmall, maxLines = 1)
    }
}

@Composable
private fun StatusPill(text: String, busy: Boolean, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(if (busy) DollUi.AmberSoft else DollUi.GreenSoft)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (busy) DollUi.Amber else DollUi.Green),
        )
        Spacer(Modifier.width(8.dp))
        Text(text, color = DollUi.Slate, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun JournalCard(
    modifier: Modifier = Modifier,
    containerColor: Color = DollUi.Paper,
    radius: Dp = DollUi.CardRadius,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(radius),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, DollUi.Hairline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) { content() }
}

@Composable
private fun StitchTabIcon(tab: AppTab, selected: Boolean) {
    val image = when (tab) {
        AppTab.Feed -> if (selected) Icons.Filled.Home else Icons.Outlined.Home
        AppTab.Map -> if (selected) Icons.Filled.Map else Icons.Outlined.Map
        AppTab.Publish -> if (selected) Icons.Filled.AddCircle else Icons.Outlined.AddCircleOutline
        AppTab.Library -> if (selected) Icons.Filled.ChatBubble else Icons.Outlined.ChatBubbleOutline
        AppTab.Me -> if (selected) Icons.Filled.Person else Icons.Outlined.PersonOutline
    }
    Icon(
        imageVector = image,
        contentDescription = tab.title,
        tint = if (selected) DollUi.Coral else DollUi.Ink,
        modifier = Modifier.size(24.dp),
    )
}

@Composable
private fun StitchChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(modifier = Modifier.clickable(onClick = onClick), color = if (selected) DollUi.Coral else DollUi.Paper, contentColor = if (selected) Color.White else DollUi.Ink, shape = RoundedCornerShape(12.dp), border = if (selected) null else BorderStroke(1.dp, DollUi.Hairline)) {
        Text(label, modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp), fontSize = 12.sp, lineHeight = 16.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun FeedScreen(
    records: List<CheckinItem>, allRecords: List<CheckinItem>, filters: BrowseFilters,
    apiBase: String, api: DollApi, pendingDeleteId: String,
    onFiltersChange: (BrowseFilters) -> Unit, onPublish: () -> Unit, onDelete: (CheckinItem) -> Unit, onOpen: (CheckinItem) -> Unit,
) {
    var feedMode by rememberSaveable { mutableStateOf("发现") }
    val visible = when (feedMode) {
        "关注" -> records.filter { it.visibility == "friends" || it.visibility == "public" }
        "类别" -> if (filters.tag.isBlank()) records else filterRecords(allRecords, filters)
        else -> records
    }
    val rows = visible.chunked(2)
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 8.dp)) {
        stickyHeader {
            Surface(color = DollUi.Surface, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    listOf("发现", "关注", "类别").forEach { tab ->
                        TextButton(onClick = { feedMode = tab }, modifier = Modifier.weight(1f)) {
                            Text(tab, color = if (feedMode == tab) DollUi.Coral else DollUi.Muted, fontWeight = if (feedMode == tab) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            }
        }
        if (feedMode == "类别") {
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(rankedTags(allRecords).take(12)) { pair ->
                        StitchChip(pair.first, filters.tag == pair.first) { onFiltersChange(filters.copy(tag = pair.first)) }
                    }
                }
            }
        }
        if (visible.isEmpty()) item { Box(Modifier.padding(12.dp)) { EmptyState("暂无照片", "登记第一张照片后会显示在这里。") } }
        items(rows, key = { it.joinToString("|") { record -> record.id } }) { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                row.forEach { record ->
                    Box(Modifier.weight(1f).clickable { onOpen(record) }) {
                        RemoteImage(url = record.displayUrl ?: record.thumbUrl, apiBase = apiBase, api = api, aspect = 0.82f)
                        Surface(modifier = Modifier.align(Alignment.BottomStart).padding(6.dp), color = Color.Black.copy(alpha = 0.55f), shape = RoundedCornerShape(4.dp)) {
                            Text(record.tags.firstOrNull()?.let { "#$it" } ?: record.placeName, color = Color.White, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp), maxLines = 1)
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
private fun MapScreen(records: List<CheckinItem>, personalMapCells: List<MapCellItem>, worldMapCells: List<MapCellItem>, onPublish: () -> Unit, onOpen: (CheckinItem) -> Unit) {
    var scope by rememberSaveable { mutableStateOf("世界地图") }
    var filter by rememberSaveable { mutableStateOf("全部") }
    val visible = when (filter) {
        "关注" -> records.filter { it.visibility == "friends" || it.visibility == "public" }
        "类别" -> records.filter { it.tags.any(::looksLikeCollectionTag) }
        else -> records
    }
    Box(Modifier.fillMaxSize()) {
        AmapNativeMap(records = visible, world = scope == "世界地图", modifier = Modifier.fillMaxSize(), onOpen = onOpen)
        Column(Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(color = Color.White.copy(alpha = 0.96f), shape = RoundedCornerShape(4.dp), border = BorderStroke(1.dp, DollUi.Hairline)) {
                Row(Modifier.height(42.dp)) {
                    listOf("世界地图", "个人地图").forEach { item ->
                        Box(modifier = Modifier.weight(1f).height(42.dp).clickable { scope = item }, contentAlignment = Alignment.Center) {
                            Text(item, textAlign = TextAlign.Center, color = if (scope == item) DollUi.Coral else DollUi.Ink, fontWeight = if (scope == item) FontWeight.Bold else FontWeight.Normal)
                            if (scope == item) Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(2.dp).background(DollUi.Coral))
                        }
                    }
                }
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(listOf("全部", "关注", "类别")) { item -> StitchChip(item, filter == item) { filter = item } }
            }
        }
        Surface(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(), color = Color.White.copy(alpha = 0.97f), shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp), border = BorderStroke(0.5.dp, DollUi.Hairline)) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(scope, fontWeight = FontWeight.Bold)
                    Text("${visible.size} 个记录地点", color = DollUi.Muted, style = MaterialTheme.typography.bodySmall)
                }
                Surface(color = DollUi.Coral, shape = RoundedCornerShape(4.dp), modifier = Modifier.clickable(onClick = onPublish)) {
                    Text("在这里登记", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp))
                }
            }
        }
    }
}

@Composable
private fun FeedTopTabs(selected: String, onSelect: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        items(listOf("发现", "关注", "类别")) { tab ->
            FilterChip(selected = selected == tab, onClick = { onSelect(tab) }, label = { Text(tab) })
        }
    }
}

@Composable
private fun MapTopTabs(selected: String, onSelect: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        items(listOf("全部", "关注", "类别")) { tab ->
            FilterChip(selected = selected == tab, onClick = { onSelect(tab) }, label = { Text(tab) })
        }
    }
}

@Composable
private fun MapScopeSummary(personalCells: List<MapCellItem>, worldCells: List<MapCellItem>, visibleRecords: Int) {
    JournalCard(containerColor = DollUi.Surface) {
        Row(Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CompactStat("个人地图", personalCells.size.toString(), Modifier.weight(1f))
            CompactStat("世界地图", worldCells.size.toString(), Modifier.weight(1f))
            CompactStat("当前筛选", visibleRecords.toString(), Modifier.weight(1f))
        }
    }
}

@Composable
private fun CompactStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(10.dp)).background(DollUi.PaperTint).padding(horizontal = 9.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(value, color = DollUi.Ink, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleSmall, maxLines = 1)
        Text(label, color = DollUi.Muted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun CategoryRankingCard(records: List<CheckinItem>, selectedTag: String, onSelectTag: (String) -> Unit) {
    val tags = rankedTags(records).take(8)
    JournalCard(containerColor = DollUi.Surface) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("角色 / IP 排行", color = DollUi.Ink, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleSmall)
            if (tags.isEmpty()) {
                Text("有分类标签后会自动生成排行。", color = DollUi.Muted, style = MaterialTheme.typography.bodySmall)
            } else {
                tags.forEachIndexed { index, pair ->
                    RankingRow(index = index + 1, tag = pair.first, count = pair.second, selected = selectedTag == pair.first, onClick = { onSelectTag(pair.first) })
                }
            }
        }
    }
}

@Composable
private fun RankingRow(index: Int, tag: String, count: Int, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(if (selected) DollUi.CoralSoft else DollUi.PaperTint).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(index.toString(), color = DollUi.Coral, fontWeight = FontWeight.Black, modifier = Modifier.width(24.dp))
        Text(tag, color = DollUi.Ink, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("$count 张", color = DollUi.Muted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun PhotoFeedTile(record: CheckinItem, apiBase: String, api: DollApi, modifier: Modifier = Modifier, tall: Boolean) {
    JournalCard(modifier = modifier, containerColor = DollUi.Surface, radius = 12.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))) {
                RemoteImage(url = record.displayUrl ?: record.thumbUrl, apiBase = apiBase, api = api, aspect = if (tall) 0.82f else 1.05f)
            }
            Column(Modifier.padding(horizontal = 8.dp, vertical = 7.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(record.placeName.ifBlank { "未命名地点" }, color = DollUi.Ink, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(shortDate(record.takenAt ?: record.createdAt), color = DollUi.Muted, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                val tag = record.tags.firstOrNull().orEmpty()
                if (tag.isNotBlank()) Text("#$tag", color = DollUi.Blue, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun PublishScreen(draft: UploadDraft, busy: Boolean, uploadPhase: String, tagGroups: List<TagGroupItem>, onDraftChange: (UploadDraft) -> Unit, onPick: () -> Unit, onCamera: () -> Unit, onLocation: () -> Unit, onUpload: () -> Unit) {
    val validation = validateDraft(draft)
    fun toggle(tag: String) = onDraftChange(draft.copy(tags = toggleTag(draft.tags, tag)))
    LazyColumn(Modifier.fillMaxSize().background(Color.White), contentPadding = PaddingValues(bottom = 20.dp)) {
        item { PhotoPickerCard(uri = draft.photoUri, onPick = onPick, onCamera = onCamera) }
        item {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("登记", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = DollUi.Ink)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(draft.placeName, { onDraftChange(draft.copy(placeName = it)) }, label = { Text("地点") }, modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(4.dp))
                    Spacer(Modifier.width(8.dp))
                    Surface(shape = RoundedCornerShape(4.dp), border = BorderStroke(1.dp, DollUi.Hairline), color = Color.White, modifier = Modifier.height(56.dp).clickable(onClick = onLocation)) {
                        Box(Modifier.padding(horizontal = 14.dp), contentAlignment = Alignment.Center) { Text("定位", color = DollUi.Coral, fontWeight = FontWeight.Bold) }
                    }
                }
                PublishTagSelector(tagGroups, parseDraftTags(draft.tags), ::toggle)
                OutlinedTextField(draft.note, { onDraftChange(draft.copy(note = it)) }, label = { Text("备注") }, modifier = Modifier.fillMaxWidth().heightIn(min = 84.dp), shape = RoundedCornerShape(4.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("可见范围", color = DollUi.Ink, fontWeight = FontWeight.Bold)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("public" to "公开", "friends" to "关注可见", "private" to "私密").forEach { pair -> StitchChip(pair.second, draft.visibility == pair.first) { onDraftChange(draft.copy(visibility = pair.first)) } }
                    }
                }
                if (busy || uploadPhase == "failed") UploadPhaseBar(uploadPhase, busy)
                Surface(color = if (!busy && validation.ready) DollUi.Coral else DollUi.Placeholder, shape = RoundedCornerShape(4.dp), modifier = Modifier.fillMaxWidth().height(48.dp).clickable(enabled = !busy && validation.ready, onClick = onUpload)) {
                    Box(contentAlignment = Alignment.Center) { Text(if (busy) "上传中" else "登记", color = if (!busy && validation.ready) Color.White else DollUi.Muted, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

@Composable
private fun LibraryScreen(messages: List<MessageItem>, records: List<CheckinItem>, albums: List<AlbumItem>, summary: LibrarySummary, personalMapCells: List<MapCellItem>, worldMapCells: List<MapCellItem>, onOpenPublish: () -> Unit) {
    var mode by rememberSaveable { mutableStateOf("全部") }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        stickyHeader {
            Surface(color = DollUi.Surface, modifier = Modifier.fillMaxWidth(), border = BorderStroke(0.5.dp, DollUi.Hairline)) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("消息", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listOf("全部", "互动", "关注", "系统")) { item -> StitchChip(item, mode == item) { mode = item } }
                    }
                }
            }
        }
        if (messages.isEmpty()) item { MessageLine("暂无消息", "互动、关注和系统通知会显示在这里", DollUi.BlueSoft, DollUi.Blue) }
        items(messages) { item ->
            val colors = messageColors(item.kind, item.priority)
            MessageLine(item.title, item.body, colors.first, colors.second)
        }
    }
}

@Composable
private fun MessageLine(title: String, subtitle: String, background: Color, foreground: Color) {
    Row(Modifier.fillMaxWidth().heightIn(min = 72.dp).padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(DollUi.PaperTint), contentAlignment = Alignment.Center) { Text(title.take(1), color = DollUi.Muted, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(subtitle, color = DollUi.Muted, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun LibrarySummaryCard(
    albums: List<AlbumItem>,
    summary: LibrarySummary,
    personalCellCount: Int,
    worldCellCount: Int,
    recordsCount: Int,
) {
    val smartCount = albums.filter { it.type == "smart" }.sumOf { it.itemCount }.takeIf { it > 0 } ?: recordsCount
    val rankCount = albums.count { it.type == "rank" }
    JournalCard(containerColor = DollUi.Surface) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                CompactStat("合集", smartCount.toString(), Modifier.weight(1f))
                CompactStat("地图", personalCellCount.toString(), Modifier.weight(1f))
                CompactStat("榜单", rankCount.toString(), Modifier.weight(1f))
                CompactStat("队列", summary.queuedJobs.toString(), Modifier.weight(1f))
            }
            Text(
                "个人地图 $personalCellCount · 世界地图 $worldCellCount · 存储 ${formatBytes(summary.storageUsageBytes)}${summary.storageProvider.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""}",
                color = DollUi.Muted,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun MessageRow(title: String, subtitle: String, background: Color, foreground: Color) {
    JournalCard(containerColor = DollUi.Surface) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).clip(CircleShape).background(background), contentAlignment = Alignment.Center) {
                Text(title.take(1), color = foreground, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, color = DollUi.Ink, fontWeight = FontWeight.Black)
                Text(subtitle, color = DollUi.Muted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

private fun messageColors(kind: String, priority: String): Pair<Color, Color> = when {
    priority == "high" -> DollUi.AmberSoft to DollUi.Amber
    kind == "photo" -> DollUi.CoralSoft to DollUi.Coral
    kind == "storage" -> DollUi.GreenSoft to DollUi.Green
    kind == "profile" -> DollUi.BlueSoft to DollUi.Blue
    else -> DollUi.PaperTint to DollUi.Slate
}

@Composable
private fun AvatarView(uri: Uri?, imageUrl: String?, apiBase: String, api: DollApi, label: String, onClick: () -> Unit) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(initialValue = null, uri) {
        value = if (uri == null) null else withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.use { input -> BitmapFactory.decodeStream(input) }
        }
    }
    val remoteBitmap by produceState<Bitmap?>(initialValue = null, imageUrl, apiBase) {
        value = imageUrl?.let { runCatching { api.downloadBitmap(resolveAssetUrl(apiBase, it)) }.getOrNull() }
    }
    val displayBitmap = bitmap ?: remoteBitmap
    Box(
        Modifier.size(80.dp).clip(CircleShape).background(Brush.linearGradient(listOf(DollUi.CoralSoft, DollUi.BlueSoft))).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (displayBitmap == null) {
            Text(label, color = DollUi.Coral, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge)
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
        Text(value, color = DollUi.Ink, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
        Text(label, color = DollUi.Muted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun MeScreen(loginName: String, password: String, user: AuthUser?, records: List<CheckinItem>, avatarUri: Uri?, apiBase: String, api: DollApi, busy: Boolean, onLoginNameChange: (String) -> Unit, onPasswordChange: (String) -> Unit, onLogin: () -> Unit, onLogout: () -> Unit, onPermissions: () -> Unit, onPickAvatar: () -> Unit, onRefresh: () -> Unit, onOpenRecord: (CheckinItem) -> Unit, onOpenPhotos: () -> Unit, onOpenPlaces: () -> Unit, onOpenTags: () -> Unit, message: String) {
    val places = records.map { it.placeName }.filter { it.isNotBlank() }.distinct().size
    val tags = records.flatMap { it.tags }.distinct().size
    if (user == null) {
        Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.Center) {
            Text("登录", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(loginName, onLoginNameChange, label = { Text("邮箱或用户名") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(password, onPasswordChange, label = { Text("密码") }, modifier = Modifier.fillMaxWidth(), singleLine = true, visualTransformation = PasswordVisualTransformation())
            Spacer(Modifier.height(14.dp))
            Button(onClick = onLogin, enabled = !busy && loginName.isNotBlank() && password.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("登录") }
        }
        return
    }
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Column(Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                AvatarView(uri = avatarUri, imageUrl = user.avatarUrl, apiBase = apiBase, api = api, label = user.displayName.take(1), onClick = onPickAvatar)
                Spacer(Modifier.height(8.dp))
                Text(user.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(user.username ?: "记录和收藏我的娃娃足迹", color = DollUi.Muted, style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = onPickAvatar) { Text("编辑资料") }
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
        item { TextButton(onClick = onLogout, modifier = Modifier.fillMaxWidth().padding(12.dp)) { Text("退出登录", color = DollUi.Muted) } }
    }
}

@Composable
private fun ProfileMenuRow(title: String, value: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(52.dp).clickable(onClick = onClick).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, modifier = Modifier.weight(1f))
        Text(value, color = DollUi.Muted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String, onAction: () -> Unit, actionLabel: String = "登记") {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = DollUi.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text(subtitle, color = DollUi.Muted, style = MaterialTheme.typography.bodySmall)
        }
        TextButton(onClick = onAction) { Text(actionLabel) }
    }
}

@Composable
private fun EmptyState(title: String, subtitle: String) {
    JournalCard(containerColor = DollUi.Surface) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, color = DollUi.Ink, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, color = DollUi.Muted)
        }
    }
}

@Composable
private fun FirstRecordCard(onPublish: () -> Unit) {
    JournalCard(containerColor = DollUi.CoralSoft) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onPublish).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("记录第一张照片", color = DollUi.Ink, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
                Text("先选照片，再补地点和标签。", color = DollUi.Slate, style = MaterialTheme.typography.bodySmall)
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
        filters.source.takeIf { it.isNotBlank() }?.let { sourceLabel(it) },
    )
    JournalCard(containerColor = DollUi.Surface) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text("相册索引", color = DollUi.Ink, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                    Text(latest?.let { "最近 ${shortDate(it.takenAt ?: it.createdAt)} · ${it.placeName}" } ?: "还没有照片记录", color = DollUi.Muted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text("${visibleCount}/${records.size}", color = DollUi.Coral, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                IndexCell("地点", placesCount.toString(), DollUi.GreenSoft, DollUi.Green, Modifier.weight(1f))
                IndexCell("标签", tagsCount.toString(), DollUi.BlueSoft, DollUi.Blue, Modifier.weight(1f))
                IndexCell("待整理", records.count { it.placeName == "未命名地点" || it.tags.isEmpty() }.toString(), DollUi.AmberSoft, DollUi.Amber, Modifier.weight(1f))
            }
            if (active.isNotEmpty()) {
                Text("当前：${active.joinToString(" · ")}", color = DollUi.Slate, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
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
        Text(label, color = DollUi.Slate, style = MaterialTheme.typography.bodySmall, maxLines = 1)
    }
}

@Composable
private fun BrowseFilterPanel(
    filters: BrowseFilters,
    tags: List<String>,
    places: List<String>,
    onFiltersChange: (BrowseFilters) -> Unit,
) {
    JournalCard(containerColor = DollUi.Surface) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = filters.query,
                onValueChange = { onFiltersChange(filters.copy(query = it)) },
                label = { Text("搜索地点、备注、标签") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = filters.source.isBlank(),
                        onClick = { onFiltersChange(filters.copy(source = "")) },
                        label = { Text("全部") },
                    )
                }
                listOf("android_capture", "new_capture", "historical_import").forEach { source ->
                    item {
                        FilterChip(
                            selected = filters.source == source,
                            onClick = { onFiltersChange(filters.copy(source = source)) },
                            label = { Text(sourceLabel(source)) },
                        )
                    }
                }
            }
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
private fun PublishReadinessCard(validation: DraftValidation, visibility: String) {
    JournalCard(containerColor = if (validation.ready) DollUi.GreenSoft else DollUi.Surface) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text("登记检查", color = DollUi.Ink, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
                    Text(if (validation.ready) "信息完整，可以登记" else validation.missingText, color = DollUi.Muted, style = MaterialTheme.typography.bodySmall)
                }
                Text(visibilityLabel(visibility), color = DollUi.Blue, fontWeight = FontWeight.Bold)
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
private fun PublishTagSelector(tagGroups: List<TagGroupItem>, selectedTags: List<String>, onToggle: (String) -> Unit) {
    val groups = compactTagGroups(tagGroups.ifEmpty { FALLBACK_TAG_GROUPS })
    var activeGroup by rememberSaveable { mutableStateOf(groups.firstOrNull()?.title.orEmpty()) }
    val visibleGroup = groups.firstOrNull { it.title == activeGroup } ?: groups.firstOrNull()
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("标签", color = DollUi.Ink, fontWeight = FontWeight.Bold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(18.dp), modifier = Modifier.fillMaxWidth()) {
            items(groups) { group ->
                Column(Modifier.clickable { activeGroup = group.title }.padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(group.title, color = if (activeGroup == group.title) DollUi.Coral else DollUi.Muted, fontWeight = if (activeGroup == group.title) FontWeight.Bold else FontWeight.Normal)
                    Spacer(Modifier.height(5.dp))
                    Box(Modifier.width(24.dp).height(2.dp).background(if (activeGroup == group.title) DollUi.Coral else Color.Transparent))
                }
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(visibleGroup?.tags.orEmpty()) { tag -> StitchChip(tag.name, selectedTags.any { it == tag.name }) { onToggle(tag.name) } }
        }
    }
}

@Composable
private fun ReadinessStep(label: String, done: Boolean, modifier: Modifier = Modifier) {
    val background = if (done) DollUi.GreenSoft else DollUi.AmberSoft
    val foreground = if (done) DollUi.Green else DollUi.Amber
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
private fun UploadPhaseBar(uploadPhase: String, busy: Boolean) {
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
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(99.dp)),
        )
        Text(text, color = if (uploadPhase == "failed") DollUi.Danger else DollUi.Muted, style = MaterialTheme.typography.bodySmall)
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
        modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(DollUi.PaperTint).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(queueText, fontWeight = FontWeight.Black, color = DollUi.Slate)
        Text(detail, color = DollUi.Muted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun MapPreview(title: String, subtitle: String, records: List<CheckinItem>, world: Boolean) {
    JournalCard(containerColor = DollUi.Surface) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AmapNativeMap(
                records = records,
                world = world,
                modifier = Modifier.fillMaxWidth().height(if (world) 210.dp else 240.dp),
                onOpen = {},
            )
            Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(title, fontWeight = FontWeight.Black, color = DollUi.Ink, modifier = Modifier.weight(1f))
                    Text("${records.size} 个记录点", color = DollUi.Blue, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                }
                Text(subtitle, color = DollUi.Muted, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun AmapNativeMap(records: List<CheckinItem>, world: Boolean, modifier: Modifier = Modifier, onOpen: (CheckinItem) -> Unit) {
    val mapRecords = remember(records) { records.filter { it.latitude in -90.0..90.0 && it.longitude in -180.0..180.0 && !(it.latitude == 0.0 && it.longitude == 0.0) } }
    val hasAmapConfig = BuildConfig.AMAP_ANDROID_KEY.isNotBlank()
    if (!hasAmapConfig) {
        StaticMapFallback(records = mapRecords, world = world, modifier = modifier, reason = "地图配置待接入", onOpen = onOpen)
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
    LaunchedEffect(mapView, mapRecords, world) {
        configureAmap(mapView.map, mapRecords, world, onOpen)
    }
    Box(modifier.clip(RoundedCornerShape(12.dp)).background(DollUi.Placeholder)) {
        AndroidView(
            factory = { mapView },
            update = { view -> configureAmap(view.map, mapRecords, world, onOpen) },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun StaticMapFallback(records: List<CheckinItem>, world: Boolean, modifier: Modifier, reason: String, onOpen: (CheckinItem) -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.linearGradient(listOf(DollUi.BlueSoft, DollUi.GreenSoft, DollUi.CoralSoft)))
            .padding(18.dp),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val grid = Color(0x77FFFFFF)
            if (world) {
                drawOval(Color(0x44FFFFFF), topLeft = Offset(size.width * 0.08f, size.height * 0.18f), size = Size(size.width * 0.26f, size.height * 0.34f))
                drawOval(Color(0x44FFFFFF), topLeft = Offset(size.width * 0.37f, size.height * 0.12f), size = Size(size.width * 0.20f, size.height * 0.28f))
                drawOval(Color(0x44FFFFFF), topLeft = Offset(size.width * 0.58f, size.height * 0.25f), size = Size(size.width * 0.28f, size.height * 0.38f))
            }
            repeat(5) { index ->
                val y = size.height * (index + 1) / 6f
                val x = size.width * (index + 1) / 6f
                drawLine(grid, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
                drawLine(grid, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1.dp.toPx())
            }
            records.take(28).forEachIndexed { index, record ->
                val x = (0.12f + mapRatio(record.longitude, index) * 0.76f) * size.width
                val y = (0.16f + mapRatio(record.latitude, index + 11) * 0.68f) * size.height
                drawCircle(Color(0x33E45B3F), radius = 13.dp.toPx(), center = Offset(x, y))
                drawCircle(DollUi.Coral, radius = 5.dp.toPx(), center = Offset(x, y))
            }
        }
        Text(reason, fontWeight = FontWeight.Black, color = DollUi.Ink, modifier = Modifier.align(Alignment.TopStart))
        Text("${records.size} 个记录点", color = DollUi.Ink, modifier = Modifier.align(Alignment.Center), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
    }
}

private fun configureAmap(amap: AMap, records: List<CheckinItem>, world: Boolean, onOpen: (CheckinItem) -> Unit) {
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
    amap.setOnMarkerClickListener { marker -> markerRecords[marker.id]?.let(onOpen); true }
    if (points.isEmpty()) {
        val fallback = if (world) LatLng(35.8617, 104.1954) else LatLng(31.2304, 121.4737)
        amap.moveCamera(CameraUpdateFactory.newLatLngZoom(fallback, if (world) 3f else 11f))
    } else if (points.size == 1) {
        amap.moveCamera(CameraUpdateFactory.newLatLngZoom(points.first().second, if (world) 5f else 13f))
    } else {
        val bounds = LatLngBounds.builder().apply { points.forEach { include(it.second) } }.build()
        amap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, if (world) 42 else 54))
    }
    Log.d(AMAP_LOG_TAG, "native-map-ready mode=${if (world) "world" else "personal"} markers=${points.size}")
}

@Composable
private fun CheckinCard(
    record: CheckinItem,
    apiBase: String,
    api: DollApi,
    pendingDelete: Boolean,
    onDelete: () -> Unit,
) {
    JournalCard(containerColor = DollUi.Surface) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            RemoteImage(url = record.displayUrl ?: record.thumbUrl, apiBase = apiBase, api = api)
            Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(record.placeName, color = DollUi.Ink, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${shortDate(record.takenAt ?: record.createdAt)} · ${sourceLabel(record.source)} · ${visibilityLabel(record.visibility)}", color = DollUi.Muted, style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick = onDelete) { Text(if (pendingDelete) "确认删除" else "删除", color = if (pendingDelete) DollUi.Danger else DollUi.Muted) }
                }
                if (record.note.isNotBlank()) Text(record.note, color = DollUi.Slate, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    record.tags.take(4).forEach { tag -> AssistChip(onClick = {}, label = { Text(tag) }) }
                    if (record.tags.isEmpty()) AssistChip(onClick = {}, label = { Text("待整理") })
                }
            }
        }
    }
}

@Composable
private fun CheckinDetailDialog(
    record: CheckinItem,
    apiBase: String,
    api: DollApi,
    busy: Boolean,
    pendingDelete: Boolean,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(record.placeName.ifBlank { "未命名地点" }, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { RemoteImage(record.originalUrl ?: record.displayUrl ?: record.thumbUrl, apiBase, api, aspect = 0.82f) }
                item { Text("${shortDate(record.takenAt ?: record.createdAt)} · ${visibilityLabel(record.visibility)}", color = DollUi.Muted, style = MaterialTheme.typography.bodySmall) }
                if (record.note.isNotBlank()) item { Text(record.note, color = DollUi.Ink) }
                if (record.tags.isNotEmpty()) item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(record.tags) { tag -> AssistChip(onClick = {}, label = { Text(tag) }) }
                    }
                }
                item { Text(formatCoordinate(record.latitude, record.longitude), color = DollUi.Muted, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = { TextButton(onClick = onEdit, enabled = !busy) { Text("编辑") } },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete, enabled = !busy) { Text(if (pendingDelete) "确认删除" else "删除", color = DollUi.Danger) }
                TextButton(onClick = onDismiss) { Text("关闭") }
            }
        },
    )
}

@Composable
private fun CheckinEditDialog(record: CheckinItem, tagGroups: List<TagGroupItem>, busy: Boolean, onDismiss: () -> Unit, onSave: (CheckinEditDraft) -> Unit) {
    var placeName by rememberSaveable(record.id) { mutableStateOf(record.placeName) }
    var latitude by rememberSaveable(record.id) { mutableStateOf(record.latitude.toString()) }
    var longitude by rememberSaveable(record.id) { mutableStateOf(record.longitude.toString()) }
    var note by rememberSaveable(record.id) { mutableStateOf(record.note) }
    var selectedTags by remember(record.id) { mutableStateOf(record.tags) }
    var visibility by rememberSaveable(record.id) { mutableStateOf(record.visibility) }
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
                tagGroups.forEach { group ->
                    item {
                        Text(group.title, color = DollUi.Muted, style = MaterialTheme.typography.bodySmall)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(group.tags) { tag ->
                                StitchChip(tag.name, tag.name in selectedTags) {
                                    selectedTags = if (tag.name in selectedTags) selectedTags - tag.name else selectedTags + tag.name
                                }
                            }
                        }
                    }
                }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listOf("private" to "私密", "friends" to "关注", "public" to "公开")) { option ->
                            StitchChip(option.second, visibility == option.first) { visibility = option.first }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid && !busy,
                onClick = { onSave(CheckinEditDraft(placeName, latitude, longitude, note, selectedTags.joinToString(","), visibility)) },
            ) { Text(if (busy) "保存中" else "保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun RemoteImage(url: String?, apiBase: String, api: DollApi, aspect: Float = 1.25f) {
    if (url.isNullOrBlank()) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(aspect).background(DollUi.Placeholder),
            contentAlignment = Alignment.Center,
        ) { Text("等待图片", color = DollUi.Muted) }
        return
    }
    val resolved = remember(url, apiBase) { resolveAssetUrl(apiBase, url) }
    val bitmap by produceState<Bitmap?>(initialValue = null, resolved) {
        value = runCatching { api.downloadBitmap(resolved) }.getOrNull()
    }
    if (bitmap == null) {
        Box(Modifier.fillMaxWidth().aspectRatio(aspect).background(DollUi.Placeholder), contentAlignment = Alignment.Center) {
            Text("加载图片", color = DollUi.Muted)
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
        value = if (uri == null) null else withContext(Dispatchers.IO) { context.contentResolver.openInputStream(uri)?.use { input -> BitmapFactory.decodeStream(input) } }
    }
    Box(Modifier.fillMaxWidth().height(360.dp).background(DollUi.Placeholder).clickable(onClick = onPick), contentAlignment = Alignment.Center) {
        if (bitmap == null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("选择一张照片", color = DollUi.Ink, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(color = DollUi.Coral, shape = RoundedCornerShape(4.dp), modifier = Modifier.clickable(onClick = onPick)) { Text("相册", color = Color.White, modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp), fontWeight = FontWeight.Bold) }
                    Surface(color = Color.White, shape = RoundedCornerShape(4.dp), border = BorderStroke(1.dp, DollUi.Hairline), modifier = Modifier.clickable(onClick = onCamera)) { Text("拍摄", color = DollUi.Ink, modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp), fontWeight = FontWeight.Bold) }
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
            Box(Modifier.size(42.dp).clip(CircleShape).background(DollUi.BlueSoft), contentAlignment = Alignment.Center) {
                Text("点", color = DollUi.Blue, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(record.placeName, color = DollUi.Ink, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(formatCoordinate(record.latitude, record.longitude), color = DollUi.Muted, style = MaterialTheme.typography.bodySmall)
            }
            Text(shortDate(record.takenAt ?: record.createdAt), color = DollUi.Muted, style = MaterialTheme.typography.bodySmall)
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
    JournalCard(modifier = modifier, containerColor = DollUi.Surface) {
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
                Text(title, color = DollUi.Ink, fontWeight = FontWeight.Black, maxLines = 1)
                Text(subtitle, color = DollUi.Muted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun LibraryGroup(title: String, chips: List<String>, selected: String, onChip: (String) -> Unit) {
    JournalCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, color = DollUi.Ink, fontWeight = FontWeight.Black)
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
        primary = DollUi.Coral,
        secondary = DollUi.Green,
        tertiary = DollUi.Blue,
        background = DollUi.Background,
        surface = DollUi.Surface,
        onPrimary = DollUi.Paper,
        onSecondary = DollUi.Paper,
        onSurface = DollUi.Ink,
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

private fun apiUrl(apiBase: String, path: String): String = apiBase.trim().trimEnd('/') + path

private fun resolveAssetUrl(apiBase: String, url: String): String = when {
    url.startsWith("http://") || url.startsWith("https://") -> url
    url.startsWith("/") -> apiBase.trim().trimEnd('/') + url
    else -> apiBase.trim().trimEnd('/') + "/" + url
}

private fun parseAuthUser(json: JSONObject, fallbackUsage: Long? = null, fallbackQuota: Long? = null): AuthUser {
    return AuthUser(
        id = json.getInt("id"),
        displayName = json.nullableString("display_name") ?: json.nullableString("username") ?: "用户",
        username = json.nullableString("username"),
        email = json.nullableString("email"),
        roles = parseStringArray(json.optJSONArray("roles")),
        storageUsageBytes = json.optLong("storage_usage_bytes", fallbackUsage ?: 0L),
        storageQuotaBytes = json.optLong("storage_quota_bytes", fallbackQuota ?: 0L),
        avatarUrl = json.nullableString("avatar_url"),
    )
}

private fun parseMessage(json: JSONObject): MessageItem {
    return MessageItem(
        id = json.optString("id", "message-${json.optString("kind", "item")}"),
        kind = json.optString("kind", "system"),
        title = json.optString("title", "消息"),
        body = json.optString("body", ""),
        priority = json.optString("priority", "normal"),
        createdAt = json.nullableString("created_at"),
    )
}

private fun parseTagCatalog(json: JSONObject): TagCatalog {
    val groupsJson = json.optJSONArray("groups")
    val groups = buildList {
        if (groupsJson != null) {
            for (index in 0 until groupsJson.length()) add(parseTagGroup(groupsJson.getJSONObject(index)))
        }
    }
    return TagCatalog(
        groups = compactTagGroups(groups).ifEmpty { FALLBACK_TAG_GROUPS },
        total = json.optInt("total", groups.sumOf { it.tags.size }),
    )
}

private fun parseTagGroup(json: JSONObject): TagGroupItem {
    val id = json.optString("id", json.optString("code", "custom")).ifBlank { "custom" }
    val tagsJson = json.optJSONArray("tags")
    val tags = buildList {
        if (tagsJson != null) {
            for (index in 0 until tagsJson.length()) add(parseTagItem(tagsJson.getJSONObject(index), id))
        }
    }
    return TagGroupItem(
        id = id,
        title = json.optString("title", id),
        description = json.optString("description", ""),
        sortOrder = json.optInt("sort_order", 0),
        tags = tags,
    )
}

private fun parseTagItem(json: JSONObject, fallbackGroup: String): TagItem {
    return TagItem(
        name = json.optString("name"),
        count = json.optInt("count", 0),
        source = json.optString("source", "default"),
        groupCode = json.optString("group_code", fallbackGroup),
        slug = json.nullableString("slug"),
    )
}

private fun parseAlbum(json: JSONObject): AlbumItem {
    return AlbumItem(
        id = json.optString("id"),
        type = json.optString("album_type", "smart"),
        title = json.optString("title", "合集"),
        description = json.optString("description", ""),
        itemCount = json.optInt("item_count", 0),
    )
}

private fun parseMapCell(json: JSONObject, scope: String): MapCellItem {
    return MapCellItem(
        id = json.optString("id"),
        scope = scope,
        photoCount = json.optInt("photo_count", 0),
        checkinCount = json.optInt("checkin_count", 0),
    )
}

private fun parseLibrarySummary(json: JSONObject): LibrarySummary {
    val albumsJson = json.optJSONObject("albums")?.optJSONArray("items")
    val albums = buildList {
        if (albumsJson != null) {
            for (index in 0 until albumsJson.length()) add(parseAlbum(albumsJson.getJSONObject(index)))
        }
    }
    val mapJson = json.optJSONObject("map")?.optJSONObject("personal_cells")
    val storageJson = json.optJSONObject("storage")
    val capabilities = storageJson?.optJSONObject("asset_capabilities")
    return LibrarySummary(
        albums = albums,
        personalCells = mapJson?.optInt("total", 0) ?: 0,
        queuedJobs = json.optJSONObject("jobs")?.optInt("queued", 0) ?: 0,
        storageUsageBytes = storageJson?.optLong("usage_bytes", 0L) ?: 0L,
        storageProvider = capabilities?.optString("provider", "") ?: "",
    )
}

private fun parseCheckin(json: JSONObject): CheckinItem {
    val assets = json.optJSONArray("assets")
    var originalUrl: String? = null
    if (assets != null) {
        for (index in 0 until assets.length()) {
            val asset = assets.optJSONObject(index) ?: continue
            if (asset.optString("variant") == "original") {
                originalUrl = asset.nullableString("url") ?: asset.nullableString("signed_url")
                break
            }
        }
    }
    return CheckinItem(
        id = json.getString("id"),
        placeName = json.optString("place_name", json.optJSONObject("place")?.optString("name") ?: "未命名地点"),
        note = json.optString("note", ""),
        latitude = json.optDouble("latitude", 0.0),
        longitude = json.optDouble("longitude", 0.0),
        tags = parseStringArray(json.optJSONArray("tags")),
        createdAt = json.nullableString("created_at"),
        takenAt = json.nullableString("taken_at"),
        source = json.optString("source", ""),
        visibility = json.optString("visibility", "private"),
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

private fun compactTagGroups(groups: List<TagGroupItem>): List<TagGroupItem> {
    return groups
        .sortedWith(compareBy<TagGroupItem> { it.sortOrder }.thenBy { it.title })
        .map { group ->
            val seen = mutableSetOf<String>()
            group.copy(tags = group.tags.filter { tag -> tag.name.isNotBlank() && seen.add(tag.name) })
        }
        .filter { it.tags.isNotEmpty() }
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
        .filter { it.isNotBlank() && looksLikeCollectionTag(it) }
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
    val source = filters.source.trim()
    return records.filter { record ->
        val searchable = buildString {
            append(record.placeName).append(' ')
            append(record.note).append(' ')
            append(record.tags.joinToString(" "))
        }.lowercase()
        (query.isBlank() || query in searchable) &&
            (tag.isBlank() || record.tags.any { it.lowercase() == tag }) &&
            (place.isBlank() || record.placeName.lowercase() == place) &&
            (source.isBlank() || record.source == source)
    }
}

private fun sourceLabel(source: String): String = when (source) {
    "android_capture" -> "手机记录"
    "new_capture" -> "新拍记录"
    "historical_import" -> "历史补录"
    "" -> "全部"
    else -> source
}

private fun sourceValue(label: String): String = when (label) {
    "手机记录" -> "android_capture"
    "新拍记录" -> "new_capture"
    "历史补录" -> "historical_import"
    else -> ""
}

private fun visibilityLabel(visibility: String): String = when (visibility) {
    "private" -> "私密"
    "friends" -> "好友"
    "public" -> "公开"
    else -> visibility.ifBlank { "私密" }
}

private fun looksLikeCollectionTag(tag: String): Boolean {
    val text = tag.lowercase()
    return "bang" in text || "mygo" in text || "ave" in text || "ip" in text || "角色" in tag || "活动" in tag || "团" in tag
}

private fun looksLikeExpiredToken(error: Throwable): Boolean {
    val text = error.message.orEmpty()
    return "访问令牌" in text || "401" in text || "token" in text.lowercase()
}

private data class MapPoint(val latitude: Double, val longitude: Double)

private fun toAmapPoint(latitude: Double, longitude: Double): MapPoint {
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

private fun mapRatio(value: Double, salt: Int): Float {
    val mixed = abs(value * 997.0 + salt * 37.0)
    return ((mixed % 100.0) / 100.0).toFloat()
}

private fun formatCoordinate(latitude: Double, longitude: Double): String = "%.5f, %.5f".format(latitude, longitude)

private fun createCaptureUri(context: Context): Uri {
    val dir = File(context.cacheDir, "capture").apply { mkdirs() }
    val file = File(dir, "doll-checkin-${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

private fun lastKnownLocation(context: Context): Location? {
    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    if (!fine && !coarse) return null
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return manager.getProviders(true)
        .mapNotNull { provider -> runCatching { manager.getLastKnownLocation(provider) }.getOrNull() }
        .maxByOrNull { it.time }
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
