package com.maxxed.compass

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs

sealed interface ServiceAction {
    data object Start : ServiceAction
    data object Pause : ServiceAction
    data object Resume : ServiceAction
    data object Stop : ServiceAction
}

object TrackingReducer {
    fun reduce(current: TrackingState, action: ServiceAction): TrackingState = when (action) {
        ServiceAction.Start -> TrackingState.ACTIVE
        ServiceAction.Pause -> if (current == TrackingState.ACTIVE) TrackingState.PAUSED else current
        ServiceAction.Resume -> if (current == TrackingState.PAUSED) TrackingState.ACTIVE else current
        ServiceAction.Stop -> TrackingState.IDLE
    }
}

object TripMath {
    fun shouldAcceptPoint(previous: TripPoint?, next: TripPoint): Boolean {
        if (previous == null) return next.accuracyMeters <= 60f
        val deltaMillis = next.timeMillis - previous.timeMillis
        if (deltaMillis <= 0L || deltaMillis > 60_000L * 15) return false
        if (next.accuracyMeters > 60f) return false
        val distance = CompassMath.haversineMeters(previous.latitude, previous.longitude, next.latitude, next.longitude)
        val impliedSpeed = distance / (deltaMillis / 1000.0)
        if (impliedSpeed > 45.0) return false
        if (distance < next.accuracyMeters * 0.5) return false
        return true
    }

    fun elevationDelta(previous: TripPoint?, next: TripPoint): Pair<Double, Double> {
        val prevAlt = previous?.altitudeMeters ?: return 0.0 to 0.0
        val nextAlt = next.altitudeMeters ?: return 0.0 to 0.0
        val delta = nextAlt - prevAlt
        if (abs(delta) < 2.5) return 0.0 to 0.0
        return if (delta > 0) delta to 0.0 else 0.0 to abs(delta)
    }

    fun updateTrip(trip: TripRecord, next: TripPoint, nowMillis: Long): TripRecord {
        if (trip.paused) return trip
        val previous = trip.points.lastOrNull()
        if (!shouldAcceptPoint(previous, next)) {
            return trip.copy(stats = trip.stats.copy(elapsedMillis = elapsedMillis(trip, nowMillis)))
        }
        val distance = previous?.let {
            CompassMath.haversineMeters(it.latitude, it.longitude, next.latitude, next.longitude)
        } ?: 0.0
        val movingMillis = trip.stats.movingMillis + if (distance > 0.5) (next.timeMillis - (previous?.timeMillis ?: next.timeMillis)).coerceAtLeast(0L) else 0L
        val currentSpeed = next.speedMps?.toDouble() ?: if (previous != null) distance / ((next.timeMillis - previous.timeMillis).coerceAtLeast(1L) / 1000.0) else 0.0
        val gainLoss = elevationDelta(previous, next)
        val totalDistance = trip.stats.distanceMeters + distance
        val totalMovingHours = movingMillis / 1000.0
        val avgSpeed = if (totalMovingHours > 0.0) totalDistance / totalMovingHours else 0.0
        val lastSegment = trip.segments.lastOrNull()
        val updatedSegments = if (lastSegment != null) {
            trip.segments.dropLast(1) + lastSegment.copy(distanceMeters = lastSegment.distanceMeters + distance)
        } else {
            trip.segments
        }
        return trip.copy(
            points = trip.points + next,
            segments = updatedSegments,
            stats = trip.stats.copy(
                elapsedMillis = elapsedMillis(trip, nowMillis),
                movingMillis = movingMillis,
                distanceMeters = totalDistance,
                currentSpeedMps = currentSpeed,
                averageSpeedMps = avgSpeed,
                elevationGainMeters = trip.stats.elevationGainMeters + gainLoss.first,
                elevationLossMeters = trip.stats.elevationLossMeters + gainLoss.second,
                bearingDegrees = next.bearingDegrees
            )
        )
    }

    fun elapsedMillis(trip: TripRecord, nowMillis: Long): Long {
        val pauseAdjustment = if (trip.paused) (trip.pauseStartedAtMillis?.let { nowMillis - it } ?: 0L) else 0L
        return (nowMillis - trip.startedAtMillis - trip.totalPausedMillis - pauseAdjustment).coerceAtLeast(0L)
    }
}

object TrackingRepository {
    private val internalState = MutableStateFlow(TrackingState.IDLE)
    val state: StateFlow<TrackingState> = internalState.asStateFlow()

    fun setState(state: TrackingState) {
        internalState.value = state
    }
}

class TrackingService : Service(), LocationListener {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var storage: AppStorage
    private lateinit var locationManager: LocationManager
    private var activeTrip: TripRecord? = null

    override fun onCreate() {
        super.onCreate()
        storage = AppStorage(this)
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        ensureChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                TrackingRepository.setState(TrackingState.ACTIVE)
                startForeground(NOTIFICATION_ID, buildNotification(TrackingState.ACTIVE))
                beginUpdates()
            }
            ACTION_PAUSE -> pauseTrip()
            ACTION_RESUME -> resumeTrip()
            ACTION_STOP -> stopTrip()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        locationManager.removeUpdates(this)
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onLocationChanged(location: Location) {
        val current = activeTrip ?: return
        val point = TripPoint(
            timeMillis = location.time.takeIf { it > 0 } ?: System.currentTimeMillis(),
            latitude = location.latitude,
            longitude = location.longitude,
            altitudeMeters = location.altitude.takeIf { location.hasAltitude() },
            accuracyMeters = location.accuracy,
            speedMps = location.speed.takeIf { location.hasSpeed() },
            bearingDegrees = location.bearing.takeIf { location.hasBearing() }
        )
        activeTrip = TripMath.updateTrip(current, point, System.currentTimeMillis())
        persist()
    }

    private fun beginUpdates() {
        scope.launch {
            activeTrip = storage.activeTripFlow.mapLatestOnce() ?: TripRecord(
                name = "Trip ${System.currentTimeMillis()}",
                createdAtMillis = System.currentTimeMillis(),
                startedAtMillis = System.currentTimeMillis(),
                segments = listOf(TripSegment(name = "Segment 1", startedAtMillis = System.currentTimeMillis()))
            )
            persist()
        }
        val provider = when {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> null
        } ?: return
        val minTime = when (TrackingRepository.state.value) {
            TrackingState.ACTIVE -> 2000L
            TrackingState.PAUSED, TrackingState.IDLE -> 10_000L
        }
        val minDistance = 1f
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            locationManager.requestLocationUpdates(provider, minTime, minDistance, this)
        }
    }

    private fun pauseTrip() {
        val trip = activeTrip ?: return
        activeTrip = trip.copy(paused = true, pauseStartedAtMillis = System.currentTimeMillis())
        TrackingRepository.setState(TrackingState.PAUSED)
        locationManager.removeUpdates(this)
        startForeground(NOTIFICATION_ID, buildNotification(TrackingState.PAUSED))
        persist()
    }

    private fun resumeTrip() {
        val trip = activeTrip ?: return
        val pausedFor = trip.pauseStartedAtMillis?.let { System.currentTimeMillis() - it } ?: 0L
        activeTrip = trip.copy(
            paused = false,
            pauseStartedAtMillis = null,
            totalPausedMillis = trip.totalPausedMillis + pausedFor
        )
        TrackingRepository.setState(TrackingState.ACTIVE)
        startForeground(NOTIFICATION_ID, buildNotification(TrackingState.ACTIVE))
        beginUpdates()
    }

    private fun stopTrip() {
        val trip = activeTrip
        TrackingRepository.setState(TrackingState.IDLE)
        locationManager.removeUpdates(this)
        scope.launch {
            if (trip != null) {
                val completed = trip.copy(endedAtMillis = System.currentTimeMillis(), paused = false, pauseStartedAtMillis = null)
                val history = storage.historyFlow.mapLatestOnce()
                storage.saveHistory(listOf(completed) + history)
                storage.saveActiveTrip(null)
            }
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun persist() {
        val trip = activeTrip ?: return
        scope.launch { storage.saveActiveTrip(trip) }
    }

    private fun buildNotification(state: TrackingState): Notification {
        val pauseAction = if (state == TrackingState.ACTIVE) {
            NotificationCompat.Action.Builder(
                0,
                "Pause",
                pendingIntent(this, ACTION_PAUSE)
            ).build()
        } else {
            NotificationCompat.Action.Builder(
                0,
                "Resume",
                pendingIntent(this, ACTION_RESUME)
            ).build()
        }
        val stopAction = NotificationCompat.Action.Builder(0, "Stop", pendingIntent(this, ACTION_STOP)).build()
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Maxxed Compass trip tracking")
            .setContentText(if (state == TrackingState.PAUSED) "Tracking paused" else "Tracking your route in the background")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .addAction(pauseAction)
            .addAction(stopAction)
            .build()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Trip tracking", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    companion object {
        private const val CHANNEL_ID = "tracking"
        private const val NOTIFICATION_ID = 2001
        const val ACTION_START = "com.maxxed.compass.tracking.START"
        const val ACTION_PAUSE = "com.maxxed.compass.tracking.PAUSE"
        const val ACTION_RESUME = "com.maxxed.compass.tracking.RESUME"
        const val ACTION_STOP = "com.maxxed.compass.tracking.STOP"

        fun intent(context: Context, action: String): Intent = Intent(context, TrackingService::class.java).setAction(action)

        private fun pendingIntent(context: Context, action: String): PendingIntent {
            return PendingIntent.getService(
                context,
                action.hashCode(),
                intent(context, action),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}

class BootReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val storage = AppStorage(context)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        scope.launch {
            val trip = storage.activeTripFlow.mapLatestOnce()
            if (trip != null && !trip.paused) {
                ContextCompat.startForegroundService(context, TrackingService.intent(context, TrackingService.ACTION_START))
            }
            scope.cancel()
        }
    }
}

suspend fun <T> kotlinx.coroutines.flow.Flow<T>.mapLatestOnce(): T = first()
