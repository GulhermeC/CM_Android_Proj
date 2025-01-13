package com.example.gps

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil3.compose.rememberAsyncImagePainter
import com.example.gps.data.Trail
import androidx.compose.ui.res.stringResource
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun TrailDetailsScreen(trail: Trail,navController: NavController) {

    var waypoints by remember { mutableStateOf<List<Pair<Double, Double>>>(emptyList()) }
    val firestore = FirebaseFirestore.getInstance()

    // Fetch the waypoints from Firestore, if we have a valid doc ID
    LaunchedEffect(trail.id) {
        if (trail.id.isNotEmpty()) {
            firestore.collection("trails")
                .document(trail.id)
                .collection("waypoints")
                .get()
                .addOnSuccessListener { snapshot ->
                    val fetchedWaypoints = snapshot.mapNotNull { doc ->
                        val lat = doc.getDouble("latitude")
                        val lng = doc.getDouble("longitude")
                        if (lat != null && lng != null) {
                            Pair(lat, lng)
                        } else null
                    }
                    waypoints = fetchedWaypoints
                }
                .addOnFailureListener { e ->
                    Log.e("TrailDetailsScreen", "Failed to fetch waypoints: ${e.message}")
                }
        }
    }

    IconButton(onClick = { navController.popBackStack()  }) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
    }
    Spacer(modifier = Modifier.height(16.dp))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Trail Image
        //println("Image URL in TrailDetailsScreen: ${trail.imageUrl}")
        Image(
            painter = rememberAsyncImagePainter(model = trail.imageUrl),
            contentDescription = stringResource(R.string.trail_image),
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(8.dp))
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Trail Name
        Text(
            text = trail.name,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Location
        Text(
            text = stringResource(R.string.location) + ": " + trail.location,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Difficulty
        Text(
            text = stringResource(R.string.difficulty) + ": " + trail.difficulty,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Button to "Choose Trail" or similar
        Button(
            onClick = {
                // Add functionality for choosing this trail
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.choose_trail))
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (waypoints.isNotEmpty()) {
            Text(
                text = stringResource(R.string.waypoints_label),
                style = MaterialTheme.typography.titleLarge
            )

            MiniMap(waypoints)
        }
    }
}
