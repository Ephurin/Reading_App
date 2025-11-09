package com.example.reading_app.ui.screens



import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.reading_app.R

@Composable
fun LibraryScreen() {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Thư viện", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))

        Spacer(Modifier.height(12.dp))

        val ownedBooks = listOf(
            R.drawable.book1, R.drawable.book2, R.drawable.book3
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(ownedBooks) { resId ->
                Card(
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth().height(120.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = resId),
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.width(12.dp))
                        Text("Tên sách mẫu", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
