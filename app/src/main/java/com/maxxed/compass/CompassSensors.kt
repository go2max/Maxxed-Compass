package com.maxxed.compass

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.ArrayDeque

data class CompassReading(
    val magneticHeading: Float?,
    val gravity: FloatArray,
    val confidence: AccuracyState,
    val sensorMode: SensorMode,
    val fieldStrengthMicroTesla: Float?,
    val interferenceDetected: Boolean
)

class CompassSensorController(context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    private val latestGravity = FloatArray(3)
    private val latestMagnetic = FloatArray(3)
    private val recentHeadings = ArrayDeque<Float>()
    private var currentAccuracy = AccuracyState.UNAVAILABLE
    private var displayRotation: Int = Surface.ROTATION_0
    private var emitter: (CompassReading) -> Unit = {}

    fun setDisplayRotation(rotation: Int) {
        displayRotation = rotation
    }

    fun readings(): Flow<CompassReading> = callbackFlow {
        emitter = { trySend(it).isSuccess }
        val mode = when {
            rotationVector != null -> {
                sensorManager.registerListener(this@CompassSensorController, rotationVector, SensorManager.SENSOR_DELAY_GAME)
                SensorMode.ROTATION_VECTOR
            }
            accelerometer != null && magnetometer != null -> {
                sensorManager.registerListener(this@CompassSensorController, accelerometer, SensorManager.SENSOR_DELAY_GAME)
                sensorManager.registerListener(this@CompassSensorController, magnetometer, SensorManager.SENSOR_DELAY_GAME)
                SensorMode.ACCEL_MAG
            }
            else -> SensorMode.UNAVAILABLE
        }
        if (mode == SensorMode.UNAVAILABLE) {
            trySend(
                CompassReading(
                    magneticHeading = null,
                    gravity = floatArrayOf(0f, 0f, 9.81f),
                    confidence = AccuracyState.UNAVAILABLE,
                    sensorMode = SensorMode.UNAVAILABLE,
                    fieldStrengthMicroTesla = null,
                    interferenceDetected = false
                )
            )
        }
        awaitClose { sensorManager.unregisterListener(this@CompassSensorController) }
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> emitFromRotationVector(event.values)
            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(event.values, 0, latestGravity, 0, 3)
                emitFromAccelMag()
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                System.arraycopy(event.values, 0, latestMagnetic, 0, 3)
                emitFromAccelMag()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        currentAccuracy = sensorAccuracyToState(accuracy)
    }

    private fun emitFromRotationVector(values: FloatArray) {
        val rotationMatrix = FloatArray(9)
        SensorManager.getRotationMatrixFromVector(rotationMatrix, values)
        val orientation = FloatArray(3)
        SensorManager.getOrientation(rotationMatrix, orientation)
        val heading = CompassMath.applyDisplayRotation(Math.toDegrees(orientation[0].toDouble()).toFloat(), displayRotation)
        recordAndEmit(
            heading = heading,
            gravity = latestGravity.copyOf(),
            mode = SensorMode.ROTATION_VECTOR,
            fieldStrength = null
        )
    }

    private fun emitFromAccelMag() {
        val rotationMatrix = FloatArray(9)
        if (!SensorManager.getRotationMatrix(rotationMatrix, null, latestGravity, latestMagnetic)) return
        val orientation = FloatArray(3)
        SensorManager.getOrientation(rotationMatrix, orientation)
        val heading = CompassMath.applyDisplayRotation(Math.toDegrees(orientation[0].toDouble()).toFloat(), displayRotation)
        recordAndEmit(
            heading = heading,
            gravity = latestGravity.copyOf(),
            mode = SensorMode.ACCEL_MAG,
            fieldStrength = CompassMath.fieldStrengthMicroTesla(latestMagnetic)
        )
    }

    private fun recordAndEmit(heading: Float, gravity: FloatArray, mode: SensorMode, fieldStrength: Float?) {
        val normalized = CompassMath.normalizeDegrees(heading)
        recentHeadings += normalized
        while (recentHeadings.size > 10) recentHeadings.removeFirst()
        val interference = CompassMath.likelyInterference(fieldStrength, recentHeadings.toList())
        emitter(
            CompassReading(
                magneticHeading = normalized,
                gravity = gravity,
                confidence = currentAccuracy.fromAndroidAccuracy(fieldStrength, interference),
                sensorMode = mode,
                fieldStrengthMicroTesla = fieldStrength,
                interferenceDetected = interference
            )
        )
    }
}

fun declinationFor(point: TripPoint?, timeMillis: Long): Float? {
    if (point == null) return null
    return GeomagneticField(
        point.latitude.toFloat(),
        point.longitude.toFloat(),
        (point.altitudeMeters ?: 0.0).toFloat(),
        timeMillis
    ).declination
}
