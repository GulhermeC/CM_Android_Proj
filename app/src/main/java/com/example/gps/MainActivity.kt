package com.example.gps

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Bundle
import android.widget.Button
import android.widget.FrameLayout
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
import com.mapbox.maps.extension.style.layers.properties.generated.TextAnchor
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.locationcomponent.location

class MainActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var mapboxMap: MapboxMap
    private lateinit var pointAnnotationManager: PointAnnotationManager
    private lateinit var databaseHelper: WaypointDatabaseHelper
    private var waypointCounter = 1

    // Variables to store the latitude and longitude of the created waypoints
    private val waypointList = mutableListOf<Pair<Double, Double>>()

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

        // Initialize SQLite database
        databaseHelper = WaypointDatabaseHelper(this)

        // Add "Load" button
        val loadButton = Button(this).apply {
            text = "Load Waypoints"
            setOnClickListener { loadWaypoints() }
        }
        val buttonLayoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            marginStart = 16
            topMargin = 16
        }
        addContentView(loadButton, buttonLayoutParams)

        // Add "Save" button
        val saveButton = Button(this).apply {
            text = "Save Waypoint"
            setOnClickListener { saveWaypoint() }
        }
        val saveButtonLayoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            marginStart = 16
            topMargin = 80 // position below the load button
        }
        addContentView(saveButton, saveButtonLayoutParams)

        // Add "Clear" button
        val clearButton = Button(this).apply {
            text = "Clear Waypoints"
            setOnClickListener { clearWaypoints() }
        }
        val clearButtonLayoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            marginStart = 16
            topMargin = 160 // position below the save button
        }
        addContentView(clearButton, clearButtonLayoutParams)

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

    @SuppressLint("ClickableViewAccessibility")
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
                    val screenPoint = ScreenCoordinate(event.x.toDouble(), event.y.toDouble())
                    val mapPoint = mapboxMap.coordinateForPixel(screenPoint)
                    createWaypoint(mapPoint)
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

        // Add the point's latitude and longitude to the list
        waypointList.add(Pair(point.latitude(), point.longitude()))

        Toast.makeText(
            this,
            "Waypoint $waypointCounter added at: ${point.latitude()}, ${point.longitude()}",
            Toast.LENGTH_SHORT
        ).show()

        // Increment counter for the next waypoint
        waypointCounter++
    }

    private fun saveWaypoint() {
        if (waypointList.isNotEmpty()) {
            // Save all waypoints from the list to the database
            for ((latitude, longitude) in waypointList) {
                val point = Point.fromLngLat(longitude, latitude)
                saveWaypointToDatabase(point, waypointCounter.toString())
                waypointCounter++ // Increment for next waypoint
            }
            // Clear the waypoint list after saving
            waypointList.clear()

            Toast.makeText(this, "Waypoints saved!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "No waypoints to save", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveWaypointToDatabase(point: Point, label: String) {
        val db = databaseHelper.writableDatabase
        db.execSQL(
            "INSERT INTO waypoints (latitude, longitude, label) VALUES (?, ?, ?)",
            arrayOf(point.latitude(), point.longitude(), label)
        )
    }

    private fun loadWaypoints() {
        val db = databaseHelper.readableDatabase
        val cursor: Cursor = db.rawQuery("SELECT * FROM waypoints", null)
        if (cursor.moveToFirst()) {
            do {
                val latitude = cursor.getDouble(cursor.getColumnIndexOrThrow("latitude"))
                val longitude = cursor.getDouble(cursor.getColumnIndexOrThrow("longitude"))
                val point = Point.fromLngLat(longitude, latitude)
                createWaypoint(point)  // Add waypoint to the map
            } while (cursor.moveToNext())
        }
        cursor.close()
        Toast.makeText(this, "Waypoints loaded!", Toast.LENGTH_SHORT).show()
    }

    private fun clearWaypoints() {
        // Clear all point annotations from the map
        pointAnnotationManager.deleteAll()

        // Clear the waypoint list
        waypointList.clear()

        waypointCounter = 1

        Toast.makeText(this, "All waypoints cleared", Toast.LENGTH_SHORT).show()
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
        databaseHelper.close()
    }

    // SQLite Database Helper
    class WaypointDatabaseHelper(context: MainActivity) :
        SQLiteOpenHelper(context, "waypoints.db", null, 1) {

        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL("CREATE TABLE waypoints (id INTEGER PRIMARY KEY AUTOINCREMENT, latitude REAL, longitude REAL, label TEXT)")
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            db.execSQL("DROP TABLE IF EXISTS waypoints")
            onCreate(db)
        }
    }
}

