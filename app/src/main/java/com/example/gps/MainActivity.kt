package com.example.gps

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.rememberNavController
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.example.gps.navigation.BottomNavBar
import android.net.Uri
import com.example.gps.MapScreen
import com.example.gps.TrailCreationScreen
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

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

        val firebaseAppCheck = FirebaseAppCheck.getInstance()
        firebaseAppCheck.installAppCheckProviderFactory(
            PlayIntegrityAppCheckProviderFactory.getInstance()
        )

        setContent {
            TrailApp()
        }

    }

    // Check for location permission and proceed accordingly
    private fun checkPermissionsAndProceed() {
        if (hasLocationPermission()) {
        } else {
            requestLocationPermission()
        }
    }

    // Function to check if location permissions are granted
    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    // Request Location Permission
    private fun requestLocationPermission() {
        requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    // Handle permission result
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
            } else {
                Toast.makeText(this, "Permission Denied. App cannot function properly.", Toast.LENGTH_LONG).show()
            }
        }

}

@Composable
fun TrailApp() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = { BottomNavBar(navController) } // Attach BottomNavBar
    ) { innerPadding ->
        NavHostContainer(navController, Modifier.padding(innerPadding))
    }
}


@Composable
fun NavHostContainer(navController: NavHostController, modifier: Modifier) {
    NavHost(
        navController = navController,
        startDestination = "map",
        modifier = modifier
    ) {
        composable("map") { MapScreen() }
        composable("create") { TrailCreationScreen() }
        composable("browse") {
            BrowseScreen { trail ->
                navController.navigate(
                    "details/${Uri.encode(trail.name)}/${Uri.encode(trail.location)}/${Uri.encode(trail.difficulty)}/${Uri.encode(trail.imageUrl)}"
                )
            }
        }
        composable(
            "details/{name}/{location}/{difficulty}/{imageUrl}",
            arguments = listOf(
                navArgument("name") { type = NavType.StringType },
                navArgument("location") { type = NavType.StringType },
                navArgument("difficulty") { type = NavType.StringType },
                navArgument("imageUrl") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val name = Uri.decode(backStackEntry.arguments?.getString("name") ?: "")
            val location = Uri.decode(backStackEntry.arguments?.getString("location") ?: "")
            val difficulty = Uri.decode(backStackEntry.arguments?.getString("difficulty") ?: "")
            val imageUrl = backStackEntry.arguments?.getString("imageUrl") ?: ""

            val trail = Trail(name, location, difficulty, imageUrl)
            TrailDetailsScreen(trail)
        }
    }
}