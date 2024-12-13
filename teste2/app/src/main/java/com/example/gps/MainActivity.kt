package com.example.gps

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapInitOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.ResourceOptions
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.Plugin
import com.mapbox.maps.plugin.compass.CompassPlugin
import com.mapbox.maps.plugin.gestures.GesturesPlugin
import com.mapbox.maps.plugin.gestures.OnMapClickListener

class MainActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var mapboxMap: MapboxMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize MapInitOptions with the access token
        val mapInitOptions = MapInitOptions(
            context = this,
            resourceOptions = ResourceOptions.Builder()
                .accessToken("sk.eyJ1IjoicnVpdWEiLCJhIjoiY200bXM4czU5MDBwZDJrcjJsZW9qNzVjOCJ9.TlDHWxGJe7rdI03udVud3w")
// Replace with your access token
                .build()
        )

        // Programmatically initialize MapView
        mapView = MapView(this, mapInitOptions)
        setContentView(mapView)

        // Get MapboxMap from the MapView
        mapboxMap = mapView.getMapboxMap()

        // Load the Mapbox style
        mapboxMap.loadStyleUri(Style.MAPBOX_STREETS) { style ->
            // Set camera position to a default location (Madrid)
            val randomLocation = Point.fromLngLat(-3.7038, 40.4168) // Madrid, Spain as an example
            val cameraOptions = CameraOptions.Builder()
                .center(randomLocation)
                .zoom(10.0)
                .build()
            mapboxMap.setCamera(cameraOptions)

            // Access plugins
            val compassPlugin = mapView.getPlugin(Plugin.MAPBOX_COMPASS_PLUGIN_ID) as? CompassPlugin
            val gesturesPlugin = mapView.getPlugin(Plugin.MAPBOX_GESTURES_PLUGIN_ID) as? GesturesPlugin

            // Use the plugins if needed
            gesturesPlugin?.addOnMapClickListener(object : OnMapClickListener {
                override fun onMapClick(point: Point): Boolean {
                    // Handle map click event here
                    return true
                }
            })
        }
    }

    // Manual lifecycle management
    override fun onStart() {
        super.onStart()
        mapView.onStart()  // Handles starting map rendering
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()  // Handles stopping map rendering
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy() // Properly cleans up the MapView
    }
}


