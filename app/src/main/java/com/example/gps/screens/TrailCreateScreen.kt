package com.example.gps.screens

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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import com.example.gps.R
import com.example.gps.showNotification


@Composable
fun TrailCreationScreen(navController: NavController) {
    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle

    var trailName by remember {
        mutableStateOf(savedStateHandle?.get<String>("trailName") ?: "")
    }
    var location by remember {
        mutableStateOf(savedStateHandle?.get<String>("location") ?: "")
    }
    var difficulty by remember {
        mutableStateOf(savedStateHandle?.get<String>("difficulty") ?: "Easy")
    }
    var selectedImageUri by remember {
        mutableStateOf(savedStateHandle?.get<Uri>("selectedImageUri"))
    }

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

    val scrollState = rememberScrollState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFE6F2EF) //  Soft pastel background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            //  Title
            Text(
                text = stringResource(R.string.create_trail),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF19731B) // Dark green title
            )

            Spacer(modifier = Modifier.height(16.dp))

            //  Trail Name Input
            OutlinedTextField(
                value = trailName,
                onValueChange = {
                    trailName = it
                    savedStateHandle?.set("trailName", it)
                },
                label = { Text(stringResource(R.string.name_of_trail)) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            //  Location Input with Icon
            OutlinedTextField(
                value = location,
                onValueChange = {
                    location = it
                    savedStateHandle?.set("location", it)
                },
                label = { Text(stringResource(R.string.location)) },
                singleLine = true,
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Location Icon",
                        tint = Color(0xFF19731B) // Dark Green icon
                    )
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            //  Difficulty Selection
            Text(
                text = stringResource(R.string.difficulty),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                DifficultyRadioButton(
                    label = stringResource(R.string.easy),
                    selectedDifficulty = difficulty
                ) { difficulty = it
                    savedStateHandle?.set("difficulty", it)}
                DifficultyRadioButton(
                    label = stringResource(R.string.medium),
                    selectedDifficulty = difficulty
                ) { difficulty = it
                    savedStateHandle?.set("difficulty", it)}
                DifficultyRadioButton(
                    label = stringResource(R.string.hard),
                    selectedDifficulty = difficulty
                ) { difficulty = it
                    savedStateHandle?.set("difficulty", it)}
            }

            Spacer(modifier = Modifier.height(24.dp))

            //  Image Picker Button
            Button(
                onClick = {
                    imagePickerLauncher.launch("image/*")
                    savedStateHandle?.set("selectedImageUri", selectedImageUri)
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF19731B))
            ) {
                Text(
                    stringResource(R.string.select_image),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            //  Display Selected Image
            selectedImageUri?.let { uri ->
                Spacer(modifier = Modifier.height(12.dp))
                Image(
                    painter = rememberAsyncImagePainter(model = uri),
                    contentDescription = null,
                    modifier = Modifier
                        .size(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .padding(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            //  Button to Open Waypoint Selection Screen
            Button(
                onClick = { navController.navigate("waypointSelection") },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 2.dp)
            ) {
                Text(
                    stringResource(R.string.select_waypoints),
                    color = Color(0xFF19731B),
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            //  Mini Map to Show Selected Waypoints
            if (selectedWaypoints.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.waypoint_preview),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                MiniMap(selectedWaypoints)
                Spacer(modifier = Modifier.height(16.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))

            //  Save Button
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
                            val message = if (success) {
                                showNotification(context, trailName)
                                context.getString(R.string.trail_saved_success)
                            } else {
                                context.getString(R.string.trail_saved_failed)
                            }
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        val fillAllFieldsMessage = context.getString(R.string.fill_all_fields)
                        Toast.makeText(context, fillAllFieldsMessage, Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF19731B))
            ) {
                Text(
                    if (isSaving) stringResource(R.string.saving) else stringResource(R.string.save_trail),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

//  Styled Difficulty Radio Buttons
@Composable
fun DifficultyRadioButton(label: String, selectedDifficulty: String, onSelection: (String) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selectedDifficulty == label,
            onClick = { onSelection(label) },
            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF19731B)) // Dark Green selection
        )
        Text(text = label, modifier = Modifier.padding(start = 4.dp), fontWeight = FontWeight.Medium)
    }
}

//  **Updated MiniMap UI
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

            // Add markers for each waypoint with better styling
            waypoints.forEachIndexed { index, (lat, lng) ->
                val point = Point.fromLngLat(lng, lat)
                val pointAnnotationOptions = PointAnnotationOptions()
                    .withPoint(point)
                    .withIconSize(2.0) // Adjusted size
                    .withIconImage("marker-15")
                    .withTextField((index + 1).toString()) // Label waypoints
                    .withTextSize(14.0)
                    .withTextColor("#19731B") // Dark Green
                    .withTextOffset(listOf(0.0, 1.2))

                pointAnnotationManager?.create(pointAnnotationOptions)
            }

            //  Center map on first waypoint
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

    // Better MiniMap Styling
    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
        }
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
                    onComplete(true)
                    showNotification(context, trailName)
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
    // Prepare the trail data
    val trailData = mapOf(
        "name" to trailName,
        "location" to location,
        "difficulty" to difficulty,
        "imageUrl" to imageUrl
    )

    // Add the trail document to the "trails" collection
    firestore.collection("trails")
        .add(trailData)
        .addOnSuccessListener { trailDocument ->
            // Reference to the "waypoints" subcollection
            val waypointsRef = trailDocument.collection("waypoints")

            // Save each waypoint in the subcollection
            val batch = firestore.batch()
            selectedWaypoints.forEachIndexed { index, (latitude, longitude) ->
                val waypointData = mapOf(
                    "latitude" to latitude,
                    "longitude" to longitude,
                    "label" to (index + 1).toString()
                )
                val waypointDoc = waypointsRef.document()
                batch.set(waypointDoc, waypointData)
            }

            // Commit the batch operation
            batch.commit()
                .addOnSuccessListener {
                    onComplete(true) // Success
                }
                .addOnFailureListener {
                    onComplete(false) // Failed to save waypoints
                }
        }
        .addOnFailureListener {
            onComplete(false) // Failed to save trail
        }
}