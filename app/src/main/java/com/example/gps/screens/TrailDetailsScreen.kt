package com.example.gps.screens

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
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
import androidx.compose.ui.text.font.FontWeight
import com.example.gps.R
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

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFE6F2EF) //  Soft pastel background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            //  Back Button
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = Color(0xFF19731B) //  Dark Green
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Trail Image with Improved Styling
            Card(
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Image(
                    painter = rememberAsyncImagePainter(model = trail.imageUrl),
                    contentDescription = stringResource(R.string.trail_image),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(16.dp))
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            //  Trail Name
            Text(
                text = trail.name,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF19731B), //  Dark Green
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            //  Location
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Location Icon",
                    tint = Color(0xFF19731B),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = trail.location,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            //  Difficulty
            Text(
                text = "${stringResource(R.string.difficulty)}: ${trail.difficulty}",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(16.dp))

            //  Button to "Choose Trail"
            Button(
                onClick = {
                    val waypointString =
                        waypoints.joinToString(";") { "${it.first},${it.second}" } // Convert waypoints to a string
                    navController.navigate("userTrail/${trail.id}?waypoints=$waypointString")
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF19731B))
            ) {
                Text(
                    stringResource(R.string.choose_trail),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            //  MiniMap with Waypoints
            if (waypoints.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.waypoints_label),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.Black
                )

                MiniMap(waypoints)
            }
        }
    }
}
