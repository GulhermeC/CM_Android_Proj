package com.example.gps

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import coil3.compose.rememberAsyncImagePainter
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.ui.res.stringResource
import com.example.gps.data.Trail
import android.content.Context
import java.util.Locale

@Composable
fun BrowseScreen(onTrailClick: (Trail) -> Unit) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf(TextFieldValue("")) }
    var trailList by remember { mutableStateOf<List<Trail>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedLanguage by remember { mutableStateOf("en") }

    // Fetch trails
    LaunchedEffect(Unit) {
        isLoading = true
        errorMessage = null
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("trails")
            .get()
            .addOnSuccessListener { result ->
                val trails = result.mapNotNull { document ->
                    val name = document.getString("name")
                    val location = document.getString("location")
                    val imageUrl = document.getString("imageUrl")
                    val difficulty = document.getString("difficulty")
                    if (name != null && location != null && imageUrl != null) {
                        Trail(name, location, difficulty ?: "Unknown", imageUrl)
                    } else {
                        null
                    }
                }
                trailList = trails
                isLoading = false
            }
            .addOnFailureListener { e ->
                errorMessage = e.message
                isLoading = false
            }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            DropdownLanguageSelector(selectedLanguage = selectedLanguage) { newLanguage ->
                selectedLanguage = newLanguage
                updateLocale(context, newLanguage)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.search)) },
            placeholder = { Text(stringResource(R.string.search)) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Loading or Error State
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else if (errorMessage != null) {
            Text(
                text = "${stringResource(R.string.error)} $errorMessage",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        } else {
            // Filtered List of Trails
            val filteredTrails = trailList.filter {
                it.name.contains(searchQuery.text, ignoreCase = true) ||
                        it.location.contains(searchQuery.text, ignoreCase = true)
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(filteredTrails) { trail ->
                    TrailItem(
                        location = trail.location,
                        trailName = trail.name,
                        imageUrl = trail.imageUrl,
                        onClick = { onTrailClick(trail) }
                    )
                }
            }
        }
    }
}

@Composable
fun DropdownLanguageSelector(selectedLanguage: String, onLanguageChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val languages = mapOf("en" to "English", "pt" to "Português", "es" to "Español")

    Box {
        Button(onClick = { expanded = true }) {
            Text(text = languages[selectedLanguage] ?: "Select Language")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            languages.forEach { (code, language) ->
                DropdownMenuItem(
                    text = { Text(language) },
                    onClick = {
                        onLanguageChange(code)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun TrailItem(location: String, trailName: String, imageUrl: String, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = location,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(Icons.Filled.FavoriteBorder, contentDescription = stringResource(R.string.favorite))
            }

            Spacer(modifier = Modifier.height(8.dp))
            println("Image URL in BrowseScreen: $imageUrl")
            Row {
                Image(
                    painter = rememberAsyncImagePainter(model = imageUrl),
                    contentDescription = "Trail Image",
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(8.dp))
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = trailName,
                    fontSize = 16.sp,
                    color = Color.Black
                )
            }
        }
    }
}

fun updateLocale(context: Context, languageCode: String) {
    val locale = Locale(languageCode)
    Locale.setDefault(locale)
    val config = context.resources.configuration
    config.setLocale(locale)
    context.createConfigurationContext(config)
    context.resources.updateConfiguration(config, context.resources.displayMetrics)
}

// Data class for Trails
data class Trail(
    val id: String = "",
    val name: String,
    val location: String,
    val difficulty: String,
    val imageUrl: String
)