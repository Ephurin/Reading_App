package com.example.reading_app.ui.screens

import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipInputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpubReaderScreen(
    bookTitle: String,
    filePath: String,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var currentChapterIndex by remember { mutableIntStateOf(0) }
    var chapters by remember { mutableStateOf<List<String>>(emptyList()) }
    var chapterContent by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(filePath) {
        isLoading = true
        error = null
        
        try {
            withContext(Dispatchers.IO) {
                val file = File(filePath)
                
                if (!file.exists()) {
                    error = "File not found"
                    isLoading = false
                    return@withContext
                }
                
                val inputStream = file.inputStream()
                val chapterList = mutableListOf<String>()
                
                try {
                    ZipInputStream(inputStream).use { zipInputStream ->
                        var zipEntry = zipInputStream.nextEntry
                        
                        // Read all HTML/XHTML files from EPUB
                        while (zipEntry != null) {
                            try {
                                if (!zipEntry.isDirectory) {
                                    val entryName = zipEntry.name.lowercase()
                                    if (entryName.endsWith(".html") || 
                                        entryName.endsWith(".xhtml") ||
                                        entryName.endsWith(".htm")) {
                                        
                                        // Read content in chunks to avoid memory issues
                                        val content = StringBuilder()
                                        val buffer = ByteArray(8192)
                                        var len: Int
                                        
                                        while (zipInputStream.read(buffer).also { len = it } > 0) {
                                            content.append(String(buffer, 0, len, Charsets.UTF_8))
                                        }
                                        
                                        if (content.isNotEmpty()) {
                                            chapterList.add(content.toString())
                                        }
                                    }
                                }
                                zipInputStream.closeEntry()
                                zipEntry = zipInputStream.nextEntry
                            } catch (e: Exception) {
                                // Skip problematic entries
                                e.printStackTrace()
                                zipEntry = zipInputStream.nextEntry
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    error = "Error reading EPUB structure: ${e.message}"
                    isLoading = false
                    return@withContext
                }
                
                withContext(Dispatchers.Main) {
                    chapters = chapterList
                    if (chapterList.isNotEmpty()) {
                        chapterContent = wrapHtmlContent(chapterList[0])
                        error = null
                    } else {
                        error = "No readable content found in EPUB file"
                    }
                    isLoading = false
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                error = "Error loading EPUB: ${e.message ?: "Unknown error"}"
                isLoading = false
            }
        }
    }

    fun loadChapter(index: Int) {
        if (index in chapters.indices) {
            chapterContent = wrapHtmlContent(chapters[index])
            currentChapterIndex = index
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(
                            text = bookTitle,
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (chapters.isNotEmpty()) {
                            Text(
                                text = "Chapter ${currentChapterIndex + 1} of ${chapters.size}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            if (chapters.isNotEmpty()) {
                NavigationBar {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { loadChapter(currentChapterIndex - 1) },
                            enabled = currentChapterIndex > 0
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.NavigateBefore,
                                contentDescription = "Previous Chapter"
                            )
                        }
                        
                        Text(
                            text = "${currentChapterIndex + 1} / ${chapters.size}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        
                        IconButton(
                            onClick = { loadChapter(currentChapterIndex + 1) },
                            enabled = currentChapterIndex < chapters.size - 1
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                                contentDescription = "Next Chapter"
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Text(
                                text = "⚠️ Error Loading EPUB",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = error ?: "Unknown error",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            Button(
                                onClick = onBackClick,
                                modifier = Modifier.padding(top = 16.dp)
                            ) {
                                Text("Go Back")
                            }
                        }
                    }
                }
                else -> {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.javaScriptEnabled = false
                                settings.builtInZoomControls = true
                                settings.displayZoomControls = false
                                settings.textZoom = 110
                                settings.loadWithOverviewMode = true
                                settings.useWideViewPort = true
                            }
                        },
                        update = { webView ->
                            webView.loadDataWithBaseURL(
                                null,
                                chapterContent,
                                "text/html",
                                "UTF-8",
                                null
                            )
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

private fun wrapHtmlContent(content: String): String {
    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=yes">
            <style>
                body {
                    font-family: Georgia, 'Times New Roman', serif;
                    font-size: 18px;
                    line-height: 1.8;
                    padding: 20px;
                    color: #2c2c2c;
                    background-color: #fefef8;
                    max-width: 800px;
                    margin: 0 auto;
                }
                p {
                    margin-bottom: 1.2em;
                    text-align: justify;
                    text-indent: 1.5em;
                }
                p:first-of-type {
                    text-indent: 0;
                }
                h1, h2, h3, h4, h5, h6 {
                    font-family: 'Helvetica Neue', Arial, sans-serif;
                    margin-top: 1.5em;
                    margin-bottom: 0.5em;
                    color: #1a1a1a;
                    text-align: left;
                }
                img {
                    max-width: 100%;
                    height: auto;
                    display: block;
                    margin: 1em auto;
                }
                a {
                    color: #0066cc;
                    text-decoration: none;
                }
            </style>
        </head>
        <body>
            $content
        </body>
        </html>
    """.trimIndent()
}
