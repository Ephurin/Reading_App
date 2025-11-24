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
    var recentBooks by mutableStateOf<List<Book>>(emptyList())
        private set

    var currentBook by mutableStateOf<Book?>(null)
        private set

    init {
        // Load books on initialization
        loadBooks()
    }

    private fun loadBooks() {
        recentBooks = BookStorage.loadBooks(getApplication())
    }

    fun selectBookForReading(book: Book) {
        currentBook = book
    }

    fun clearCurrentBook() {
        currentBook = null
    }

    fun selectBook(context: Context, uri: android.net.Uri, fileName: String) {
        val bookType = when {
            fileName.endsWith(".pdf", ignoreCase = true) -> BookType.PDF
            fileName.endsWith(".epub", ignoreCase = true) -> BookType.EPUB
            else -> return
        }

        try {
            val booksDir = File(context.filesDir, "books")
            if (!booksDir.exists()) {
                booksDir.mkdirs()
            }

            val destFile = File(booksDir, fileName)

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

            addBook(book)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun addBook(book: Book) {
        val updatedBooks = listOf(book) + recentBooks.filterNot { it.filePath == book.filePath }
        recentBooks = updatedBooks
        BookStorage.saveBooks(getApplication(), updatedBooks)
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
