package com.example.gps.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gps.managers.DataStoreManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class LoginViewModel(application: Application) : AndroidViewModel(application) {
    private val dataStoreManager = DataStoreManager(application)

    val rememberMeFlow: Flow<Boolean> = dataStoreManager.rememberMeFlow
    val savedEmailFlow: Flow<String> = dataStoreManager.savedEmailFlow

    fun saveUser(email: String, rememberMe: Boolean) {
        viewModelScope.launch {
            dataStoreManager.saveUser(email, rememberMe)
        }
    }

    fun logout() {
        viewModelScope.launch {
            FirebaseAuth.getInstance().signOut() // 🔹 Sign out from Firebase
            dataStoreManager.clearUser() // 🔹 Clear "Remember Me" data
        }
    }
}