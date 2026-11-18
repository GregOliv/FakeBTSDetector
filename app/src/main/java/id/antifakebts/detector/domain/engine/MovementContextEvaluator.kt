package id.antifakebts.detector.domain.engine

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class MovementContextEvaluator {

    fun isMoving(
        currentLat: Double?,
        currentLon: Double?,
        prevLat: Double?,
        prevLon: Double?,
        timeDeltaMillis: Long
    ): Boolean {
        if (currentLat == null || currentLon == null || prevLat == null || prevLon == null) {
            return false
        }
        if (timeDeltaMillis <= 0) return false

        val distanceMeters = calculateHaversineDistanceMeters(currentLat, currentLon, prevLat, prevLon)
        val speedMps = distanceMeters / (timeDeltaMillis / 1000.0)

        // Speed > 1.5 m/s (~5.4 km/h) indicates active walking, cycling, or transit
        return distanceMeters >= MOVEMENT_DISTANCE_MIN_METERS || speedMps >= WALKING_SPEED_THRESHOLD_MPS
    }

    fun calculateHaversineDistanceMeters(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val earthRadiusMeters = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadiusMeters * c
    }

    companion object {
        const val MOVEMENT_DISTANCE_MIN_METERS = 200.0
        const val WALKING_SPEED_THRESHOLD_MPS = 1.4
    }
}
