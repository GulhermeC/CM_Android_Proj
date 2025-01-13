package com.example.gps

import LoginScreen
import RegisterScreen
import android.Manifest
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
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.example.gps.navigation.BottomNavBar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.example.gps.data.Trail
import com.example.gps.screens.BrowseScreen
import com.example.gps.screens.TrailCreationScreen
import com.example.gps.screens.TrailDetailsScreen
import com.example.gps.screens.UserTrailScreen
import com.example.gps.screens.WaypointSelectionScreen
import com.example.gps.viewmodels.LoginViewModel
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.gps.screens.FavoritesScreen
import com.example.gps.ui.theme.GpsTheme

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

        createNotificationChannel(this)

        setContent {
            val navController = rememberNavController()
            val viewModel: LoginViewModel = viewModel()

            GpsTheme {
                TrailApp(navController, viewModel)
            }
        }

    }

    // Check for location permission and proceed accordingly
    private fun checkPermissionsAndProceed() {
        val requiredPermissions = mutableListOf<String>()

        if (!hasLocationPermission()) {
            requiredPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requiredPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (requiredPermissions.isNotEmpty()) {
            requestPermissionLauncher.launch(requiredPermissions.toTypedArray())
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
        requestPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
    }

    // Handle permission result
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
            val notificationsGranted = permissions[Manifest.permission.POST_NOTIFICATIONS] ?: false

            if (!locationGranted) {
                Toast.makeText(this, "Location permission is required for the app to function.", Toast.LENGTH_LONG).show()
            }

            if (!notificationsGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Toast.makeText(this, "Notification permission is required for notifications.", Toast.LENGTH_LONG).show()
            }
        }

}

@Composable
fun TrailApp(navController: NavHostController,viewModel: LoginViewModel = viewModel()) {
    val auth = Firebase.auth
    val rememberMe by viewModel.rememberMeFlow.collectAsState(initial = false)

    // 🔹 Compute `startDestination` dynamically
    val startDestination by remember {
        derivedStateOf {
            if (rememberMe && auth.currentUser != null) "create" else "login"
        }
    }

    Scaffold(
        bottomBar = { BottomNavBar(navController) }
    ) { innerPadding ->
        NavHostContainer(navController, Modifier.padding(innerPadding), startDestination!!, viewModel)
    }

}


@Composable
fun NavHostContainer(navController: NavHostController, modifier: Modifier,startDestination: String,viewModel: LoginViewModel) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable("login") { LoginScreen(navController, viewModel) }
        composable("register") { RegisterScreen(navController) }
        composable("map") { MapScreen() }
        composable("favorites") { FavoritesScreen(navController) }
        composable("create") { TrailCreationScreen( navController) }
        composable("browse") {
            BrowseScreen (){ trail ->
                navController.currentBackStackEntry?.savedStateHandle?.set("selectedTrail", trail)
                navController.navigate("details")
            }
        }
        composable("details") { backStackEntry ->
            val trail = navController.previousBackStackEntry
                ?.savedStateHandle
                ?.get<Trail>("selectedTrail")

            trail?.let { TrailDetailsScreen(it, navController) }
        }
        composable("waypointSelection") { WaypointSelectionScreen(navController) }
        composable("userTrail/{trailId}?waypoints={waypoints}",
            arguments = listOf(
                navArgument("trailId") { type = NavType.StringType },
                navArgument("waypoints") { type = NavType.StringType; nullable = true }
            )
        ) { backStackEntry ->
            val trailId = backStackEntry.arguments?.getString("trailId") ?: ""
            val waypointsArg = backStackEntry.arguments?.getString("waypoints")
            UserTrailScreen(navController, trailId, waypointsArg)
        }
    }
}

@Composable
fun LoadingScreen() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = androidx.compose.ui.graphics.Color.White
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator() // 🔹 Show a loading spinner
            Spacer(modifier = Modifier.height(16.dp))
            Text("Loading...")
        }
    }
}

// Notification Channel Setup
fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            "trail_creation_channel",
            "Trail Creation Notifications",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifies when a new trail is created"
        }
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}

// Show Notification
fun showNotification(context: Context, trailName: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) {
        Toast.makeText(context, "Notification permission is required to show notifications.", Toast.LENGTH_SHORT).show()
        return
    }

    val notification = NotificationCompat.Builder(context, "trail_creation_channel")
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle("Trail Created")
        .setContentText("A new Trail has been created!")
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .build()

    NotificationManagerCompat.from(context).notify(1, notification)
}

