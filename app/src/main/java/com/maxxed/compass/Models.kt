package com.maxxed.compass

import android.hardware.SensorManager
import kotlinx.serialization.Serializable
import java.util.UUID

enum class ThemeChoice { SYSTEM, LIGHT, DARK }

enum class SkinChoice { CLASSIC, MARINE }

enum class NorthReference { MAGNETIC, TRUE }

enum class Units { IMPERIAL, METRIC }

enum class BatteryMode { ECONOMY, BALANCED, PRECISION }

enum class AccuracyState { GOOD, REDUCED, LOW, UNAVAILABLE }

enum class SensorMode { ROTATION_VECTOR, ACCEL_MAG, UNAVAILABLE }

enum class TrackingState { IDLE, ACTIVE, PAUSED }

enum class PermissionPrompt { LOCATION, CAMERA, NOTIFICATIONS }

@Serializable
data class AppSettings(
    val themeChoice: ThemeChoice = ThemeChoice.SYSTEM,
    val skinChoice: SkinChoice = SkinChoice.CLASSIC,
    val northReference: NorthReference = NorthReference.MAGNETIC,
    val units: Units = Units.IMPERIAL,
    val batteryMode: BatteryMode = BatteryMode.BALANCED,
    val advancedMode: Boolean = false,
    val nightMode: Boolean = false,
    val keepScreenOn: Boolean = false,
    val smoothing: Float = 0.18f,
    val calibrationNagDismissed: Boolean = false,
    val hiddenConstellationIds: Set<String> = emptySet()
)

data class HeadingSample(
    val magneticHeading: Float? = null,
    val trueHeading: Float? = null,
    val cardinal: String = "--",
    val confidence: AccuracyState = AccuracyState.UNAVAILABLE,
    val sensorMode: SensorMode = SensorMode.UNAVAILABLE,
    val interferenceDetected: Boolean = false,
    val status: String = "Sensors unavailable",
    val declinationDegrees: Float? = null,
    val fieldStrengthMicroTesla: Float? = null
)

data class CompassUiState(
    val headingSample: HeadingSample = HeadingSample(),
    val settings: AppSettings = AppSettings(),
    val trackingState: TrackingState = TrackingState.IDLE,
    val activeTrip: TripRecord? = null,
    val tripHistory: List<TripRecord> = emptyList(),
    val selectedTripId: String? = null,
    val waypoint: Waypoint? = null,
    val coordinatesText: String = "Waiting for GPS",
    val lockScreenVisible: Boolean = false,
    val skyState: SkyUiState = SkyUiState(),
    val showCalibrationOverlay: Boolean = false,
    val calibrationMessage: String? = null,
    val unavailableReason: String? = null,
    val pendingPermission: PermissionPrompt? = null
)

@Serializable
data class TripPoint(
    val timeMillis: Long,
    val latitude: Double,
    val longitude: Double,
    val altitudeMeters: Double?,
    val accuracyMeters: Float,
    val speedMps: Float?,
    val bearingDegrees: Float?
)

@Serializable
data class TripSegment(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val startedAtMillis: Long,
    val endedAtMillis: Long? = null,
    val distanceMeters: Double = 0.0
)

@Serializable
data class TripStats(
    val elapsedMillis: Long = 0L,
    val movingMillis: Long = 0L,
    val distanceMeters: Double = 0.0,
    val currentSpeedMps: Double = 0.0,
    val averageSpeedMps: Double = 0.0,
    val elevationGainMeters: Double = 0.0,
    val elevationLossMeters: Double = 0.0,
    val bearingDegrees: Float? = null
)

@Serializable
data class TripRecord(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "New trip",
    val createdAtMillis: Long,
    val startedAtMillis: Long,
    val endedAtMillis: Long? = null,
    val paused: Boolean = false,
    val pauseStartedAtMillis: Long? = null,
    val totalPausedMillis: Long = 0L,
    val segments: List<TripSegment> = emptyList(),
    val points: List<TripPoint> = emptyList(),
    val stats: TripStats = TripStats()
)

@Serializable
data class Waypoint(
    val name: String,
    val latitude: Double,
    val longitude: Double
)

data class DistanceBearing(
    val distanceMeters: Double,
    val bearingDegrees: Float
)

data class SkyObject(
    val id: String,
    val name: String,
    val type: String,
    val azimuthDegrees: Double,
    val altitudeDegrees: Double,
    val description: String
)

data class SkyPoint(
    val id: String,
    val name: String,
    val azimuthDegrees: Double,
    val altitudeDegrees: Double
)

data class SkyLine(
    val fromPointId: String,
    val toPointId: String
)

data class ConstellationOverlay(
    val id: String,
    val name: String,
    val points: List<SkyPoint>,
    val lines: List<SkyLine>
)

data class ConstellationOption(
    val id: String,
    val name: String
)

data class SkyUiState(
    val useCamera: Boolean = false,
    val searchQuery: String = "",
    val nearestObject: SkyObject? = null,
    val visibleObjects: List<SkyObject> = emptyList(),
    val constellationOverlays: List<ConstellationOverlay> = emptyList(),
    val constellationOptions: List<ConstellationOption> = emptyList(),
    val enabledConstellationIds: Set<String> = emptySet(),
    val status: String = "Location and orientation needed",
    val cameraPermissionGranted: Boolean = false
)

fun AccuracyState.fromAndroidAccuracy(fieldStrength: Float?, unstable: Boolean): AccuracyState {
    if (this == AccuracyState.UNAVAILABLE) return this
    if (fieldStrength != null && (fieldStrength < 25f || fieldStrength > 65f || unstable)) {
        return when (this) {
            AccuracyState.GOOD -> AccuracyState.REDUCED
            AccuracyState.REDUCED -> AccuracyState.LOW
            AccuracyState.LOW, AccuracyState.UNAVAILABLE -> this
        }
    }
    return this
}

fun sensorAccuracyToState(accuracy: Int): AccuracyState = when (accuracy) {
    SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> AccuracyState.GOOD
    SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> AccuracyState.REDUCED
    SensorManager.SENSOR_STATUS_ACCURACY_LOW -> AccuracyState.LOW
    else -> AccuracyState.UNAVAILABLE
}
