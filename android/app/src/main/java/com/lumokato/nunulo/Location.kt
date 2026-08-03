package com.lumokato.nunulo

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

internal fun hasLocationPermission(context: Context): Boolean {
    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    return fine || coarse
}

internal suspend fun currentLocation(context: Context): Location? {
    if (!hasLocationPermission(context)) return null
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val enabledProviders = manager.getProviders(true)
    val provider = when {
        LocationManager.GPS_PROVIDER in enabledProviders -> LocationManager.GPS_PROVIDER
        LocationManager.NETWORK_PROVIDER in enabledProviders -> LocationManager.NETWORK_PROVIDER
        enabledProviders.isNotEmpty() -> enabledProviders.first()
        else -> return lastKnownLocation(context)
    }
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return lastKnownLocation(context)
    return suspendCancellableCoroutine { continuation ->
        val cancellationSignal = CancellationSignal()
        continuation.invokeOnCancellation { cancellationSignal.cancel() }
        runCatching {
            manager.getCurrentLocation(provider, cancellationSignal, ContextCompat.getMainExecutor(context)) { location ->
                if (continuation.isActive) continuation.resume(location ?: lastKnownLocation(context))
            }
        }.onFailure {
            if (continuation.isActive) continuation.resume(lastKnownLocation(context))
        }
    }
}

internal fun lastKnownLocation(context: Context): Location? {
    if (!hasLocationPermission(context)) return null
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return manager.getProviders(true)
        .mapNotNull { provider -> runCatching { manager.getLastKnownLocation(provider) }.getOrNull() }
        .maxByOrNull { it.time }
}
