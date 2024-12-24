package com.example.gps

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
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
import com.mapbox.maps.plugin.locationcomponent.location

class MainActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var mapboxMap: MapboxMap

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
            // Enable the LocationComponent
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

            // Add touch listener for map clicks
            setupTouchListener()
        }
    }

    private fun setupTouchListener() {
        mapView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                // Use Mapbox's ScreenCoordinate class for screen coordinates
                val screenCoordinate = com.mapbox.maps.ScreenCoordinate(event.x.toDouble(), event.y.toDouble())

                // Convert ScreenCoordinate to geographical coordinates
                val clickedLatLng = mapboxMap.coordinateForPixel(screenCoordinate)

                // Pass the geographical coordinates to handleMapClick
                handleMapClick(clickedLatLng)
            }
            true
        }
    }

    private fun handleMapClick(latLng: com.mapbox.geojson.Point) {
        // Show a Toast with the clicked location's coordinates
        Toast.makeText(
            this,
            "Clicked location: Latitude ${latLng.latitude()}, Longitude ${latLng.longitude()}",
            Toast.LENGTH_SHORT
        ).show()

        // TODO: Add logic to display a marker or save waypoint data
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

