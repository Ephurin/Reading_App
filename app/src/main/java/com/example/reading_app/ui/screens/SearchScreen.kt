package com.example.reading_app.ui.screens


import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.reading_app.R

@Composable
fun SearchScreen() {
    var query by remember { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            placeholder = { Text("Tìm kiếm sách...") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        // Gợi ý kết quả
        Text("Gợi ý nổi bật", style = MaterialTheme.typography.titleMedium)

        Spacer(Modifier.height(8.dp))
        Image(
            painter = painterResource(id = R.drawable.banner_search),
            contentDescription = null,
            modifier = Modifier.fillMaxWidth().height(180.dp),
            contentScale = ContentScale.Crop
        )
    }
}
