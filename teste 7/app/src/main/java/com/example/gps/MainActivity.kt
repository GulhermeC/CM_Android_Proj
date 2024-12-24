package com.example.gps

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle

import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapInitOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.ResourceOptions
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.properties.generated.TextAnchor
import com.mapbox.maps.ScreenCoordinate
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.locationcomponent.location

class MainActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var mapboxMap: MapboxMap
    private lateinit var pointAnnotationManager: PointAnnotationManager
    private var waypointCounter = 1


    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
    }
    private fun logAvailableIcons(style: Style) {
        try {
            val layers = style.styleLayers.map { it.id } // Logs all layer IDs
            Toast.makeText(this, "Layers: $layers", Toast.LENGTH_LONG).show()

            // Check explicitly for default marker icons
            val markerDefault = style.getStyleImage("marker-default")
            val marker15 = style.getStyleImage("marker-15")

            if (markerDefault != null) {
                Toast.makeText(this, "marker-default exists in style", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "marker-default is missing", Toast.LENGTH_SHORT).show()
            }

            if (marker15 != null) {
                Toast.makeText(this, "marker-15 exists in style", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "marker-15 is missing", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error while checking images: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize MapInitOptions with the access token
        val mapInitOptions = MapInitOptions(
            context = this,
            resourceOptions = ResourceOptions.Builder()
                .accessToken("sk.eyJ1IjoicnVpdWEiLCJhIjoiY200bXM4czU5MDBwZDJrcjJsZW9qNzVjOCJ9.TlDHWxGJe7rdI03udVud3w")
                .build()
        )

        // Programmatically initialize MapView
        mapView = MapView(this, mapInitOptions)
        setContentView(mapView)

        // Get MapboxMap from the MapView
        mapboxMap = mapView.getMapboxMap()

        // Check and request location permissions
        if (hasLocationPermission()) {
            initializeMap()
        } else {
            requestLocationPermission()
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            PERMISSION_REQUEST_CODE
        )
    }

    private fun initializeMap() {
        mapboxMap.loadStyleUri(Style.MAPBOX_STREETS) { style ->
            // Enable the LocationComponent for GPS tracking
            val locationComponentPlugin = mapView.location
            locationComponentPlugin.updateSettings {
                enabled = true
                pulsingEnabled = true
            }
            mapboxMap.loadStyleUri(Style.MAPBOX_STREETS) { style ->
                logAvailableIcons(style)
            }

            // Set camera position to user's location when available
            locationComponentPlugin.addOnIndicatorPositionChangedListener { point ->
                mapboxMap.setCamera(
                    CameraOptions.Builder()
                        .center(point)
                        .zoom(15.0)
                        .build()
                )
            }

            // Initialize the PointAnnotationManager
            pointAnnotationManager = mapView.annotations.createPointAnnotationManager()

            // Debug: Log available layers and sources
            logStyleDetails(style)

            // Use a touch listener to detect map clicks
            mapView.setOnTouchListener { _, event ->
                if (event.action == android.view.MotionEvent.ACTION_UP) {
                    val screenPoint = ScreenCoordinate(event.x.toDouble(), event.y.toDouble())
                    val mapPoint = mapboxMap.coordinateForPixel(screenPoint)
                    if (mapPoint != null) {
                        createWaypoint(mapPoint)
                    }
                }
                false // Return false to allow further event processing
            }
        }
    }

    private fun createWaypoint(point: Point) {
        val pointAnnotationOptions = PointAnnotationOptions()
            .withPoint(point)
            .withIconSize(5.0)
            .withIconImage("marker-15") // Use default marker
            .withTextField(waypointCounter.toString()) // Add waypoint number as text
            .withTextSize(14.0) // Adjust text size
            .withTextColor("black") // Text color
            .withTextAnchor(TextAnchor.TOP) // Use TextAnchor enum for positioning text above the marker

        // Add the point annotation to the manager
        pointAnnotationManager.create(pointAnnotationOptions)

        Toast.makeText(
            this,
            "Waypoint $waypointCounter added at: ${point.latitude()}, ${point.longitude()}",
            Toast.LENGTH_SHORT
        ).show()

        // Increment counter for the next waypoint
        waypointCounter++
    }





    private fun logStyleDetails(style: Style) {
        val layers = style.styleLayers.map { it.id }
        val sources = style.styleSources.map { it.id }
        Toast.makeText(this, "Layers: $layers\nSources: $sources", Toast.LENGTH_LONG).show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initializeMap()
        }
    }

    // Manual lifecycle management
    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }
}

