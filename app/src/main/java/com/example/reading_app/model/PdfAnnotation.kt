package com.example.reading_app.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

data class PdfAnnotation(
    val id: String = java.util.UUID.randomUUID().toString(),
    val bookFilePath: String,
    val pageNumber: Int,
    val type: AnnotationType,
    val note: String? = null,
    val color: String = "#FFFF00",
    val timestamp: Long = System.currentTimeMillis(),
    // For drawing annotations
    val drawingPaths: List<DrawingPath> = emptyList(),
    // For text annotations
    val position: AnnotationPosition? = null,
    val width: Float? = null,
    val height: Float? = null
)

data class DrawingPath(
    val points: List<AnnotationPoint>,
    val color: String,
    val strokeWidth: Float
)

data class AnnotationPoint(
    val x: Float,
    val y: Float
)

data class AnnotationPosition(
    val x: Float,
    val y: Float
)

enum class AnnotationType {
    NOTE,           // Text note at a position
    DRAWING,        // Free-hand drawing
    HIGHLIGHT,      // Highlight rectangle
    TEXT            // Custom text annotation
}

enum class AnnotationColor(val colorHex: String, val displayName: String) {
    YELLOW("#FFFF00", "Yellow"),
    RED("#FF6B6B", "Red"),
    GREEN("#90EE90", "Green"),
    BLUE("#ADD8E6", "Blue"),
    ORANGE("#FFA500", "Orange"),
    BLACK("#000000", "Black")
}
