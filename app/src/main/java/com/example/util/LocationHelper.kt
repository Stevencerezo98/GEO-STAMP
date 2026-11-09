package com.example.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

data class LocationData(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val altitude: Double? = null,
    val address: String = ""
)

object LocationHelper {

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context): LocationData = withContext(Dispatchers.IO) {
        try {
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            val cts = CancellationTokenSource()

            val location: Location? = suspendCancellableCoroutine { cont ->
                fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                    .addOnSuccessListener { loc ->
                        if (loc != null) {
                            cont.resume(loc)
                        } else {
                            // Fallback to lastLocation
                            fusedClient.lastLocation.addOnSuccessListener { lastLoc ->
                                cont.resume(lastLoc)
                            }.addOnFailureListener {
                                cont.resume(null)
                            }
                        }
                    }
                    .addOnFailureListener {
                        cont.resume(null)
                    }
            }

            if (location != null) {
                val address = reverseGeocode(context, location.latitude, location.longitude)
                LocationData(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    altitude = if (location.hasAltitude()) location.altitude else null,
                    address = address
                )
            } else {
                LocationData(
                    latitude = null,
                    longitude = null,
                    altitude = null,
                    address = "Ubicación no disponible"
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            LocationData(
                latitude = null,
                longitude = null,
                altitude = null,
                address = ""
            )
        }
    }

    suspend fun reverseGeocode(context: Context, latitude: Double, longitude: Double): String =
        withContext(Dispatchers.IO) {
            try {
                if (!Geocoder.isPresent()) {
                    return@withContext String.format(Locale.getDefault(), "Lat: %.5f, Lon: %.5f", latitude, longitude)
                }
                val geocoder = Geocoder(context, Locale.getDefault())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    var resultAddress = ""
                    val addresses: List<Address> = suspendCancellableCoroutine { cont ->
                        geocoder.getFromLocation(latitude, longitude, 1, object : Geocoder.GeocodeListener {
                            override fun onGeocode(addresses: MutableList<Address>) {
                                cont.resume(addresses)
                            }
                            override fun onError(errorMessage: String?) {
                                cont.resume(emptyList())
                            }
                        })
                    }
                    if (addresses.isNotEmpty()) {
                        val addr = addresses[0]
                        resultAddress = formatAddress(addr)
                    }
                    if (resultAddress.isNotEmpty()) resultAddress
                    else String.format(Locale.getDefault(), "Lat: %.5f, Lon: %.5f", latitude, longitude)
                } else {
                    @Suppress("DEPRECATION")
                    val list = geocoder.getFromLocation(latitude, longitude, 1)
                    if (!list.isNullOrEmpty()) {
                        formatAddress(list[0])
                    } else {
                        String.format(Locale.getDefault(), "Lat: %.5f, Lon: %.5f", latitude, longitude)
                    }
                }
            } catch (e: Exception) {
                String.format(Locale.getDefault(), "Lat: %.5f, Lon: %.5f", latitude, longitude)
            }
        }

    private fun formatAddress(address: Address): String {
        val parts = mutableListOf<String>()
        address.thoroughfare?.let { thoroughfare ->
            val feature = address.subThoroughfare
            if (feature != null) parts.add("$thoroughfare $feature") else parts.add(thoroughfare)
        }
        address.subLocality?.let { parts.add(it) }
        address.locality?.let { parts.add(it) }
        address.adminArea?.let { parts.add(it) }
        address.countryName?.let { parts.add(it) }

        return if (parts.isNotEmpty()) parts.joinToString(", ")
        else address.getAddressLine(0) ?: ""
    }
}
