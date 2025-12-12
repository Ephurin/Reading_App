package com.example.reading_app.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.reading_app.network.GoogleBooksApi
import com.example.reading_app.network.OnlineBook
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BookStoreViewModel : ViewModel() {
    var selectedOnlineBook by mutableStateOf<OnlineBook?>(null)
        private set

    private val _trendingBooks = MutableStateFlow<List<OnlineBook>>(emptyList())
    val trendingBooks = _trendingBooks.asStateFlow()

    private val _recommendedBooks = MutableStateFlow<List<OnlineBook>>(emptyList())
    val recommendedBooks = _recommendedBooks.asStateFlow()

    private val searchHistory = mutableListOf<String>()

    init {
        fetchTrendingBooks()
    }

    private fun fetchTrendingBooks() {
        viewModelScope.launch {
            try {
                // For simplicity, trending books are the most popular programming books
                _trendingBooks.value = GoogleBooksApi.searchBooks("programming")
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun addSearchQuery(query: String) {
        if (query.isNotBlank() && !searchHistory.contains(query)) {
            searchHistory.add(query)
            fetchRecommendedBooks()
        }
    }

    private fun fetchRecommendedBooks() {
        if (searchHistory.isEmpty()) return

        viewModelScope.launch {
            try {
                // Recommend books based on the last search query
                val lastQuery = searchHistory.last()
                _recommendedBooks.value = GoogleBooksApi.searchBooks(lastQuery, maxResults = 10)
            } catch (e: Exception) {
                // Handle error
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
