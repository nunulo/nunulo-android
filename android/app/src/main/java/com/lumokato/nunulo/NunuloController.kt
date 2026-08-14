package com.lumokato.nunulo

import android.content.Context
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

private const val CONTROLLER_LOG_TAG = "NunuloAndroid"

internal data class RecordCollection(
    val title: String,
    val subtitle: String = "",
    val filters: Map<String, String> = emptyMap(),
    val checkinIds: List<String> = emptyList(),
)

internal class DetailRequestGate {
    private var version = 0

    fun next(): Int = ++version

    fun invalidate() {
        version += 1
    }

    fun isCurrent(requestVersion: Int, selectedId: String?, targetId: String): Boolean =
        requestVersion == version && selectedId == targetId
}

internal data class LoadedPart<T>(val value: T, val error: String?)

internal fun <T> Result<T>.withFallback(fallback: T, failureLabel: String): LoadedPart<T> = fold(
    onSuccess = { LoadedPart(it, null) },
    onFailure = { LoadedPart(fallback, it.message?.takeIf(String::isNotBlank) ?: failureLabel) },
)

internal data class PartialItems<T>(val items: List<T>, val failedCount: Int, val firstError: Throwable?)

internal fun <T> List<Result<T>>.successfulItems(): PartialItems<T> = PartialItems(
    items = mapNotNull(Result<T>::getOrNull),
    failedCount = count(Result<T>::isFailure),
    firstError = firstNotNullOfOrNull(Result<T>::exceptionOrNull),
)

private data class LoadedState(
    val user: AuthUser,
    val feed: List<CheckinItem>,
    val mine: List<CheckinItem>,
    val discovery: DiscoveryState,
    val partners: List<PartnerItem>,
    val partnerRequests: List<PartnerRequestItem>,
    val footprint: FootprintState,
    val notifications: List<NotificationItem>,
    val people: List<PersonItem>,
    val albums: List<AlbumItem>,
    val exports: List<ExportItem>,
    val places: List<PlaceItem>,
    val eventSeries: List<EventSeriesItem>,
    val screenErrors: Map<AppTab, String>,
    val notificationsError: String?,
)

internal class NunuloController(
    private val context: Context,
    private val preferences: SharedPreferences,
    private val api: NunuloApi,
    private val coroutineScope: CoroutineScope,
) {
    private val apiBase = BuildConfig.NUNULO_API_BASE_URL.trim().trimEnd('/')
    private val tokenRefreshCoordinator = TokenRefreshCoordinator()
    private var accessToken by mutableStateOf(preferences.getString("accessToken", "").orEmpty())
    private var refreshToken by mutableStateOf(preferences.getString("refreshToken", "").orEmpty())
    private var pendingLocationPurpose: LocationPurpose = LocationPurpose.Draft

    val loggedIn: Boolean get() = accessToken.isNotBlank()
    var requestLocationPermission by mutableStateOf(false)
        private set
    var activeTab by mutableStateOf(runCatching { AppTab.valueOf(preferences.getString("activeTab", AppTab.Feed.name) ?: AppTab.Feed.name) }.getOrDefault(AppTab.Feed))
        private set
    var feedScope by mutableStateOf(runCatching { FeedScope.valueOf(preferences.getString("feedScope", FeedScope.Discover.name) ?: FeedScope.Discover.name) }.getOrDefault(FeedScope.Discover))
        private set
    var feedOrder by mutableStateOf(runCatching { FeedOrder.valueOf(preferences.getString("feedOrder", FeedOrder.Popular.name) ?: FeedOrder.Popular.name) }.getOrDefault(FeedOrder.Popular))
        private set
    var currentUser by mutableStateOf<AuthUser?>(null)
        private set
    var publicConfig by mutableStateOf<PublicConfig?>(null)
        private set
    var feedItems by mutableStateOf<List<CheckinItem>>(emptyList())
        private set
    var mineItems by mutableStateOf<List<CheckinItem>>(emptyList())
        private set
    var discovery by mutableStateOf(DiscoveryState())
        private set
    var partners by mutableStateOf<List<PartnerItem>>(emptyList())
        private set
    var partnerRequests by mutableStateOf<List<PartnerRequestItem>>(emptyList())
        private set
    var footprint by mutableStateOf(FootprintState(null, emptyList()))
        private set
    var notifications by mutableStateOf<List<NotificationItem>>(emptyList())
        private set
    var people by mutableStateOf<List<PersonItem>>(emptyList())
        private set
    var albums by mutableStateOf<List<AlbumItem>>(emptyList())
        private set
    var exports by mutableStateOf<List<ExportItem>>(emptyList())
        private set
    var places by mutableStateOf<List<PlaceItem>>(emptyList())
        private set
    var eventSeries by mutableStateOf<List<EventSeriesItem>>(emptyList())
        private set
    var selectedRecord by mutableStateOf<CheckinItem?>(null)
        private set
    var recordDetailLoading by mutableStateOf(false)
        private set
    var recordDetailError by mutableStateOf<String?>(null)
        private set
    var comments by mutableStateOf<List<CommentItem>>(emptyList())
        private set
    var commentsLoading by mutableStateOf(false)
        private set
    var commentsError by mutableStateOf<String?>(null)
        private set
    var selectedPartner by mutableStateOf<PartnerItem?>(null)
        private set
    var partnerDetailLoading by mutableStateOf(false)
        private set
    var partnerDetailError by mutableStateOf<String?>(null)
        private set
    var selectedPartnerMeetings by mutableStateOf<List<PartnerMeetingItem>>(emptyList())
        private set
    var partnerMeetingsLoading by mutableStateOf(false)
        private set
    var partnerMeetingsError by mutableStateOf<String?>(null)
        private set
    var collection by mutableStateOf<RecordCollection?>(null)
        private set
    var collectionLoading by mutableStateOf(false)
        private set
    var collectionError by mutableStateOf<String?>(null)
        private set
    var notificationsOpen by mutableStateOf(false)
    var draft by mutableStateOf(UploadDraft())
        private set
    var currentLocation by mutableStateOf<MapPoint?>(null)
        private set
    var avatarUri by mutableStateOf<Uri?>(null)
        private set
    var inviteCode by mutableStateOf("")
        private set
    var exportCreating by mutableStateOf(false)
        private set
    var exportDownloadingId by mutableStateOf<String?>(null)
        private set
    var inviteCreating by mutableStateOf(false)
        private set
    var screenErrors by mutableStateOf<Map<AppTab, String>>(emptyMap())
        private set
    var notificationsError by mutableStateOf<String?>(null)
        private set
    var likingRecordIds by mutableStateOf<Set<String>>(emptySet())
        private set
    var commentingRecordIds by mutableStateOf<Set<String>>(emptySet())
        private set
    var reportingRecordIds by mutableStateOf<Set<String>>(emptySet())
        private set
    var partnerRequestRecordIds by mutableStateOf<Set<String>>(emptySet())
        private set
    var resolvingPartnerRequestKeys by mutableStateOf<Set<String>>(emptySet())
        private set
    var deletingRecordIds by mutableStateOf<Set<String>>(emptySet())
        private set
    var mutatingPartnerRelationKeys by mutableStateOf<Set<String>>(emptySet())
        private set
    var catalogCandidateCreating by mutableStateOf(false)
        private set
    var placeCreating by mutableStateOf(false)
        private set
    var albumCreating by mutableStateOf(false)
        private set
    var message by mutableStateOf("记录、发现并整理你的伙伴足迹")
        private set
    var syncError by mutableStateOf<String?>(null)
        private set
    var busy by mutableStateOf(false)
        private set
    var stateReady by mutableStateOf(false)
        private set
    private var draftRequestId by mutableStateOf(UUID.randomUUID().toString())
    private val recordDetailRequests = DetailRequestGate()
    private val partnerDetailRequests = DetailRequestGate()
    private val collectionRequests = DetailRequestGate()

    internal val mediaApi: NunuloApi get() = api
    internal val baseUrl: String get() = apiBase

    suspend fun initialize() {
        publicConfig = runCatching { api.publicConfig(apiBase) }.getOrNull()
        loadPendingUpload(preferences)?.let { pending ->
            draft = pending.draft
            draftRequestId = pending.requestId
            activeTab = AppTab.Capture
            message = if (pending.attempted) "上次发布被中断，已成功照片会按 checksum 复用" else "未完成草稿已恢复"
        }
        api.setAccessToken(accessToken)
        if (loggedIn) refreshAll("已恢复登录并同步内容")
    }

    private fun persistTokens(newAccessToken: String, newRefreshToken: String = refreshToken) {
        accessToken = newAccessToken
        refreshToken = newRefreshToken
        api.setAccessToken(newAccessToken)
        preferences.edit().putString("accessToken", newAccessToken).putString("refreshToken", newRefreshToken).apply()
    }

    private suspend fun <T> authed(block: suspend (String) -> T): T = tokenRefreshCoordinator.run(
        currentAccessToken = { accessToken },
        currentRefreshToken = { refreshToken },
        persistAccessToken = { persistTokens(it) },
        refreshAccessToken = { api.refreshAccessToken(apiBase, it) },
        block = block,
    )

    private suspend fun loadState(): LoadedState = authed { token ->
        coroutineScope {
        val userRequest = async { runCatching { api.me(apiBase, token) } }
        val feedRequest = async { runCatching { api.listCheckins(apiBase, token, feedScope, feedOrder) } }
        val mineRequest = if (feedScope == FeedScope.Mine) null else async { runCatching { api.listCheckins(apiBase, token, FeedScope.Mine, FeedOrder.Latest) } }
        val discoveryRequest = async { runCatching { api.discovery(apiBase, token) } }
        val catalogRequest = async {
            runCatching {
                listOf("item_type", "work", "group", "character").associateWith { entityType ->
                    api.listCatalog(apiBase, token, entityType)
                }
            }
        }
        val partnersRequest = async { runCatching { api.listPartners(apiBase, token) } }
        val partnerRequestsRequest = async { runCatching { api.listPartnerRequests(apiBase, token) } }
        val footprintRequest = async { runCatching { api.footprint(apiBase, token) } }
        val notificationsRequest = async { runCatching { api.listNotifications(apiBase, token) } }
        val peopleRequest = async { runCatching { api.listPeople(apiBase, token) } }
        val albumsRequest = async { runCatching { api.listAlbums(apiBase, token) } }
        val exportsRequest = async { runCatching { api.listExports(apiBase, token) } }
        val placesRequest = async { runCatching { api.listPlaces(apiBase, token) } }
        val eventsRequest = async { runCatching { api.listEvents(apiBase, token) } }
        val eventSeriesRequest = async { runCatching { api.listEventSeries(apiBase, token) } }

        val userResult = userRequest.await()
        val feedResult = feedRequest.await()
        val mineResult = mineRequest?.await() ?: feedResult
        val discoveryResult = discoveryRequest.await()
        val catalogResult = catalogRequest.await()
        val partnersResult = partnersRequest.await()
        val partnerRequestsResult = partnerRequestsRequest.await()
        val footprintResult = footprintRequest.await()
        val notificationsResult = notificationsRequest.await()
        val peopleResult = peopleRequest.await()
        val albumsResult = albumsRequest.await()
        val exportsResult = exportsRequest.await()
        val placesResult = placesRequest.await()
        val eventsResult = eventsRequest.await()
        val eventSeriesResult = eventSeriesRequest.await()

        listOf(
            userResult,
            feedResult,
            mineResult,
            discoveryResult,
            catalogResult,
            partnersResult,
            partnerRequestsResult,
            footprintResult,
            notificationsResult,
            peopleResult,
            albumsResult,
            exportsResult,
            placesResult,
            eventsResult,
            eventSeriesResult,
        ).firstNotNullOfOrNull { result -> result.exceptionOrNull()?.takeIf(::looksLikeExpiredToken) }?.let { throw it }

        val nextErrors = linkedMapOf<AppTab, String>()
        fun <T> value(result: Result<T>, fallback: T, tab: AppTab, label: String): T {
            val part = result.withFallback(fallback, "$label 加载失败")
            part.error?.let { nextErrors.putIfAbsent(tab, it) }
            return part.value
        }

        val nextFeed = value(feedResult, feedItems, AppTab.Feed, "动态")
        val nextMine = value(mineResult, mineItems, AppTab.Profile, "我的记录")
        val nextDiscovery = value(discoveryResult, discovery, AppTab.Discover, "发现内容")
        val catalogFallback = discovery.catalog.ifEmpty { nextDiscovery.catalog }
        val nextCatalog = value(catalogResult, catalogFallback, AppTab.Discover, "作品与角色目录")
        val nextPartners = value(partnersResult, partners, AppTab.Partners, "伙伴")
        val nextPartnerRequests = value(partnerRequestsResult, partnerRequests, AppTab.Partners, "伙伴补登记")
        val nextFootprint = value(footprintResult, footprint, AppTab.Profile, "个人足迹")
        val nextPeople = value(peopleResult, people, AppTab.Profile, "成员与关注")
        val nextAlbums = value(albumsResult, albums, AppTab.Profile, "合集")
        val nextExports = value(exportsResult, exports, AppTab.Profile, "数据导出")
        val nextPlaces = value(placesResult, places, AppTab.Discover, "地点")
        val nextEvents = value(eventsResult, nextDiscovery.events, AppTab.Discover, "活动目录")
        val nextEventSeries = value(eventSeriesResult, eventSeries, AppTab.Discover, "活动系列")
        val nextNotifications = notificationsResult.withFallback(notifications, "通知加载失败")
        listOfNotNull(nextErrors[AppTab.Discover], nextErrors[AppTab.Partners]).firstOrNull()?.let { nextErrors[AppTab.Capture] = it }

        LoadedState(
            user = userResult.getOrThrow(),
            feed = nextFeed,
            mine = nextMine,
            discovery = nextDiscovery.copy(catalog = nextCatalog, events = nextEvents),
            partners = nextPartners,
            partnerRequests = nextPartnerRequests,
            footprint = nextFootprint,
            notifications = nextNotifications.value,
            people = nextPeople,
            albums = nextAlbums,
            exports = nextExports,
            places = nextPlaces,
            eventSeries = nextEventSeries,
            screenErrors = nextErrors,
            notificationsError = nextNotifications.error,
        )
        }
    }

    private fun applyState(state: LoadedState) {
        currentUser = state.user
        feedItems = state.feed
        mineItems = state.mine
        discovery = state.discovery
        partners = state.partners
        partnerRequests = state.partnerRequests
        footprint = state.footprint
        notifications = state.notifications
        people = state.people
        albums = state.albums
        exports = state.exports
        places = state.places
        eventSeries = state.eventSeries
        screenErrors = state.screenErrors
        notificationsError = state.notificationsError
        clearCollectionLoadState()
        collection = null
        stateReady = true
    }

    fun refreshAll(nextMessage: String = "已同步最新内容") {
        if (!loggedIn) return
        busy = true
        coroutineScope.launch {
            try {
                val state = loadState()
                applyState(state)
                syncError = null
                message = if (state.screenErrors.isEmpty() && state.notificationsError == null) nextMessage else "已更新可用内容；部分页面可重新同步"
            } catch (error: Exception) {
                if (looksLikeExpiredToken(error)) {
                    logout("登录已过期，请重新登录")
                } else {
                    syncError = error.message ?: "同步失败"
                    message = syncError.orEmpty()
                }
            } finally {
                busy = false
            }
        }
    }

    fun selectTab(tab: AppTab) {
        activeTab = tab
        preferences.edit().putString("activeTab", tab.name).apply()
        if (tab == AppTab.Capture && draft.latitude.isBlank() && draft.longitude.isBlank()) requestLocation(LocationPurpose.Draft)
    }

    fun screenError(tab: AppTab): String? = screenErrors[tab] ?: syncError

    fun consumeMessage(value: String) {
        if (message == value) message = ""
    }

    fun requestLocation(purpose: LocationPurpose) {
        pendingLocationPurpose = purpose
        if (!hasLocationPermission(context)) {
            requestLocationPermission = true
        } else {
            coroutineScope.launch { applyLocation(purpose, currentLocation(context)) }
        }
    }

    fun consumeLocationPermissionRequest() {
        requestLocationPermission = false
    }

    fun onLocationPermissionResult(granted: Boolean) {
        if (!granted) {
            message = "未开启定位权限，仍可使用照片 GNSS、地图选点或无地点发布"
            return
        }
        coroutineScope.launch { applyLocation(pendingLocationPurpose, currentLocation(context)) }
    }

    private fun applyLocation(purpose: LocationPurpose, fix: LocationFix?) {
        if (fix == null) {
            message = "暂时拿不到当前位置，可稍后重试或手动选点"
            return
        }
        val point = MapPoint(fix.latitude, fix.longitude)
        currentLocation = point
        if (purpose == LocationPurpose.Draft && draft.locationSource !in setOf("photo_exif", "map", "map_picker", "manual")) {
            draft = draft.copy(
                latitude = "%.6f".format(point.latitude),
                longitude = "%.6f".format(point.longitude),
                locationSource = if (fix.isLastKnown) "device_last_known" else "device_current",
                locationProvider = fix.provider,
                locationCapturedAtMillis = fix.capturedAtMillis,
                locationAccuracyMeters = fix.accuracyMeters,
            )
        }
        val quality = listOfNotNull(fix.provider.takeIf(String::isNotBlank), fix.accuracyMeters?.let { "约 ${it.toInt()} 米" }).joinToString(" · ")
        message = when (purpose) {
            LocationPurpose.Draft -> if (fix.isLastKnown) "仅取得较早的位置（$quality），请确认后使用" else "已取得设备位置（$quality）；照片含 GNSS 时会优先采用照片位置"
            LocationPurpose.Home -> "已取得当前位置（$quality），确认后可登记为家位置"
            LocationPurpose.Place -> "已取得当前位置（$quality），确认后可保存为活动地点"
        }
    }

    fun updateDraft(value: UploadDraft) {
        draft = value
        persistDraft()
    }

    private fun persistDraft(attempted: Boolean = false) {
        if (draft.photos.isEmpty()) {
            clearPendingUpload(preferences)
            return
        }
        runCatching { savePendingUpload(preferences, PendingUpload(draftRequestId, draft, attempted)) }
            .onFailure { Log.e(CONTROLLER_LOG_TAG, "Failed to persist draft", it) }
    }

    fun addMedia(uris: List<Uri>, source: String) {
        val selected = uris.take(9 - draft.photos.size)
        if (selected.isEmpty()) {
            message = "一条记录最多 9 张照片"
            return
        }
        busy = true
        coroutineScope.launch {
            try {
                val additions = selected.map { sourceUri ->
                    runCatching { context.contentResolver.takePersistableUriPermission(sourceUri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                    val cachedUri = withContext(Dispatchers.IO) { cacheSelectedMedia(context, sourceUri) }
                    if (source == "camera") runCatching { context.contentResolver.delete(sourceUri, null, null) }
                    DraftPhotoItem(
                        localUri = cachedUri,
                        captureSource = source,
                    )
                }
                draft = draft.copy(photos = draft.photos + additions)
                persistDraft()
                for (item in additions) uploadDraftPhoto(item.key)
                message = if (selected.size < uris.size) "已保留前 ${selected.size} 张；一条记录最多 9 张" else "照片已就绪，可补充伙伴、类别、活动和地点"
            } catch (error: Exception) {
                message = error.message ?: "无法读取图片"
            } finally {
                busy = false
            }
        }
    }

    private suspend fun uploadDraftPhoto(key: String) {
        val target = draft.photos.firstOrNull { it.key == key } ?: return
        val uri = target.localUri ?: return
        try {
            draft = draft.updatePhoto(key) { it.copy(status = "validating", progress = 0, error = null) }
            val checksum = withContext(Dispatchers.IO) { sha256Hex(context.contentResolver, uri) }
            draft = draft.updatePhoto(key) { it.copy(status = "uploading", checksum = checksum, progress = 0) }
            val uploaded = authed { token ->
                api.uploadPhoto(context, apiBase, token, uri, checksum) { progress ->
                    coroutineScope.launch(Dispatchers.Main.immediate) {
                        draft = draft.updatePhoto(key) { item -> item.copy(progress = progress) }
                    }
                }
            }
            draft = draft.updatePhoto(key) { it.copy(photo = uploaded, status = "ready", progress = 100, error = null) }
            if (uploaded.exifTakenAt != null && draft.takenAt.isBlank()) draft = draft.copy(takenAt = uploaded.exifTakenAt)
            if (uploaded.exifLatitude != null && uploaded.exifLongitude != null && draft.locationSource !in setOf("map", "map_picker", "manual")) {
                draft = draft.copy(
                    latitude = uploaded.exifLatitude.toString(),
                    longitude = uploaded.exifLongitude.toString(),
                    locationSource = "photo_exif",
                    locationProvider = "exif",
                    locationCapturedAtMillis = null,
                    locationAccuracyMeters = null,
                )
            }
            persistDraft()
        } catch (error: Exception) {
            draft = draft.updatePhoto(key) { it.copy(status = "error", progress = 0, error = error.message ?: "上传失败") }
            persistDraft()
            Log.e(CONTROLLER_LOG_TAG, "Photo upload failed: $uri", error)
        }
    }

    fun retryPhoto(key: String) {
        coroutineScope.launch { uploadDraftPhoto(key) }
    }

    fun removePhoto(key: String) {
        val removed = draft.photos.firstOrNull { it.key == key }
        deleteCachedMedia(context, removed?.localUri)
        draft = draft.copy(photos = draft.photos.filterNot { it.key == key })
        persistDraft()
    }

    fun movePhoto(key: String, direction: Int) {
        draft = draft.movePhoto(key, direction)
        persistDraft()
    }

    fun clearDraft() {
        draft.photos.forEach { deleteCachedMedia(context, it.localUri) }
        draft = UploadDraft()
        draftRequestId = UUID.randomUUID().toString()
        clearPendingUpload(preferences)
        message = "草稿已清除"
    }

    fun cancelCapture(uri: Uri?) {
        uri?.let { runCatching { context.contentResolver.delete(it, null, null) } }
        message = "拍照已取消"
    }

    fun login(login: String, password: String) {
        busy = true
        coroutineScope.launch {
            try {
                val (user, tokens) = api.login(apiBase, login, password)
                persistTokens(tokens.accessToken, tokens.refreshToken.orEmpty())
                currentUser = user
                preferences.edit().putString("lastLogin", login).apply()
                applyState(loadState())
                selectTab(AppTab.Feed)
                message = "欢迎回来，${user.displayName}"
            } catch (error: Exception) {
                message = error.message ?: "登录失败"
                if (loggedIn) syncError = message
            } finally {
                busy = false
            }
        }
    }

    fun register(username: String, displayName: String, invite: String, password: String, acceptedPolicies: Boolean) {
        busy = true
        coroutineScope.launch {
            try {
                val (user, tokens) = api.register(apiBase, username, displayName, invite, password, acceptedPolicies, publicConfig)
                persistTokens(tokens.accessToken, tokens.refreshToken.orEmpty())
                currentUser = user
                applyState(loadState())
                selectTab(AppTab.Feed)
                message = "账号已创建，欢迎 ${user.displayName}"
            } catch (error: Exception) {
                message = error.message ?: "注册失败"
                if (loggedIn) syncError = message
            } finally {
                busy = false
            }
        }
    }

    fun logout(nextMessage: String = "已退出登录") {
        accessToken = ""
        refreshToken = ""
        api.setAccessToken("")
        currentUser = null
        feedItems = emptyList()
        mineItems = emptyList()
        discovery = DiscoveryState()
        partners = emptyList()
        partnerRequests = emptyList()
        footprint = FootprintState(null, emptyList())
        notifications = emptyList()
        people = emptyList()
        albums = emptyList()
        exports = emptyList()
        places = emptyList()
        eventSeries = emptyList()
        selectedRecord = null
        clearRecordDetailState()
        selectedPartner = null
        clearPartnerDetailState()
        collection = null
        clearCollectionLoadState()
        syncError = null
        screenErrors = emptyMap()
        notificationsError = null
        likingRecordIds = emptySet()
        commentingRecordIds = emptySet()
        reportingRecordIds = emptySet()
        partnerRequestRecordIds = emptySet()
        resolvingPartnerRequestKeys = emptySet()
        deletingRecordIds = emptySet()
        mutatingPartnerRelationKeys = emptySet()
        catalogCandidateCreating = false
        placeCreating = false
        albumCreating = false
        stateReady = false
        preferences.edit().remove("accessToken").remove("refreshToken").apply()
        message = nextMessage
    }

    fun loadFeed(nextScope: FeedScope = feedScope, nextOrder: FeedOrder = feedOrder) {
        feedScope = nextScope
        feedOrder = nextOrder
        collection = null
        clearCollectionLoadState()
        preferences.edit().putString("feedScope", nextScope.name).putString("feedOrder", nextOrder.name).apply()
        busy = true
        coroutineScope.launch {
            try {
                feedItems = authed { token -> api.listCheckins(apiBase, token, nextScope, nextOrder) }
                if (nextScope == FeedScope.Mine) mineItems = feedItems
                syncError = null
                message = "已切换到${nextScope.label} · ${nextOrder.label}"
            } catch (error: Exception) {
                syncError = error.message ?: "动态加载失败"
                message = syncError.orEmpty()
            } finally {
                busy = false
            }
        }
    }

    fun openCollection(target: RecordCollection) {
        selectedRecord = null
        clearRecordDetailState()
        selectedPartner = null
        clearPartnerDetailState()
        collection = target
        selectTab(AppTab.Feed)
        feedItems = emptyList()
        loadCollection(target)
    }

    fun reloadCollection() {
        collection?.let(::loadCollection)
    }

    private fun loadCollection(target: RecordCollection) {
        val requestVersion = collectionRequests.next()
        collectionLoading = true
        collectionError = null
        busy = true
        coroutineScope.launch {
            try {
                val loaded = authed { token ->
                    if (target.checkinIds.isNotEmpty()) {
                        val results = target.checkinIds.map { id -> runCatching { api.getCheckin(apiBase, token, id) } }
                        results.firstNotNullOfOrNull { it.exceptionOrNull()?.takeIf(::looksLikeExpiredToken) }?.let { throw it }
                        val partial = results.successfulItems()
                        if (partial.items.isEmpty() && partial.firstError != null) throw partial.firstError
                        partial
                    } else {
                        PartialItems(api.listCheckins(apiBase, token, FeedScope.Discover, FeedOrder.Latest, target.filters), 0, null)
                    }
                }
                if (!collectionRequests.isCurrent(requestVersion, collection?.title, target.title)) return@launch
                feedItems = loaded.items
                if (loaded.failedCount > 0) collectionError = "${loaded.failedCount} 条记录暂时无法读取；已显示其余内容"
                message = "已打开 ${target.title}"
            } catch (error: Exception) {
                if (!collectionRequests.isCurrent(requestVersion, collection?.title, target.title)) return@launch
                collectionError = error.message ?: "聚合内容加载失败"
                message = collectionError.orEmpty()
            } finally {
                if (collectionRequests.isCurrent(requestVersion, collection?.title, target.title)) {
                    collectionLoading = false
                    busy = false
                }
            }
        }
    }

    private fun clearCollectionLoadState() {
        collectionRequests.invalidate()
        collectionLoading = false
        collectionError = null
    }

    fun openRecord(record: CheckinItem) {
        selectedRecord = record
        comments = emptyList()
        loadRecordDetails(record)
    }

    fun reloadRecordDetails() {
        selectedRecord?.let(::loadRecordDetails)
    }

    private fun loadRecordDetails(record: CheckinItem) {
        val requestVersion = recordDetailRequests.next()
        recordDetailLoading = true
        recordDetailError = null
        commentsLoading = true
        commentsError = null
        coroutineScope.launch {
            val detailResult = runCatching { authed { token -> api.getCheckin(apiBase, token, record.id) } }
            if (!recordDetailRequests.isCurrent(requestVersion, selectedRecord?.id, record.id)) return@launch
            detailResult
                .onSuccess { selectedRecord = it }
                .onFailure { recordDetailError = it.message?.takeIf(String::isNotBlank) ?: "记录完整内容加载失败" }
            recordDetailLoading = false
        }
        coroutineScope.launch {
            val commentsResult = runCatching { authed { token -> api.listComments(apiBase, token, record.id) } }
            if (!recordDetailRequests.isCurrent(requestVersion, selectedRecord?.id, record.id)) return@launch
            commentsResult
                .onSuccess { comments = it }
                .onFailure { commentsError = it.message?.takeIf(String::isNotBlank) ?: "评论加载失败" }
            commentsLoading = false
        }
    }

    fun closeRecord() {
        selectedRecord = null
        clearRecordDetailState()
    }

    private fun clearRecordDetailState() {
        recordDetailRequests.invalidate()
        comments = emptyList()
        recordDetailLoading = false
        recordDetailError = null
        commentsLoading = false
        commentsError = null
    }

    fun hasConflictingDraft(record: CheckinItem): Boolean = draft.conflictsWithRecordEdit(record.id)

    fun editRecord(record: CheckinItem, replaceConflictingDraft: Boolean = false) {
        if (hasConflictingDraft(record) && !replaceConflictingDraft) {
            message = "已有未完成草稿，请先确认是否替换"
            return
        }
        if (hasConflictingDraft(record)) draft.photos.forEach { deleteCachedMedia(context, it.localUri) }
        draft = draftFromCheckin(record)
        draftRequestId = UUID.randomUUID().toString()
        selectedRecord = null
        clearRecordDetailState()
        selectTab(AppTab.Capture)
        persistDraft()
    }

    fun saveDraft() {
        val validation = validateDraft(draft)
        if (!validation.ready) {
            message = validation.missingText
            return
        }
        busy = true
        persistDraft(attempted = true)
        coroutineScope.launch {
            val currentDraft = draft
            try {
                val saved = authed { token -> api.saveCheckin(apiBase, token, currentDraft, draftRequestId) }
                currentDraft.photos.forEach { deleteCachedMedia(context, it.localUri) }
                clearPendingUpload(preferences)
                draft = UploadDraft()
                draftRequestId = UUID.randomUUID().toString()
                mineItems = listOf(saved) + mineItems.filterNot { it.id == saved.id }
                clearRecordDetailState()
                selectedRecord = saved
                collection = null
                selectTab(AppTab.Feed)
                feedItems = authed { token -> api.listCheckins(apiBase, token, feedScope, feedOrder) }
                message = if (currentDraft.editingId == null) "记录已发布" else "记录已更新"
            } catch (error: Exception) {
                message = error.message ?: "记录保存失败"
            } finally {
                busy = false
            }
        }
    }

    fun deleteRecord(record: CheckinItem) {
        if (record.id in deletingRecordIds) return
        deletingRecordIds = deletingRecordIds + record.id
        busy = true
        coroutineScope.launch {
            try {
                authed { token -> api.deleteCheckin(apiBase, token, record.id) }
                feedItems = feedItems.filterNot { it.id == record.id }
                mineItems = mineItems.filterNot { it.id == record.id }
                selectedRecord = null
                clearRecordDetailState()
                message = "记录已删除；照片资产仍保留在你的媒体库"
            } catch (error: Exception) {
                message = error.message ?: "删除失败"
            } finally {
                deletingRecordIds = deletingRecordIds - record.id
                busy = false
            }
        }
    }

    fun isDeletingRecord(record: CheckinItem): Boolean = record.id in deletingRecordIds

    private fun updateRecordEverywhere(updated: CheckinItem) {
        feedItems = feedItems.map { if (it.id == updated.id) updated else it }
        mineItems = mineItems.map { if (it.id == updated.id) updated else it }
        if (selectedRecord?.id == updated.id) selectedRecord = updated
    }

    private fun updateRecordRelations(recordId: String, transform: (CheckinItem) -> CheckinItem) {
        feedItems = feedItems.map { if (it.id == recordId) transform(it) else it }
        mineItems = mineItems.map { if (it.id == recordId) transform(it) else it }
        selectedRecord = selectedRecord?.let { if (it.id == recordId) transform(it) else it }
    }

    private fun partnerRelationKey(recordId: String, partnerId: String): String = "$recordId:$partnerId"

    fun isMutatingPartnerRelation(record: CheckinItem, partner: PartnerItem): Boolean =
        partnerRelationKey(record.id, partner.id) in mutatingPartnerRelationKeys

    fun setPartnerRelationVisibility(record: CheckinItem, partner: PartnerItem, visibility: String) {
        val normalized = visibility.trim().lowercase()
        val key = partnerRelationKey(record.id, partner.id)
        if (normalized !in setOf("public", "private") || normalized == partner.relationVisibility || key in mutatingPartnerRelationKeys) return
        mutatingPartnerRelationKeys = mutatingPartnerRelationKeys + key
        coroutineScope.launch {
            try {
                val savedVisibility = authed { token ->
                    api.updateCheckinPartnerVisibility(apiBase, token, record.id, partner.id, normalized)
                }
                updateRecordRelations(record.id) { it.withPartnerRelationVisibility(partner.id, savedVisibility) }
                message = if (savedVisibility == "public") "${partner.name} 的出镜关系已公开显示" else "${partner.name} 的出镜关系仅相关成员可见"
            } catch (error: Exception) {
                message = error.message ?: "伙伴关系可见性更新失败"
            } finally {
                mutatingPartnerRelationKeys = mutatingPartnerRelationKeys - key
            }
        }
    }

    fun removePartnerFromRecord(record: CheckinItem, partner: PartnerItem) {
        val key = partnerRelationKey(record.id, partner.id)
        if (key in mutatingPartnerRelationKeys) return
        mutatingPartnerRelationKeys = mutatingPartnerRelationKeys + key
        coroutineScope.launch {
            try {
                authed { token -> api.removeCheckinPartner(apiBase, token, record.id, partner.id) }
                updateRecordRelations(record.id) { it.withoutPartnerRelation(partner.id) }
                message = "已从这条记录移除 ${partner.name}；伙伴资料与其他记录不受影响"
            } catch (error: Exception) {
                message = error.message ?: "移除伙伴关系失败"
            } finally {
                mutatingPartnerRelationKeys = mutatingPartnerRelationKeys - key
            }
        }
    }

    fun toggleLike(record: CheckinItem) {
        if (record.id in likingRecordIds) return
        likingRecordIds = likingRecordIds + record.id
        coroutineScope.launch {
            try {
                updateRecordEverywhere(authed { token -> api.setLike(apiBase, token, record) })
            } catch (error: Exception) {
                message = error.message ?: "互动失败"
            } finally {
                likingRecordIds = likingRecordIds - record.id
            }
        }
    }

    fun isLiking(record: CheckinItem): Boolean = record.id in likingRecordIds

    fun addComment(record: CheckinItem, body: String, onSuccess: () -> Unit = {}) {
        if (body.isBlank() || record.id in commentingRecordIds) return
        commentingRecordIds = commentingRecordIds + record.id
        coroutineScope.launch {
            try {
                comments = comments + authed { token -> api.addComment(apiBase, token, record.id, body) }
                updateRecordEverywhere(record.copy(commentCount = record.commentCount + 1))
                onSuccess()
            } catch (error: Exception) {
                message = error.message ?: "评论失败"
            } finally {
                commentingRecordIds = commentingRecordIds - record.id
            }
        }
    }

    fun isCommenting(record: CheckinItem): Boolean = record.id in commentingRecordIds

    fun reportRecord(record: CheckinItem, reason: String, onSuccess: () -> Unit = {}) {
        if (reason.isBlank() || record.id in reportingRecordIds) return
        reportingRecordIds = reportingRecordIds + record.id
        coroutineScope.launch {
            try {
                authed { token -> api.reportCheckin(apiBase, token, record.id, reason) }
                message = "投诉已提交给管理员"
                onSuccess()
            } catch (error: Exception) {
                message = error.message ?: "投诉失败"
            } finally {
                reportingRecordIds = reportingRecordIds - record.id
            }
        }
    }

    fun isReporting(record: CheckinItem): Boolean = record.id in reportingRecordIds

    fun requestPartnerForRecord(record: CheckinItem, code: String, onSuccess: () -> Unit = {}) {
        if (code.isBlank() || record.id in partnerRequestRecordIds) return
        partnerRequestRecordIds = partnerRequestRecordIds + record.id
        coroutineScope.launch {
            try {
                val partner = authed { token -> api.listPartners(apiBase, token, code) }
                    .firstOrNull { it.publicCode.equals(code.trim(), ignoreCase = true) }
                    ?: throw IllegalArgumentException("没有找到这个伙伴编号")
                authed { token -> api.requestPartnerRegistration(apiBase, token, record.id, partner.id) }
                selectedRecord = authed { token -> api.getCheckin(apiBase, token, record.id) }
                message = if (partner.ownerUserId == currentUser?.id) "伙伴已登记到记录" else "已提交伙伴补登记，等待相关用户确认"
                onSuccess()
            } catch (error: Exception) {
                message = error.message ?: "伙伴补登记失败"
            } finally {
                partnerRequestRecordIds = partnerRequestRecordIds - record.id
            }
        }
    }

    fun isRequestingPartner(record: CheckinItem): Boolean = record.id in partnerRequestRecordIds

    fun addToAlbum(record: CheckinItem, album: AlbumItem) {
        coroutineScope.launch {
            try {
                val updated = authed { token -> api.addAlbumItem(apiBase, token, album.id, record.id) }
                albums = albums.map { if (it.id == updated.id) updated else it }
                message = "已加入 ${album.title}"
            } catch (error: Exception) {
                message = error.message ?: "加入合集失败"
            }
        }
    }

    fun savePartner(id: String?, name: String, itemTypeId: String, workId: String?, characterId: String?, visibility: String, onSuccess: () -> Unit = {}) {
        busy = true
        coroutineScope.launch {
            try {
                val saved = authed { token -> api.savePartner(apiBase, token, id, name, itemTypeId, workId, characterId, visibility) }
                partners = listOf(saved) + partners.filterNot { it.id == saved.id }
                selectedPartner = saved
                message = if (id == null) "伙伴已登记：${saved.publicCode}" else "伙伴已更新"
                onSuccess()
            } catch (error: Exception) {
                message = error.message ?: "伙伴保存失败"
            } finally {
                busy = false
            }
        }
    }

    fun deletePartner(partner: PartnerItem) {
        busy = true
        coroutineScope.launch {
            try {
                authed { token -> api.deletePartner(apiBase, token, partner.id) }
                partners = partners.filterNot { it.id == partner.id }
                selectedPartner = null
                clearPartnerDetailState()
                message = "伙伴已删除，历史记录保留名称快照"
            } catch (error: Exception) {
                message = error.message ?: "伙伴删除失败"
            } finally {
                busy = false
            }
        }
    }

    fun selectPartner(partner: PartnerItem) {
        selectedPartner = partner
        selectedPartnerMeetings = emptyList()
        loadPartnerDetails(partner)
    }

    fun reloadPartnerDetails() {
        selectedPartner?.let(::loadPartnerDetails)
    }

    private fun loadPartnerDetails(partner: PartnerItem) {
        val requestVersion = partnerDetailRequests.next()
        partnerDetailLoading = true
        partnerDetailError = null
        partnerMeetingsLoading = true
        partnerMeetingsError = null
        coroutineScope.launch {
            val detailResult = runCatching { authed { token -> api.getPartner(apiBase, token, partner.id) } }
            if (!partnerDetailRequests.isCurrent(requestVersion, selectedPartner?.id, partner.id)) return@launch
            detailResult
                .onSuccess { selectedPartner = it }
                .onFailure { partnerDetailError = it.message?.takeIf(String::isNotBlank) ?: "伙伴完整资料加载失败" }
            partnerDetailLoading = false
        }
        coroutineScope.launch {
            val meetingsResult = runCatching { authed { token -> api.listPartnerMeetings(apiBase, token, partner.id) } }
            if (!partnerDetailRequests.isCurrent(requestVersion, selectedPartner?.id, partner.id)) return@launch
            meetingsResult
                .onSuccess { selectedPartnerMeetings = it }
                .onFailure { partnerMeetingsError = it.message?.takeIf(String::isNotBlank) ?: "相遇记录加载失败" }
            partnerMeetingsLoading = false
        }
    }

    fun clearSelectedPartner() {
        selectedPartner = null
        clearPartnerDetailState()
    }

    private fun clearPartnerDetailState() {
        partnerDetailRequests.invalidate()
        selectedPartnerMeetings = emptyList()
        partnerDetailLoading = false
        partnerDetailError = null
        partnerMeetingsLoading = false
        partnerMeetingsError = null
    }

    fun resolvePartnerRequest(request: PartnerRequestItem, approved: Boolean) {
        if (request.requestKey in resolvingPartnerRequestKeys) return
        resolvingPartnerRequestKeys = resolvingPartnerRequestKeys + request.requestKey
        coroutineScope.launch {
            try {
                authed { token -> api.resolvePartnerRequest(apiBase, token, request, approved) }
                partnerRequests = authed { token -> api.listPartnerRequests(apiBase, token) }
                message = if (approved) "伙伴补登记已确认" else "伙伴补登记已拒绝"
            } catch (error: Exception) {
                message = error.message ?: "处理失败"
            } finally {
                resolvingPartnerRequestKeys = resolvingPartnerRequestKeys - request.requestKey
            }
        }
    }

    fun isResolvingPartnerRequest(request: PartnerRequestItem): Boolean = request.requestKey in resolvingPartnerRequestKeys

    fun searchPartner(code: String) {
        coroutineScope.launch {
            try {
                val found = authed { token -> api.listPartners(apiBase, token, code) }.firstOrNull()
                    ?: throw IllegalArgumentException("没有找到这个伙伴编号")
                selectPartner(found)
            } catch (error: Exception) {
                message = error.message ?: "伙伴查询失败"
            }
        }
    }

    fun openPartnerRecords(partner: PartnerItem) {
        openCollection(RecordCollection(partner.name, partner.publicCode, mapOf("partner_id" to partner.id)))
    }

    fun createCatalogCandidate(type: String, name: String, workId: String?, onSuccess: () -> Unit = {}) {
        if (catalogCandidateCreating) return
        catalogCandidateCreating = true
        coroutineScope.launch {
            try {
                val created = authed { token -> api.createCatalogCandidate(apiBase, token, type, name, workId) }
                discovery = discovery.copy(
                    catalog = discovery.catalog.toMutableMap().apply {
                        this[type] = (get(type).orEmpty() + created).distinctBy(CatalogEntityItem::id)
                    }
                )
                message = "${created.canonicalName} 已作为候选提交"
                onSuccess()
            } catch (error: Exception) {
                message = error.message ?: "候选提交失败"
            } finally {
                catalogCandidateCreating = false
            }
        }
    }

    fun toggleCatalogFollow(entity: CatalogEntityItem) {
        coroutineScope.launch {
            try {
                val updated = authed { token -> api.setCatalogFollow(apiBase, token, entity) }
                discovery = discovery.copy(
                    catalog = discovery.catalog.toMutableMap().apply {
                        this[entity.entityType] = get(entity.entityType).orEmpty().map { if (it.id == updated.id) updated else it }
                    }
                )
                message = if (updated.followed) "已关注 ${updated.canonicalName}" else "已取消关注 ${updated.canonicalName}"
            } catch (error: Exception) {
                message = error.message ?: "关注操作失败"
            }
        }
    }

    fun openCatalog(entity: CatalogEntityItem) {
        openCollection(RecordCollection(entity.canonicalName, "${entity.recordCount} 条记录", mapOf("${entity.entityType}_id" to entity.id)))
    }

    fun saveEvent(id: String?, name: String, type: String, visibility: String, placeId: String?, seriesId: String?, startsAt: String?, endsAt: String?, description: String, onSuccess: () -> Unit = {}) {
        busy = true
        coroutineScope.launch {
            try {
                val saved = authed { token -> api.saveEvent(apiBase, token, id, name, type, visibility, placeId, seriesId, startsAt, endsAt, description) }
                discovery = discovery.copy(events = listOf(saved) + discovery.events.filterNot { it.id == saved.id })
                message = if (id == null) "活动已创建" else "活动已更新"
                onSuccess()
            } catch (error: Exception) {
                message = error.message ?: "活动保存失败"
            } finally {
                busy = false
            }
        }
    }

    fun deleteEvent(event: EventItem) {
        coroutineScope.launch {
            try {
                authed { token -> api.deleteEvent(apiBase, token, event.id) }
                discovery = discovery.copy(events = discovery.events.filterNot { it.id == event.id })
                message = "活动已删除"
            } catch (error: Exception) {
                message = error.message ?: "活动删除失败"
            }
        }
    }

    fun openEvent(event: EventItem) {
        openCollection(RecordCollection(event.name, eventTypeLabel(event.eventType), mapOf("event_id" to event.id)))
    }

    fun openTopic(topic: TopicItem) {
        openCollection(RecordCollection(topic.title, topic.description, checkinIds = topic.checkinIds))
    }

    fun openRegion(region: WorldRegionItem) {
        openCollection(RecordCollection(region.name, "${region.recordCount} 条 · ${region.userCount} 人", mapOf("region" to region.name, "world_only" to "true")))
    }

    fun createPlace(name: String, latitude: Double, longitude: Double, onSuccess: (PlaceItem) -> Unit = {}) {
        if (placeCreating) return
        placeCreating = true
        coroutineScope.launch {
            try {
                val place = authed { token -> api.createPlace(apiBase, token, name, latitude, longitude) }
                places = listOf(place) + places
                message = "活动地点已保存"
                onSuccess(place)
            } catch (error: Exception) {
                message = error.message ?: "地点保存失败"
            } finally {
                placeCreating = false
            }
        }
    }

    fun setHome(name: String, latitude: Double, longitude: Double) {
        coroutineScope.launch {
            try {
                val home = authed { token -> api.setHomeLocation(apiBase, token, name, latitude, longitude) }
                footprint = footprint.copy(home = home)
                message = "家位置已登记，仅你本人可见"
            } catch (error: Exception) {
                message = error.message ?: "家位置保存失败"
            }
        }
    }

    fun deleteHome() {
        coroutineScope.launch {
            try {
                authed { token -> api.deleteHomeLocation(apiBase, token) }
                footprint = footprint.copy(home = null)
                message = "家位置已删除"
            } catch (error: Exception) {
                message = error.message ?: "删除失败"
            }
        }
    }

    fun toggleFollow(person: PersonItem) {
        coroutineScope.launch {
            try {
                val updated = authed { token -> api.setFollowing(apiBase, token, person) }
                people = people.map { if (it.id == updated.id) updated else it }
            } catch (error: Exception) {
                message = error.message ?: "关注操作失败"
            }
        }
    }

    fun blockPerson(person: PersonItem) {
        coroutineScope.launch {
            try {
                authed { token -> api.blockPerson(apiBase, token, person.id) }
                people = people.filterNot { it.id == person.id }
                feedItems = feedItems.filterNot { it.userId == person.id }
                message = "已屏蔽 ${person.displayName}"
            } catch (error: Exception) {
                message = error.message ?: "屏蔽失败"
            }
        }
    }

    fun createAlbum(title: String, onSuccess: () -> Unit = {}) {
        if (albumCreating) return
        albumCreating = true
        coroutineScope.launch {
            try {
                albums = listOf(authed { token -> api.createAlbum(apiBase, token, title) }) + albums
                message = "合集已创建"
                onSuccess()
            } catch (error: Exception) {
                message = error.message ?: "合集创建失败"
            } finally {
                albumCreating = false
            }
        }
    }

    fun createExport() {
        if (exportCreating) return
        exportCreating = true
        coroutineScope.launch {
            try {
                val created = authed { token -> api.createExport(apiBase, token) }
                exports = listOf(created) + exports.filterNot { it.id == created.id }
                message = if (created.canDownload()) "个人数据导出已可保存" else "个人数据导出任务已创建"
            } catch (error: Exception) {
                message = error.message ?: "导出失败"
            } finally {
                exportCreating = false
            }
        }
    }

    fun downloadExport(record: ExportItem) {
        if (!record.canDownload() || exportDownloadingId != null) return
        exportDownloadingId = record.id
        coroutineScope.launch {
            try {
                val uri = authed { token -> api.downloadExport(context, apiBase, token, record) }
                context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                    type = "application/zip"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }, "保存或分享 Nunulo 数据导出"))
                message = "已打开保存与分享菜单"
            } catch (error: Exception) {
                message = error.message ?: "导出下载失败"
            } finally {
                exportDownloadingId = null
            }
        }
    }

    fun createInvite() {
        if (inviteCreating) return
        inviteCreating = true
        coroutineScope.launch {
            try {
                inviteCode = authed { token -> api.createInvite(apiBase, token) }
                message = "个人邀请码已生成"
            } catch (error: Exception) {
                message = error.message ?: "邀请码生成失败"
            } finally {
                inviteCreating = false
            }
        }
    }

    fun copyInvite() {
        if (inviteCode.isBlank()) return
        message = runCatching {
            context.getSystemService(ClipboardManager::class.java)
                .setPrimaryClip(ClipData.newPlainText("Nunulo 邀请码", inviteCode))
            "邀请码已复制"
        }.getOrElse { "邀请码复制失败" }
    }

    fun shareInvite() {
        if (inviteCode.isBlank()) return
        message = runCatching {
            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "Nunulo 邀请码：$inviteCode\n注册：${resolveAssetUrl(apiBase, "/app/")}")
            }, "分享 Nunulo 邀请码"))
            "已打开邀请码分享菜单"
        }.getOrElse { "当前设备没有可用的分享应用" }
    }

    fun uploadAvatar(uri: Uri) {
        avatarUri = uri
        busy = true
        coroutineScope.launch {
            try {
                currentUser = authed { token -> api.uploadAvatar(context, apiBase, token, uri) }
                message = "头像已更新"
            } catch (error: Exception) {
                message = error.message ?: "头像上传失败"
            } finally {
                busy = false
            }
        }
    }

    fun markNotificationsRead() {
        coroutineScope.launch {
            try {
                authed { token -> api.markNotificationsRead(apiBase, token) }
                notifications = notifications.map { it.copy(readAt = it.readAt ?: "now") }
                message = "通知已全部标记为已读"
            } catch (error: Exception) {
                message = error.message ?: "通知状态更新失败"
            }
        }
    }

    fun openNotification(notification: NotificationItem) {
        notificationsOpen = false
        when {
            notification.targetType == "checkin" && !notification.targetId.isNullOrBlank() -> {
                coroutineScope.launch {
                    val record = runCatching { authed { token -> api.getCheckin(apiBase, token, notification.targetId) } }.getOrNull()
                    if (record == null) message = "这条记录暂时无法打开，可能已被删除或隐藏" else openRecord(record)
                }
            }
            notification.targetType == "checkin_partner" -> selectTab(AppTab.Partners)
            else -> selectTab(AppTab.Profile)
        }
    }
}

internal data class DraftValidation(
    val photoCount: Int,
    val allPhotosReady: Boolean,
    val coordinatesValid: Boolean,
    val ready: Boolean,
    val missingText: String,
)

internal fun validateDraft(draft: UploadDraft): DraftValidation {
    val photoCount = draft.photos.size
    val allReady = draft.photos.all { it.status == "ready" && it.photo != null }
    val latitude = draft.latitude.trim()
    val longitude = draft.longitude.trim()
    val coordinatesValid = when {
        latitude.isBlank() && longitude.isBlank() -> true
        latitude.isBlank() || longitude.isBlank() -> false
        else -> latitude.toDoubleOrNull()?.let { it in -90.0..90.0 } == true && longitude.toDoubleOrNull()?.let { it in -180.0..180.0 } == true
    }
    val ready = photoCount in 1..9 && allReady && coordinatesValid
    val missing = when {
        photoCount == 0 -> "请至少拍摄或选择 1 张照片"
        photoCount > 9 -> "一条记录最多 9 张照片"
        !allReady -> "请重试或移除上传失败的照片"
        !coordinatesValid -> "经纬度必须同时为空或同时有效"
        else -> "可以发布"
    }
    return DraftValidation(photoCount, allReady, coordinatesValid, ready, missing)
}

internal fun UploadDraft.updatePhoto(key: String, transform: (DraftPhotoItem) -> DraftPhotoItem): UploadDraft =
    copy(photos = photos.map { if (it.key == key) transform(it) else it })

internal fun UploadDraft.movePhoto(key: String, direction: Int): UploadDraft {
    val from = photos.indexOfFirst { it.key == key }
    val to = from + direction
    if (from < 0 || to !in photos.indices) return this
    val reordered = photos.toMutableList()
    val item = reordered.removeAt(from)
    reordered.add(to, item)
    return copy(photos = reordered)
}
