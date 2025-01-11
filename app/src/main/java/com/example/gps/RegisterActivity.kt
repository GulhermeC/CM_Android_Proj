package com.example.gps

import RegisterScreen
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class RegisterActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            RegisterScreen(
                onRegisterAttempt = { email, password ->
                    // Your colleague will handle registration logic here
                },
                context = this
            )
        }
    }
}
