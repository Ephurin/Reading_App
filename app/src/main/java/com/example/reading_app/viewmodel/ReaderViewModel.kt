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
import com.example.reading_app.utils.CoverExtractor
import java.io.File
import java.io.FileOutputStream

class ReaderViewModel(application: Application) : AndroidViewModel(application) {
    var selectedBook by mutableStateOf<Book?>(null)

    fun selectBookForReading(book: Book) {
        selectedBook = book
    }

    var recentBooks by mutableStateOf<List<Book>>(emptyList())
        private set

    init {
        loadBooks()
    }

    private fun loadBooks() {
        recentBooks = BookStorage.loadBooks(getApplication())
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
            
            val coverPath = when (bookType) {
                BookType.PDF -> CoverExtractor.extractPdfCover(context, destFile)
                BookType.EPUB -> CoverExtractor.extractEpubCover(context, destFile)
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
        BookStorage.saveBooks(getApplication(), recentBooks)
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
