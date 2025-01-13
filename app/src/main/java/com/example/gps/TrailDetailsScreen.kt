package com.example.gps

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.rememberAsyncImagePainter

@Composable
fun TrailDetailsScreen(trail: Trail) {
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
            contentDescription = "Trail Image",
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
            text = "Location: ${trail.location}",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Difficulty
        Text(
            text = "Difficulty: ${trail.difficulty}",
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
            Text("Choose Trail")
        }
    }
}
