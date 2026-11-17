package com.example.system

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

data class CapturedLocation(
    val latitude: Double,
    val longitude: Double,
    val locationName: String?,
    val cityName: String?,
    val areaName: String?,
    val streetName: String?
)

class LocationHelper(private val context: Context) {

    fun hasLocationPermission(): Boolean {
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return coarse || fine
    }

    @SuppressLint("MissingPermission")
    suspend fun fetchCurrentLocation(): CapturedLocation? {
        if (!hasLocationPermission()) return null
        val client = LocationServices.getFusedLocationProviderClient(context)

        val location = suspendCancellableCoroutine<android.location.Location?> { cont ->
            val cts = CancellationTokenSource()
            client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
                .addOnSuccessListener { current ->
                    if (current != null) {
                        cont.resume(current)
                    } else {
                        client.lastLocation
                            .addOnSuccessListener { last -> cont.resume(last) }
                            .addOnFailureListener { cont.resume(null) }
                    }
                }
                .addOnFailureListener {
                    client.lastLocation
                        .addOnSuccessListener { last -> cont.resume(last) }
                        .addOnFailureListener { cont.resume(null) }
                }
            cont.invokeOnCancellation { cts.cancel() }
        } ?: return null

        return reverseGeocode(location.latitude, location.longitude)
    }

    suspend fun reverseGeocode(latitude: Double, longitude: Double): CapturedLocation {
        val resolved = withContext(Dispatchers.IO) {
            runCatching {
                if (!Geocoder.isPresent()) return@runCatching null
                val geocoder = Geocoder(context, Locale.getDefault())
                @Suppress("DEPRECATION")
                val addresses = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    suspendCancellableCoroutine { cont ->
                        geocoder.getFromLocation(latitude, longitude, 1) { list ->
                            cont.resume(list)
                        }
                    }
                } else {
                    geocoder.getFromLocation(latitude, longitude, 1)
                }
                addresses?.firstOrNull()
            }.getOrNull()
        }

        val city = resolved?.locality ?: resolved?.subAdminArea ?: resolved?.adminArea
        val area = resolved?.subLocality ?: resolved?.featureName
        val street = resolved?.thoroughfare ?: resolved?.subThoroughfare
        val display = listOfNotNull(street, area, city)
            .distinct()
            .filter { it.isNotBlank() }
            .joinToString(", ")
            .ifBlank { "${"%.4f".format(latitude)}, ${"%.4f".format(longitude)}" }

        return CapturedLocation(
            latitude = latitude,
            longitude = longitude,
            locationName = display,
            cityName = city,
            areaName = area,
            streetName = street
        )
    }
}
