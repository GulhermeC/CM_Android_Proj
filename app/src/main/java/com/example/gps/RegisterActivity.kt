package com.example.gps

import RegisterScreen
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.ktx.auth
import androidx.compose.ui.res.stringResource

class RegisterActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(this)
        auth = Firebase.auth

        setContent {
            RegisterScreen(
                onRegisterAttempt = { email, password ->
                    registerUser(email, password)
                },
                context = this
            )
        }
    }

    private fun registerUser(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            Toast.makeText(this, getString(R.string.email_or_password_blank), Toast.LENGTH_SHORT).show()
            return
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Registration succeeded
                    Toast.makeText(this, getString(R.string.registration_successful), Toast.LENGTH_SHORT).show()
                    // Navigate to LoginActivity
                    val intent = Intent(this, LoginActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    // Registration failed
                    val errorMessage = task.exception?.message ?: getString(R.string.registration_failed)
                    Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                }
            }
    }
}
