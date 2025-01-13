package com.example.gps.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.example.gps.viewmodels.LoginViewModel
import androidx.compose.ui.text.font.FontWeight
import com.example.gps.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(navController: androidx.navigation.NavController, viewModel: LoginViewModel) {
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
                                        val imageUrl = it.getString("imageUrl") ?: ""
                                        val difficulty = it.getString("difficulty")

                                        if (!name.isNullOrEmpty() && !location.isNullOrEmpty()) {
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
            TopAppBar(
                title = { Text(stringResource(R.string.favorites_title), color = Color.White) },
                colors = TopAppBarDefaults.mediumTopAppBarColors(containerColor = Color(0xFF19731B)) // ✅ Dark Green Top Bar
                actions = {
                    LogoutButton(navController = navController, viewModel = viewModel)
                }
                
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFF386C5F)) // ✅ Soft pastel background
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.Gray)
            } else if (errorMessage != null) {
                Text(
                    text = stringResource(R.string.error_generic, errorMessage.orEmpty()),
                    color = Color.Red,
                    fontWeight = FontWeight.Bold
                )
            } else if (favTrails.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_favorites),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Gray
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp) // ✅ More space between cards
                ) {
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
        shape = RoundedCornerShape(16.dp), // ✅ Rounded edges for consistency
        colors = CardDefaults.cardColors(containerColor = Color.White), // ✅ Clean white background
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .shadow(4.dp, RoundedCornerShape(16.dp)), // ✅ Light shadow for depth
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ✅ Trail Image / Placeholder
            if (trail.imageUrl.isBlank()) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color.LightGray, shape = RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = "Default Trail Icon",
                        tint = Color.Gray,
                        modifier = Modifier.size(32.dp)
                    )
                }
            } else {
                Image(
                    painter = rememberAsyncImagePainter(model = trail.imageUrl),
                    contentDescription = stringResource(R.string.trail_image),
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // ✅ Text Section
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // 🔹 Trail Name (Bold)
                Text(
                    text = trail.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 🔹 Location with Pin Icon
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = "Location Icon",
                        tint = Color(0xFF19731B), // ✅ Dark Green
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = trail.location,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF19731B)
                    )
                }
            }

            // ✅ Favorite Heart Icon (Right Aligned)
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = stringResource(R.string.favorite_icon),
                tint = Color.Red,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun LogoutButton(navController: NavController, viewModel: LoginViewModel) {
    Button(
        onClick = {
            viewModel.logout()
            navController.navigate("login") {
                popUpTo("create") { inclusive = true } // 🔹 Remove all previous screens
            }
        }
    ) {
        Text("Logout")
    }
}