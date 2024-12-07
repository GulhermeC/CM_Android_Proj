package com.example.gps


import com.google.android.gms.location.*
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

import com.google.android.gms.location.LocationServices
import android.location.LocationManager



class MainActivity : ComponentActivity() {
    private fun isGpsEnabled(): Boolean {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setContent {
            var location by remember { mutableStateOf("Fetching location...") }
            var permissionGranted by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                requestLocationPermission { granted ->
                    permissionGranted = granted
                    if (granted) {
                        fetchLocation { lat, lng ->
                            location = "Lat: $lat, Lng: $lng"
                        }
                    } else {
                        location = "Permission denied"
                    }
                }
            }

            LocationScreen(location, permissionGranted)
        }
    }

    private fun requestLocationPermission(onResult: (Boolean) -> Unit) {
        val locationPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            onResult(isGranted)
        }

        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                onResult(true)
            }
            else -> {
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    private fun fetchLocation(onLocationFetched: (Double, Double) -> Unit) {
        if (!isGpsEnabled()) {
            Toast.makeText(this, "GPS is disabled. Please enable it.", Toast.LENGTH_LONG).show()
            return
        }

        try {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location ->
                        if (location != null) {
                            onLocationFetched(location.latitude, location.longitude)
                        } else {
                            requestNewLocationData(onLocationFetched)
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Failed to get location: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "Location permission is required.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun requestNewLocationData(onLocationFetched: (Double, Double) -> Unit) {
        // Check if fine location permission is granted
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "Location permission is required to get updates", Toast.LENGTH_LONG).show()
            return
        }

        // Create a LocationRequest for high accuracy updates every second
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            1000L // 1 second
        ).build()

        // Define a LocationCallback to handle updates
        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation
                if (location != null) {
                    onLocationFetched(location.latitude, location.longitude)
                }
                // Remove location updates after fetching location
                fusedLocationClient.removeLocationUpdates(this)
            }
        }

        try {
            // Request location updates
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                null // Use the main thread looper
            )
        } catch (e: SecurityException) {
            Toast.makeText(this, "Permission issue: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    @Composable
    fun LocationScreen(location: String, permissionGranted: Boolean) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            CustomAppBar("GPS Location App")
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (!permissionGranted) {
                    Text(
                        text = "Permission not granted!",
                        fontSize = 16.sp,
                        color = Color.Red,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    Text(
                        text = location,
                        fontSize = 16.sp,
                        color = Color.Black,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }

    @Composable
    fun CustomAppBar(title: String) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(Color.Blue),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = title,
                fontSize = 20.sp,
                color = Color.White,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}
