package com.example.reading_app.auth

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class PasswordChangeStatus { NONE, SUCCESS, INCORRECT_PASSWORD }

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

    private val _passwordChangeStatus = MutableStateFlow(PasswordChangeStatus.NONE)
    val passwordChangeStatus = _passwordChangeStatus.asStateFlow()

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        _username.value = sharedPreferences.getString("username", "") ?: ""
        _email.value = sharedPreferences.getString("email", "") ?: ""
    }

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
                loadUserProfile()
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

    fun updateUserProfile(newUsername: String, newEmail: String) {
        viewModelScope.launch {
            with(sharedPreferences.edit()) {
                putString("username", newUsername)
                putString("email", newEmail)
                apply()
            }
            _username.value = newUsername
            _email.value = newEmail
        }
    }

    fun changePassword(oldPassword: String, newPassword: String) {
        viewModelScope.launch {
            val savedPassword = sharedPreferences.getString("password", null)
            if (savedPassword == oldPassword) {
                with(sharedPreferences.edit()) {
                    putString("password", newPassword)
                    apply()
                }
                _passwordChangeStatus.value = PasswordChangeStatus.SUCCESS
            } else {
                _passwordChangeStatus.value = PasswordChangeStatus.INCORRECT_PASSWORD
            }
        }
    }

    fun resetPasswordChangeStatus() {
        _passwordChangeStatus.value = PasswordChangeStatus.NONE
    }
}
