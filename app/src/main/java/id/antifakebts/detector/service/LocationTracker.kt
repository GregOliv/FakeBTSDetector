package id.antifakebts.detector.service

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager

class LocationTracker(private val context: Context) {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    @SuppressLint("MissingPermission")
    fun getLastKnownCoarseLocation(): Pair<Double, Double>? {
        val lm = locationManager ?: return null
        return try {
            val networkLoc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            val passiveLoc = lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
            val bestLocation: Location? = when {
                networkLoc != null && passiveLoc != null -> {
                    if (networkLoc.time >= passiveLoc.time) networkLoc else passiveLoc
                }
                networkLoc != null -> networkLoc
                else -> passiveLoc
            }

            bestLocation?.let {
                Pair(it.latitude, it.longitude)
            }
        } catch (e: SecurityException) {
            null
        }
    }
}
