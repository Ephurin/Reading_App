package com.example.reading_app.utils

import android.content.Context
import com.example.reading_app.model.Highlight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object HighlightManager {
    private const val HIGHLIGHTS_FILE = "highlights.json"
    
    /**
     * Save a highlight for a book
     */
    suspend fun saveHighlight(context: Context, highlight: Highlight) = withContext(Dispatchers.IO) {
        try {
            val highlights = loadHighlights(context).toMutableList()
            
            // Remove existing highlight with same ID (for updates)
            highlights.removeAll { it.id == highlight.id }
            
            // Add new/updated highlight
            highlights.add(highlight)
            
            // Save to file
            val highlightsFile = File(context.filesDir, HIGHLIGHTS_FILE)
            val jsonArray = JSONArray()
            
            highlights.forEach { hl ->
                val jsonObject = JSONObject().apply {
                    put("id", hl.id)
                    put("bookFilePath", hl.bookFilePath)
                    put("chapterIndex", hl.chapterIndex)
                    put("selectedText", hl.selectedText)
                    put("rangeStart", hl.rangeStart)
                    put("rangeEnd", hl.rangeEnd)
                    put("startOffset", hl.startOffset)
                    put("endOffset", hl.endOffset)
                    put("color", hl.color)
                    put("note", hl.note ?: "")
                    put("timestamp", hl.timestamp)
                }
                jsonArray.put(jsonObject)
            }
            
            highlightsFile.writeText(jsonArray.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Load all highlights from storage
     */
    suspend fun loadHighlights(context: Context): List<Highlight> = withContext(Dispatchers.IO) {
        try {
            val highlightsFile = File(context.filesDir, HIGHLIGHTS_FILE)
            
            if (!highlightsFile.exists()) {
                return@withContext emptyList()
            }
            
            val jsonArray = JSONArray(highlightsFile.readText())
            val highlights = mutableListOf<Highlight>()
            
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val highlight = Highlight(
                    id = jsonObject.getString("id"),
                    bookFilePath = jsonObject.getString("bookFilePath"),
                    chapterIndex = jsonObject.getInt("chapterIndex"),
                    selectedText = jsonObject.getString("selectedText"),
                    rangeStart = jsonObject.getString("rangeStart"),
                    rangeEnd = jsonObject.getString("rangeEnd"),
                    startOffset = jsonObject.getInt("startOffset"),
                    endOffset = jsonObject.getInt("endOffset"),
                    color = jsonObject.getString("color"),
                    note = jsonObject.optString("note").takeIf { it.isNotEmpty() },
                    timestamp = jsonObject.getLong("timestamp")
                )
                highlights.add(highlight)
            }
            
            return@withContext highlights
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext emptyList()
        }
    }
    
    /**
     * Get highlights for a specific book and chapter
     */
    suspend fun getHighlightsForChapter(
        context: Context, 
        bookFilePath: String, 
        chapterIndex: Int
    ): List<Highlight> {
        return loadHighlights(context).filter { 
            it.bookFilePath == bookFilePath && it.chapterIndex == chapterIndex 
        }.sortedBy { it.timestamp }
    }
    
    /**
     * Get all highlights for a specific book
     */
    suspend fun getHighlightsForBook(context: Context, bookFilePath: String): List<Highlight> {
        return loadHighlights(context).filter { it.bookFilePath == bookFilePath }
            .sortedWith(compareBy({ it.chapterIndex }, { it.timestamp }))
    }
    
    /**
     * Delete a specific highlight
     */
    suspend fun deleteHighlight(context: Context, highlightId: String) = withContext(Dispatchers.IO) {
        try {
            val highlights = loadHighlights(context).toMutableList()
            highlights.removeAll { it.id == highlightId }
            
            val highlightsFile = File(context.filesDir, HIGHLIGHTS_FILE)
            val jsonArray = JSONArray()
            
            highlights.forEach { hl ->
                val jsonObject = JSONObject().apply {
                    put("id", hl.id)
                    put("bookFilePath", hl.bookFilePath)
                    put("chapterIndex", hl.chapterIndex)
                    put("selectedText", hl.selectedText)
                    put("rangeStart", hl.rangeStart)
                    put("rangeEnd", hl.rangeEnd)
                    put("startOffset", hl.startOffset)
                    put("endOffset", hl.endOffset)
                    put("color", hl.color)
                    put("note", hl.note ?: "")
                    put("timestamp", hl.timestamp)
                }
                jsonArray.put(jsonObject)
            }
            
            highlightsFile.writeText(jsonArray.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Update highlight note
     */
    suspend fun updateHighlightNote(context: Context, highlightId: String, newNote: String) {
        val highlights = loadHighlights(context)
        val highlight = highlights.find { it.id == highlightId }
        if (highlight != null) {
            val updated = highlight.copy(note = newNote.takeIf { it.isNotEmpty() })
            saveHighlight(context, updated)
        }
    }
    
    /**
     * Check if a text range is highlighted
     */
    suspend fun isHighlighted(
        context: Context, 
        bookFilePath: String, 
        chapterIndex: Int,
        text: String
    ): Boolean {
        return getHighlightsForChapter(context, bookFilePath, chapterIndex)
            .any { it.selectedText == text }
    }
}
