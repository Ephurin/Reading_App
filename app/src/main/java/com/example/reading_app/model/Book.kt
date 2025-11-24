package com.example.reading_app.model

data class Book(
    val filePath: String,  // Changed from Uri to String (file path)
    val title: String,
    val author: String = "Không xác định",  // Author name
    val type: BookType,
    val coverImagePath: String? = null,  // Path to the extracted cover image
    val currentProgress: Float = 0f,  // Reading progress (0f to 1f)
    val totalPages: Int = 0,  // Total pages for PDF or chapters for EPUB
    val currentPage: Int = 0  // Current page/chapter
)

enum class BookType {
    PDF,
    EPUB
}
