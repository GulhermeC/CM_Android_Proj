package com.example.gps

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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

    fun clearUser() {
        viewModelScope.launch {
            dataStoreManager.clearUser()
        }
    }
}