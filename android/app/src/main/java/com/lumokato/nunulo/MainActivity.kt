package com.lumokato.nunulo

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.amap.api.maps.MapsInitializer

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapsInitializer.updatePrivacyShow(this, true, true)
        MapsInitializer.updatePrivacyAgree(this, true)
        setContent { NunuloApp() }
    }
}

internal enum class AppTab(val title: String) {
    Feed("动态"),
    Discover("发现"),
    Capture("记录"),
    Partners("伙伴"),
    Profile("我的"),
}

internal enum class FeedScope(val apiValue: String, val label: String) {
    Discover("discover", "整体"),
    Following("following", "关注"),
    Mine("mine", "我的"),
}

internal enum class FeedOrder(val apiValue: String, val label: String) {
    Popular("popular", "热门"),
    Latest("latest", "最新"),
}

internal enum class LocationPurpose { Draft, Home, Place }

@Composable
private fun NunuloApp() {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("nunulo", Context.MODE_PRIVATE) }
    val coroutineScope = rememberCoroutineScope()
    val controller = remember { NunuloController(context, preferences, NunuloApi(), coroutineScope) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        controller.onLocationPermissionResult(permissions.values.any { it })
    }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) controller.addMedia(uris, "gallery")
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val uri = pendingCameraUri
        pendingCameraUri = null
        if (success && uri != null) controller.addMedia(listOf(uri), "camera") else controller.cancelCapture(uri)
    }
    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) controller.uploadAvatar(uri)
    }

    LaunchedEffect(Unit) { controller.initialize() }
    LaunchedEffect(controller.requestLocationPermission) {
        if (controller.requestLocationPermission) {
            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            controller.consumeLocationPermissionRequest()
        }
    }

    NunuloTheme {
        if (!controller.loggedIn) {
            AuthScreen(
                initialLogin = preferences.getString("lastLogin", "").orEmpty(),
                config = controller.publicConfig,
                busy = controller.busy,
                message = controller.message,
                onLogin = controller::login,
                onRegister = controller::register,
            )
            return@NunuloTheme
        }

        if (!controller.stateReady) {
            InitialSyncScreen(
                busy = controller.busy,
                error = controller.syncError,
                onRetry = { controller.refreshAll("已恢复登录并同步内容") },
                onLogout = controller::logout,
            )
            return@NunuloTheme
        }

        val snackbarHostState = remember { SnackbarHostState() }
        LaunchedEffect(controller.message) {
            val nextMessage = controller.message
            if (nextMessage.isNotBlank()) {
                snackbarHostState.showSnackbar(nextMessage)
                controller.consumeMessage(nextMessage)
            }
        }

        Scaffold(
            topBar = {
                NunuloTopBar(
                    tab = controller.activeTab,
                    busy = controller.busy,
                    unreadCount = controller.notifications.count { it.readAt == null },
                    onRefresh = controller::refreshAll,
                    onNotifications = { controller.notificationsOpen = true },
                )
            },
            bottomBar = { NunuloBottomBar(controller.activeTab, controller::selectTab) },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { padding ->
            Surface(color = NunuloColors.Background, modifier = Modifier.padding(padding).fillMaxSize()) {
                when (controller.activeTab) {
                    AppTab.Feed -> FeedScreen(controller)
                    AppTab.Discover -> DiscoveryScreen(controller)
                    AppTab.Capture -> CaptureScreen(
                        controller = controller,
                        onPick = { photoPicker.launch(arrayOf("image/jpeg", "image/png", "image/webp")) },
                        onCamera = {
                            val captureUri = createCaptureUri(context)
                            pendingCameraUri = captureUri
                            cameraLauncher.launch(captureUri)
                        },
                    )
                    AppTab.Partners -> PartnersScreen(controller)
                    AppTab.Profile -> ProfileScreen(controller, onPickAvatar = { avatarPicker.launch("image/*") })
                }
            }
        }

        controller.selectedRecord?.let { RecordDetailDialog(controller, it) }
        if (controller.notificationsOpen) NotificationsDialog(controller)
    }
}

@Composable
internal fun InitialSyncScreen(
    busy: Boolean,
    error: String?,
    onRetry: () -> Unit,
    onLogout: () -> Unit,
) {
    Box(
        Modifier.fillMaxSize().background(NunuloColors.Background).padding(horizontal = 20.dp, vertical = 28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            color = NunuloColors.Paper,
            shape = RoundedCornerShape(30.dp),
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth().widthIn(max = 520.dp),
        ) {
            Column(Modifier.padding(horizontal = 22.dp, vertical = 26.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    RecordStamp("N", if (error == null) NunuloColors.MapBlue else NunuloColors.Coral)
                    Text("NUNULO · 伙伴旅行记录册", color = NunuloColors.Muted, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
                if (error == null) {
                    Text("正在整理你的旅行记录", style = MaterialTheme.typography.headlineMedium)
                    Text("同步伙伴、照片、活动、足迹与通知。草稿保存在本机，同步中断也不会丢失。", color = NunuloColors.Muted)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator(color = NunuloColors.MapBlue, modifier = Modifier.size(28.dp))
                        Text(if (busy) "正在连接 Nunulo…" else "正在准备同步…", color = NunuloColors.MapBlue, fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = onLogout) { Text("切换账号") }
                } else {
                    Text("暂时无法打开你的记录", style = MaterialTheme.typography.headlineMedium)
                    Text("登录仍然有效，本机草稿也已保留。检查网络后重新同步，或切换账号。", color = NunuloColors.Muted)
                    Surface(color = NunuloColors.Soft, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                        Text(error, color = NunuloColors.Danger, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp))
                    }
                    Button(enabled = !busy, onClick = onRetry, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text(if (busy) "同步中" else "重新同步") }
                    TextButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) { Text("切换账号") }
                }
            }
        }
    }
}

@Composable
internal fun NunuloTopBar(tab: AppTab, busy: Boolean, unreadCount: Int, onRefresh: () -> Unit, onNotifications: () -> Unit) {
    Surface(color = NunuloColors.Paper, shadowElevation = 1.dp) {
        Row(Modifier.fillMaxWidth().height(62.dp).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            RecordStamp(if (tab == AppTab.Capture) "记" else "N", if (tab == AppTab.Discover) NunuloColors.MapBlue else NunuloColors.Coral)
            Column(Modifier.padding(start = 10.dp).weight(1f)) {
                Text(tab.title, style = MaterialTheme.typography.titleLarge)
                Text(if (tab == AppTab.Capture) "此刻与伙伴在哪里" else "伙伴旅行记录册", color = NunuloColors.Muted, style = MaterialTheme.typography.labelSmall)
            }
            Box(Modifier.size(44.dp).clickable(enabled = !busy, onClick = onRefresh), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Refresh, contentDescription = if (busy) "同步中" else "刷新", tint = if (busy) NunuloColors.Muted else NunuloColors.MapBlue)
            }
            Box(Modifier.size(44.dp).clickable(onClick = onNotifications), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.NotificationsNone, contentDescription = "通知")
                if (unreadCount > 0) Badge(Modifier.align(Alignment.TopEnd)) { Text(unreadCount.coerceAtMost(99).toString()) }
            }
        }
    }
}

@Composable
private fun NunuloBottomBar(selected: AppTab, onSelect: (AppTab) -> Unit) {
    val icons = mapOf<AppTab, Pair<ImageVector, ImageVector>>(
        AppTab.Feed to (Icons.Filled.Home to Icons.Outlined.Home),
        AppTab.Discover to (Icons.Filled.Explore to Icons.Outlined.Explore),
        AppTab.Capture to (Icons.Filled.CameraAlt to Icons.Outlined.CameraAlt),
        AppTab.Partners to (Icons.Filled.Groups to Icons.Outlined.Groups),
        AppTab.Profile to (Icons.Filled.Person to Icons.Outlined.PersonOutline),
    )
    Surface(color = NunuloColors.Paper, shadowElevation = 10.dp, modifier = Modifier.navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().height(66.dp)) {
            AppTab.entries.forEach { tab ->
                val active = selected == tab
                val icon = icons.getValue(tab)
                Column(
                    Modifier.weight(1f).fillMaxSize().clickable { onSelect(tab) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    if (tab == AppTab.Capture) {
                        Surface(color = NunuloColors.Coral, shape = androidx.compose.foundation.shape.CircleShape) {
                            Icon(icon.first, contentDescription = tab.title, tint = Color.White, modifier = Modifier.padding(7.dp))
                        }
                    } else {
                        Icon(if (active) icon.first else icon.second, contentDescription = tab.title, tint = if (active) NunuloColors.Coral else NunuloColors.Muted)
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(tab.title, style = MaterialTheme.typography.labelSmall, color = if (active) NunuloColors.Coral else NunuloColors.Muted, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
    }
}
