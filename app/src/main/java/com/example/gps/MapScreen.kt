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

    // UI Layout
    Box(modifier = Modifier.fillMaxSize()) {
        // Map View
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize()
        )

        // Control Buttons
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    pointAnnotationManager?.let {
                        loadWaypoints(db, it, waypointList, context)
                    }
                }
            ) {
                Text("Load Waypoints")
            }

            Button(
                onClick = {
                    saveWaypoint(waypointList, db, context)
                }
            ) {
                Text("Save Waypoint")
            }

            Button(
                onClick = {
                    pointAnnotationManager?.let { manager ->
                        clearWaypoints(db, manager, waypointList, context, waypointCounter)
                    }
                }
            ) {
                Text("Clear Waypoints")
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

    Toast.makeText(
        context,
        "Waypoint ${waypointCounter.value} added at: ${point.latitude()}, ${point.longitude()}",
        Toast.LENGTH_SHORT
    ).show()
}

private fun saveWaypoint(
    waypointList: List<Pair<Double, Double>>,
    db: FirebaseFirestore,
    context: Context
) {
    if (waypointList.isEmpty()) {
        Toast.makeText(context, "No waypoints to save", Toast.LENGTH_SHORT).show()
        return
    }

    waypointList.forEachIndexed { index, (latitude, longitude) ->
        val waypoint = hashMapOf(
            "latitude" to latitude,
            "longitude" to longitude,
            "label" to (index + 1).toString()
        )

        db.collection("waypoints")
            .add(waypoint)
            .addOnSuccessListener {
                Toast.makeText(context, "Waypoint saved!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to save: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}

fun createMutableWaypointState(initialValue: Int): MutableState<Int> {
    return mutableIntStateOf(initialValue)
}

private fun loadWaypoints(
    db: FirebaseFirestore,
    pointAnnotationManager: PointAnnotationManager,
    waypointList: MutableList<Pair<Double, Double>>,
    context: Context
) {
    waypointList.clear() // Ensure previous waypoints are removed before loading
    db.collection("waypoints")
        .get()
        .addOnSuccessListener { result ->
            for (document in result) {
                val latitude = document.getDouble("latitude") ?: continue
                val longitude = document.getDouble("longitude") ?: continue
                val point = Point.fromLngLat(longitude, latitude)

                createWaypoint(
                    pointAnnotationManager,
                    point,
                    mutableIntStateOf(waypointList.size + 1), // Ensures correct numbering, // Wrap in MutableState<Int> ,
                    waypointList,
                    context
                )
            }
            Toast.makeText(context, "Waypoints loaded!", Toast.LENGTH_SHORT).show()
        }
        .addOnFailureListener { e ->
            Toast.makeText(context, "Failed to load: ${e.message}", Toast.LENGTH_SHORT).show()
        }
}

private fun clearWaypoints(
    db: FirebaseFirestore,
    pointAnnotationManager: PointAnnotationManager,
    waypointList: MutableList<Pair<Double, Double>>,
    context: Context,
    waypointCounter: MutableState<Int>
) {
    db.collection("waypoints")
        .get()
        .addOnSuccessListener { result ->
            for (document in result) {
                db.collection("waypoints").document(document.id).delete()
            }
            pointAnnotationManager.deleteAll()
            waypointList.clear()
            waypointCounter.value = 1 // Correctly update mutable state
            Toast.makeText(context, "All waypoints cleared!", Toast.LENGTH_SHORT).show()
        }
        .addOnFailureListener { e ->
            Toast.makeText(context, "Failed to clear: ${e.message}", Toast.LENGTH_SHORT).show()
        }
}




