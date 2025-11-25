package com.example.reading_app.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.reading_app.network.GoogleBooksApi
import com.example.reading_app.network.OnlineBook
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class BookStoreViewModel : ViewModel() {
    var selectedOnlineBook by mutableStateOf<OnlineBook?>(null)
        private set

    private val _trendingBooks = MutableStateFlow<List<OnlineBook>>(emptyList())
    val trendingBooks: StateFlow<List<OnlineBook>> = _trendingBooks

    private val _discoverBooks = MutableStateFlow<List<OnlineBook>>(emptyList())
    val discoverBooks: StateFlow<List<OnlineBook>> = _discoverBooks

    private val _discountedBooks = MutableStateFlow<List<OnlineBook>>(emptyList())
    val discountedBooks: StateFlow<List<OnlineBook>> = _discountedBooks

    private val _recommendedBooks = MutableStateFlow<List<OnlineBook>>(emptyList())
    val recommendedBooks: StateFlow<List<OnlineBook>> = _recommendedBooks

    private val searchHistory = mutableListOf<String>()

    init {
        fetchBooks()
    }

    private fun fetchBooks() {
        viewModelScope.launch {
            try {
                _trendingBooks.value = GoogleBooksApi.searchBooks("sách thịnh hành").take(10)
            } catch (e: Exception) {
                // Handle error
            }
        }
        viewModelScope.launch {
            try {
                _discoverBooks.value = GoogleBooksApi.searchBooks("sách mới phát hành").take(10)
            } catch (e: Exception) {
                // Handle error
            }
        }
        viewModelScope.launch {
            try {
                _discountedBooks.value = GoogleBooksApi.searchBooks("sách giảm giá").take(10)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun searchBooks(query: String) {
        if (query.isNotBlank() && searchHistory.lastOrNull() != query) {
            searchHistory.add(query)
            fetchRecommendedBooks()
        }
    }

    private fun fetchRecommendedBooks() {
        searchHistory.lastOrNull()?.let { lastQuery ->
            viewModelScope.launch {
                try {
                    _recommendedBooks.value = GoogleBooksApi.searchBooks(lastQuery).take(10)
                } catch (e: Exception) {
                    // Handle error
                }
            }
        }
    }

    fun select(book: OnlineBook) {
        selectedOnlineBook = book
    }

    fun clearSelection() {
        selectedOnlineBook = null
    }
}
