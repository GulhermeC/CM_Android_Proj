package com.example.gps

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.rememberAsyncImagePainter
import com.example.gps.data.Trail
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(navController: androidx.navigation.NavController) {
    val user = FirebaseAuth.getInstance().currentUser
    val firestore = FirebaseFirestore.getInstance()

    var favTrails by remember { mutableStateOf<List<Trail>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (user == null) {
            errorMessage = "User not logged in"
            isLoading = false
            return@LaunchedEffect
        }

        try {
            firestore.collection("users")
                .document(user.uid)
                .collection("favorites")
                .get()
                .addOnSuccessListener { favSnapshot ->
                    val favIds = favSnapshot.documents.map { it.id }

                    if (favIds.isEmpty()) {
                        favTrails = emptyList()
                        isLoading = false
                    } else {
                        val tasks = favIds.map { favId ->
                            firestore.collection("trails")
                                .document(favId)
                                .get()
                        }

                        com.google.android.gms.tasks.Tasks.whenAllSuccess<Any>(tasks)
                            .addOnSuccessListener { results ->
                                val trailsFetched = results.mapNotNull { result ->
                                    val doc = result as? com.google.firebase.firestore.DocumentSnapshot
                                    doc?.let {
                                        val name = it.getString("name")
                                        val location = it.getString("location")
                                        val imageUrl = it.getString("imageUrl")
                                        val difficulty = it.getString("difficulty")

                                        if (!name.isNullOrEmpty() && !location.isNullOrEmpty() && !imageUrl.isNullOrEmpty()) {
                                            Trail(
                                                id = doc.id,
                                                name = name,
                                                location = location,
                                                difficulty = difficulty ?: "Unknown",
                                                imageUrl = imageUrl,
                                                isFavorite = true // Because it's in favorites
                                            )
                                        } else null
                                    }
                                }
                                favTrails = trailsFetched
                                isLoading = false
                            }
                            .addOnFailureListener { e ->
                                errorMessage = e.message
                                isLoading = false
                            }
                    }
                }
                .addOnFailureListener { e ->
                    errorMessage = e.message
                    isLoading = false
                }

        } catch (e: Exception) {
            errorMessage = e.message
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.favorites_title)) })
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else if (errorMessage != null) {
                Text(
                    text = stringResource(R.string.error_generic, errorMessage.orEmpty()),
                    color = Color.Red,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            } else if (favTrails.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_favorites),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            } else {
                LazyColumn {
                    items(favTrails) { trail ->
                        FavoriteTrailItem(trail = trail) {
                            navController.currentBackStackEntry?.savedStateHandle?.set("selectedTrail", trail)
                            navController.navigate("details")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FavoriteTrailItem(trail: Trail, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onClick() }, // Handle click
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = trail.location,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = stringResource(R.string.favorite_icon),
                    tint = Color.Red
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row {
                Image(
                    painter = rememberAsyncImagePainter(model = trail.imageUrl),
                    contentDescription = stringResource(R.string.trail_image),
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(8.dp))
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = trail.name,
                    fontSize = 16.sp,
                    color = Color.Black
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

