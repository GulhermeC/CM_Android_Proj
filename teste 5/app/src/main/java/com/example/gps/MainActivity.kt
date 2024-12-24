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

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
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

            // Use a touch listener to detect map clicks
            mapView.setOnTouchListener { _, event ->
                if (event.action == android.view.MotionEvent.ACTION_UP) {
                    val screenPoint = com.mapbox.maps.ScreenCoordinate(event.x.toDouble(), event.y.toDouble())
                    val mapPoint = mapboxMap.coordinateForPixel(screenPoint)
                    if (mapPoint != null) {
                        createWaypoint(mapPoint)
                    }
                }
                false // Return false to allow further event processing
            }
        }
    }


    private fun createWaypoint(point: com.mapbox.geojson.Point) {
        val pointAnnotationOptions = PointAnnotationOptions()
            .withPoint(point)
            .withIconImage("marker-default") // Use the default icon for testing

        // Add the point annotation to the manager
        pointAnnotationManager.create(pointAnnotationOptions)

        Toast.makeText(this, "Waypoint added at: ${point.latitude()}, ${point.longitude()}", Toast.LENGTH_SHORT).show()
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
