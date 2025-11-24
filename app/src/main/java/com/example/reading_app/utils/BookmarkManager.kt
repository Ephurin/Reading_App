package com.example.reading_app.utils

import android.content.Context
import com.example.reading_app.model.Bookmark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object BookmarkManager {
    private const val BOOKMARKS_FILE = "bookmarks.json"
    
    /**
     * Save a bookmark for a book
     */
    suspend fun saveBookmark(context: Context, bookmark: Bookmark) = withContext(Dispatchers.IO) {
        try {
            val bookmarks = loadBookmarks(context).toMutableList()
            
            // Remove existing bookmark for the same book and page/chapter
            bookmarks.removeAll { 
                it.bookFilePath == bookmark.bookFilePath && it.pageOrChapter == bookmark.pageOrChapter 
            }
            
            // Add new bookmark
            bookmarks.add(bookmark)
            
            // Save to file
            val bookmarksFile = File(context.filesDir, BOOKMARKS_FILE)
            val jsonArray = JSONArray()
            
            bookmarks.forEach { bm ->
                val jsonObject = JSONObject().apply {
                    put("bookFilePath", bm.bookFilePath)
                    put("pageOrChapter", bm.pageOrChapter)
                    put("title", bm.title)
                    put("timestamp", bm.timestamp)
                }
                jsonArray.put(jsonObject)
            }
            
            bookmarksFile.writeText(jsonArray.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Load all bookmarks from storage
     */
    suspend fun loadBookmarks(context: Context): List<Bookmark> = withContext(Dispatchers.IO) {
        try {
            val bookmarksFile = File(context.filesDir, BOOKMARKS_FILE)
            
            if (!bookmarksFile.exists()) {
                return@withContext emptyList()
            }
            
            val jsonArray = JSONArray(bookmarksFile.readText())
            val bookmarks = mutableListOf<Bookmark>()
            
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val bookmark = Bookmark(
                    bookFilePath = jsonObject.getString("bookFilePath"),
                    pageOrChapter = jsonObject.getInt("pageOrChapter"),
                    title = jsonObject.getString("title"),
                    timestamp = jsonObject.getLong("timestamp")
                )
                bookmarks.add(bookmark)
            }
            
            return@withContext bookmarks
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext emptyList()
        }
    }
    
    /**
     * Get bookmarks for a specific book
     */
    suspend fun getBookmarksForBook(context: Context, bookFilePath: String): List<Bookmark> {
        return loadBookmarks(context).filter { it.bookFilePath == bookFilePath }
            .sortedBy { it.pageOrChapter }
    }
    
    /**
     * Delete a specific bookmark
     */
    suspend fun deleteBookmark(context: Context, bookmark: Bookmark) = withContext(Dispatchers.IO) {
        try {
            val bookmarks = loadBookmarks(context).toMutableList()
            bookmarks.removeAll { 
                it.bookFilePath == bookmark.bookFilePath && it.pageOrChapter == bookmark.pageOrChapter 
            }
            
            val bookmarksFile = File(context.filesDir, BOOKMARKS_FILE)
            val jsonArray = JSONArray()
            
            bookmarks.forEach { bm ->
                val jsonObject = JSONObject().apply {
                    put("bookFilePath", bm.bookFilePath)
                    put("pageOrChapter", bm.pageOrChapter)
                    put("title", bm.title)
                    put("timestamp", bm.timestamp)
                }
                jsonArray.put(jsonObject)
            }
            
            bookmarksFile.writeText(jsonArray.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Check if a page/chapter is bookmarked
     */
    suspend fun isBookmarked(context: Context, bookFilePath: String, pageOrChapter: Int): Boolean {
        return loadBookmarks(context).any { 
            it.bookFilePath == bookFilePath && it.pageOrChapter == pageOrChapter 
        }
    }
}
