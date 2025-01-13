import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import androidx.compose.runtime.LaunchedEffect
import com.example.gps.LoginViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavController,viewModel: LoginViewModel) {
    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    val coroutineScope = rememberCoroutineScope()

    val rememberMe = remember { mutableStateOf(false) }
    val username = remember { mutableStateOf("") }
    val password = remember { mutableStateOf("") }
    val loading = remember { mutableStateOf(false) }

    // ✅ Load saved email & "Remember Me" state when screen is displayed
    LaunchedEffect(Unit) {
        viewModel.rememberMeFlow.collect { rememberMe.value = it }
        viewModel.savedEmailFlow.collect { username.value = it }
    }


    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            OutlinedTextField(
                value = username.value,
                onValueChange = { username.value = it },
                label = { Text("Email") },
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = password.value,
                onValueChange = { password.value = it },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .toggleable(
                        value = rememberMe.value,
                        onValueChange = { rememberMe.value = it },
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    )
                    .padding(8.dp)
            ) {
                Checkbox(checked = rememberMe.value, onCheckedChange = { rememberMe.value = it })
                Spacer(modifier = Modifier.width(8.dp))
                Text("Remember Me")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = {
                val usernameInput = username.value.trim()
                val passwordInput = password.value.trim()
                // Here you would add your login logic
                if (usernameInput.isNotBlank() && passwordInput.isNotBlank()) {
                    loading.value = true
                    // Simulate successful login
                    auth.signInWithEmailAndPassword(usernameInput, passwordInput)
                        .addOnCompleteListener { task ->
                            loading.value = false
                            if (task.isSuccessful) {
                                coroutineScope.launch {
                                    viewModel.saveUser(usernameInput, rememberMe.value) // ✅ Save user data
                                }
                                navController.navigate("create"){
                                    popUpTo("login") { inclusive = true }
                                }
                            } else {
                                Toast.makeText(context, "Login failed", Toast.LENGTH_SHORT).show()
                                }
                            }
                } else {
                    Toast.makeText(context, "Please enter username and password", Toast.LENGTH_SHORT).show()
                }
            },
            enabled = !loading.value
            ) {
                Text("Login")
            }
            // Spacer to separate buttons
            Spacer(modifier = Modifier.height(8.dp))

            // Register Button
            Button(
                onClick = { navController.navigate("register") },
                enabled = !loading.value
            ) {
                Text("Register")
            }
        }
    }
}