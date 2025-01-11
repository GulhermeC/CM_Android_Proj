package com.example.gps

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapInitOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.ResourceOptions
import com.mapbox.maps.Style
import com.mapbox.maps.ScreenCoordinate
import com.mapbox.maps.extension.style.layers.properties.generated.TextAnchor
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.locationcomponent.location
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.ktx.auth
import kotlinx.coroutines.launch

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


    // Initialize map and location tracking
    LaunchedEffect(mapView) {
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

        // Set up touch listener for waypoint creation
        mapView.setOnTouchListener { _, event ->
            if (event.action == android.view.MotionEvent.ACTION_UP) {
                val screenPoint = ScreenCoordinate(event.x.toDouble(), event.y.toDouble())
                val mapPoint = mapboxMap.coordinateForPixel(screenPoint)

                coroutineScope.launch {
                    pointAnnotationManager?.let {
                        createWaypoint(it, mapPoint, waypointCounter, waypointList, context)
                        waypointCounter.intValue++
                    }
                }
            }
            false
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Map View
            AndroidView(
                factory = { mapView },
                modifier = Modifier.fillMaxSize()
            )

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
                        Text("Load Waypoints")
                    }

                    FilledTonalButton(
                        onClick = {
                            saveWaypoint(waypointList, db, context)
                        }
                    ) {
                        Text("Save Waypoint")
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
                        Text("Clear Waypoints")
                    }
                }
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
        Toast.makeText(context, "No waypoints to save", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(context, "All waypoints saved successfully!", Toast.LENGTH_SHORT).show()
            println("Successfully saved ${waypointList.size} waypoints")
        }
        .addOnFailureListener { e ->
            Toast.makeText(context, "Failed to save waypoints: ${e.message}", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(context, "No waypoints found!", Toast.LENGTH_SHORT).show()
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

            Toast.makeText(
                context,
                "Loaded ${waypointList.size} waypoints",
                Toast.LENGTH_SHORT
            ).show()
            println("Finished loading ${waypointList.size} waypoints")
        }
        .addOnFailureListener { e ->
            Toast.makeText(context, "Failed to load: ${e.message}", Toast.LENGTH_SHORT).show()
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
    Toast.makeText(context, "Waypoints cleared from map, but still saved in Firestore!", Toast.LENGTH_SHORT).show()
}
