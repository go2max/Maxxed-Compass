package com.maxxed.compass

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin

private data class CatalogBody(
    val id: String,
    val name: String,
    val type: String,
    val raHours: Double,
    val decDegrees: Double,
    val description: String
)

object SkyMath {
    private val starCatalog = listOf(
        CatalogBody("polaris", "Polaris", "Star", 2.5303, 89.2641, "North Star used for reliable north guidance."),
        CatalogBody("sirius", "Sirius", "Star", 6.7525, -16.7161, "Brightest night-sky star in Canis Major."),
        CatalogBody("betelgeuse", "Betelgeuse", "Star", 5.9195, 7.4071, "Red supergiant marking Orion's shoulder."),
        CatalogBody("rigel", "Rigel", "Star", 5.2423, -8.2016, "Blue-white Orion star near the western foot."),
        CatalogBody("vega", "Vega", "Star", 18.6156, 38.7837, "Summer Triangle star in Lyra."),
        CatalogBody("altair", "Altair", "Star", 19.8464, 8.8683, "Summer Triangle star in Aquila."),
        CatalogBody("deneb", "Deneb", "Star", 20.6905, 45.2803, "Summer Triangle star in Cygnus.")
    )

    private val constellations = listOf(
        CatalogBody("orion", "Orion", "Constellation", 5.5, 0.0, "Prominent winter constellation with belt stars."),
        CatalogBody("ursa_major", "Ursa Major", "Constellation", 11.0, 55.0, "Contains the Big Dipper asterism."),
        CatalogBody("cassiopeia", "Cassiopeia", "Constellation", 1.0, 60.0, "W-shaped northern constellation.")
    )

    private val planets = listOf(
        CatalogBody("mercury", "Mercury", "Planet", 7.0, 12.0, "Inner planet with fast-changing position."),
        CatalogBody("venus", "Venus", "Planet", 9.5, 15.0, "Very bright inner planet often near twilight."),
        CatalogBody("mars", "Mars", "Planet", 12.0, -2.0, "Red planet with a wandering track against the stars."),
        CatalogBody("jupiter", "Jupiter", "Planet", 3.0, 17.0, "Bright gas giant visible in many seasons."),
        CatalogBody("saturn", "Saturn", "Planet", 22.0, -12.0, "Ringed planet with a steady golden appearance.")
    )

    fun visibleObjects(timeMillis: Long, latitude: Double, longitude: Double): List<SkyObject> {
        val all = buildList {
            addAll(starCatalog.map { toSkyObject(it, timeMillis, latitude, longitude) })
            addAll(constellations.map { toSkyObject(it, timeMillis, latitude, longitude) })
            addAll(planets.map { toSkyObject(shiftPlanet(it, timeMillis), timeMillis, latitude, longitude) })
            add(moon(timeMillis, latitude, longitude))
        }
        return all.sortedByDescending { it.altitudeDegrees }
    }

    fun nearestToCenter(objects: List<SkyObject>, azimuth: Double, altitude: Double): SkyObject? {
        return objects.minByOrNull {
            abs(angleDelta(it.azimuthDegrees, azimuth)) + abs(it.altitudeDegrees - altitude)
        }
    }

    fun polarisGuidance(objects: List<SkyObject>): String {
        val polaris = objects.firstOrNull { it.id == "polaris" } ?: return "Polaris not visible from this hemisphere/time."
        val altitudeRounded = polaris.altitudeDegrees.roundToInt()
        return "Polaris near ${polaris.azimuthDegrees.roundToInt()}° azimuth and ${altitudeRounded}° altitude."
    }

    private fun toSkyObject(body: CatalogBody, timeMillis: Long, latitude: Double, longitude: Double): SkyObject {
        val altAz = equatorialToHorizontal(body.raHours, body.decDegrees, timeMillis, latitude, longitude)
        return SkyObject(body.id, body.name, body.type, altAz.first, altAz.second, body.description)
    }

    private fun shiftPlanet(body: CatalogBody, timeMillis: Long): CatalogBody {
        val days = (timeMillis - 946684800000L) / 86_400_000.0
        val adjustedRa = (body.raHours + days / 365.25 * when (body.id) {
            "mercury" -> 4.2
            "venus" -> 1.6
            "mars" -> 0.5
            "jupiter" -> 0.08
            "saturn" -> 0.03
            else -> 0.0
        }) % 24.0
        val adjustedDec = body.decDegrees + sin(days / 70.0) * when (body.id) {
            "mercury" -> 7.0
            "venus" -> 5.0
            "mars" -> 8.0
            "jupiter" -> 3.0
            "saturn" -> 2.0
            else -> 0.0
        }
        return body.copy(raHours = if (adjustedRa < 0) adjustedRa + 24.0 else adjustedRa, decDegrees = adjustedDec.coerceIn(-28.0, 28.0))
    }

    private fun moon(timeMillis: Long, latitude: Double, longitude: Double): SkyObject {
        val days = (timeMillis - 946684800000L) / 86_400_000.0
        val ra = ((13.1763966 * days + 218.316) / 15.0) % 24.0
        val dec = 5.145 * sin(Math.toRadians(13.064993 * days))
        val altAz = equatorialToHorizontal(ra, dec, timeMillis, latitude, longitude)
        return SkyObject("moon", "Moon", "Moon", altAz.first, altAz.second, "Calculated lunar position for field identification.")
    }

    private fun equatorialToHorizontal(raHours: Double, decDegrees: Double, timeMillis: Long, latitude: Double, longitude: Double): Pair<Double, Double> {
        val lst = localSiderealTime(timeMillis, longitude)
        val hourAngle = Math.toRadians((lst - raHours) * 15.0)
        val latRad = Math.toRadians(latitude)
        val decRad = Math.toRadians(decDegrees)
        val altitude = asin(sin(decRad) * sin(latRad) + cos(decRad) * cos(latRad) * cos(hourAngle))
        val azimuth = atan2(
            -sin(hourAngle) * cos(decRad),
            sin(decRad) * cos(latRad) - cos(decRad) * sin(latRad) * cos(hourAngle)
        )
        return normalizeDegrees(Math.toDegrees(azimuth)) to Math.toDegrees(altitude)
    }

    private fun localSiderealTime(timeMillis: Long, longitude: Double): Double {
        val jd = timeMillis / 86_400_000.0 + 2440587.5
        val t = (jd - 2451545.0) / 36525.0
        val gst = 280.46061837 + 360.98564736629 * (jd - 2451545.0) + 0.000387933 * t * t - t * t * t / 38710000.0
        return ((gst + longitude) / 15.0).let {
            val wrapped = it % 24.0
            if (wrapped < 0) wrapped + 24.0 else wrapped
        }
    }

    private fun angleDelta(a: Double, b: Double): Double {
        var delta = normalizeDegrees(a) - normalizeDegrees(b)
        if (delta > 180) delta -= 360.0
        if (delta < -180) delta += 360.0
        return delta
    }

    private fun normalizeDegrees(value: Double): Double {
        var normalized = value % 360.0
        if (normalized < 0.0) normalized += 360.0
        return normalized
    }
}
