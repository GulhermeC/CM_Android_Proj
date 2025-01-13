import android.widget.Toast
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import com.example.gps.viewmodels.LoginViewModel
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.example.gps.R


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

    // Load saved email & "Remember Me" state when screen is displayed
    LaunchedEffect(Unit) {
        viewModel.rememberMeFlow.collect { rememberMe.value = it }
        viewModel.savedEmailFlow.collect { username.value = it }
    }


    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFE6F2EF) //  Soft pastel background
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp) //  More padding for better spacing
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 🔹 Email Field
            OutlinedTextField(
                value = username.value,
                onValueChange = { username.value = it },
                label = { Text(stringResource(R.string.email)) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp), //  Rounded corners for modern feel
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 🔹 Password Field
            OutlinedTextField(
                value = password.value,
                onValueChange = { password.value = it },
                label = { Text(stringResource(R.string.password)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                shape = RoundedCornerShape(12.dp), //  Rounded corners
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 🔹 Remember Me Row
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
                Checkbox(
                    checked = rememberMe.value,
                    onCheckedChange = { rememberMe.value = it },
                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF19731B)) //  Dark Green Checkbox
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Remember Me", color = Color.Black)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 🔹 Login Button
            Button(
                onClick = {
                    val usernameInput = username.value.trim()
                    val passwordInput = password.value.trim()
                    if (usernameInput.isNotBlank() && passwordInput.isNotBlank()) {
                        loading.value = true
                        auth.signInWithEmailAndPassword(usernameInput, passwordInput)
                            .addOnCompleteListener { task ->
                                loading.value = false
                                if (task.isSuccessful) {
                                    coroutineScope.launch {
                                        viewModel.saveUser(usernameInput, rememberMe.value)
                                    }
                                    navController.navigate("create") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                } else {
                                    Toast.makeText(context, "Login failed", Toast.LENGTH_SHORT)
                                        .show()
                                }
                            }
                    } else {
                        Toast.makeText(
                            context,
                            context.getString(R.string.enter_username_password),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                enabled = !loading.value,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF19731B)), //  Dark Green Button
                shape = RoundedCornerShape(12.dp), //  Rounded corners
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    stringResource(R.string.login),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Register Button
            Button(
                onClick = { navController.navigate("register") },
                enabled = !loading.value,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp), //  Rounded corners
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 2.dp) //  Soft shadow
            ) {
                Text(
                    stringResource(R.string.register),
                    color = Color(0xFF19731B),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}