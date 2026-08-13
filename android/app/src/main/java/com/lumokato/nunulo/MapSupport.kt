package com.lumokato.nunulo

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import java.io.File
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.sqrt

internal data class MapPoint(val latitude: Double, val longitude: Double)

internal fun nativeAmapUnavailableReason(): String? = when {
    BuildConfig.AMAP_ANDROID_KEY.isBlank() -> "高德 Android Key 未配置"
    Build.SUPPORTED_ABIS.none { it == "arm64-v8a" || it == "armeabi-v7a" } -> "当前设备 ABI 不支持高德原生地图"
    else -> null
}

internal fun toAmapPoint(latitude: Double, longitude: Double): MapPoint {
    if (isOutsideChina(latitude, longitude)) return MapPoint(latitude, longitude)
    val semiMajorAxis = 6378245.0
    val eccentricitySquared = 0.00669342162296594323
    var latitudeDelta = transformLatitude(longitude - 105.0, latitude - 35.0)
    var longitudeDelta = transformLongitude(longitude - 105.0, latitude - 35.0)
    val radLatitude = latitude / 180.0 * PI
    var magic = sin(radLatitude)
    magic = 1 - eccentricitySquared * magic * magic
    val squareRootMagic = sqrt(magic)
    latitudeDelta = (latitudeDelta * 180.0) / ((semiMajorAxis * (1 - eccentricitySquared)) / (magic * squareRootMagic) * PI)
    longitudeDelta = (longitudeDelta * 180.0) / (semiMajorAxis / squareRootMagic * kotlin.math.cos(radLatitude) * PI)
    return MapPoint(latitude + latitudeDelta, longitude + longitudeDelta)
}

internal fun fromAmapPoint(latitude: Double, longitude: Double): MapPoint {
    if (isOutsideChina(latitude, longitude)) return MapPoint(latitude, longitude)
    var guess = MapPoint(latitude, longitude)
    repeat(3) {
        val converted = toAmapPoint(guess.latitude, guess.longitude)
        guess = MapPoint(
            latitude = guess.latitude - (converted.latitude - latitude),
            longitude = guess.longitude - (converted.longitude - longitude),
        )
    }
    return guess
}

private fun isOutsideChina(latitude: Double, longitude: Double): Boolean =
    longitude < 72.004 || longitude > 137.8347 || latitude < 0.8293 || latitude > 55.8271

private fun transformLatitude(x: Double, y: Double): Double {
    var result = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * sqrt(abs(x))
    result += (20.0 * sin(6.0 * x * PI) + 20.0 * sin(2.0 * x * PI)) * 2.0 / 3.0
    result += (20.0 * sin(y * PI) + 40.0 * sin(y / 3.0 * PI)) * 2.0 / 3.0
    result += (160.0 * sin(y / 12.0 * PI) + 320 * sin(y * PI / 30.0)) * 2.0 / 3.0
    return result
}

private fun transformLongitude(x: Double, y: Double): Double {
    var result = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * sqrt(abs(x))
    result += (20.0 * sin(6.0 * x * PI) + 20.0 * sin(2.0 * x * PI)) * 2.0 / 3.0
    result += (20.0 * sin(x * PI) + 40.0 * sin(x / 3.0 * PI)) * 2.0 / 3.0
    result += (150.0 * sin(x / 12.0 * PI) + 300.0 * sin(x / 30.0 * PI)) * 2.0 / 3.0
    return result
}

internal fun createCaptureUri(context: Context): Uri {
    val directory = File(context.cacheDir, "capture").apply(File::mkdirs)
    val file = File(directory, "capture-${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

internal fun resolveAssetUrl(apiBase: String, url: String): String = when {
    url.startsWith("http://") || url.startsWith("https://") -> url
    url.startsWith("/") -> apiBase.trimEnd('/') + url
    else -> apiBase.trimEnd('/') + "/" + url
}

internal fun formatCoordinate(latitude: Double?, longitude: Double?): String =
    if (latitude == null || longitude == null) "未登记地点" else "%.5f, %.5f".format(latitude, longitude)

internal fun visibilityLabel(value: String): String = when (value) {
    "public" -> "全部成员"
    "followers" -> "关注者"
    else -> "仅自己"
}

internal fun eventTypeLabel(value: String): String = when (value) {
    "offline_live" -> "线下 Live"
    "offline_meetup" -> "小团体面基"
    "online_birthday" -> "线上生日合集"
    else -> value
}

internal fun formatBytes(value: Long): String {
    val units = listOf("B", "KiB", "MiB", "GiB")
    var size = value.toDouble()
    var index = 0
    while (size >= 1024 && index < units.lastIndex) {
        size /= 1024
        index += 1
    }
    return if (index == 0) "${value} ${units[index]}" else "%.1f %s".format(size, units[index])
}

internal fun shortDate(value: String?): String = value?.take(10)?.takeIf(String::isNotBlank) ?: "刚刚"
