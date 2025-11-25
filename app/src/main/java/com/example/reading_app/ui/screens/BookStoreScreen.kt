package com.example.reading_app.ui.screens


import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.clickable
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusState
import com.example.reading_app.network.GoogleBooksApi
import com.example.reading_app.network.OnlineBook
import com.example.reading_app.network.BookDownloader

import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.reading_app.viewmodel.ReaderViewModel
import androidx.navigation.NavHostController
import com.example.reading_app.viewmodel.BookStoreViewModel

@Composable
fun BookStoreScreen(
    navController: NavHostController,
    bookStoreViewModel: BookStoreViewModel
) {
    var searchQuery by remember { mutableStateOf("") }
    var showInstruction by remember { mutableStateOf(true) }
    var books by remember { mutableStateOf<List<OnlineBook>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var downloadingBookId by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val readerViewModel: ReaderViewModel = viewModel()

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Book Store", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
            },
            label = { Text("Search books") },
            placeholder = {
                if (showInstruction) Text("Type to search books")
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { state: FocusState ->
                    if (state.isFocused) {
                        showInstruction = false
                    } else {
                        if (searchQuery.isBlank()) showInstruction = true
                    }
                }
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                isLoading = true
                error = null
                bookStoreViewModel.searchBooks(searchQuery)
                scope.launch {
                    try {
                        books = GoogleBooksApi.searchBooks(searchQuery)
                    } catch (_: Exception) {
                        error = "Failed to fetch books"
                    } finally {
                        isLoading = false
                    }
                }
            },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Search")
        }
        Spacer(Modifier.height(12.dp))

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (error != null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(error!!, color = MaterialTheme.colorScheme.error)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(books) { book ->
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .height(260.dp)
                            .clickable {
                                bookStoreViewModel.select(book)
                                navController.navigate("book_details")
                            }
                    ) {
                        Column(
                            Modifier.fillMaxSize().padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            AsyncImage(
                                model = book.coverUrl,
                                contentDescription = book.title,
                                modifier = Modifier.height(120.dp).fillMaxWidth(),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(book.title, fontWeight = FontWeight.Bold, maxLines = 2)
                            Text(book.authors.joinToString(), style = MaterialTheme.typography.bodySmall, maxLines = 1)
                            Spacer(Modifier.height(8.dp))
                            // Small description snippet
                            val snippet = book.description?.take(120)
                            if (!snippet.isNullOrBlank()) {
                                Text(snippet + if (book.description!!.length > 120) "..." else "", style = MaterialTheme.typography.bodySmall, maxLines = 2)
                            }
                        }
                    }
                }
            }
        }
    }

    // details are now shown on a dedicated screen
}
