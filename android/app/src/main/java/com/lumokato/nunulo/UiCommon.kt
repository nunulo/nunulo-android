package com.lumokato.nunulo

import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.text.font.FontFamily
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
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal object NunuloColors {
    val Coral = Color(0xFFEE675F)
    val CoralDeep = Color(0xFFB73E3B)
    val Ink = Color(0xFF292634)
    val Muted = Color(0xFF716C7C)
    val Background = Color(0xFFF3F1F8)
    val Paper = Color(0xFFFFFBFF)
    val Soft = Color(0xFFFFE8E5)
    val Lilac = Color(0xFFEAE5F5)
    val MapBlue = Color(0xFF356E82)
    val Leaf = Color(0xFF3F745D)
    val Hairline = Color(0xFFDED8E6)
    val Success = Leaf
    val Danger = Color(0xFFB43D45)
    val Placeholder = Color(0xFFE7E2EC)
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
        secondary = NunuloColors.MapBlue,
        tertiary = NunuloColors.Leaf,
    )
    val defaults = Typography()
    val typography = Typography(
        bodyLarge = defaults.bodyLarge.copy(fontSize = 16.sp, lineHeight = 24.sp),
        bodyMedium = defaults.bodyMedium.copy(fontSize = 14.sp, lineHeight = 21.sp),
        bodySmall = defaults.bodySmall.copy(fontSize = 12.sp, lineHeight = 18.sp),
        labelLarge = defaults.labelLarge.copy(fontSize = 14.sp),
        labelMedium = defaults.labelMedium.copy(fontSize = 12.sp),
        labelSmall = defaults.labelSmall.copy(fontSize = 12.sp),
        titleMedium = defaults.titleMedium.copy(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold),
        titleLarge = defaults.titleLarge.copy(fontSize = 25.sp, lineHeight = 31.sp, fontWeight = FontWeight.Black),
        headlineMedium = defaults.headlineMedium.copy(fontSize = 31.sp, lineHeight = 38.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Serif),
    )
    MaterialTheme(colorScheme = colors, typography = typography, content = content)
}

@Composable
internal fun AuthScreen(
    initialLogin: String,
    config: PublicConfig?,
    busy: Boolean,
    message: String,
    onLogin: (String, String) -> Unit,
    onRegister: (String, String, String, String, Boolean) -> Unit,
    initialRegisterMode: Boolean = false,
) {
    var registerMode by rememberSaveable { mutableStateOf(initialRegisterMode) }
    var login by rememberSaveable { mutableStateOf(initialLogin) }
    var displayName by rememberSaveable { mutableStateOf("") }
    var invite by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var accepted by rememberSaveable { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    Box(Modifier.fillMaxSize().background(NunuloColors.Background).padding(horizontal = 20.dp, vertical = 24.dp), contentAlignment = Alignment.Center) {
        Card(
            Modifier.fillMaxWidth().widthIn(max = 480.dp),
            colors = CardDefaults.cardColors(containerColor = NunuloColors.Paper),
            shape = RoundedCornerShape(30.dp),
            border = BorderStroke(1.dp, NunuloColors.Hairline),
        ) {
            Column(
                Modifier.fillMaxHeight(0.96f).verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    RecordStamp("N", NunuloColors.Coral)
                    Text("NUNULO · 伙伴旅行记录册", color = NunuloColors.Muted, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
                Text("把伙伴走过的地方，\n收进同一本相册。", style = MaterialTheme.typography.headlineMedium)
                Text("从一张照片开始，留下伙伴、角色、活动与地点之间真实的联系。", color = NunuloColors.Muted)
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
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(accepted, { accepted = it })
                            Text("我已阅读并同意以下协议", style = MaterialTheme.typography.bodySmall)
                        }
                        Row(Modifier.padding(start = 36.dp), verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { uriHandler.openUri(resolveAssetUrl(BuildConfig.NUNULO_API_BASE_URL, config?.termsUrl ?: "/terms/")) }, contentPadding = PaddingValues(horizontal = 4.dp)) { Text("服务条款") }
                            Text("与", color = NunuloColors.Muted, style = MaterialTheme.typography.bodySmall)
                            TextButton(onClick = { uriHandler.openUri(resolveAssetUrl(BuildConfig.NUNULO_API_BASE_URL, config?.privacyUrl ?: "/privacy/")) }, contentPadding = PaddingValues(horizontal = 4.dp)) { Text("隐私政策") }
                        }
                    }
                }
                Button(
                    onClick = {
                        if (registerMode) onRegister(login, displayName, invite, password, accepted) else onLogin(login, password)
                    },
                    enabled = !busy && login.isNotBlank() && password.isNotBlank() && (!registerMode || (displayName.isNotBlank() && accepted)),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) { Text(if (busy) "处理中" else if (registerMode) "创建账号" else "登录") }
                if (message.isNotBlank()) {
                    Surface(color = NunuloColors.Lilac, shape = RoundedCornerShape(12.dp)) {
                        Text(message, color = NunuloColors.Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp))
                    }
                }
            }
        }
    }
}

@Composable
internal fun RecordStamp(label: String, color: Color = NunuloColors.Coral, modifier: Modifier = Modifier) {
    Surface(
        color = color,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier,
        shadowElevation = 1.dp,
    ) {
        Text(
            label,
            color = Color.White,
            fontWeight = FontWeight.Black,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
        )
    }
}

@Composable
internal fun CoordinateTag(label: String, modifier: Modifier = Modifier) {
    Surface(color = NunuloColors.Lilac, shape = RoundedCornerShape(50), modifier = modifier) {
        Text(label, color = NunuloColors.MapBlue, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp))
    }
}

@Composable
internal fun DateTimeField(
    label: String,
    value: String,
    emptyLabel: String,
    clearLabel: String,
    onChange: (String) -> Unit,
) {
    val context = LocalContext.current
    val zone = remember { ZoneId.systemDefault() }
    val current = remember(value) {
        runCatching { OffsetDateTime.parse(value).atZoneSameInstant(zone) }
            .recoverCatching { Instant.parse(value).atZone(zone) }
            .getOrElse { java.time.ZonedDateTime.now(zone) }
    }
    val display = value.takeIf(String::isNotBlank)?.let {
        runCatching { current.format(DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm")) }.getOrNull()
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, fontWeight = FontWeight.Bold)
        Text(display ?: emptyLabel, color = NunuloColors.Muted)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = {
                DatePickerDialog(
                    context,
                    { _, year, month, day ->
                        TimePickerDialog(
                            context,
                            { _, hour, minute -> onChange(java.time.ZonedDateTime.of(year, month + 1, day, hour, minute, 0, 0, zone).toOffsetDateTime().toString()) },
                            current.hour,
                            current.minute,
                            true,
                        ).show()
                    },
                    current.year,
                    current.monthValue - 1,
                    current.dayOfMonth,
                ).show()
            }) { Text(if (value.isBlank()) "选择日期与时间" else "修改时间") }
            if (value.isNotBlank()) TextButton(onClick = { onChange("") }) { Text(clearLabel) }
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
    Surface(
        color = NunuloColors.Paper,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, NunuloColors.Hairline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (subtitle.isNotBlank()) Text(subtitle, color = NunuloColors.Muted, style = MaterialTheme.typography.bodySmall)
            HorizontalDivider(color = NunuloColors.Hairline.copy(alpha = 0.72f))
            content()
        }
    }
}

@Composable
internal fun EmptyState(title: String, subtitle: String) {
    SectionCard(title, subtitle) {}
}

@Composable
internal fun DetailLoadState(
    title: String,
    detail: String,
    loading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = if (error == null) NunuloColors.Lilac else NunuloColors.Soft,
        shape = RoundedCornerShape(18.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            if (loading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = NunuloColors.MapBlue,
                    trackColor = NunuloColors.Hairline,
                )
            }
            Text(error ?: detail, color = NunuloColors.Muted, style = MaterialTheme.typography.bodySmall)
            if (error != null) {
                TextButton(onClick = onRetry, contentPadding = PaddingValues(0.dp)) { Text("重新加载") }
            }
        }
    }
}

@Composable
internal fun ConfirmActionDialog(
    title: String,
    body: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body, color = NunuloColors.Muted) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirmLabel, color = NunuloColors.Danger, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
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
