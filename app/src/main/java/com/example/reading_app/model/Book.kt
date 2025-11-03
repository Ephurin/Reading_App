package com.example.reading_app.model

data class Book(
    val filePath: String,  // Changed from Uri to String (file path)
    val title: String,
    val type: BookType
)

enum class BookType {
    PDF,
    EPUB
}
