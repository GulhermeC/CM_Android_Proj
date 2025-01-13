package com.example.gps

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.rememberAsyncImagePainter
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager


@Composable
fun TrailCreationScreen(navController: NavController) {
    var trailName by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var difficulty by remember { mutableStateOf("Easy") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    val selectedWaypoints = navController
        .currentBackStackEntry
        ?.savedStateHandle
        ?.get<List<Pair<Double, Double>>>("waypoints") ?: emptyList()

    val firestore = FirebaseFirestore.getInstance()
    val storage = FirebaseStorage.getInstance()
    val context = LocalContext.current

    // Image Picker Launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            selectedImageUri = uri
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Create a Trail",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Trail Name Input
        OutlinedTextField(
            value = trailName,
            onValueChange = { trailName = it },
            label = { Text("Name of the trail") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Location Input with Icon
        OutlinedTextField(
            value = location,
            onValueChange = { location = it },
            label = { Text("Location") },
            singleLine = true,
            trailingIcon = {
                Icon(imageVector = Icons.Default.LocationOn, contentDescription = "Location Icon")
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Difficulty Selection
        Text(text = "Difficulty", fontSize = 18.sp, color = Color.Black)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            DifficultyRadioButton(label = "Easy", selectedDifficulty = difficulty) { difficulty = it }
            DifficultyRadioButton(label = "Medium", selectedDifficulty = difficulty) { difficulty = it }
            DifficultyRadioButton(label = "Hard", selectedDifficulty = difficulty) { difficulty = it }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Image Picker
        Button(onClick = { imagePickerLauncher.launch("image/*") }) {
            Text("Select Image")
        }

        // Display Selected Image
        selectedImageUri?.let { uri ->
            Spacer(modifier = Modifier.height(12.dp))
            Image(
                painter = rememberAsyncImagePainter(model = uri),
                contentDescription = "Selected Image",
                modifier = Modifier
                    .size(200.dp)
                    .padding(8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Button to open Waypoint Selection Screen
        Button(onClick = { navController.navigate("waypointSelection") }) {
            Text("Select Waypoints")
        }


        // **Mini Map to Show Selected Waypoints**
        if (selectedWaypoints.isNotEmpty()) {
            Text("Waypoint Preview:", fontSize = 16.sp, color = Color.Black)
            MiniMap(selectedWaypoints)
            Spacer(modifier = Modifier.height(16.dp))
        }


        Spacer(modifier = Modifier.height(12.dp))

        // Save Button
        Button(
            onClick = {
                if (trailName.isNotBlank() && location.isNotBlank()) {
                    isSaving = true
                    saveTrail(
                        trailName,
                        location,
                        difficulty,
                        selectedWaypoints,
                        selectedImageUri,
                        firestore,
                        storage,
                        context
                    ) { success ->
                        isSaving = false
                        if (success) {
                            Toast.makeText(context, "Trail saved successfully!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Failed to save trail", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSaving
        ) {
            Text(if (isSaving) "Saving..." else "Save Trail")
        }
    }
}

@Composable
fun DifficultyRadioButton(label: String, selectedDifficulty: String, onSelection: (String) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selectedDifficulty == label,
            onClick = { onSelection(label) }
        )
        Text(text = label, modifier = Modifier.padding(start = 4.dp))
    }
}

@Composable
fun MiniMap(waypoints: List<Pair<Double, Double>>) {
    val context = LocalContext.current
    val mapView = rememberMapViewWithLifecycle()
    val mapboxMap = remember { mapView.getMapboxMap() }
    var pointAnnotationManager by remember { mutableStateOf<PointAnnotationManager?>(null) }

    LaunchedEffect(waypoints) {
        mapboxMap.loadStyleUri(Style.MAPBOX_STREETS) { style ->
            pointAnnotationManager = mapView.annotations.createPointAnnotationManager()

            // Clear existing markers
            pointAnnotationManager?.deleteAll()

            // Add markers for each waypoint
            waypoints.forEachIndexed { index, (lat, lng) ->
                val point = Point.fromLngLat(lng, lat)
                val pointAnnotationOptions = PointAnnotationOptions()
                    .withPoint(point)
                    .withIconSize(1.5)
                    .withIconImage("marker-15")
                    .withTextField((index + 1).toString()) // Label with numbers
                    .withTextSize(14.0)
                    .withTextColor("#000000")
                    .withTextOffset(listOf(0.0, 0.5))

                pointAnnotationManager?.create(pointAnnotationOptions)
            }

            // Center map on first waypoint (if available)
            if (waypoints.isNotEmpty()) {
                val firstPoint = waypoints.first()
                mapboxMap.setCamera(
                    CameraOptions.Builder()
                        .center(Point.fromLngLat(firstPoint.second, firstPoint.first))
                        .zoom(13.0)
                        .build()
                )
            }
        }
    }

    Box(modifier = Modifier.height(200.dp).fillMaxWidth()) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
    }
}

private fun saveTrail(
    trailName: String,
    location: String,
    difficulty: String,
    selectedWaypoints: List<Pair<Double, Double>>,
    imageUri: Uri?,
    firestore: FirebaseFirestore,
    storage: FirebaseStorage,
    context: Context,
    onComplete: (Boolean) -> Unit
) {
    // Upload image to Firebase Storage
    if (imageUri != null) {
        val imageRef = storage.reference.child("trails/${trailName}_${System.currentTimeMillis()}.jpg")
        imageRef.putFile(imageUri)
            .addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                    saveTrailData(trailName, location, difficulty, selectedWaypoints, downloadUrl.toString(), firestore, onComplete)
                }.addOnFailureListener {
                    onComplete(false)
                }
            }
            .addOnFailureListener {
                saveTrailData(trailName, location, difficulty, selectedWaypoints, null, firestore, onComplete) // Fallback
            }
    } else {
        saveTrailData(trailName, location, difficulty, selectedWaypoints, null, firestore, onComplete)
    }
}

private fun saveTrailData(
    trailName: String,
    location: String,
    difficulty: String,
    selectedWaypoints: List<Pair<Double, Double>>,
    imageUrl: String?,
    firestore: FirebaseFirestore,
    onComplete: (Boolean) -> Unit
) {
    val trailData = mapOf(
        "name" to trailName,
        "location" to location,
        "difficulty" to difficulty,
        "waypoints" to selectedWaypoints.map { mapOf("latitude" to it.first, "longitude" to it.second) },
        "imageUrl" to imageUrl
    )

    firestore.collection("trails")
        .add(trailData)
        .addOnSuccessListener {
            onComplete(true)
        }
        .addOnFailureListener {
            onComplete(false)
        }
}

