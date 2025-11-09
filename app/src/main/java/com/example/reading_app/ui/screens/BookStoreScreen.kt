package com.example.reading_app.ui.screens


import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.reading_app.R

@Composable
fun BookStoreScreen() {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Book Store", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))

        Spacer(Modifier.height(12.dp))

        val books = listOf(
            R.drawable.book1, R.drawable.book2, R.drawable.book3,
            R.drawable.book4, R.drawable.book5, R.drawable.book6
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(books) { resId ->
                Card(
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.height(220.dp)
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
