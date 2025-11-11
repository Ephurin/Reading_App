package com.example.reading_app.model

data class Book(
    val filePath: String,  // Changed from Uri to String (file path)
    val title: String,
    val type: BookType,
    val coverImagePath: String? = null  // Path to the extracted cover image
)

enum class BookType {
    PDF,
    EPUB
}
