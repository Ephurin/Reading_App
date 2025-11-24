package com.example.reading_app.ui.screens

import android.content.Intent
import androidx.core.net.toUri
import java.io.File
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import androidx.navigation.NavHostController
import com.example.reading_app.viewmodel.BookStoreViewModel
import com.example.reading_app.viewmodel.ReaderViewModel
import com.example.reading_app.network.BookDownloader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.size
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailsScreen(
    navController: NavHostController,
    bookStoreViewModel: BookStoreViewModel,
    readerViewModel: ReaderViewModel
) {
    val book = bookStoreViewModel.selectedOnlineBook
    val context = LocalContext.current
    if (book == null) {
        // nothing selected, navigate back
        navController.popBackStack()
        return
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(book.title) },
                navigationIcon = {
                    IconButton(onClick = { bookStoreViewModel.clearSelection(); navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { inner ->
        Column(modifier = Modifier
            .padding(inner)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())) {
            // Top row: cover + basic metadata
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!book.coverUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = book.coverUrl,
                        contentDescription = book.title,
                        modifier = Modifier.size(140.dp),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(modifier = Modifier.size(140.dp), contentAlignment = Alignment.Center) {
                        Text("No Cover", style = MaterialTheme.typography.bodySmall)
                    }
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    if (!book.subtitle.isNullOrBlank()) Text(book.subtitle, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!book.authors.isNullOrEmpty()) Text(book.authors.joinToString(), style = MaterialTheme.typography.bodySmall)
                        if (!book.publisher.isNullOrBlank()) Text("• ${book.publisher}", style = MaterialTheme.typography.bodySmall)
                        if (!book.publishedDate.isNullOrBlank()) Text("(${book.publishedDate})", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            if (book.pageCount != null) Text("Pages: ${book.pageCount}")
            if (book.averageRating != null) Text("Rating: ${book.averageRating} (${book.ratingsCount ?: 0})")
            Spacer(Modifier.height(8.dp))
            if (!book.description.isNullOrBlank()) {
                Text(book.description, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!book.previewLink.isNullOrBlank()) {
                    TextButton(onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, book.previewLink.toUri())
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                    }) { Text("Preview") }
                }
                if (!book.infoLink.isNullOrBlank()) {
                    TextButton(onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, book.infoLink.toUri())
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                    }) { Text("Info") }
                }
                if (!book.buyLink.isNullOrBlank()) {
                    TextButton(onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, book.buyLink.toUri())
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                    }) { Text("Buy") }
                }
            }

            Spacer(Modifier.height(8.dp))
            val downloadUrl = book.downloadUrl
            if (downloadUrl != null) {
                Button(onClick = {
                    // download in IO
                    CoroutineScope(Dispatchers.IO).launch {
                        val fileExt = if (downloadUrl.contains(".pdf")) ".pdf" else ".epub"
                        val fileName = book.title.replace(" ", "_") + fileExt
                        val filePath = BookDownloader.downloadBook(context, downloadUrl, fileName)
                        if (filePath != null) {
                            // import on main thread
                            CoroutineScope(Dispatchers.Main).launch {
                                readerViewModel.selectBook(context, android.net.Uri.fromFile(File(filePath)), fileName)
                                bookStoreViewModel.clearSelection()
                                navController.popBackStack()
                            }
                        }
                    }
                }) { Text("Download & Import") }
            } else {
                Text("Download not available for this item.")
            }
        }
    }
}
