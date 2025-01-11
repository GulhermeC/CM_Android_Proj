package com.example.gps

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.compass.CompassPlugin
import com.mapbox.maps.plugin.gestures.GesturesPlugin
import com.mapbox.maps.plugin.gestures.OnMapClickListener

class MainActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var mapboxMap: MapboxMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set the layout containing the MapView
        setContentView(R.layout.activity_main)

        // Initialize the MapView
        mapView = findViewById(R.id.mapView)
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

            // Enable Compass Plugin
            CompassPlugin(mapView)

            // Enable Gestures Plugin
            val gesturesPlugin = GesturesPlugin(mapView)
            gesturesPlugin.addOnMapClickListener(object : OnMapClickListener {
                override fun onMapClick(point: Point): Boolean {
                    // Handle map click event here
                    return true
                }
            })
        }
    }

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

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }
}
