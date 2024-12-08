package com.example.gps

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.osmdroid.config.Configuration
import org.osmdroid.views.MapView
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.ScaleBarOverlay

class MainActivity : AppCompatActivity() {
    private lateinit var mapView: MapView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize osmdroid configuration
        Configuration.getInstance().userAgentValue = packageName

        // Create the map view
        mapView = MapView(this)
        mapView.setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        setContentView(mapView)

        // Check location permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
        } else {
            setupMap()
        }
    }

    private fun setupMap() {
        // Enable user location
        val locationOverlay = MyLocationNewOverlay(mapView)
        locationOverlay.enableMyLocation()
        mapView.overlays.add(locationOverlay)

        // Add compass overlay
        val compassOverlay = CompassOverlay(this, mapView)
        compassOverlay.enableCompass()
        mapView.overlays.add(compassOverlay)

        // Add scale bar
        val scaleBarOverlay = ScaleBarOverlay(mapView)
        mapView.overlays.add(scaleBarOverlay)

        // Set default zoom and position to a specific location (e.g., Lisbon, Portugal)
        val startPoint = GeoPoint(38.7169, -9.1395) // Coordinates for Lisbon
        mapView.controller.setZoom(15.0) // Adjust the zoom level as needed
        mapView.controller.setCenter(startPoint)
    }


    override fun onResume() {
        super.onResume()
        mapView.onResume() // Needed for osmdroid
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause() // Needed for osmdroid
    }
}
