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
import androidx.compose.foundation.background
import androidx.compose.material.icons.filled.Place
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)) {
        // Language Selector Aligned to the Right
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            DropdownLanguageSelector(selectedLanguage = selectedLanguage) { newLanguage ->
                selectedLanguage = newLanguage
                updateLocale(context, newLanguage)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        //  Search Bar with Rounded Corners
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            leadingIcon = {
                Icon(
                    Icons.Filled.Search,
                    contentDescription = stringResource(R.string.search),
                    tint = Color.Black //  Simple black icon for better contrast
                )
            },
            placeholder = {
                Text(
                    text = stringResource(R.string.search),
                    color = Color.Black // Simple black text for readability
                )
            },
            shape = RoundedCornerShape(12.dp), // Slightly rounded but clean
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp) // Ensures proper height
        )

        Spacer(modifier = Modifier.height(16.dp))

        //  Loading or Error State with Styling
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                color = Color.Gray
            )
        } else if (errorMessage != null) {
            Text(
                text = "${stringResource(R.string.error)} $errorMessage",
                color = Color.Red,
                modifier = Modifier.align(Alignment.CenterHorizontally),
                fontWeight = FontWeight.Bold
            )
        } else {
            //  Styled List of Trails
            val filteredTrails = trailList.filter {
                it.name.contains(searchQuery.text, ignoreCase = true) ||
                        it.location.contains(searchQuery.text, ignoreCase = true)
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp) // More space between cards
            ) {
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
        Button(
            onClick = { expanded = true },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF19731B))
        ) {
            Text(
                stringResource(R.string.select_language),
                color = Color.White //  Ensure contrast
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            languageCodes.forEach { code ->
                DropdownMenuItem(
                    text = {
                        Text(
                            getLanguageLabel(code),
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF19731B)
                        )
                    },
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
        shape = RoundedCornerShape(16.dp), //  More rounded edges
        colors = CardDefaults.cardColors(containerColor = Color.White), //  White background
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .clickable { onClick() }
            .shadow(4.dp, RoundedCornerShape(16.dp)), //  Light shadow
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (imageUrl.isBlank()) {
                //  Placeholder Image with Soft Rounded Corners
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.LightGray, shape = RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = "Default Trail Icon",
                        tint = Color(0xFF19731B),
                        modifier = Modifier.size(24.dp)
                    )
                }
            } else {
                Image(
                    painter = rememberAsyncImagePainter(model = imageUrl),
                    contentDescription = "Trail Image",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                //  Location with Map Pin Icon
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = "Location Icon",
                        tint = Color(0xFF19731B),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = location,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF19731B)
                    )
                }

                //  Trail Name with Bold Font
                Text(
                    text = trailName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            //  Favorite Icon Styled Like the Reference Image
            Icon(
                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = stringResource(R.string.favorite),
                tint = if (isFavorite) Color(0xFFFF0000) else Color.Gray, //  Red when favorited
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onFavoriteClick() }
            )
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


