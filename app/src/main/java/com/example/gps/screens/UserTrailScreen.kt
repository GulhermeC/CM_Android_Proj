package com.example.gps.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.SystemClock
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.google.firebase.firestore.FirebaseFirestore
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapInitOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.ResourceOptions
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.locationcomponent.location
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import com.example.gps.R
import kotlinx.coroutines.delay

@SuppressLint("ClickableViewAccessibility")
@Composable
fun UserTrailScreen(navController: NavHostController , trailId: String,waypointsArg: String?) {
    val context = LocalContext.current
    // Timer state
    var isRunning by remember { mutableStateOf(false) }
    var elapsedTime by remember { mutableStateOf(0L) }
    var startTime by remember { mutableStateOf(0L) }

    // Waypoints state
    val waypointList = remember { mutableStateListOf<Pair<Double, Double>>() }
    var pointAnnotationManager by remember { mutableStateOf<PointAnnotationManager?>(null) }
    var hasLocationPermission by remember { mutableStateOf(checkLocationPermission(context)) }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasLocationPermission = isGranted
            if (!isGranted) {
                Toast.makeText(context, context.getString(R.string.permission_denied), Toast.LENGTH_LONG).show()
            }
        }
    )

    // Convert waypoints string back to a list
    LaunchedEffect(waypointsArg) {
        if (!waypointsArg.isNullOrEmpty()) {
            val parsedWaypoints = waypointsArg.split(";").mapNotNull { pair ->
                val coords = pair.split(",")
                if (coords.size == 2) {
                    coords[0].toDoubleOrNull()?.let { lat ->
                        coords[1].toDoubleOrNull()?.let { lng ->
                            Pair(lat, lng)
                        }
                    }
                } else null
            }

            waypointList.clear()
            waypointList.addAll(parsedWaypoints)
            println("Received waypoints: $waypointList") // Debugging log
        }
    }

    // Initialize MapView with lifecycle management
    val mapView = rememberMapViewWithLifecycle()
    val mapboxMap = remember { mapView.getMapboxMap() }

    // Request permissions
    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    // Load waypoints when permission is granted
    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            mapboxMap.loadStyleUri(Style.MAPBOX_STREETS) { style ->
                mapView.location.apply {
                    updateSettings {
                        enabled = true
                        pulsingEnabled = true
                    }

                    // Center camera on user location
                    addOnIndicatorPositionChangedListener { point ->
                        mapboxMap.setCamera(
                            CameraOptions.Builder()
                                .center(point)
                                .zoom(15.0)
                                .build()
                        )
                    }
                }

                // Initialize the annotation manager for waypoints
                pointAnnotationManager = mapView.annotations.createPointAnnotationManager()
                // Add waypoints to the map
                waypointList.forEach { (lat, lng) ->
                    val point = Point.fromLngLat(lng, lat)
                    val pointAnnotationOptions = PointAnnotationOptions()
                        .withPoint(point)
                        .withIconSize(1.5)
                        .withIconImage("marker-15")
                    pointAnnotationManager?.create(pointAnnotationOptions)
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (hasLocationPermission) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Map View
                AndroidView(
                    factory = { mapView },
                    modifier = Modifier.fillMaxSize()
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Timer Display
                    Text(
                        text = "Elapsed Time: ${formatTime(elapsedTime)}",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.7f))
                            .padding(8.dp)
                    )

                    Button(
                        onClick = {
                            if (isRunning) {
                                isRunning = false
                            } else {
                                isRunning = true
                                startTime = SystemClock.elapsedRealtime()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isRunning) "Stop Trail" else "Start Trail")
                    }

                    if (isRunning) {
                        LaunchedEffect(isRunning) {
                            while (isRunning) {
                                elapsedTime = SystemClock.elapsedRealtime() - startTime
                                delay(1000L) // Update every second
                            }
                        }
                    }
                }
            }
        } else {
            Button(onClick = { requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }) {
                Text(stringResource(R.string.request_permission))
            }
        }
    }
}

// Format time in HH:mm:ss
private fun formatTime(millis: Long): String {
    val seconds = millis / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    return "%02d:%02d:%02d".format(hours, minutes % 60, seconds % 60)
}

// Function to check if location permission is granted
private fun checkLocationPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
}
