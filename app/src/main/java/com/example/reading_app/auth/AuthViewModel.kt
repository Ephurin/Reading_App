package com.example.reading_app.auth

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPreferences = application.getSharedPreferences("auth", Context.MODE_PRIVATE)

    private val _username = MutableStateFlow("")
    val username = _username.asStateFlow()

    private val _email = MutableStateFlow("")
    val email = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password = _password.asStateFlow()

    private val _loginStatus = MutableStateFlow<Boolean?>(null)
    val loginStatus = _loginStatus.asStateFlow()

    fun onUsernameChange(username: String) {
        _loginStatus.value = null
        _username.value = username
    }

    fun onEmailChange(email: String) {
        _loginStatus.value = null
        _email.value = email
    }

    fun onPasswordChange(password: String) {
        _loginStatus.value = null
        _password.value = password
    }

    fun login() {
        viewModelScope.launch {
            val savedEmail = sharedPreferences.getString("email", null)
            val savedPassword = sharedPreferences.getString("password", null)
            if (savedEmail == _email.value && savedPassword == _password.value) {
                Log.d("AuthViewModel", "Login successful")
                _loginStatus.value = true
            } else {
                Log.d("AuthViewModel", "Login failed")
                _loginStatus.value = false
            }
        }
    }

    fun signUp() {
        viewModelScope.launch {
            with(sharedPreferences.edit()) {
                putString("username", _username.value)
                putString("email", _email.value)
                putString("password", _password.value)
                apply()
            }
            Log.d("AuthViewModel", "Sign up successful")
        }
    }
}
