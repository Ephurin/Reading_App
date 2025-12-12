package com.example.reading_app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.reading_app.R
import com.example.reading_app.model.Book
import com.example.reading_app.network.OnlineBook
import com.example.reading_app.viewmodel.BookStoreViewModel
import java.io.File

@Composable
fun HomeScreen(
    recentBooks: List<Book>,
    onBookSelected: (Book) -> Unit,
    onOnlineBookSelected: (OnlineBook) -> Unit, // New navigation handler
    bookStoreViewModel: BookStoreViewModel
) {
    val trendingBooks by bookStoreViewModel.trendingBooks.collectAsState()
    val recommendedBooks by bookStoreViewModel.recommendedBooks.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Khám phá", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
                IconButton(onClick = { }) {
                    Icon(Icons.Default.Notifications, contentDescription = null)
                }
            }
        }

        // Featured Image
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .padding(horizontal = 16.dp),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Image(
                    painter = painterResource(id = R.drawable.banner_home),
                    contentDescription = null,
                    contentScale = ContentScale.Crop
                )
            }
        }

        // Continue Reading
        if (recentBooks.isNotEmpty()) {
            item { Text("Tiếp tục đọc", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(horizontal = 16.dp)) }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(recentBooks) { book ->
                        BookItem(book = book, onBookSelected = onBookSelected)
                    }
                }
            }
        }

        // Trending Books
        if (trendingBooks.isNotEmpty()) {
            item { Text("Đang thịnh hành", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(horizontal = 16.dp)) }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(trendingBooks) { onlineBook ->
                        OnlineBookItem(book = onlineBook, onBookSelected = { onOnlineBookSelected(onlineBook) })
                    }
                }
            }
        }

        // Recommended Books
        if (recommendedBooks.isNotEmpty()) {
            item { Text("Đề xuất cho bạn", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(horizontal = 16.dp)) }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(recommendedBooks) { onlineBook ->
                        OnlineBookItem(book = onlineBook, onBookSelected = { onOnlineBookSelected(onlineBook) })
                    }
                }
            }
        }
    }
}

@Composable
private fun BookItem(book: Book, onBookSelected: (Book) -> Unit) {
    Card(
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .size(120.dp, 180.dp)
            .clickable { onBookSelected(book) }
    ) {
        if (book.coverImagePath != null && File(book.coverImagePath).exists()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(File(book.coverImagePath))
                    .crossfade(true)
                    .build(),
                contentDescription = "Book cover",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Image(
                painter = painterResource(id = R.drawable.book1),
                contentDescription = null,
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
private fun OnlineBookItem(book: OnlineBook, onBookSelected: (OnlineBook) -> Unit) {
    Card(
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .size(120.dp, 180.dp)
            .clickable { onBookSelected(book) }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(book.coverUrl)
                .crossfade(true)
                .build(),
            contentDescription = "Book cover",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            error = painterResource(id = R.drawable.book1) // Placeholder in case of error
        )
    }
}
