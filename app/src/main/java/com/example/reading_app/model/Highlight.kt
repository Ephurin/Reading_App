package com.example.reading_app.model

data class Highlight(
    val id: String = java.util.UUID.randomUUID().toString(),
    val bookFilePath: String,
    val chapterIndex: Int,
    val selectedText: String,
    val rangeStart: String,  // XPath or CSS selector for start node
    val rangeEnd: String,    // XPath or CSS selector for end node
    val startOffset: Int,    // Character offset in start node
    val endOffset: Int,      // Character offset in end node
    val color: String = "#FFFF00",  // Default yellow color
    val note: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

enum class HighlightColor(val colorHex: String, val displayName: String) {
    YELLOW("#FFFF00", "Yellow"),
    GREEN("#90EE90", "Green"),
    BLUE("#ADD8E6", "Blue"),
    PINK("#FFB6C1", "Pink"),
    ORANGE("#FFA500", "Orange")
}
