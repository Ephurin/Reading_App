package com.example.reading_app.viewmodel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.reading_app.model.Book
import com.example.reading_app.model.BookType
import java.io.File
import java.io.FileOutputStream

class ReaderViewModel : ViewModel() {
    var selectedBook by mutableStateOf<Book?>(null)
        private set

    var recentBooks by mutableStateOf<List<Book>>(emptyList())
        private set

    fun selectBook(context: Context, uri: android.net.Uri, fileName: String) {
        val bookType = when {
            fileName.endsWith(".pdf", ignoreCase = true) -> BookType.PDF
            fileName.endsWith(".epub", ignoreCase = true) -> BookType.EPUB
            else -> return
        }

        // Copy file to internal storage
        try {
            val booksDir = File(context.filesDir, "books")
            if (!booksDir.exists()) {
                booksDir.mkdirs()
            }
            
            val destFile = File(booksDir, fileName)
            
            // Copy file if not already exists
            if (!destFile.exists()) {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }
            
            val book = Book(
                filePath = destFile.absolutePath,
                title = fileName.removeSuffix(".pdf").removeSuffix(".epub"),
                author = "Không xác định", // Default author
                type = bookType,
                coverImagePath = coverPath,
                currentProgress = 0f,
                totalPages = 0, // Will be updated when reading
                currentPage = 0
            )

            selectedBook = book
            addToRecentBooks(book)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun addToRecentBooks(book: Book) {
        recentBooks = listOf(book) + recentBooks.filterNot { it.filePath == book.filePath }
    }

    fun clearSelection() {
        selectedBook = null
    }

    fun updateBookProgress(bookFilePath: String, currentPage: Int, totalPages: Int) {
        val progress = if (totalPages > 0) currentPage.toFloat() / totalPages.toFloat() else 0f
        
        val updatedBooks = recentBooks.map { book ->
            if (book.filePath == bookFilePath) {
                book.copy(
                    currentProgress = progress.coerceIn(0f, 1f),
                    totalPages = totalPages,
                    currentPage = currentPage
                )
            } else {
                book
            }
        }
        
        recentBooks = updatedBooks
        BookStorage.saveBooks(getApplication(), updatedBooks)
        
        // Update selected book if it's the one being read
        if (selectedBook?.filePath == bookFilePath) {
            selectedBook = selectedBook?.copy(
                currentProgress = progress.coerceIn(0f, 1f),
                totalPages = totalPages,
                currentPage = currentPage
            )
        }
    }
}
