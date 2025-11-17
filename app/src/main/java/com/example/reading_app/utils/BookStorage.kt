package com.example.reading_app.utils

import android.content.Context
import com.example.reading_app.model.Book
import com.example.reading_app.model.BookType
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object BookStorage {
    private const val BOOKS_FILE = "books.json"

    fun saveBooks(context: Context, books: List<Book>) {
        try {
            val booksFile = File(context.filesDir, BOOKS_FILE)
            val jsonArray = JSONArray()
            books.forEach { book ->
                val jsonObject = JSONObject().apply {
                    put("filePath", book.filePath)
                    put("title", book.title)
                    put("type", book.type.name)
                    put("coverImagePath", book.coverImagePath)
                }
                jsonArray.put(jsonObject)
            }
            booksFile.writeText(jsonArray.toString(4)) // Use indentation for readability
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadBooks(context: Context): List<Book> {
        try {
            val booksFile = File(context.filesDir, BOOKS_FILE)
            if (!booksFile.exists()) {
                return emptyList()
            }

            val jsonString = booksFile.readText()
            val jsonArray = JSONArray(jsonString)
            val books = mutableListOf<Book>()

            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val book = Book(
                    filePath = jsonObject.getString("filePath"),
                    title = jsonObject.getString("title"),
                    type = BookType.valueOf(jsonObject.getString("type")),
                    coverImagePath = jsonObject.optString("coverImagePath", null)
                )
                books.add(book)
            }
            return books
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }
    }
}
