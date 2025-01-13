package com.example.gps

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.locationcomponent.location
import com.google.firebase.firestore.FirebaseFirestore
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import com.example.gps.screens.rememberMapViewWithLifecycle

//@SuppressLint("Lifecycle")
//@Composable
//fun rememberMapViewWithLifecycle(): MapView {
//    val context = LocalContext.current
//    val mapView = remember {
//        MapView(
//            context = context,
//            mapInitOptions = MapInitOptions(
//                context = context,
//                resourceOptions = ResourceOptions.Builder()
//                    .accessToken("sk.eyJ1IjoicnVpdWEiLCJhIjoiY200bXM4czU5MDBwZDJrcjJsZW9qNzVjOCJ9.TlDHWxGJe7rdI03udVud3w")
//                    .build()
//            )
//        )
//    }
//
//    DisposableEffect(mapView) {
//        mapView.onStart()
//        onDispose {
//            mapView.onStop()
//            mapView.onDestroy()
//        }
//    }
//    return mapView
//}

@SuppressLint("ClickableViewAccessibility")
@Composable
fun MapScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()


    // State
    val waypointList = remember { mutableStateListOf<Pair<Double, Double>>() }
    var waypointCounter = remember { mutableIntStateOf(1) }
    var pointAnnotationManager by remember { mutableStateOf<PointAnnotationManager?>(null) }

    // Firebase instance
    val db = remember { FirebaseFirestore.getInstance() }

    // Initialize MapView with lifecycle management
    val mapView = rememberMapViewWithLifecycle()
    val mapboxMap = remember { mapView.getMapboxMap() }

    var hasLocationPermission by remember { mutableStateOf(checkLocationPermission(context)) }
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasLocationPermission = isGranted
            if (!isGranted) {
                val permissionDenied = context.getString(R.string.permission_denied)
                Toast.makeText(
                    context,
                    permissionDenied,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    )

    // Initialize map and location tracking
    LaunchedEffect(Unit) {

        if (!hasLocationPermission) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {

                mapboxMap.loadStyleUri(Style.MAPBOX_STREETS) { style ->
                    // Enable location component
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

                    // Initialize point annotation manager
                    pointAnnotationManager = mapView.annotations.createPointAnnotationManager()
                }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        if(hasLocationPermission) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Map View
                AndroidView(
                    factory = { mapView },
                    modifier = Modifier.fillMaxSize()
                )

                // Touch Listener for Waypoints
                LaunchedEffect(mapboxMap) {
                    mapView.getMapboxMap().addOnMapClickListener { point ->
                        coroutineScope.launch {
                            pointAnnotationManager?.let {
                                createWaypoint(it, point, waypointCounter, waypointList, context)
                                waypointCounter.intValue++
                            }
                        }
                        true
                    }
                }

                // Control Buttons in a Card
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ElevatedButton(
                            onClick = {
                                pointAnnotationManager?.let {
                                    loadWaypoints(db, it, waypointList, context,waypointCounter)
                                }
                            },
                            colors = ButtonDefaults.elevatedButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Text(stringResource(R.string.load_waypoints))
                        }

                        FilledTonalButton(
                            onClick = {
                                saveWaypoint(waypointList, db, context)
                            }
                        ) {
                            Text(stringResource(R.string.save_waypoint))
                        }

                        OutlinedButton(
                            onClick = {
                                pointAnnotationManager?.let {
                                    clearWaypoints(
                                        it,
                                        waypointList,
                                        context,
                                        waypointCounter
                                    )
                                }
                            }
                        ) {
                            Text(stringResource(R.string.clear_waypoints))
                        }
                    }
                }
            }
        }
        else{
            Button(onClick = { requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }) {
                Text(stringResource(R.string.request_location_permission))
            }
        }
    }
}

private fun createWaypoint(
    pointAnnotationManager: PointAnnotationManager,
    point: Point,
    waypointCounter: MutableState<Int>,
    waypointList: MutableList<Pair<Double, Double>>,
    context: Context
) {
    val pointAnnotationOptions = PointAnnotationOptions()
        .withPoint(point)
        .withIconSize(1.5)
        .withIconImage("marker-15")
        .withTextField(waypointCounter.value.toString())
        .withTextSize(14.0)
        .withTextColor("#000000")
        .withTextOffset(listOf(0.0, 0.5))

    pointAnnotationManager.create(pointAnnotationOptions)
    waypointList.add(Pair(point.latitude(), point.longitude()))

//    Toast.makeText(
//        context,
//        "Waypoint ${waypointCounter.value} added at: ${point.latitude()}, ${point.longitude()}",
//        Toast.LENGTH_SHORT
//    ).show()
}

private fun saveWaypoint(
    waypointList: List<Pair<Double, Double>>,
    db: FirebaseFirestore,
    context: Context
) {
    if (waypointList.isEmpty()) {
        Toast.makeText(context, context.getString(R.string.no_waypoints_to_save), Toast.LENGTH_SHORT).show()
        println("No waypoints to save")
        return
    }

    // Create a batch write operation
    val batch = db.batch()
    val waypointsRef = db.collection("waypoints")

    waypointList.forEachIndexed { index, (latitude, longitude) ->
        val waypoint = hashMapOf(
            "latitude" to latitude,
            "longitude" to longitude,
            "label" to (index + 1).toString(),
            "timestamp" to com.google.firebase.Timestamp.now()
        )
        println("Preparing to save waypoint: $waypoint")

        // Add to batch
        val docRef = waypointsRef.document()
        batch.set(docRef, waypoint)
    }

    // Commit the batch
    batch.commit()
        .addOnSuccessListener {
            Toast.makeText(context, context.getString(R.string.waypoints_saved_success), Toast.LENGTH_SHORT).show()
            println("Successfully saved ${waypointList.size} waypoints")
        }
        .addOnFailureListener { e ->
            Toast.makeText(context, context.getString(R.string.waypoints_saved_failed, e.message), Toast.LENGTH_SHORT).show()
            println("Error saving waypoints: ${e.message}")
            e.printStackTrace()
        }
}

private fun loadWaypoints(
    db: FirebaseFirestore,
    pointAnnotationManager: PointAnnotationManager,
    waypointList: MutableList<Pair<Double, Double>>,
    context: Context,
    waypointCounter: MutableState<Int>,
) {
    println(context.getString(R.string.starting_load))

    // Clear existing waypoints
    waypointList.clear()
    pointAnnotationManager.deleteAll()

    db.collection("waypoints")
        .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)
        .get()
        .addOnSuccessListener { result ->
            println("Firestore query completed. Document count: ${result.size()}")

            if (result.isEmpty) {
                Toast.makeText(context, context.getString(R.string.no_waypoints_found), Toast.LENGTH_SHORT).show()
                println("No waypoints found in Firestore")
                return@addOnSuccessListener
            }

            result.documents.forEachIndexed { index, document ->
                println("Processing document ${index + 1}: ${document.data}")

                val latitude = document.getDouble("latitude")
                val longitude = document.getDouble("longitude")

                if (latitude == null || longitude == null) {
                    println("Invalid waypoint data in document ${document.id}: lat=$latitude, lng=$longitude")
                    return@forEachIndexed
                }

                try {
                    val point = Point.fromLngLat(longitude, latitude)
                    createWaypoint(
                        pointAnnotationManager,
                        point,
                        mutableIntStateOf(waypointList.size + 1),
                        waypointList,
                        context
                    )
                    println("Successfully created waypoint ${waypointList.size} at lat=$latitude, lng=$longitude")
                } catch (e: Exception) {
                    println("Error creating waypoint: ${e.message}")
                    e.printStackTrace()
                }
            }

            // ✅ Update waypointCounter to be the next available number
            waypointCounter.value = waypointList.size + 1

            Toast.makeText(context, context.getString(R.string.loaded_waypoints, waypointList.size), Toast.LENGTH_SHORT).show()
            println("Finished loading ${waypointList.size} waypoints")
        }
        .addOnFailureListener { e ->
            Toast.makeText(context, context.getString(R.string.failed_to_load, e.message), Toast.LENGTH_SHORT).show()
            println("Error loading waypoints: ${e.message}")
            e.printStackTrace()
        }
}

private fun clearWaypoints(
    pointAnnotationManager: PointAnnotationManager,
    waypointList: MutableList<Pair<Double, Double>>,
    context: Context,
    waypointCounter: MutableState<Int>
) {
    // Clear markers from the map
    pointAnnotationManager.deleteAll()

    // Clear local waypoint list (but NOT Firestore)
    waypointList.clear()

    // Reset waypoint counter
    waypointCounter.value = 1

    // Inform the user that only the map was cleared
    Toast.makeText(context, context.getString(R.string.waypoints_cleared), Toast.LENGTH_SHORT).show()
}

// Function to check if location permission is granted
private fun checkLocationPermission(context: android.content.Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
}



