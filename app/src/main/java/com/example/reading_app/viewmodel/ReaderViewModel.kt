package com.example.reading_app.viewmodel

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.example.reading_app.model.Book
import com.example.reading_app.model.BookType
import com.example.reading_app.utils.BookStorage
import java.io.File
import java.io.FileOutputStream

class ReaderViewModel(application: Application) : AndroidViewModel(application) {
    var selectedBook by mutableStateOf<Book?>(null)
        private set

    var currentBook by mutableStateOf<Book?>(null)
        private set

    var recentBooks by mutableStateOf<List<Book>>(emptyList())
        private set

    init {
        // Load persisted books
        try {
            recentBooks = BookStorage.loadBooks(getApplication())
        } catch (e: Exception) {
            recentBooks = emptyList()
        }
    }

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
                author = "Không xác định",
                type = bookType,
                coverImagePath = null,
                currentProgress = 0f,
                totalPages = 0,
                currentPage = 0
            )

            // Set both selection and current book for reading
            selectedBook = book
            currentBook = book
            addToRecentBooks(book)
            BookStorage.saveBooks(getApplication(), recentBooks)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun selectBookForReading(book: Book) {
        selectedBook = book
        currentBook = book
        addToRecentBooks(book)
        BookStorage.saveBooks(getApplication(), recentBooks)
    }

    private fun addToRecentBooks(book: Book) {
        recentBooks = listOf(book) + recentBooks.filterNot { it.filePath == book.filePath }
    }

    fun clearSelection() {
        selectedBook = null
        currentBook = null
    }

    fun clearCurrentBook() {
        currentBook = null
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
        try {
            BookStorage.saveBooks(getApplication(), updatedBooks)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Update current book if it's the one being read
        if (currentBook?.filePath == bookFilePath) {
            currentBook = currentBook?.copy(
                currentProgress = progress.coerceIn(0f, 1f),
                totalPages = totalPages,
                currentPage = currentPage
            )
        }
    }
}
