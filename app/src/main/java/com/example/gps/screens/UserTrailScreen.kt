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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
    var clearedWaypoints by remember { mutableStateOf(setOf<Pair<Double, Double>>()) }
    val userLocation = remember { mutableStateOf<Pair<Double, Double>?>(null) }


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
                        .withIconSize(3.5) // 🔹 Increased size
                        .withIconImage("marker-15")
                        .withTextField("Waypoint")
                        .withTextColor("#FF5733") // 🔹 Distinct color
                        .withTextSize(12.0)
                    pointAnnotationManager?.create(pointAnnotationOptions)
                }
            }
        }
    }

    // 🔹 Improved UI Layout
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 🔹 Header Section
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.Start
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "User Trail",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(start = 12.dp)
            )
        }

        // 🔹 Map Section
        Card(
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(500.dp)
        ) {
            AndroidView(
                factory = { mapView },
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        // 🔹 Timer Display
        Text(
            text = "Elapsed Time: ${formatTime(elapsedTime)}",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.7f))
                .padding(8.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 🔹 Start Trail Button (Fixing Timer)
        Button(
            onClick = {
                if (isRunning) {
                    isRunning = false // Stop the timer
                } else {
                    isRunning = true
                    startTime = SystemClock.elapsedRealtime()
                }
            },
            colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .height(50.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(if (isRunning) "Stop Trail" else "Start Trail", style = MaterialTheme.typography.bodyLarge)
        }

        // 🔹 Timer Update (Updates every second when running)
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
