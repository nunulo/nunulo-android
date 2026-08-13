package com.lumokato.nunulo

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

internal const val LOCATION_TIMEOUT_MS = 8_000L
internal const val CURRENT_LOCATION_MAX_AGE_MS = 2 * 60 * 1000L
internal const val LAST_KNOWN_MAX_AGE_MS = 30 * 60 * 1000L
internal const val FINE_LOCATION_MAX_ACCURACY_METERS = 100f
internal const val COARSE_LOCATION_MAX_ACCURACY_METERS = 2_000f

internal data class LocationFix(
    val latitude: Double,
    val longitude: Double,
    val provider: String,
    val capturedAtMillis: Long,
    val accuracyMeters: Float?,
    val isLastKnown: Boolean,
)

internal fun Location.toFix(isLastKnown: Boolean = false): LocationFix = LocationFix(
    latitude = latitude,
    longitude = longitude,
    provider = provider.orEmpty(),
    capturedAtMillis = time,
    accuracyMeters = accuracy.takeIf { hasAccuracy() },
    isLastKnown = isLastKnown,
)

internal fun LocationFix.isUsable(nowMillis: Long, coarseOnly: Boolean, allowLastKnown: Boolean): Boolean {
    val maxAge = if (allowLastKnown) LAST_KNOWN_MAX_AGE_MS else CURRENT_LOCATION_MAX_AGE_MS
    val maxAccuracy = if (coarseOnly) COARSE_LOCATION_MAX_ACCURACY_METERS else FINE_LOCATION_MAX_ACCURACY_METERS
    val age = (nowMillis - capturedAtMillis).coerceAtLeast(0L)
    return latitude in -90.0..90.0 && longitude in -180.0..180.0 && age <= maxAge &&
        (accuracyMeters == null || accuracyMeters <= maxAccuracy)
}

internal fun chooseBestLocation(
    fixes: List<LocationFix>,
    nowMillis: Long,
    coarseOnly: Boolean,
    allowLastKnown: Boolean,
): LocationFix? = fixes
    .filter { (!it.isLastKnown || allowLastKnown) && it.isUsable(nowMillis, coarseOnly, allowLastKnown = it.isLastKnown) }
    .minWithOrNull(compareBy<LocationFix> { it.isLastKnown }.thenBy { it.accuracyMeters ?: Float.MAX_VALUE }.thenByDescending { it.capturedAtMillis })

internal fun hasLocationPermission(context: Context): Boolean {
    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    return fine || coarse
}

internal suspend fun currentLocation(context: Context): LocationFix? {
    if (!hasLocationPermission(context)) return null
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val enabledProviders = manager.getProviders(true)
    val coarseOnly = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
    val current = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) coroutineScope {
        enabledProviders.map { provider -> async { providerLocation(context, manager, provider) } }.mapNotNull { it.await() }
    } else emptyList()
    val now = System.currentTimeMillis()
    chooseBestLocation(current, now, coarseOnly, allowLastKnown = false)?.let { return it }
    return chooseBestLocation(lastKnownLocations(manager), now, coarseOnly, allowLastKnown = true)
}

private suspend fun providerLocation(context: Context, manager: LocationManager, provider: String): LocationFix? =
    withTimeoutOrNull(LOCATION_TIMEOUT_MS) { suspendCancellableCoroutine { continuation ->
        val cancellationSignal = CancellationSignal()
        continuation.invokeOnCancellation { cancellationSignal.cancel() }
        runCatching {
            manager.getCurrentLocation(provider, cancellationSignal, ContextCompat.getMainExecutor(context)) { location ->
                if (continuation.isActive) continuation.resume(location?.toFix())
            }
        }.onFailure {
            if (continuation.isActive) continuation.resume(null)
        }
    } }

private fun lastKnownLocations(manager: LocationManager): List<LocationFix> = manager.getProviders(true)
    .mapNotNull { provider -> runCatching { manager.getLastKnownLocation(provider)?.toFix(isLastKnown = true) }.getOrNull() }
