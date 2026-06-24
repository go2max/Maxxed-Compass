package com.maxxed.compass

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application), LocationListener {
    private val appContext = application.applicationContext
    private val storage = AppStorage(appContext)
    private val sensorController = CompassSensorController(appContext)
    private val locationManager = appContext.getSystemService(LocationManager::class.java)
    private val constellationCatalog = ConstellationCatalog.load(appContext)
    private val constellationOptions = constellationCatalog.map { ConstellationOption(it.id, it.name) }
    private val headingFlow = MutableStateFlow(HeadingSample())
    private val currentPointFlow = MutableStateFlow<TripPoint?>(null)
    private val skyFlow = MutableStateFlow(
        SkyUiState(
            constellationOptions = constellationOptions,
            enabledConstellationIds = constellationCatalog.mapTo(linkedSetOf()) { it.id }
        )
    )
    private val promptFlow = MutableStateFlow<PermissionPrompt?>(null)
    private val selectionFlow = MutableStateFlow<String?>(null)
    private var sensorJob: Job? = null
    private var startTripAfterNotificationPermission = false
    private var lastSkyUpdateMillis = 0L

    val uiState: StateFlow<CompassUiState> = combine(
        combine(storage.settingsFlow, storage.activeTripFlow, storage.historyFlow) { settings, activeTrip, history ->
            Triple(settings, activeTrip, history)
        },
        combine(
            combine(headingFlow, currentPointFlow, skyFlow) { heading, currentPoint, sky ->
                Triple(heading, currentPoint, sky)
            },
            combine(promptFlow, selectionFlow, TrackingRepository.state) { prompt, selectedTrip, tracking ->
                Triple(prompt, selectedTrip, tracking)
            }
        ) { stateTriple, uiTriple ->
            Pair(stateTriple, uiTriple)
        }
    ) { persisted, runtime ->
        val (settings, activeTrip, history) = persisted
        val (stateTriple, uiTriple) = runtime
        val (heading, currentPoint, sky) = stateTriple
        val (prompt, selectedTrip, tracking) = uiTriple
        val showCalibration = heading.confidence != AccuracyState.GOOD && !settings.calibrationNagDismissed
        CompassUiState(
            headingSample = heading,
            settings = settings,
            trackingState = tracking,
            activeTrip = activeTrip,
            tripHistory = history,
            selectedTripId = selectedTrip,
            waypoint = currentPoint?.let { point -> Waypoint("Current point", point.latitude, point.longitude) },
            coordinatesText = currentPoint?.let { point -> "${"%.5f".format(point.latitude)}, ${"%.5f".format(point.longitude)}" } ?: "Waiting for GPS",
            skyState = sky,
            showCalibrationOverlay = showCalibration,
            calibrationMessage = if (showCalibration) "Move in a figure-eight until accuracy improves." else null,
            unavailableReason = if (heading.sensorMode == SensorMode.UNAVAILABLE) "Compass sensors not available on this device." else null,
            pendingPermission = prompt
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CompassUiState())

    fun setDisplayRotation(rotation: Int) {
        sensorController.setDisplayRotation(rotation)
    }

    fun startSensors() {
        if (sensorJob != null) return
        sensorJob = viewModelScope.launch {
            sensorController.readings().collect { reading ->
                val settings = uiState.value.settings
                val declination = if (settings.northReference == NorthReference.TRUE) {
                    declinationFor(currentPointFlow.value, System.currentTimeMillis())
                } else {
                    null
                }
                val smoothedMagnetic = CompassMath.smoothHeading(headingFlow.value.magneticHeading, reading.magneticHeading ?: 0f, settings.smoothing)
                val displayHeading = if (settings.northReference == NorthReference.TRUE && declination != null) {
                    CompassMath.trueHeading(smoothedMagnetic, declination)
                } else {
                    smoothedMagnetic
                }
                headingFlow.value = HeadingSample(
                    magneticHeading = smoothedMagnetic,
                    trueHeading = declination?.let { CompassMath.trueHeading(smoothedMagnetic, it) },
                    cardinal = CompassMath.cardinalFor(displayHeading),
                    confidence = reading.confidence,
                    sensorMode = reading.sensorMode,
                    interferenceDetected = reading.interferenceDetected,
                    status = buildStatus(reading, declination, settings.northReference),
                    declinationDegrees = declination,
                    fieldStrengthMicroTesla = reading.fieldStrengthMicroTesla
                )
                updateSkyIfDue()
            }
        }
    }

    fun stopSensors() {
        sensorJob?.cancel()
        sensorJob = null
    }

    fun refreshLocation() {
        if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            promptFlow.value = PermissionPrompt.LOCATION
            return
        }
        val provider = when {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> null
        } ?: return
        try {
            locationManager.requestLocationUpdates(provider, 2000L, 1f, this)
            locationManager.getLastKnownLocation(provider)?.let(::consumeLocation)
        } catch (_: SecurityException) {
            promptFlow.value = PermissionPrompt.LOCATION
        }
    }

    override fun onLocationChanged(location: Location) {
        consumeLocation(location)
    }

    private fun consumeLocation(location: Location) {
        currentPointFlow.value = TripPoint(
            timeMillis = location.time.takeIf { it > 0 } ?: System.currentTimeMillis(),
            latitude = location.latitude,
            longitude = location.longitude,
            altitudeMeters = location.altitude.takeIf { location.hasAltitude() },
            accuracyMeters = location.accuracy,
            speedMps = location.speed.takeIf { location.hasSpeed() },
            bearingDegrees = location.bearing.takeIf { location.hasBearing() }
        )
        updateSky()
    }

    fun setTheme(choice: ThemeChoice) = mutateSettings { copy(themeChoice = choice) }
    fun setNorthReference(reference: NorthReference) = mutateSettings { copy(northReference = reference) }
    fun setUnits(units: Units) = mutateSettings { copy(units = units) }
    fun setSkin(skin: SkinChoice) = mutateSettings { copy(skinChoice = skin) }
    fun setBatteryMode(mode: BatteryMode) = mutateSettings { copy(batteryMode = mode) }
    fun setAdvanced(enabled: Boolean) = mutateSettings { copy(advancedMode = enabled) }
    fun setNightMode(enabled: Boolean) = mutateSettings { copy(nightMode = enabled) }
    fun setKeepScreenOn(enabled: Boolean) = mutateSettings { copy(keepScreenOn = enabled) }
    fun setSmoothing(value: Float) = mutateSettings { copy(smoothing = value) }
    fun dismissCalibrationPermanently() = mutateSettings { copy(calibrationNagDismissed = true) }
    fun clearPermissionPrompt() { promptFlow.value = null }
    fun selectTrip(id: String?) { selectionFlow.value = id }
    fun setSkySearch(query: String) { skyFlow.value = skyFlow.value.copy(searchQuery = query); updateSky() }
    fun setConstellationEnabled(id: String, enabled: Boolean) {
        val settings = uiState.value.settings
        val hidden = settings.hiddenConstellationIds.toMutableSet().apply {
            if (enabled) remove(id) else add(id)
        }
        saveConstellationSelection(settings.copy(hiddenConstellationIds = hidden))
    }

    fun setAllConstellationsEnabled(enabled: Boolean) {
        val settings = uiState.value.settings
        val hidden = if (enabled) emptySet() else constellationCatalog.mapTo(linkedSetOf()) { it.id }
        saveConstellationSelection(settings.copy(hiddenConstellationIds = hidden))
    }
    fun setCameraMode(enabled: Boolean) {
        if (enabled && !hasPermission(Manifest.permission.CAMERA)) {
            promptFlow.value = PermissionPrompt.CAMERA
            return
        }
        skyFlow.value = skyFlow.value.copy(useCamera = enabled, cameraPermissionGranted = enabled || skyFlow.value.cameraPermissionGranted)
    }

    fun startTrip() {
        if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            promptFlow.value = PermissionPrompt.LOCATION
            return
        }
        if (Build.VERSION.SDK_INT >= 33 && !hasPermission(Manifest.permission.POST_NOTIFICATIONS)) {
            startTripAfterNotificationPermission = true
            promptFlow.value = PermissionPrompt.NOTIFICATIONS
            return
        }
        startTrackingService()
    }

    fun onNotificationPermissionResult(granted: Boolean) {
        promptFlow.value = null
        val shouldStart = startTripAfterNotificationPermission
        startTripAfterNotificationPermission = false
        if (granted && shouldStart) startTrackingService()
    }

    private fun startTrackingService() {
        ContextCompat.startForegroundService(appContext, TrackingService.intent(appContext, TrackingService.ACTION_START))
        TrackingRepository.setState(TrackingState.ACTIVE)
    }

    fun pauseTrip() {
        appContext.startService(TrackingService.intent(appContext, TrackingService.ACTION_PAUSE))
        TrackingRepository.setState(TrackingState.PAUSED)
    }

    fun resumeTrip() {
        ContextCompat.startForegroundService(appContext, TrackingService.intent(appContext, TrackingService.ACTION_RESUME))
        TrackingRepository.setState(TrackingState.ACTIVE)
    }

    fun stopTrip() {
        appContext.startService(TrackingService.intent(appContext, TrackingService.ACTION_STOP))
        TrackingRepository.setState(TrackingState.IDLE)
    }

    fun addSegment(name: String) {
        viewModelScope.launch {
            val active = storage.activeTripFlow.mapLatestOnce() ?: return@launch
            val updated = active.copy(
                segments = active.segments + TripSegment(name = name, startedAtMillis = System.currentTimeMillis())
            )
            storage.saveActiveTrip(updated)
        }
    }

    fun renameTrip(id: String, name: String) {
        viewModelScope.launch {
            val history = storage.historyFlow.mapLatestOnce().map { if (it.id == id) it.copy(name = name) else it }
            storage.saveHistory(history)
            val active = storage.activeTripFlow.mapLatestOnce()
            if (active?.id == id) storage.saveActiveTrip(active.copy(name = name))
        }
    }

    fun deleteTrip(id: String) {
        viewModelScope.launch {
            storage.saveHistory(storage.historyFlow.mapLatestOnce().filterNot { it.id == id })
        }
    }

    fun deleteAllData() {
        viewModelScope.launch { storage.clearAll() }
    }

    private fun saveConstellationSelection(settings: AppSettings) {
        viewModelScope.launch { storage.saveSettings(settings) }
        updateSky(settings.hiddenConstellationIds)
    }

    private fun updateSkyIfDue() {
        val now = System.currentTimeMillis()
        if (now - lastSkyUpdateMillis < 1_000L) return
        lastSkyUpdateMillis = now
        updateSky()
    }

    private fun updateSky(hiddenConstellationIds: Set<String> = uiState.value.settings.hiddenConstellationIds) {
        lastSkyUpdateMillis = System.currentTimeMillis()
        val enabledConstellations = constellationCatalog.filterNot { it.id in hiddenConstellationIds }
        skyFlow.value = skyFlow.value.copy(
            constellationOptions = constellationOptions,
            enabledConstellationIds = enabledConstellations.mapTo(linkedSetOf()) { it.id }
        )
        val point = currentPointFlow.value
        val heading = headingFlow.value.magneticHeading
        if (point == null || heading == null) {
            skyFlow.value = skyFlow.value.copy(status = "Location and compass needed for sky overlay.")
            return
        }
        val objects = SkyMath.visibleObjects(
            System.currentTimeMillis(),
            point.latitude,
            point.longitude,
            enabledConstellations
        )
            .filter { it.altitudeDegrees > if (uiState.value.settings.advancedMode) -5 else 0 }
            .filter { skyFlow.value.searchQuery.isBlank() || it.name.contains(skyFlow.value.searchQuery, ignoreCase = true) }
        val constellationOverlays = SkyMath.constellationOverlays(
            enabledConstellations,
            System.currentTimeMillis(),
            point.latitude,
            point.longitude
        )
            .filter { overlay ->
                skyFlow.value.searchQuery.isBlank() ||
                    overlay.name.contains(skyFlow.value.searchQuery, ignoreCase = true)
            }
        val nearest = SkyMath.nearestToCenter(objects, heading.toDouble(), 35.0)
        skyFlow.value = skyFlow.value.copy(
            nearestObject = nearest,
            visibleObjects = objects.take(20),
            constellationOverlays = constellationOverlays,
            status = nearest?.let { "${it.name} nearest center target. ${SkyMath.polarisGuidance(objects)}" }
                ?: "No supported object near the center target."
        )
    }

    private fun mutateSettings(transform: AppSettings.() -> AppSettings) {
        viewModelScope.launch { storage.saveSettings(uiState.value.settings.transform()) }
    }

    private fun hasPermission(permission: String): Boolean {
        if (permission == Manifest.permission.POST_NOTIFICATIONS && Build.VERSION.SDK_INT < 33) return true
        return ContextCompat.checkSelfPermission(appContext, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun buildStatus(reading: CompassReading, declination: Float?, reference: NorthReference): String {
        return when {
            reading.sensorMode == SensorMode.UNAVAILABLE -> "Compass sensors unavailable"
            reading.interferenceDetected -> "Possible magnetic interference detected"
            reference == NorthReference.TRUE && declination == null -> "Using magnetic north until location fix is available"
            reference == NorthReference.TRUE -> "True north active"
            else -> "Magnetic north active"
        }
    }
}
