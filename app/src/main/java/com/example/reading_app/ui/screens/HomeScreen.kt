package com.example.reading_app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import java.io.File

@Composable
fun HomeScreen(
    recentBooks: List<Book>,
    onBookSelected: (Book) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
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

        Spacer(Modifier.height(12.dp))

        // Ảnh nổi bật
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Image(
                painter = painterResource(id = R.drawable.banner_home),
                contentDescription = null,
                contentScale = ContentScale.Crop
            )
        }

        Spacer(Modifier.height(16.dp))
        Text("Tiếp tục đọc", fontWeight = FontWeight.Bold, fontSize = 18.sp)

        // Danh sách tiếp tục đọc
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

        // Danh sách ngang
        val trending = listOf(
            R.drawable.book1, R.drawable.book2, R.drawable.book3, R.drawable.book4
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            items(trending) { resId ->
                Card(
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.size(120.dp, 180.dp)
                ) {
                    Image(
                        painter = painterResource(id = resId),
                        contentDescription = null,
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}
