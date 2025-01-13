package com.example.gps.screens

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.rememberAsyncImagePainter
import com.google.firebase.firestore.FirebaseFirestore
import com.example.gps.data.Trail
import androidx.navigation.NavController
import java.util.Locale
import android.app.Activity
import androidx.compose.material.icons.filled.Place
import androidx.compose.ui.res.painterResource
import com.google.firebase.auth.FirebaseAuth
import com.example.gps.R
import com.example.gps.viewmodels.LoginViewModel

@Composable
fun BrowseScreen(onTrailClick: (Trail) -> Unit) {
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val user = auth.currentUser

    var searchQuery by remember { mutableStateOf(TextFieldValue("")) }
    var trailList by remember { mutableStateOf<List<Trail>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedLanguage by remember { mutableStateOf("en") }

    // Fetch trails
    LaunchedEffect(Unit) {
        isLoading = true
        errorMessage = null

        firestore.collection("trails")
            .get()
            .addOnSuccessListener { trailSnapshot ->
                val allTrails = trailSnapshot.mapNotNull { doc ->
                    val name = doc.getString("name")
                    val location = doc.getString("location")
                    val imageUrl = doc.getString("imageUrl") ?: ""
                    val difficulty = doc.getString("difficulty")

                    if (name != null && location != null) {
                            Trail(
                                id = doc.id,
                                name = name,
                                location = location,
                                difficulty = difficulty ?: "Unknown",
                                imageUrl = imageUrl,
                                isFavorite = false
                            )
                    } else null
                }

                println("Total Trails Retrieved: ${allTrails.size}") //Log the trail count

                if (user != null) {
                    // fetch user favorites
                    firestore.collection("users")
                        .document(user.uid)
                        .collection("favorites")
                        .get()
                        .addOnSuccessListener { favSnapshot ->
                            val favoriteIds = favSnapshot.documents.map { it.id }.toSet()

                            // Merge
                            val mergedTrails = allTrails.map { t ->
                                if (favoriteIds.contains(t.id)) t.copy(isFavorite = true)
                                else t
                            }

                            trailList = mergedTrails
                            isLoading = false
                        }
                        .addOnFailureListener { e ->
                            errorMessage = e.message
                            isLoading = false
                        }
                } else {
                    trailList = allTrails
                    isLoading = false
                }
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

            LazyColumn {
                items(filteredTrails) { trail ->
                    TrailItem(
                        location = trail.location,
                        trailName = trail.name,
                        imageUrl = trail.imageUrl,
                        isFavorite = trail.isFavorite,
                        onClick = { onTrailClick(trail) },
                        onFavoriteClick = {
                            trailList = trailList.map {
                                if (it.id == trail.id) it.copy(isFavorite = !it.isFavorite) else it
                            }
                            val newValue = !trail.isFavorite
                            if (user != null) {
                                val userDoc = firestore.collection("users").document(user.uid)
                                val favDocRef = userDoc.collection("favorites").document(trail.id)

                                if (newValue) {
                                    favDocRef.set(mapOf("favorite" to true))
                                } else {
                                    favDocRef.delete()
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DropdownLanguageSelector(
    selectedLanguage: String,
    onLanguageChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val languageCodes = listOf("en", "pt", "es")

    Box {
        Button(onClick = { expanded = true }) {
            Text(stringResource(R.string.select_language))
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            languageCodes.forEach { code ->
                DropdownMenuItem(
                    text = { Text(getLanguageLabel(code)) },
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
fun TrailItem(
    location: String,
    trailName: String,
    imageUrl: String,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {

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
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = stringResource(R.string.favorite),
                    modifier = Modifier.clickable { onFavoriteClick() }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            println("Image URL in BrowseScreen: $imageUrl")

            Row {
                if (imageUrl.isBlank()) {
                    // Show an icon instead of an image when no image is available
                    Icon(
                        imageVector = Icons.Default.Place, // Replace with an appropriate Material icon
                        contentDescription = "Default Trail Icon",
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                } else {
                    Image(
                        painter = rememberAsyncImagePainter(model = imageUrl),
                        contentDescription = "Trail Image",
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                }
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

@Composable
fun getLanguageLabel(langCode: String): String {
    return when (langCode) {
        "en" -> stringResource(R.string.lang_en)
        "pt" -> stringResource(R.string.lang_pt)
        "es" -> stringResource(R.string.lang_es)
        else -> stringResource(R.string.lang_en) // fallback
    }
}

fun updateLocale(context: Context, languageCode: String) {
    val locale = Locale(languageCode)
    Locale.setDefault(locale)
    val config = context.resources.configuration
    config.setLocale(locale)
    context.createConfigurationContext(config)
    context.resources.updateConfiguration(config, context.resources.displayMetrics)
    (context as? Activity)?.recreate()
}


