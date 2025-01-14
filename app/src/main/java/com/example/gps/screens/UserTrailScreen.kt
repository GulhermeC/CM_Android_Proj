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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.gps.R
import kotlinx.coroutines.delay
import kotlin.math.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import com.mapbox.maps.plugin.gestures.gestures

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
    var userLocation by remember { mutableStateOf<Pair<Double, Double>?>(null) }


    var pointAnnotationManager by remember { mutableStateOf<PointAnnotationManager?>(null) }
    var hasLocationPermission by remember { mutableStateOf(checkLocationPermission(context)) }


    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasLocationPermission = isGranted
            if (!isGranted) {
                Toast.makeText(
                    context,
                    context.getString(R.string.permission_denied),
                    Toast.LENGTH_LONG
                ).show()
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

                    // Enable zoom controls
                    val gesturesPlugin = mapView.gestures
                    gesturesPlugin.updateSettings {
                        pinchToZoomEnabled = true
                        doubleTapToZoomInEnabled = true
                        quickZoomEnabled = true
                    }


                    // Store User Location and Move Camera in a Single Listener
                    addOnIndicatorPositionChangedListener { point ->
                        userLocation = Pair(point.latitude(), point.longitude()) //Store location
                        println("User Location Updated: $userLocation")

                        // Center camera on user location
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
                //  Add waypoints to the map
                waypointList.forEach { (lat, lng) ->
                    val point = Point.fromLngLat(lng, lat)
                    val isCleared = clearedWaypoints.contains(Pair(lat, lng)) // Check if cleared
                    val iconColor =
                        if (isCleared) "#00FF00" else "#FF5733" // Green for cleared, Red otherwise

                    val pointAnnotationOptions = PointAnnotationOptions()
                        .withPoint(point)
                        .withIconSize(3.5)
                        .withIconImage("marker-15")
                        .withTextField("Waypoint")
                        .withTextColor(iconColor) // Change color dynamically
                        .withTextSize(12.0)

                    pointAnnotationManager?.create(pointAnnotationOptions)
                }
            }
        }
    }

    //  Check if User is Near Any Waypoint
    LaunchedEffect(userLocation) {
        userLocation?.let { (userLat, userLng) ->
            waypointList.filterNot { clearedWaypoints.contains(it) } // Only check uncleared waypoints
                .forEach { (waypointLat, waypointLng) ->
                    val distance = calculateDistance(userLat, userLng, waypointLat, waypointLng)
                    if (distance < 1) { //  10 meters threshold
                        clearedWaypoints = clearedWaypoints + Pair(waypointLat, waypointLng)
                    }
                }

            //  Stop timer when all waypoints are reached
            if (clearedWaypoints.size == waypointList.size && isRunning) {
                isRunning = false
                println(" All waypoints cleared! Timer stopped.")
            }
        }
    }

//  Improved UI Layout with Soft Colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(Color(0xFFF1F1EB)) // Beige/Nude Background
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        //  Header Section
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.Start
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFF19731B)
                ) // Dark Green
            }
            Text(
                text = "Trail",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF19731B) // Dark Green
                ),
                modifier = Modifier.padding(start = 12.dp)
            )
        }

        //  Map Section with Rounded Borders
        Card(
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(350.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        //  Timer Display with Simple Styling
        Text(
            text = formatTime(elapsedTime),
            style = TextStyle(
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                fontFamily = FontFamily.Monospace
            ),
            modifier = Modifier
                .background(Color(0xFFE0E0E0), shape = RoundedCornerShape(12.dp)) // Light Gray
                .padding(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        //  Start Trail Button with Adjusted Height
        Button(
            onClick = {
                if (isRunning) {
                    isRunning = false // Stop the timer
                } else {
                    isRunning = true
                    startTime = SystemClock.elapsedRealtime()
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF19731B), // Ensures background is correct
                contentColor = Color.White // Ensures text is visible
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .height(60.dp), //  Increased height for better text fitting
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(vertical = 12.dp) //  Ensures internal padding does not shrink text
        ) {
            Icon(
                if (isRunning) Icons.Filled.Check else Icons.Filled.PlayArrow,
                contentDescription = "Start/Stop Trail",
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isRunning) "Stop Trail" else "Start Trail",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                ),
                modifier = Modifier.wrapContentSize(), // Prevents text from being cropped
                textAlign = TextAlign.Center // Ensures text is well-centered
            )
        }

        //  Timer Update (Updates every second when running)
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

//  Function to Calculate Distance Between Two GPS Coordinates
private fun calculateDistance(
    lat1: Double, lon1: Double, lat2: Double, lon2: Double
): Double {
    val radius = 6371e3 // Earth's radius in meters
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)

    val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))

    return radius * c // Distance in meters
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
