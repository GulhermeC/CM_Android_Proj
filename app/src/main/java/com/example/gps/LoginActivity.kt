package com.example.gps

import LoginScreen
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import android.widget.Toast

import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.ktx.auth

class LoginActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(this)

        auth = Firebase.auth


        setContent {
            LoginScreen(
                onLoginAttempt = { email, password ->
                    signInWithFirebase(email, password)
                },
                context = this
            )
        }
    }

    private fun signInWithFirebase(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            // Just a guard check
            Toast.makeText(this, getString(R.string.email_or_password_blank), Toast.LENGTH_SHORT).show()
            return
        }

        auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Login succeeded -> Go to MainActivity
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    // Login failed -> show error
                    Toast.makeText(this, getString(R.string.wrong_email_or_password), Toast.LENGTH_SHORT).show()
                }
            }
    }
}