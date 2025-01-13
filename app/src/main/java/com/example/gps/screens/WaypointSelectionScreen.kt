package com.example.gps.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import com.mapbox.maps.plugin.locationcomponent.location
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.gps.R

@SuppressLint("Lifecycle")
@Composable
fun rememberMapViewWithLifecycle(): MapView {
    val context = LocalContext.current
    val mapView = remember {
        MapView(
            context = context,
            mapInitOptions = MapInitOptions(
                context = context,
                resourceOptions = ResourceOptions.Builder()
                    .accessToken("sk.eyJ1IjoicnVpdWEiLCJhIjoiY200bXM4czU5MDBwZDJrcjJsZW9qNzVjOCJ9.TlDHWxGJe7rdI03udVud3w")
                    .build()
            )
        )
    }

    DisposableEffect(mapView) {
        mapView.onStart()
        onDispose {
            mapView.onStop()
            mapView.onDestroy()
        }
    }
    return mapView
}

@SuppressLint("ClickableViewAccessibility")
@Composable
fun WaypointSelectionScreen(navController: NavHostController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val db = remember { FirebaseFirestore.getInstance() }
    var showDialog by remember { mutableStateOf(false) }

    // State
    val waypointList = remember { mutableStateListOf<Pair<Double, Double>>() }
    var waypointCounter = remember { mutableIntStateOf(1) }
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

    // Initialize MapView with lifecycle management
    val mapView = rememberMapViewWithLifecycle()
    val mapboxMap = remember { mapView.getMapboxMap() }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {

            mapboxMap.loadStyleUri(Style.MAPBOX_STREETS) { style ->

                mapView.location.apply {
                    updateSettings {
                        enabled = true
                        pulsingEnabled = true
                    }

                    //Center camera on user location
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
                if (waypointList.isEmpty())
                    loadWaypoints(
                        db,
                        pointAnnotationManager!!,
                        waypointList,
                        context,
                        waypointCounter
                    )

            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        if (hasLocationPermission) {
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
                            }
                        }
                        true
                    }
                }

                // Control Buttons (Updated)
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp) // Added more spacing
                ) {
                    Button(
                        onClick = {
                            // Save waypoints to Firebase
                            saveWaypoint(waypointList, db, context)
                            // Pass waypoints back to TrailCreationScreen
                            navController.previousBackStackEntry
                                ?.savedStateHandle
                                ?.set("waypoints", ArrayList(waypointList))

                            navController.popBackStack()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF19731B))
                    ) {
                        Text(
                            stringResource(R.string.done_save_waypoints),
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = { showDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF19731B))
                    ) {
                        Text(stringResource(R.string.clear_waypoints), fontWeight = FontWeight.Bold)
                    }
                }

                // ✅ Improved Delete Confirmation Dialog
                if (showDialog) {
                    AlertDialog(
                        onDismissRequest = { showDialog = false },
                        title = {
                            Text(
                                text = stringResource(R.string.confirm_deletion),
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        },
                        text = {
                            Text(
                                text = stringResource(R.string.delete_all_waypoints_message),
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    deleteAllWaypointsFromFirebase(db, context)
                                    pointAnnotationManager?.deleteAll()
                                    waypointList.clear()
                                    waypointCounter.intValue = 1
                                    showDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    stringResource(R.string.yes_delete),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        },
                        dismissButton = {
                            OutlinedButton(
                                onClick = { showDialog = false },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(stringResource(R.string.cancel), fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                }

//                // Updated Waypoint List UI
//                Card(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .height(200.dp)
//                        .align(Alignment.BottomStart)
//                        .padding(bottom = 80.dp), // Adds spacing above buttons
//                    shape = RoundedCornerShape(12.dp),
//                    elevation = CardDefaults.cardElevation(6.dp)
//                ) {
//                    Column(
//                        modifier = Modifier.padding(12.dp)
//                    ) {
//                        Text(
//                            text = "List",
//                            fontWeight = FontWeight.Bold,
//                            fontSize = 16.sp,
//                            color = Color.Black
//                        )
//                        Spacer(modifier = Modifier.height(8.dp))
//
//                        LazyColumn(
//                            modifier = Modifier.fillMaxWidth()
//                        ) {
//                            items(waypointList) { waypoint ->
//                                Card(
//                                    modifier = Modifier
//                                        .fillMaxWidth()
//                                        .padding(vertical = 4.dp),
//                                    shape = RoundedCornerShape(8.dp),
//                                    colors = CardDefaults.cardColors(
//                                        containerColor = Color(
//                                            0xFFF0F0F0
//                                        )
//                                    )
//                                ) {
//                                    Row(
//                                        modifier = Modifier
//                                            .fillMaxWidth()
//                                            .padding(12.dp),
//                                        horizontalArrangement = Arrangement.SpaceBetween,
//                                        verticalAlignment = Alignment.CenterVertically
//                                    ) {
//                                        Text(
//                                            text = stringResource(
//                                                R.string.clear_waypoints,
//                                                waypoint.first
//                                            ),
//                                            fontSize = 14.sp,
//                                            color = Color.Black
//                                        )
//
//                                        IconButton(
//                                            onClick = {
//                                                removeWaypoint(
//                                                    waypoint,
//                                                    waypointList,
//                                                    pointAnnotationManager
//                                                )
//                                            },
//                                            modifier = Modifier.size(24.dp)
//                                        ) {
//                                            Icon(
//                                                Icons.Default.Close,
//                                                contentDescription = "Delete",
//                                                tint = Color.Red
//                                            )
//                                        }
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }
            }
        } else {
            Button(
                onClick = { requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF19731B))
            ) {
                Text(stringResource(R.string.request_permission), color = Color.White)
            }
        }
    }
}

// Helper function to create waypoints
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
    waypointCounter.value++

//    Toast.makeText(
//        context,
//        "Waypoint added at: ${point.latitude()}, ${point.longitude()}",
//        Toast.LENGTH_SHORT
//    ).show()
}

@SuppressLint("StringFormatInvalid")
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

    val waypointsRef = db.collection("waypoints")

    // Step 1: Delete all old waypoints before saving new ones
    waypointsRef.get().addOnSuccessListener { result ->
        val batch = db.batch()
        for (document in result) {
            batch.delete(document.reference)
        }
        batch.commit().addOnSuccessListener {

            val reorderedWaypoints = waypointList.mapIndexed { index, (latitude, longitude) ->
                    hashMapOf(
                        "latitude" to latitude,
                        "longitude" to longitude,
                        "label" to (index + 1).toString(),
                        "timestamp" to com.google.firebase.Timestamp.now()
                    )
                }

                // Step 3: Save new waypoints after old ones are deleted
                val newBatch = db.batch()
                reorderedWaypoints.forEach { waypoint ->
                    val docRef = waypointsRef.document()
                    newBatch.set(docRef, waypoint)
            }

            // Commit the batch
            newBatch.commit()
                .addOnSuccessListener {
                    Toast.makeText(
                        context,
                        context.getString(R.string.waypoints_saved_success, waypointList.size),
                        Toast.LENGTH_SHORT
                    ).show()
                    println("Successfully saved ${waypointList.size} waypoints")
                }
                .addOnFailureListener { e ->
                    Toast.makeText(
                        context,
                        context.getString(R.string.failed_to_save_waypoints, e.message),
                        Toast.LENGTH_SHORT
                    ).show()
                    println("Error saving waypoints: ${e.message}")
                    e.printStackTrace()
                }
        }
    }
    }

private fun removeWaypoint(
    waypoint: Pair<Double, Double>,
    waypointList: MutableList<Pair<Double, Double>>,
    pointAnnotationManager: PointAnnotationManager?
) {
    // Remove from local list
    waypointList.remove(waypoint)

    // Find the corresponding annotation on the map and remove it
    pointAnnotationManager?.annotations?.find { annotation ->
        annotation.point.latitude() == waypoint.first && annotation.point.longitude() == waypoint.second
    }?.let { annotationToRemove ->
        pointAnnotationManager.delete(annotationToRemove)
    }

    // Reorder labels after removal
    waypointList.forEachIndexed { index, _ ->
        waypointList[index] = Pair(waypointList[index].first, waypointList[index].second)
    }

}


private fun loadWaypoints(
    db: FirebaseFirestore,
    pointAnnotationManager: PointAnnotationManager,
    waypointList: MutableList<Pair<Double, Double>>,
    context: Context,
    waypointCounter: MutableState<Int>,
) {
    println("Starting to load waypoints...")

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

            // Update waypointCounter to be the next available number
            waypointCounter.value = waypointList.size + 1

            Toast.makeText(
                context,
                context.getString(R.string.loaded_waypoints_count, waypointList.size),
                Toast.LENGTH_SHORT
            ).show()
            println("Finished loading ${waypointList.size} waypoints")
        }
        .addOnFailureListener { e ->
            Toast.makeText(
                context,
                context.getString(R.string.failed_to_load_waypoints, e.message ?: "Unknown error"),
                Toast.LENGTH_SHORT
            ).show()
            println("Error loading waypoints: ${e.message}")
            e.printStackTrace()
        }
}

// Function to delete all waypoints from Firebase
private fun deleteAllWaypointsFromFirebase(db: FirebaseFirestore, context: Context) {
    val waypointsRef = db.collection("waypoints")

    waypointsRef.get().addOnSuccessListener { result ->
        if (result.isEmpty) {
            Toast.makeText(context, context.getString(R.string.no_waypoints_to_delete), Toast.LENGTH_SHORT).show()
            return@addOnSuccessListener
        }

        val batch = db.batch()
        for (document in result) {
            batch.delete(document.reference)
        }

        batch.commit()
            .addOnSuccessListener {
                Toast.makeText(context, context.getString(R.string.all_waypoints_deleted), Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    context,
                    context.getString(R.string.failed_to_delete_waypoints, e.message),
                    Toast.LENGTH_SHORT
                ).show()
            }
    }.addOnFailureListener { e ->
        Toast.makeText(
            context,
            context.getString(R.string.error_fetching_waypoints, e.message),
            Toast.LENGTH_SHORT
        ).show()
    }
}

// Function to check if location permission is granted
private fun checkLocationPermission(context: android.content.Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
}
