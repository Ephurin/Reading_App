package com.example.reading_app.model

data class Bookmark(
    val bookFilePath: String,
    val pageOrChapter: Int,
    val title: String,
    val timestamp: Long = System.currentTimeMillis()
)
