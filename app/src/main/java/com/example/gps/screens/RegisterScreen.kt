import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.example.gps.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(navController: NavController) {
    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }

    val username = remember { mutableStateOf("") }
    val password = remember { mutableStateOf("") }
    val confirmPassword = remember { mutableStateOf("") }
    val loading = remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFE6F2EF) //  Soft pastel background
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp) // More padding for better spacing
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Email Field
            OutlinedTextField(
                value = username.value,
                onValueChange = { username.value = it },
                label = { Text(stringResource(R.string.email)) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp), // Rounded corners for modern feel
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Password Field
            OutlinedTextField(
                value = password.value,
                onValueChange = { password.value = it },
                label = { Text(stringResource(R.string.password)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                shape = RoundedCornerShape(12.dp), // Rounded corners
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Confirm Password Field
            OutlinedTextField(
                value = confirmPassword.value,
                onValueChange = { confirmPassword.value = it },
                label = { Text(stringResource(R.string.confirm_password)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                shape = RoundedCornerShape(12.dp), // Rounded corners
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Register Button
            Button(
                onClick = {
                    val usernameInput = username.value.trim()
                    val passwordInput = password.value.trim()
                    val confirmPasswordInput = confirmPassword.value.trim()

                    if (usernameInput.isBlank() || passwordInput.isBlank() || confirmPasswordInput.isBlank()) {
                        Toast.makeText(context, "All fields are required", Toast.LENGTH_SHORT)
                            .show()
                        return@Button
                    }

                    if (passwordInput != confirmPasswordInput) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.passwords_do_not_match),
                            Toast.LENGTH_SHORT
                        ).show()
                        return@Button
                    }

                    loading.value = true
                    auth.createUserWithEmailAndPassword(usernameInput, passwordInput)
                        .addOnCompleteListener { task ->
                            loading.value = false
                            if (task.isSuccessful) {
                                navController.navigate("login") {
                                    popUpTo("register") { inclusive = true }
                                }
                            } else {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.fill_all_fields),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                },
                enabled = !loading.value,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF19731B)), // Dark Green Button
                shape = RoundedCornerShape(12.dp), // Rounded corners
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    stringResource(R.string.register),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Login Button
            Button(
                onClick = { navController.navigate("login") },
                enabled = !loading.value,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp), // Rounded corners
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 2.dp) // Soft shadow
            ) {
                Text(
                    stringResource(R.string.login),
                    color = Color(0xFF19731B),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}