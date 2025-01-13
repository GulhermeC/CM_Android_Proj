import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.gps.LoginActivity
import com.example.gps.RegisterActivity
import androidx.compose.ui.res.stringResource
import com.example.gps.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(onRegisterAttempt: (String, String) -> Unit, context: Context) {
    val username = remember { mutableStateOf("") }
    val password = remember { mutableStateOf("") }
    val confirmPassword = remember { mutableStateOf("") }

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
                label = { Text(stringResource(R.string.email)) },
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = password.value,
                onValueChange = { password.value = it },
                label = { Text(stringResource(R.string.password)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = confirmPassword.value,
                onValueChange = { confirmPassword.value = it },
                label = { Text(stringResource(R.string.confirm_password)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                if (username.value.isNotBlank() && password.value.isNotBlank() && confirmPassword.value.isNotBlank()) {
                    if (password.value == confirmPassword.value) {
                        // Placeholder for functionality
                        onRegisterAttempt(username.value.trim(), password.value.trim())
                    } else {
                        Toast.makeText(context, context.getString(R.string.passwords_do_not_match), Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, context.getString(R.string.fill_all_fields), Toast.LENGTH_SHORT).show()
                }
            }) {
                Text(stringResource(R.string.register))
            }
            // Spacer to separate buttons
            Spacer(modifier = Modifier.height(8.dp))

            // Register Button
            Button(onClick = {
                val intent = Intent(context, LoginActivity::class.java)
                context.startActivity(intent)
            }) {
                Text(stringResource(R.string.login))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RegisterScreenPreview() {
    val context = LocalContext.current
    RegisterScreen(
        onRegisterAttempt = { _, _ -> },
        context = context
    )
}
