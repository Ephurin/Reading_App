package com.example.reading_app.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ThemeViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPreferences = application.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)

    private val _isDarkTheme = MutableStateFlow(false)
    val isDarkTheme = _isDarkTheme.asStateFlow()

    init {
        _isDarkTheme.value = sharedPreferences.getBoolean("is_dark_theme", false)
    }

    fun setTheme(isDark: Boolean) {
        viewModelScope.launch {
            _isDarkTheme.value = isDark
            with(sharedPreferences.edit()) {
                putBoolean("is_dark_theme", isDark)
                apply()
            }
        }
    }
}
