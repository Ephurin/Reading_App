package com.example.reading_app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.reading_app.R
import com.example.reading_app.model.Book
import com.example.reading_app.viewmodel.BookStoreViewModel
import java.io.File

@Composable
fun HomeScreen(
    recentBooks: List<Book>,
    onBookSelected: (Book) -> Unit,
    bookStoreViewModel: BookStoreViewModel,
    navController: NavHostController
) {
    val trendingBooks by bookStoreViewModel.trendingBooks.collectAsState()
    val discoverBooks by bookStoreViewModel.discoverBooks.collectAsState()
    val discountedBooks by bookStoreViewModel.discountedBooks.collectAsState()
    val recommendedBooks by bookStoreViewModel.recommendedBooks.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        // Thanh tiêu đề
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Khám phá", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
            IconButton(onClick = { }) {
                Icon(Icons.Default.Notifications, contentDescription = null)
            }
        }

        if (recommendedBooks.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text("Đề xuất cho bạn", fontWeight = FontWeight.Bold, fontSize = 18.sp)

            // Recommended books list
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                items(recommendedBooks) { book ->
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .width(130.dp)
                            .clickable {
                                bookStoreViewModel.select(book)
                                navController.navigate("book_details")
                            }
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(book.coverUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = book.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .height(160.dp)
                                    .fillMaxWidth()
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = book.title,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.heightIn(min = 32.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("Sách giảm giá", fontWeight = FontWeight.Bold, fontSize = 18.sp)

        // Discounted books list
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            items(discountedBooks) { book ->
                Card(
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .width(130.dp)
                        .clickable {
                            bookStoreViewModel.select(book)
                            navController.navigate("book_details")
                        }
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(book.coverUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = book.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .height(160.dp)
                                .fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = book.title,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.heightIn(min = 32.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("Tiếp tục đọc", fontWeight = FontWeight.Bold, fontSize = 18.sp)

        // Continue reading list
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            items(recentBooks) { book ->
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
        }

        Spacer(Modifier.height(16.dp))
        Text("Đang thịnh hành", fontWeight = FontWeight.Bold, fontSize = 18.sp)

        // Trending books list
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            items(trendingBooks) { book ->
                Card(
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .width(130.dp)
                        .clickable {
                            bookStoreViewModel.select(book)
                            navController.navigate("book_details")
                        }
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(book.coverUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = book.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .height(160.dp)
                                .fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = book.title,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.heightIn(min = 32.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("Khám phá thêm", fontWeight = FontWeight.Bold, fontSize = 18.sp)

        // Discover books list
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            items(discoverBooks) { book ->
                Card(
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .width(130.dp)
                        .clickable {
                            bookStoreViewModel.select(book)
                            navController.navigate("book_details")
                        }
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(book.coverUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = book.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .height(160.dp)
                                .fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = book.title,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.heightIn(min = 32.dp)
                        )
                    }
                }
            }
        }
    }
}
