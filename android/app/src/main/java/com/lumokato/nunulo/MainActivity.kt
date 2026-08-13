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
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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

        Scaffold(
            topBar = {
                NunuloTopBar(
                    tab = controller.activeTab,
                    message = controller.message,
                    busy = controller.busy,
                    unreadCount = controller.notifications.count { it.readAt == null },
                    onRefresh = controller::refreshAll,
                    onNotifications = { controller.notificationsOpen = true },
                )
            },
            bottomBar = { NunuloBottomBar(controller.activeTab, controller::selectTab) },
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
private fun NunuloTopBar(tab: AppTab, message: String, busy: Boolean, unreadCount: Int, onRefresh: () -> Unit, onNotifications: () -> Unit) {
    Surface(color = Color.White, shadowElevation = 2.dp) {
        Column(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().height(54.dp).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(tab.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                TextButton(onClick = onRefresh, enabled = !busy) { Text(if (busy) "同步中" else "刷新") }
                Box(Modifier.size(44.dp).clickable(onClick = onNotifications), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.NotificationsNone, contentDescription = "通知")
                    if (unreadCount > 0) Badge(Modifier.align(Alignment.TopEnd)) { Text(unreadCount.coerceAtMost(99).toString()) }
                }
            }
            Text(message, color = NunuloColors.Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp))
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
    Surface(color = Color.White, shadowElevation = 8.dp, modifier = Modifier.navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().height(60.dp)) {
            AppTab.entries.forEach { tab ->
                val active = selected == tab
                val icon = icons.getValue(tab)
                Column(
                    Modifier.weight(1f).fillMaxSize().clickable { onSelect(tab) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(if (active) icon.first else icon.second, contentDescription = tab.title, tint = if (active) NunuloColors.Coral else NunuloColors.Muted)
                    Spacer(Modifier.height(2.dp))
                    Text(tab.title, style = MaterialTheme.typography.labelSmall, color = if (active) NunuloColors.Coral else NunuloColors.Muted, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
    }
}
