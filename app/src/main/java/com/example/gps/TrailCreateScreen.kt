package com.example.gps

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TrailCreationScreen(
//    onSaveTrail: (String, String, String) -> Unit // Callback function for Firebase integration
) {
    var trailName by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var difficulty by remember { mutableStateOf("Easy") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Back Button Placeholder (If needed, wrap with Row and Align to Start)
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

//        // Save Button
//        Button(
//            onClick = { onSaveTrail(trailName, location, difficulty) },
//            modifier = Modifier.fillMaxWidth(),
//            enabled = trailName.isNotBlank() && location.isNotBlank()
//        ) {
//            Text(text = "Save Trail")
//        }
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
