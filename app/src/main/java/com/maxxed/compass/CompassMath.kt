package com.maxxed.compass

import android.view.Surface
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

object CompassMath {
    private val cardinalPoints = listOf(
        "N", "NNE", "NE", "ENE",
        "E", "ESE", "SE", "SSE",
        "S", "SSW", "SW", "WSW",
        "W", "WNW", "NW", "NNW"
    )

    fun normalizeDegrees(value: Float): Float {
        var normalized = value % 360f
        if (normalized < 0f) normalized += 360f
        if (normalized >= 360f) normalized -= 360f
        return normalized
    }

    fun cardinalFor(heading: Float): String {
        val normalized = normalizeDegrees(heading)
        val index = floor((normalized + 11.25f) / 22.5f).toInt() % cardinalPoints.size
        return cardinalPoints[index]
    }

    fun applyDisplayRotation(azimuth: Float, displayRotation: Int): Float = when (displayRotation) {
        Surface.ROTATION_90 -> normalizeDegrees(azimuth + 90f)
        Surface.ROTATION_180 -> normalizeDegrees(azimuth + 180f)
        Surface.ROTATION_270 -> normalizeDegrees(azimuth + 270f)
        else -> normalizeDegrees(azimuth)
    }

    fun trueHeading(magnetic: Float, declination: Float?): Float =
        normalizeDegrees(magnetic + (declination ?: 0f))

    fun shortestSignedDelta(from: Float, to: Float): Float {
        val delta = normalizeDegrees(to) - normalizeDegrees(from)
        return when {
            delta > 180f -> delta - 360f
            delta < -180f -> delta + 360f
            else -> delta
        }
    }

    fun smoothHeading(previous: Float?, newHeading: Float, smoothing: Float): Float {
        if (previous == null) return normalizeDegrees(newHeading)
        val clamped = smoothing.coerceIn(0.02f, 0.85f)
        val delta = shortestSignedDelta(previous, newHeading)
        return normalizeDegrees(previous + delta * clamped)
    }

    fun fieldStrengthMicroTesla(values: FloatArray): Float {
        return sqrt(values[0].pow(2) + values[1].pow(2) + values[2].pow(2))
    }

    fun likelyInterference(strengthMicroTesla: Float?, recentHeadings: List<Float>): Boolean {
        val strengthProblem = strengthMicroTesla != null && (strengthMicroTesla < 25f || strengthMicroTesla > 65f)
        if (strengthProblem) return true
        if (recentHeadings.size < 6) return false
        val deltas = recentHeadings.zipWithNext { a, b -> abs(shortestSignedDelta(a, b)) }
        return deltas.average() > 18.0
    }

    fun clinometerDegrees(gravity: FloatArray): Float {
        val gx = gravity.getOrElse(0) { 0f }
        val gy = gravity.getOrElse(1) { 0f }
        val gz = gravity.getOrElse(2) { 0f }
        return Math.toDegrees(atan2(gx.toDouble(), sqrt((gy * gy + gz * gz).toDouble()))).toFloat()
    }

    fun levelBubble(gravity: FloatArray): Pair<Float, Float> {
        val gx = gravity.getOrElse(0) { 0f }.coerceIn(-9.81f, 9.81f) / 9.81f
        val gy = gravity.getOrElse(1) { 0f }.coerceIn(-9.81f, 9.81f) / 9.81f
        return gx to gy
    }

    fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return 2 * earthRadius * atan2(sqrt(a), sqrt(1 - a))
    }

    fun bearingBetween(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val lambda = Math.toRadians(lon2 - lon1)
        val y = sin(lambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(lambda)
        return normalizeDegrees(Math.toDegrees(atan2(y, x)).toFloat())
    }

    fun distanceAndBearingToWaypoint(current: TripPoint, waypoint: Waypoint): DistanceBearing {
        return DistanceBearing(
            distanceMeters = haversineMeters(current.latitude, current.longitude, waypoint.latitude, waypoint.longitude),
            bearingDegrees = bearingBetween(current.latitude, current.longitude, waypoint.latitude, waypoint.longitude)
        )
    }

    fun metersToDistanceText(meters: Double, units: Units): String {
        return when (units) {
            Units.METRIC -> if (meters >= 1000.0) "%.2f km".format(meters / 1000.0) else "${meters.roundToInt()} m"
            Units.IMPERIAL -> {
                val feet = meters * 3.28084
                if (feet >= 5280.0) "%.2f mi".format(feet / 5280.0) else "${feet.roundToInt()} ft"
            }
        }
    }

    fun metersPerSecondText(speed: Double, units: Units): String {
        return when (units) {
            Units.METRIC -> "%.1f km/h".format(speed * 3.6)
            Units.IMPERIAL -> "%.1f mph".format(speed * 2.23694)
        }
    }
}
