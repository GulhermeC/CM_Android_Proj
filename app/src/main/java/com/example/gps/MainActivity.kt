package com.example.gps

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class MainActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(this)
        auth = Firebase.auth

        checkPermissionsAndProceed()
    }

    // ✅ Check for location permission and proceed accordingly
    private fun checkPermissionsAndProceed() {
        if (hasLocationPermission()) {
            navigateToMapScreen()
        } else {
            requestLocationPermission()
        }
    }

    // ✅ Function to check if location permissions are granted
    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    // ✅ Request Location Permission
    private fun requestLocationPermission() {
        requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    // ✅ Handle permission result
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                navigateToMapScreen()
            } else {
                Toast.makeText(this, "Permission Denied. App cannot function properly.", Toast.LENGTH_LONG).show()
            }
        }

    // ✅ Navigate to `MapScreen.kt`
    private fun navigateToMapScreen() {
        setContent {
            MapScreen()
        }
    }
}
