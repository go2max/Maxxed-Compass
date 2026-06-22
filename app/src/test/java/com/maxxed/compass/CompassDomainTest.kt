package com.maxxed.compass

import android.view.Surface
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CompassDomainTest {
    @Test
    fun normalizeHeadingWrapsIntoRange() {
        assertEquals(350f, CompassMath.normalizeDegrees(-10f), 0.001f)
        assertEquals(10f, CompassMath.normalizeDegrees(370f), 0.001f)
    }

    @Test
    fun displayRotationMappingAppliesExpectedOffsets() {
        assertEquals(180f, CompassMath.applyDisplayRotation(90f, Surface.ROTATION_90), 0.001f)
        assertEquals(270f, CompassMath.applyDisplayRotation(90f, Surface.ROTATION_180), 0.001f)
    }

    @Test
    fun cardinalBoundariesMapCorrectly() {
        assertEquals("N", CompassMath.cardinalFor(0f))
        assertEquals("NNE", CompassMath.cardinalFor(20f))
        assertEquals("E", CompassMath.cardinalFor(90f))
        assertEquals("NNW", CompassMath.cardinalFor(340f))
    }

    @Test
    fun smoothingHandlesWraparoundNearNorth() {
        val smoothed = CompassMath.smoothHeading(359f, 1f, 0.5f)
        assertTrue(smoothed < 360f)
        assertTrue(smoothed < 5f || smoothed > 355f)
    }

    @Test
    fun trueNorthAddsDeclinationWithCorrectSign() {
        assertEquals(100f, CompassMath.trueHeading(90f, 10f), 0.001f)
        assertEquals(80f, CompassMath.trueHeading(90f, -10f), 0.001f)
        assertEquals(5f, CompassMath.trueHeading(355f, 10f), 0.001f)
    }

    @Test
    fun interferenceTransitionsAccuracyDownward() {
        assertEquals(AccuracyState.REDUCED, AccuracyState.GOOD.fromAndroidAccuracy(90f, false))
        assertEquals(AccuracyState.LOW, AccuracyState.REDUCED.fromAndroidAccuracy(10f, false))
        assertEquals(AccuracyState.LOW, AccuracyState.LOW.fromAndroidAccuracy(50f, true))
    }

    @Test
    fun tripFilterRejectsGpsJumpsAndPausedAccumulation() {
        val first = TripPoint(1_000L, 34.0, -118.0, 10.0, 4f, 1f, 90f)
        val jump = TripPoint(2_000L, 35.0, -118.0, 10.0, 4f, 90f, 90f)
        val valid = TripPoint(6_000L, 34.0005, -118.0005, 15.0, 5f, 1.5f, 90f)
        assertFalse(TripMath.shouldAcceptPoint(first, jump))
        assertTrue(TripMath.shouldAcceptPoint(first, valid))

        val trip = TripRecord(
            createdAtMillis = 0L,
            startedAtMillis = 0L,
            paused = true,
            points = listOf(first)
        )
        val updated = TripMath.updateTrip(trip, valid, 10_000L)
        assertEquals(1, updated.points.size)
    }

    @Test
    fun elevationFilterIgnoresNoiseButCapturesRealGainLoss() {
        val first = TripPoint(1_000L, 34.0, -118.0, 100.0, 4f, null, null)
        val noisy = TripPoint(2_000L, 34.0, -118.0, 101.0, 4f, null, null)
        val climb = TripPoint(3_000L, 34.0, -118.0, 106.0, 4f, null, null)
        val drop = TripPoint(4_000L, 34.0, -118.0, 96.0, 4f, null, null)
        assertEquals(0.0, TripMath.elevationDelta(first, noisy).first, 0.001)
        assertEquals(6.0, TripMath.elevationDelta(first, climb).first, 0.001)
        assertEquals(4.0, TripMath.elevationDelta(first, drop).second, 0.001)
    }

    @Test
    fun reducerTransitionsTrackingStatesSafely() {
        assertEquals(TrackingState.ACTIVE, TrackingReducer.reduce(TrackingState.IDLE, ServiceAction.Start))
        assertEquals(TrackingState.PAUSED, TrackingReducer.reduce(TrackingState.ACTIVE, ServiceAction.Pause))
        assertEquals(TrackingState.ACTIVE, TrackingReducer.reduce(TrackingState.PAUSED, ServiceAction.Resume))
        assertEquals(TrackingState.IDLE, TrackingReducer.reduce(TrackingState.ACTIVE, ServiceAction.Stop))
    }

    @Test
    fun waypointBearingAndDistanceReturnReasonableValues() {
        val point = TripPoint(1_000L, 34.0, -118.0, null, 4f, null, null)
        val waypoint = Waypoint("Target", 34.001, -118.0)
        val result = CompassMath.distanceAndBearingToWaypoint(point, waypoint)
        assertTrue(result.distanceMeters > 100.0)
        assertEquals(0f, result.bearingDegrees, 15f)
    }

    @Test
    fun clinometerCalculationRespondsToTilt() {
        val degrees = CompassMath.clinometerDegrees(floatArrayOf(4.9f, 0f, 8.49f))
        assertTrue(degrees > 20f)
        assertTrue(degrees < 40f)
    }

    @Test
    fun skyMathFindsPolarisAndNearestObject() {
        val objects = SkyMath.visibleObjects(
            timeMillis = 1_704_067_200_000L,
            latitude = 47.6062,
            longitude = -122.3321
        )
        assertTrue(objects.any { it.id == "polaris" })
        val nearest = SkyMath.nearestToCenter(objects, 0.0, 40.0)
        assertNotNull(nearest)
    }
}
