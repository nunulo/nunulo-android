package com.lumokato.nunulo

import android.graphics.Bitmap
import android.os.Bundle
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.MapView
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.LatLngBounds
import com.amap.api.maps.model.MarkerOptions

internal object NunuloColors {
    val Coral = Color(0xFFE6635C)
    val Ink = Color(0xFF26232A)
    val Muted = Color(0xFF74707A)
    val Background = Color(0xFFF7F5F2)
    val Paper = Color(0xFFFFFFFF)
    val Soft = Color(0xFFFFE9E4)
    val Hairline = Color(0xFFE8E1DB)
    val Success = Color(0xFF357A56)
    val Danger = Color(0xFFB63F3F)
    val Placeholder = Color(0xFFEDE8E3)
}

@Composable
internal fun NunuloTheme(content: @Composable () -> Unit) {
    val colors = lightColorScheme(
        primary = NunuloColors.Coral,
        onPrimary = Color.White,
        background = NunuloColors.Background,
        surface = NunuloColors.Paper,
        onSurface = NunuloColors.Ink,
        outline = NunuloColors.Hairline,
        error = NunuloColors.Danger,
    )
    MaterialTheme(colorScheme = colors, typography = Typography(), content = content)
}

@Composable
internal fun AuthScreen(
    initialLogin: String,
    config: PublicConfig?,
    busy: Boolean,
    message: String,
    onLogin: (String, String) -> Unit,
    onRegister: (String, String, String, String, Boolean) -> Unit,
) {
    var registerMode by rememberSaveable { mutableStateOf(false) }
    var login by rememberSaveable { mutableStateOf(initialLogin) }
    var displayName by rememberSaveable { mutableStateOf("") }
    var invite by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var accepted by rememberSaveable { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    Box(Modifier.fillMaxSize().background(NunuloColors.Background).padding(24.dp), contentAlignment = Alignment.Center) {
        Card(Modifier.fillMaxWidth().widthIn(max = 480.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Surface(color = NunuloColors.Soft, shape = CircleShape) {
                    Text("N", color = NunuloColors.Coral, fontSize = 26.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                }
                Text("Nunulo", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                Text("动态、伙伴、活动与足迹，共用同一条真实记录。", color = NunuloColors.Muted)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !registerMode, onClick = { registerMode = false }, label = { Text("登录") })
                    FilterChip(selected = registerMode, onClick = { registerMode = true }, label = { Text("注册") })
                }
                OutlinedTextField(login, { login = it }, label = { Text(if (registerMode) "用户名" else "用户名或邮箱") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                if (registerMode) {
                    OutlinedTextField(displayName, { displayName = it }, label = { Text("显示名") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(invite, { invite = it }, label = { Text("邀请码（可选）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
                OutlinedTextField(password, { password = it }, label = { Text("密码") }, visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth())
                if (registerMode) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(accepted, { accepted = it })
                        Text("我同意")
                        TextButton(onClick = { uriHandler.openUri(resolveAssetUrl(BuildConfig.NUNULO_API_BASE_URL, config?.termsUrl ?: "/terms/")) }) { Text("服务条款") }
                        Text("与")
                        TextButton(onClick = { uriHandler.openUri(resolveAssetUrl(BuildConfig.NUNULO_API_BASE_URL, config?.privacyUrl ?: "/privacy/")) }) { Text("隐私政策") }
                    }
                }
                Button(
                    onClick = {
                        if (registerMode) onRegister(login, displayName, invite, password, accepted) else onLogin(login, password)
                    },
                    enabled = !busy && login.isNotBlank() && password.isNotBlank() && (!registerMode || displayName.isNotBlank()),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) { Text(if (busy) "处理中" else if (registerMode) "创建账号" else "登录") }
                Text(message, color = NunuloColors.Muted, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
internal fun RemoteImage(url: String?, apiBase: String, api: NunuloApi, modifier: Modifier = Modifier, aspect: Float = 1f) {
    if (url.isNullOrBlank()) {
        Box(modifier.fillMaxWidth().aspectRatio(aspect).background(NunuloColors.Placeholder), contentAlignment = Alignment.Center) {
            Text("等待图片", color = NunuloColors.Muted)
        }
        return
    }
    val resolved = remember(url, apiBase) { resolveAssetUrl(apiBase, url) }
    val bitmap by produceState<Bitmap?>(initialValue = null, resolved) {
        value = runCatching { api.downloadBitmap(apiBase, resolved) }.getOrNull()
    }
    if (bitmap == null) {
        Box(modifier.fillMaxWidth().aspectRatio(aspect).background(NunuloColors.Placeholder), contentAlignment = Alignment.Center) {
            Text("加载图片", color = NunuloColors.Muted)
        }
    } else {
        Image(bitmap!!.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop, modifier = modifier.fillMaxWidth().aspectRatio(aspect))
    }
}

@Composable
internal fun SectionCard(title: String, subtitle: String = "", content: @Composable () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            if (subtitle.isNotBlank()) Text(subtitle, color = NunuloColors.Muted, style = MaterialTheme.typography.bodySmall)
            content()
        }
    }
}

@Composable
internal fun EmptyState(title: String, subtitle: String) {
    SectionCard(title, subtitle) {}
}

@Composable
internal fun FootprintMap(
    home: HomeLocationItem?,
    items: List<FootprintItem>,
    onOpen: (FootprintItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    nativeAmapUnavailableReason()?.let { reason ->
        CoordinateFallback(
            reason = reason,
            currentLabel = home?.let { "${it.name} · ${formatCoordinate(it.latitude, it.longitude)}" },
            rows = items.map { item -> "${item.placeName} · ${formatCoordinate(item.latitude, item.longitude)}" to { onOpen(item) } },
            modifier = modifier,
        )
        return
    }
    val context = LocalContext.current
    val mapView = remember { MapView(context).apply { onCreate(Bundle()) } }
    DisposableEffect(mapView) {
        mapView.onResume()
        onDispose { mapView.onPause(); mapView.onDestroy() }
    }
    AndroidView(
        factory = { mapView },
        modifier = modifier.fillMaxWidth().height(300.dp),
        update = { view ->
            val map = view.map
            map.clear()
            home?.let {
                val point = toAmapPoint(it.latitude, it.longitude)
                map.addMarker(MarkerOptions().position(LatLng(point.latitude, point.longitude)).title(it.name).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE)))
            }
            val byMarker = mutableMapOf<String, FootprintItem>()
            items.forEach { item ->
                val point = toAmapPoint(item.latitude, item.longitude)
                val marker = map.addMarker(MarkerOptions().position(LatLng(point.latitude, point.longitude)).title(item.placeName))
                byMarker[marker.id] = item
            }
            map.setOnMarkerClickListener { marker -> byMarker[marker.id]?.let(onOpen); false }
            val first = home?.let { MapPoint(it.latitude, it.longitude) } ?: items.firstOrNull()?.let { MapPoint(it.latitude, it.longitude) }
            if (first != null) {
                val point = toAmapPoint(first.latitude, first.longitude)
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(point.latitude, point.longitude), if (items.size > 1) 5f else 12f))
            }
        },
    )
}

@Composable
internal fun WorldRegionMap(
    regions: List<WorldRegionItem>,
    onOpen: (WorldRegionItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    nativeAmapUnavailableReason()?.let { reason ->
        CoordinateFallback(
            reason = reason,
            currentLabel = null,
            rows = regions.map { region -> "${region.name} · ${region.recordCount} 条 / ${region.userCount} 人 · ${formatCoordinate(region.latitude, region.longitude)}" to { onOpen(region) } },
            modifier = modifier,
        )
        return
    }
    val context = LocalContext.current
    val mapView = remember { MapView(context).apply { onCreate(Bundle()) } }
    DisposableEffect(mapView) {
        mapView.onResume()
        onDispose { mapView.onPause(); mapView.onDestroy() }
    }
    AndroidView(
        factory = { mapView },
        modifier = modifier.fillMaxWidth().height(300.dp),
        update = { view ->
            val map = view.map
            map.clear()
            val markerRegions = mutableMapOf<String, WorldRegionItem>()
            val points = regions.map { region ->
                val point = toAmapPoint(region.latitude, region.longitude)
                val latLng = LatLng(point.latitude, point.longitude)
                val marker = map.addMarker(
                    MarkerOptions()
                        .position(latLng)
                        .title(region.name)
                        .snippet("${region.recordCount} 条 · ${region.userCount} 人"),
                )
                markerRegions[marker.id] = region
                latLng
            }
            map.setOnMarkerClickListener { marker -> markerRegions[marker.id]?.let(onOpen); true }
            when (points.size) {
                1 -> map.moveCamera(CameraUpdateFactory.newLatLngZoom(points.first(), 8f))
                in 2..Int.MAX_VALUE -> {
                    val bounds = LatLngBounds.builder().apply { points.forEach(::include) }.build()
                    map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 48))
                }
            }
        },
    )
}

@Composable
internal fun CoordinateFallback(
    reason: String,
    currentLabel: String?,
    rows: List<Pair<String, () -> Unit>>,
    modifier: Modifier = Modifier,
) {
    Card(colors = CardDefaults.cardColors(containerColor = NunuloColors.Soft), modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(reason, fontWeight = FontWeight.Bold)
            Text("这里只显示真实坐标，不绘制伪造地图或随机点。", color = NunuloColors.Muted, style = MaterialTheme.typography.bodySmall)
            currentLabel?.let { Text("家位置：$it") }
            if (rows.isEmpty()) Text("暂无可显示坐标", color = NunuloColors.Muted)
            rows.take(12).forEach { (label, onClick) ->
                Text(label, color = NunuloColors.Ink, modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 3.dp))
            }
            if (rows.size > 12) Text("另有 ${rows.size - 12} 个坐标", color = NunuloColors.Muted)
        }
    }
}
