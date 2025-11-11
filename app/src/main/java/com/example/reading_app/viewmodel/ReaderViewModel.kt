package com.example.reading_app.viewmodel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.reading_app.model.Book
import com.example.reading_app.model.BookType
import com.example.reading_app.utils.CoverExtractor
import java.io.File
import java.io.FileOutputStream

class ReaderViewModel : ViewModel() {
    var selectedBook by mutableStateOf<Book?>(null)
        private set

    var recentBooks by mutableStateOf<List<Book>>(emptyList())
        private set
    
    var currentBook by mutableStateOf<Book?>(null)
        private set

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
            
            // Extract cover image
            val coverPath = when (bookType) {
                BookType.PDF -> CoverExtractor.extractPdfCover(context, destFile)
                BookType.EPUB -> CoverExtractor.extractEpubCover(context, destFile)
            }
            
            val book = Book(
                filePath = destFile.absolutePath,
                title = fileName.removeSuffix(".pdf").removeSuffix(".epub"),
                type = bookType,
                coverImagePath = coverPath
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
}
