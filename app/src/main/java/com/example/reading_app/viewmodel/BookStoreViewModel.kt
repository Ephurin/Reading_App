package com.example.reading_app.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.reading_app.network.OnlineBook

class BookStoreViewModel : ViewModel() {
    var selectedOnlineBook by mutableStateOf<OnlineBook?>(null)
        private set

    fun select(book: OnlineBook) {
        selectedOnlineBook = book
    }

    fun clearSelection() {
        selectedOnlineBook = null
    }
}
