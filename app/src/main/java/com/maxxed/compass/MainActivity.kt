package com.maxxed.compass

import android.Manifest
import android.graphics.Paint as AndroidPaint
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import kotlin.math.cos
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        setContent {
            val uiState by viewModel.uiState.collectAsState()
            val locationPermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) {
                viewModel.clearPermissionPrompt()
                viewModel.refreshLocation()
            }
            val cameraPermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) {
                viewModel.clearPermissionPrompt()
                if (it) viewModel.setCameraMode(true)
            }
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { granted -> viewModel.onNotificationPermissionResult(granted) }

            LaunchedEffect(Unit) {
                @Suppress("DEPRECATION")
                val displayRotation =
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                        display?.rotation ?: android.view.Surface.ROTATION_0
                    } else {
                        windowManager.defaultDisplay.rotation
                    }
                viewModel.setDisplayRotation(displayRotation)
                viewModel.startSensors()
                viewModel.refreshLocation()
            }
            DisposableEffect(Unit) { onDispose { viewModel.stopSensors() } }

            LaunchedEffect(uiState.pendingPermission) {
                when (uiState.pendingPermission) {
                    PermissionPrompt.LOCATION -> locationPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                    PermissionPrompt.CAMERA -> cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    PermissionPrompt.NOTIFICATIONS -> {
                        if (Build.VERSION.SDK_INT >= 33) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            viewModel.clearPermissionPrompt()
                        }
                    }
                    null -> Unit
                }
            }

            CompassTheme(
                darkTheme = when (uiState.settings.themeChoice) {
                    ThemeChoice.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
                    ThemeChoice.LIGHT -> false
                    ThemeChoice.DARK -> true
                },
                nightMode = uiState.settings.nightMode
            ) {
                ScreenFlags(keepScreenOn = uiState.settings.keepScreenOn)
                CompassApp(uiState = uiState, viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun CompassTheme(darkTheme: Boolean, nightMode: Boolean, content: @Composable () -> Unit) {
    val scheme = if (nightMode) {
        androidx.compose.material3.darkColorScheme(
            primary = Color(0xFFFF5D55),
            background = Color(0xFF140808),
            surface = Color(0xFF231010),
            onSurface = Color(0xFFFFECE9),
            onBackground = Color(0xFFFFECE9),
            surfaceVariant = Color(0xFF3A1B1B),
            onSurfaceVariant = Color(0xFFFFB6AD)
        )
    } else if (darkTheme) {
        androidx.compose.material3.darkColorScheme(
            primary = Color(0xFF42D392),
            background = Color(0xFF0E1418),
            surface = Color(0xFF162027),
            onSurface = Color(0xFFF4F7F6),
            onBackground = Color(0xFFF4F7F6),
            surfaceVariant = Color(0xFF223039),
            onSurfaceVariant = Color(0xFFB9C8CC)
        )
    } else {
        androidx.compose.material3.lightColorScheme(
            primary = Color(0xFF0E9B62),
            background = Color(0xFFF2F6F3),
            surface = Color.White,
            onSurface = Color(0xFF162027),
            onBackground = Color(0xFF162027),
            surfaceVariant = Color(0xFFE3ECE7),
            onSurfaceVariant = Color(0xFF55666A)
        )
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

@Composable
private fun ScreenFlags(keepScreenOn: Boolean) {
    val activity = requireNotNull(androidx.activity.compose.LocalActivity.current) as ComponentActivity
    DisposableEffect(keepScreenOn) {
        if (keepScreenOn) {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose { activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
}

@Composable
private fun CompassApp(uiState: CompassUiState, viewModel: MainViewModel) {
    var renameTripId by remember { mutableStateOf<String?>(null) }
    var deleteTripId by remember { mutableStateOf<String?>(null) }
    var newSegmentName by remember { mutableStateOf("") }
    var renameText by remember { mutableStateOf("") }

    Surface(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                HeaderSection(uiState, viewModel)
            }
            item {
                StatusCard(uiState)
            }
            item {
                CompassInstrument(uiState)
            }
            item {
                QuickControls(uiState, viewModel)
            }
            item {
                TripSection(uiState, viewModel, newSegmentName) { newSegmentName = it }
            }
            item {
                SkyScannerSection(uiState, viewModel)
            }
            item {
                AdvancedSection(uiState, viewModel)
            }
            item {
                HistorySection(uiState, onRename = {
                    renameTripId = it.id
                    renameText = it.name
                }, onDelete = { deleteTripId = it.id }, onSelect = { viewModel.selectTrip(it.id) })
            }
        }
    }

    if (uiState.showCalibrationOverlay) {
        CalibrationDialog(
            message = uiState.calibrationMessage.orEmpty(),
            onDismiss = viewModel::clearPermissionPrompt,
            onStopNagging = viewModel::dismissCalibrationPermanently
        )
    }
    if (renameTripId != null) {
        AlertDialog(
            onDismissRequest = { renameTripId = null },
            title = { Text("Rename trip") },
            text = {
                OutlinedTextField(value = renameText, onValueChange = { renameText = it }, label = { Text("Trip name") })
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.renameTrip(renameTripId!!, renameText.ifBlank { "Trip" })
                    renameTripId = null
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { renameTripId = null }) { Text("Cancel") } }
        )
    }
    if (deleteTripId != null) {
        AlertDialog(
            onDismissRequest = { deleteTripId = null },
            title = { Text("Delete trip") },
            text = { Text("Delete this local trip permanently?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteTrip(deleteTripId!!)
                    deleteTripId = null
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleteTripId = null }) { Text("Cancel") } }
        )
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun HeaderSection(uiState: CompassUiState, viewModel: MainViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("MAXXED COMPASS", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text("Offline compass, trail tracking, and sky guidance", style = MaterialTheme.typography.headlineMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = uiState.settings.northReference == NorthReference.MAGNETIC,
                onClick = { viewModel.setNorthReference(NorthReference.MAGNETIC) },
                label = { Text("Magnetic") },
                leadingIcon = { Icon(Icons.Outlined.Navigation, contentDescription = null) }
            )
            FilterChip(
                selected = uiState.settings.northReference == NorthReference.TRUE,
                onClick = { viewModel.setNorthReference(NorthReference.TRUE) },
                label = { Text("True north") },
                leadingIcon = { Icon(Icons.Outlined.Public, contentDescription = null) }
            )
            FilterChip(
                selected = uiState.settings.units == Units.IMPERIAL,
                onClick = { viewModel.setUnits(Units.IMPERIAL) },
                label = { Text("ft / mi") }
            )
            FilterChip(
                selected = uiState.settings.units == Units.METRIC,
                onClick = { viewModel.setUnits(Units.METRIC) },
                label = { Text("m / km") }
            )
        }
    }
}

@Composable
private fun StatusCard(uiState: CompassUiState) {
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(uiState.headingSample.status, style = MaterialTheme.typography.titleMedium)
            Text(
                "Confidence ${uiState.headingSample.confidence.name.lowercase()} | Sensor ${uiState.headingSample.sensorMode.name.replace('_', ' ')} | ${uiState.coordinatesText}",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (uiState.headingSample.interferenceDetected) {
                Text("Magnetic interference looks likely. Step away from metal or electronics and recalibrate.", color = Color(0xFFE4A63E))
            }
            if (uiState.headingSample.fieldStrengthMicroTesla != null) {
                Text("Field strength ${"%.1f".format(uiState.headingSample.fieldStrengthMicroTesla)} uT", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun CompassInstrument(uiState: CompassUiState) {
    val heading = when (uiState.settings.northReference) {
        NorthReference.MAGNETIC -> uiState.headingSample.magneticHeading
        NorthReference.TRUE -> uiState.headingSample.trueHeading ?: uiState.headingSample.magneticHeading
    } ?: 0f
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Heading", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${heading.toInt()}° ${uiState.headingSample.cardinal}", style = MaterialTheme.typography.headlineLarge)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(if (uiState.settings.northReference == NorthReference.TRUE) "True north" else "Magnetic north")
                    uiState.headingSample.declinationDegrees?.let { Text("Declination ${"%.1f".format(it)}°", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
            Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) {
                CompassDial(heading = heading, marineSkin = uiState.settings.skinChoice == SkinChoice.MARINE)
                Text("N", modifier = Modifier.align(Alignment.TopCenter).padding(top = 20.dp), color = MaterialTheme.colorScheme.primary)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Metric("Bearing", "${heading.toInt()}°")
                Metric("Reference", if (uiState.settings.northReference == NorthReference.TRUE) "True" else "Mag")
                Metric("Accuracy", uiState.headingSample.confidence.name.lowercase().replaceFirstChar { it.uppercase() })
            }
        }
    }
}

@Composable
private fun CompassDial(heading: Float, marineSkin: Boolean) {
    val primary = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    Canvas(modifier = Modifier.fillMaxSize()) {
        val radius = size.minDimension / 2f - 28.dp.toPx()
        val center = center
        drawCircle(color = if (marineSkin) Color(0xFF0B2438) else Color(0xFF0E1214), radius = radius + 18.dp.toPx(), center = center)
        drawCircle(color = surfaceVariant, radius = radius, center = center, style = Stroke(width = 2.dp.toPx()))
        for (i in 0 until 72) {
            val angle = Math.toRadians((i * 5 - 90).toDouble())
            val outer = Offset(center.x + (radius * cos(angle)).toFloat(), center.y + (radius * sin(angle)).toFloat())
            val innerRadius = if (i % 6 == 0) radius - 24.dp.toPx() else radius - 12.dp.toPx()
            val inner = Offset(center.x + (innerRadius * cos(angle)).toFloat(), center.y + (innerRadius * sin(angle)).toFloat())
            drawLine(
                color = if (i % 18 == 0) primary else Color.White.copy(alpha = 0.55f),
                start = outer,
                end = inner,
                strokeWidth = if (i % 6 == 0) 3.dp.toPx() else 1.5.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
        rotate(-heading, center) {
            val needle = Path().apply {
                moveTo(center.x, center.y - radius + 36.dp.toPx())
                lineTo(center.x - 12.dp.toPx(), center.y + 18.dp.toPx())
                lineTo(center.x + 12.dp.toPx(), center.y + 18.dp.toPx())
                close()
            }
            drawPath(needle, color = Color.Black)
            val target = Path().apply {
                moveTo(center.x, center.y + radius - 36.dp.toPx())
                lineTo(center.x - 12.dp.toPx(), center.y - 18.dp.toPx())
                lineTo(center.x + 12.dp.toPx(), center.y - 18.dp.toPx())
                close()
            }
            drawPath(target, color = primary)
        }
        drawCircle(color = Color(0xFF0E1214), radius = 14.dp.toPx(), center = center)
        drawCircle(color = primary, radius = 6.dp.toPx(), center = center)
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun QuickControls(uiState: CompassUiState, viewModel: MainViewModel) {
    AppCard {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionChip("Trip", Icons.Outlined.Route) {
                when (uiState.trackingState) {
                    TrackingState.IDLE -> viewModel.startTrip()
                    TrackingState.ACTIVE -> viewModel.pauseTrip()
                    TrackingState.PAUSED -> viewModel.resumeTrip()
                }
            }
            ActionChip("Stop", Icons.Outlined.Stop, enabled = uiState.trackingState != TrackingState.IDLE) { viewModel.stopTrip() }
            ActionChip("Sky", Icons.Outlined.Explore) { viewModel.setCameraMode(!uiState.skyState.useCamera) }
            ActionChip("Night", Icons.Outlined.NightsStay) { viewModel.setNightMode(!uiState.settings.nightMode) }
            ActionChip("Advanced", Icons.Outlined.Settings) { viewModel.setAdvanced(!uiState.settings.advancedMode) }
        }
        Spacer(Modifier.height(8.dp))
        Text("Every feature stays local to the device. This app is not a certified marine navigation instrument.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun TripSection(uiState: CompassUiState, viewModel: MainViewModel, newSegmentName: String, onSegmentNameChange: (String) -> Unit) {
    val trip = uiState.activeTrip
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Trip tracker", style = MaterialTheme.typography.titleLarge)
                Text(uiState.trackingState.name, color = MaterialTheme.colorScheme.primary)
            }
            if (trip == null) {
                Text("Start tracking to log elapsed time, moving time, distance, elevation, and segments.")
                Button(onClick = viewModel::startTrip) { Text("Start trip") }
            } else {
                val stats = trip.stats
                FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Metric("Elapsed", formatDuration(stats.elapsedMillis))
                    Metric("Moving", formatDuration(stats.movingMillis))
                    Metric("Distance", CompassMath.metersToDistanceText(stats.distanceMeters, uiState.settings.units))
                    Metric("Speed", CompassMath.metersPerSecondText(stats.currentSpeedMps, uiState.settings.units))
                    Metric("Avg", CompassMath.metersPerSecondText(stats.averageSpeedMps, uiState.settings.units))
                    Metric("Gain", CompassMath.metersToDistanceText(stats.elevationGainMeters, uiState.settings.units))
                    Metric("Loss", CompassMath.metersToDistanceText(stats.elevationLossMeters, uiState.settings.units))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        when (uiState.trackingState) {
                            TrackingState.ACTIVE -> viewModel.pauseTrip()
                            TrackingState.PAUSED -> viewModel.resumeTrip()
                            TrackingState.IDLE -> viewModel.startTrip()
                        }
                    }) {
                        Icon(if (uiState.trackingState == TrackingState.ACTIVE) Icons.Outlined.Pause else Icons.Outlined.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (uiState.trackingState == TrackingState.ACTIVE) "Pause" else "Resume")
                    }
                    Button(onClick = viewModel::stopTrip) {
                        Icon(Icons.Outlined.Stop, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Stop")
                    }
                }
                OutlinedTextField(
                    value = newSegmentName,
                    onValueChange = onSegmentNameChange,
                    label = { Text("New segment/lap") },
                    modifier = Modifier.fillMaxWidth()
                )
                Button(onClick = { viewModel.addSegment(newSegmentName.ifBlank { "Segment ${trip.segments.size + 1}" }); onSegmentNameChange("") }) {
                    Icon(Icons.Outlined.Save, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Save segment")
                }
                trip.segments.takeLast(3).forEach {
                    Text("${it.name} • ${CompassMath.metersToDistanceText(it.distanceMeters, uiState.settings.units)}")
                }
            }
        }
    }
}

@Composable
private fun SkyScannerSection(uiState: CompassUiState, viewModel: MainViewModel) {
    var showConstellationPicker by remember { mutableStateOf(false) }
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Sky scanner", style = MaterialTheme.typography.titleLarge)
            Text("Calculated sky overlay using device time, location, and orientation. This is not camera plate-solving or cloud recognition.")
            OutlinedTextField(
                value = uiState.skyState.searchQuery,
                onValueChange = viewModel::setSkySearch,
                label = { Text("Search stars, constellations, Moon, planets") },
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = { viewModel.setCameraMode(!uiState.skyState.useCamera) }) {
                    Icon(Icons.Outlined.CameraAlt, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (uiState.skyState.useCamera) "Use red map" else "Use camera")
                }
                Text(uiState.skyState.status, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(onClick = { showConstellationPicker = true }) {
                Text(
                    "Constellations ${uiState.skyState.enabledConstellationIds.size}/${uiState.skyState.constellationOptions.size}"
                )
            }
            if (uiState.skyState.useCamera && uiState.skyState.cameraPermissionGranted) {
                CameraPreview()
            } else {
                RedSkyMap(uiState.skyState)
            }
            uiState.skyState.nearestObject?.let {
                Text("${it.name} • ${it.type} • az ${it.azimuthDegrees.toInt()}° • alt ${it.altitudeDegrees.toInt()}°")
                Text(it.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    if (showConstellationPicker) {
        ConstellationPickerDialog(
            skyState = uiState.skyState,
            onSetAll = viewModel::setAllConstellationsEnabled,
            onSetConstellation = viewModel::setConstellationEnabled,
            onDismiss = { showConstellationPicker = false }
        )
    }
}

@Composable
private fun ConstellationPickerDialog(
    skyState: SkyUiState,
    onSetAll: (Boolean) -> Unit,
    onSetConstellation: (String, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val allChecked = skyState.constellationOptions.isNotEmpty() &&
        skyState.enabledConstellationIds.size == skyState.constellationOptions.size
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Visible constellations") },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = allChecked, onCheckedChange = onSetAll)
                    Text("All")
                }
                LazyColumn(modifier = Modifier.fillMaxWidth().height(360.dp)) {
                    items(skyState.constellationOptions, key = { it.id }) { option ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = option.id in skyState.enabledConstellationIds,
                                onCheckedChange = { enabled -> onSetConstellation(option.id, enabled) }
                            )
                            Text(option.name)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}

@Composable
private fun CameraPreview() {
    val context = LocalContext.current
    AndroidView(
        modifier = Modifier.fillMaxWidth().height(240.dp).border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)),
        factory = {
            PreviewView(it).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                val providerFuture = ProcessCameraProvider.getInstance(it)
                providerFuture.addListener({
                    val provider = providerFuture.get()
                    val preview = Preview.Builder().build().also { p -> p.surfaceProvider = surfaceProvider }
                    provider.unbindAll()
                    provider.bindToLifecycle(context as ComponentActivity, CameraSelector.DEFAULT_BACK_CAMERA, preview)
                }, ContextCompat.getMainExecutor(it))
            }
        }
    )
}

@Composable
private fun RedSkyMap(skyState: SkyUiState) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .background(Color(0xFF250909), RoundedCornerShape(12.dp))
            .border(1.dp, Color(0x55FF6A6A), RoundedCornerShape(12.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            fun project(azimuthDegrees: Double, altitudeDegrees: Double): Offset {
                val centeredAzimuth = (azimuthDegrees + 180.0) % 360.0
                val x = size.width * (centeredAzimuth / 360.0).toFloat()
                val y = size.height * (1f - ((altitudeDegrees + 10.0) / 100.0).toFloat().coerceIn(0f, 1f))
                return Offset(x, y)
            }

            val constellationColor = Color(0xCCFF6A6A)
            val labelPaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.rgb(255, 167, 167)
                textSize = 12.dp.toPx()
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            skyState.constellationOverlays.forEach { constellation ->
                val pointsById = constellation.points.associateBy { it.id }
                constellation.lines.forEach { line ->
                    val from = pointsById[line.fromPointId]
                    val to = pointsById[line.toPointId]
                    if (from != null && to != null && from.altitudeDegrees >= -10.0 && to.altitudeDegrees >= -10.0) {
                        val start = project(from.azimuthDegrees, from.altitudeDegrees)
                        val end = project(to.azimuthDegrees, to.altitudeDegrees)
                        if (kotlin.math.abs(start.x - end.x) <= size.width / 2f) {
                            drawLine(constellationColor, start, end, 2.dp.toPx(), StrokeCap.Round)
                        }
                    }
                }
                val visiblePoints = constellation.points.filter { it.altitudeDegrees >= -10.0 }
                visiblePoints.forEach { point ->
                    drawCircle(Color(0xFFFFD0D0), 4.dp.toPx(), project(point.azimuthDegrees, point.altitudeDegrees))
                }
                if (skyState.enabledConstellationIds.size <= 12 || skyState.searchQuery.isNotBlank()) {
                    visiblePoints.maxByOrNull { it.altitudeDegrees }?.let { anchor ->
                        val labelAt = project(anchor.azimuthDegrees, anchor.altitudeDegrees)
                        drawContext.canvas.nativeCanvas.drawText(
                            constellation.name,
                            labelAt.x + 7.dp.toPx(),
                            labelAt.y - 7.dp.toPx(),
                            labelPaint
                        )
                    }
                }
            }
            val visible = skyState.visibleObjects.take(10)
            visible.forEachIndexed { index, skyObject ->
                drawCircle(
                    color = if (skyObject.type == "Moon") Color.White else Color(0xFFFF7D7D),
                    radius = if (index == 0) 8f else 5f,
                    center = project(skyObject.azimuthDegrees, skyObject.altitudeDegrees)
                )
            }
            drawCircle(color = Color(0x44FFFFFF), radius = 18f, center = center, style = Stroke(width = 2f))
            drawLine(color = Color(0x66FFFFFF), start = Offset(center.x, 0f), end = Offset(center.x, size.height))
            drawLine(color = Color(0x66FFFFFF), start = Offset(0f, center.y), end = Offset(size.width, center.y))
        }
        Text("N", modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp), color = Color(0xFFFFA7A7))
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun AdvancedSection(uiState: CompassUiState, viewModel: MainViewModel) {
    if (!uiState.settings.advancedMode) return
    val point = uiState.activeTrip?.points?.lastOrNull()
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Advanced tools", style = MaterialTheme.typography.titleLarge)
            Text("Bearing lock, reverse bearing, clinometer, waypoint, and power modes stay behind Advanced mode to keep the primary instrument simple.")
            Text("Smoothing ${"%.2f".format(uiState.settings.smoothing)}")
            Slider(value = uiState.settings.smoothing, onValueChange = viewModel::setSmoothing, valueRange = 0.05f..0.6f)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BatteryMode.entries.forEach { mode ->
                    FilterChip(selected = uiState.settings.batteryMode == mode, onClick = { viewModel.setBatteryMode(mode) }, label = { Text(mode.name) })
                }
                SkinChoice.entries.forEach { skin ->
                    FilterChip(selected = uiState.settings.skinChoice == skin, onClick = { viewModel.setSkin(skin) }, label = { Text(skin.name) })
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Switch(checked = uiState.settings.keepScreenOn, onCheckedChange = viewModel::setKeepScreenOn)
                Text("Keep screen on while viewing. Higher battery use.")
            }
            val clinometer = point?.let { CompassMath.clinometerDegrees(floatArrayOf(0f, 0f, 9.81f)) } ?: 0f
            Text("Clinometer ${"%.1f".format(clinometer)}°")
            Text("Coordinates copy ${uiState.coordinatesText}")
            Text("Back bearing ${uiState.headingSample.magneticHeading?.let { CompassMath.normalizeDegrees(it + 180f).toInt() } ?: "--"}°")
            point?.let { active ->
                val waypoint = Waypoint("Saved waypoint", active.latitude, active.longitude)
                val distanceBearing = CompassMath.distanceAndBearingToWaypoint(active, waypoint)
                Text("Waypoint ${CompassMath.metersToDistanceText(distanceBearing.distanceMeters, uiState.settings.units)} at ${distanceBearing.bearingDegrees.toInt()}°")
            }
            TextButton(onClick = viewModel::deleteAllData) { Text("Delete all local data") }
        }
    }
}

@Composable
private fun HistorySection(uiState: CompassUiState, onRename: (TripRecord) -> Unit, onDelete: (TripRecord) -> Unit, onSelect: (TripRecord) -> Unit) {
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Trip history", style = MaterialTheme.typography.titleLarge)
                Icon(Icons.Outlined.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            if (uiState.tripHistory.isEmpty()) {
                Text("Saved trips will appear here after you stop and save a local session.")
            } else {
                uiState.tripHistory.take(8).forEach { trip ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(trip.name, fontWeight = FontWeight.SemiBold)
                            Text("${formatDuration(trip.stats.elapsedMillis)} • ${CompassMath.metersToDistanceText(trip.stats.distanceMeters, uiState.settings.units)}")
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { onSelect(trip) }) { Text("Open") }
                                TextButton(onClick = { onRename(trip) }) { Text("Rename") }
                                TextButton(onClick = { onDelete(trip) }) {
                                    Icon(Icons.Outlined.Delete, contentDescription = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Delete")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalibrationDialog(message: String, onDismiss: () -> Unit, onStopNagging: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Calibration suggested") },
        text = { Text("$message Dismiss if you understand the current limitations.") },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Dismiss") } },
        dismissButton = { TextButton(onClick = onStopNagging) { Text("Do not keep nagging") } }
    )
}

@Composable
private fun AppCard(content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = { content() })
    }
}

@Composable
private fun ActionChip(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, enabled: Boolean = true, onClick: () -> Unit) {
    FilterChip(
        selected = false,
        enabled = enabled,
        onClick = onClick,
        label = { Text(text) },
        leadingIcon = { Icon(icon, contentDescription = text) }
    )
}

@Composable
private fun Metric(label: String, value: String) {
    Column {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

private fun formatDuration(durationMillis: Long): String {
    val totalSeconds = durationMillis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d:%02d".format(hours, minutes, seconds)
}
