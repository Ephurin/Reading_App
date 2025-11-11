package com.example.reading_app.ui.screens

import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.reading_app.model.Bookmark
import com.example.reading_app.utils.BookmarkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
    val scope = rememberCoroutineScope()
    var currentChapterIndex by remember { mutableIntStateOf(0) }
    var chapters by remember { mutableStateOf<List<String>>(emptyList()) }
    var chapterContent by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var showGoToChapterDialog by remember { mutableStateOf(false) }
    var chapterInputText by remember { mutableStateOf("") }
    val isDarkMode = isSystemInDarkTheme()
    var bookmarks by remember { mutableStateOf<List<Bookmark>>(emptyList()) }
    var isCurrentChapterBookmarked by remember { mutableStateOf(false) }
    var showBookmarksDialog by remember { mutableStateOf(false) }
    var epubExtractedDir by remember { mutableStateOf<File?>(null) }
    var chapterFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var currentChapterFile by remember { mutableStateOf<File?>(null) }
    
    // Load bookmarks for this book
    LaunchedEffect(filePath) {
        bookmarks = BookmarkManager.getBookmarksForBook(context, filePath)
    }
    
    // Check if current chapter is bookmarked
    LaunchedEffect(currentChapterIndex, bookmarks) {
        isCurrentChapterBookmarked = BookmarkManager.isBookmarked(context, filePath, currentChapterIndex)
    }
    
    // Cleanup extracted files when screen is disposed
    DisposableEffect(filePath) {
        onDispose {
            epubExtractedDir?.deleteRecursively()
        }
    }

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
                
                // Create extraction directory for this EPUB
                val extractDir = File(context.cacheDir, "epub_${file.name.hashCode()}")
                if (extractDir.exists()) {
                    extractDir.deleteRecursively()
                }
                extractDir.mkdirs()
                epubExtractedDir = extractDir
                
                val inputStream = file.inputStream()
                val chapterList = mutableListOf<String>()
                val chapterFileList = mutableListOf<File>()
                
                try {
                    ZipInputStream(inputStream).use { zipInputStream ->
                        var zipEntry = zipInputStream.nextEntry
                        
                        // Extract all files from EPUB
                        while (zipEntry != null) {
                            try {
                                if (!zipEntry.isDirectory) {
                                    val entryName = zipEntry.name
                                    val entryFile = File(extractDir, entryName)
                                    
                                    // Create parent directories
                                    entryFile.parentFile?.mkdirs()
                                    
                                    // Extract file
                                    entryFile.outputStream().use { output ->
                                        zipInputStream.copyTo(output)
                                    }
                                    
                                    // Collect HTML/XHTML chapters
                                    val lowerName = entryName.lowercase()
                                    if (lowerName.endsWith(".html") || 
                                        lowerName.endsWith(".xhtml") ||
                                        lowerName.endsWith(".htm")) {
                                        
                                        val content = entryFile.readText(Charsets.UTF_8)
                                        if (content.isNotEmpty()) {
                                            chapterList.add(content)
                                            chapterFileList.add(entryFile)
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
                    chapterFiles = chapterFileList
                    if (chapterList.isNotEmpty() && chapterFileList.isNotEmpty()) {
                        currentChapterFile = chapterFileList[0]
                        // Create a styled version of the chapter
                        chapterContent = injectStyles(chapterList[0], isDarkMode)
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
        if (index in chapters.indices && index in chapterFiles.indices) {
            currentChapterFile = chapterFiles[index]
            chapterContent = injectStyles(chapters[index], isDarkMode)
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
                            TextButton(
                                onClick = { showGoToChapterDialog = true },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(
                                    text = "Chapter ${currentChapterIndex + 1} of ${chapters.size}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
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
                actions = {
                    // Bookmark button
                    IconButton(
                        onClick = {
                            scope.launch {
                                if (isCurrentChapterBookmarked) {
                                    // Remove bookmark
                                    val bookmark = bookmarks.find { it.pageOrChapter == currentChapterIndex }
                                    bookmark?.let {
                                        BookmarkManager.deleteBookmark(context, it)
                                        bookmarks = BookmarkManager.getBookmarksForBook(context, filePath)
                                    }
                                } else {
                                    // Add bookmark
                                    val bookmark = Bookmark(
                                        bookFilePath = filePath,
                                        pageOrChapter = currentChapterIndex,
                                        title = "Chapter ${currentChapterIndex + 1}"
                                    )
                                    BookmarkManager.saveBookmark(context, bookmark)
                                    bookmarks = BookmarkManager.getBookmarksForBook(context, filePath)
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isCurrentChapterBookmarked) 
                                Icons.Default.Bookmark 
                            else 
                                Icons.Default.BookmarkBorder,
                            contentDescription = if (isCurrentChapterBookmarked) 
                                "Remove Bookmark" 
                            else 
                                "Add Bookmark"
                        )
                    }
                    
                    // Show bookmarks list button
                    if (bookmarks.isNotEmpty()) {
                        IconButton(onClick = { showBookmarksDialog = true }) {
                            Badge {
                                Text("${bookmarks.size}")
                            }
                        }
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
                        
                        TextButton(
                            onClick = { showGoToChapterDialog = true }
                        ) {
                            Text(
                                text = "${currentChapterIndex + 1} / ${chapters.size}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        
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
                                settings.allowFileAccess = true
                                settings.allowContentAccess = true
                                settings.domStorageEnabled = true
                            }
                        },
                        update = { webView ->
                            // Use base URL pointing to the extracted directory
                            val baseUrl = currentChapterFile?.parentFile?.let { 
                                "file://${it.absolutePath}/"
                            } ?: epubExtractedDir?.let { 
                                "file://${it.absolutePath}/" 
                            } ?: "file:///"
                            
                            webView.loadDataWithBaseURL(
                                baseUrl,
                                chapterContent,
                                "text/html; charset=UTF-8",
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
    
    // Go to Chapter Dialog
    if (showGoToChapterDialog) {
        AlertDialog(
            onDismissRequest = { showGoToChapterDialog = false },
            title = { Text("Go to Chapter") },
            text = {
                Column {
                    Text(
                        text = "Enter chapter number (1-${chapters.size})",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = chapterInputText,
                        onValueChange = { chapterInputText = it },
                        label = { Text("Chapter number") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Go
                        ),
                        keyboardActions = KeyboardActions(
                            onGo = {
                                val chapterNumber = chapterInputText.toIntOrNull()
                                if (chapterNumber != null && chapterNumber in 1..chapters.size) {
                                    loadChapter(chapterNumber - 1)
                                    showGoToChapterDialog = false
                                    chapterInputText = ""
                                }
                            }
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val chapterNumber = chapterInputText.toIntOrNull()
                        if (chapterNumber != null && chapterNumber in 1..chapters.size) {
                            loadChapter(chapterNumber - 1)
                            showGoToChapterDialog = false
                            chapterInputText = ""
                        }
                    }
                ) {
                    Text("Go")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showGoToChapterDialog = false
                        chapterInputText = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Bookmarks List Dialog
    if (showBookmarksDialog) {
        AlertDialog(
            onDismissRequest = { showBookmarksDialog = false },
            title = { Text("Bookmarks") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (bookmarks.isEmpty()) {
                        Text("No bookmarks yet")
                    } else {
                        bookmarks.forEach { bookmark ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                onClick = {
                                    loadChapter(bookmark.pageOrChapter)
                                    showBookmarksDialog = false
                                }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = bookmark.title,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Text(
                                            text = "Chapter ${bookmark.pageOrChapter + 1}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                BookmarkManager.deleteBookmark(context, bookmark)
                                                bookmarks = BookmarkManager.getBookmarksForBook(context, filePath)
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Bookmark,
                                            contentDescription = "Remove Bookmark",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBookmarksDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

private fun wrapHtmlContent(content: String, isDarkMode: Boolean): String {
    val backgroundColor = if (isDarkMode) "#1e1e1e" else "#fefef8"
    val textColor = if (isDarkMode) "#e0e0e0" else "#2c2c2c"
    val headingColor = if (isDarkMode) "#ffffff" else "#1a1a1a"
    val linkColor = if (isDarkMode) "#66b3ff" else "#0066cc"
    
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
                    color: $textColor;
                    background-color: $backgroundColor;
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
                    color: $headingColor;
                    text-align: left;
                }
                img {
                    max-width: 100%;
                    height: auto;
                    display: block;
                    margin: 1em auto;
                }
                a {
                    color: $linkColor;
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

private fun createStyledChapterFile(originalFile: File, content: String, isDarkMode: Boolean, extractDir: File): File {
    val styledDir = File(extractDir, "styled")
    styledDir.mkdirs()
    
    val styledFile = File(styledDir, originalFile.name)
    val wrappedContent = injectStyles(content, isDarkMode)
    styledFile.writeText(wrappedContent, Charsets.UTF_8)
    
    return styledFile
}

private fun injectStyles(htmlContent: String, isDarkMode: Boolean): String {
    val backgroundColor = if (isDarkMode) "#1e1e1e" else "#fefef8"
    val textColor = if (isDarkMode) "#e0e0e0" else "#2c2c2c"
    val headingColor = if (isDarkMode) "#ffffff" else "#1a1a1a"
    val linkColor = if (isDarkMode) "#66b3ff" else "#0066cc"
    
    val customStyles = """
        <style>
            * {
                max-width: 100%;
                box-sizing: border-box;
            }
            body {
                color: $textColor !important;
                background-color: $backgroundColor !important;
                padding: 16px !important;
                margin: 0 !important;
                line-height: 1.7 !important;
                font-family: Georgia, 'Times New Roman', serif !important;
                font-size: 16px !important;
                word-wrap: break-word !important;
                overflow-wrap: break-word !important;
            }
            p {
                margin: 0 0 1em 0 !important;
                text-align: justify !important;
                text-indent: 1.5em !important;
                line-height: 1.8 !important;
            }
            p:first-of-type, p:first-child {
                text-indent: 0 !important;
            }
            h1, h2, h3, h4, h5, h6 {
                color: $headingColor !important;
                margin: 1.5em 0 0.5em 0 !important;
                line-height: 1.3 !important;
                font-weight: bold !important;
                text-align: left !important;
                text-indent: 0 !important;
            }
            h1 {
                font-size: 1.8em !important;
                margin-top: 0.5em !important;
            }
            h2 {
                font-size: 1.5em !important;
            }
            h3 {
                font-size: 1.3em !important;
            }
            h4, h5, h6 {
                font-size: 1.1em !important;
            }
            a {
                color: $linkColor !important;
                text-decoration: underline !important;
            }
            img {
                max-width: 100% !important;
                height: auto !important;
                display: block !important;
                margin: 1em auto !important;
            }
            ul, ol {
                margin: 0.5em 0 1em 0 !important;
                padding-left: 2em !important;
            }
            li {
                margin: 0.3em 0 !important;
                line-height: 1.6 !important;
            }
            blockquote {
                margin: 1em 0 !important;
                padding: 0.5em 1em !important;
                border-left: 3px solid $linkColor !important;
                font-style: italic !important;
            }
            pre, code {
                font-family: 'Courier New', monospace !important;
                background-color: ${if (isDarkMode) "#2a2a2a" else "#f5f5f5"} !important;
                padding: 0.2em 0.4em !important;
                border-radius: 3px !important;
                font-size: 0.9em !important;
            }
            pre {
                padding: 1em !important;
                overflow-x: auto !important;
            }
            table {
                border-collapse: collapse !important;
                width: 100% !important;
                margin: 1em 0 !important;
            }
            th, td {
                border: 1px solid ${if (isDarkMode) "#444" else "#ddd"} !important;
                padding: 0.5em !important;
                text-align: left !important;
            }
            th {
                background-color: ${if (isDarkMode) "#2a2a2a" else "#f5f5f5"} !important;
                font-weight: bold !important;
            }
            hr {
                border: none !important;
                border-top: 1px solid ${if (isDarkMode) "#444" else "#ddd"} !important;
                margin: 2em 0 !important;
            }
            div, section, article {
                max-width: 100% !important;
            }
        </style>
    """.trimIndent()
    
    // Try to inject style after <head> tag
    return if (htmlContent.contains("<head>", ignoreCase = true)) {
        htmlContent.replace(
            Regex("<head>", RegexOption.IGNORE_CASE),
            "<head>$customStyles"
        )
    } else if (htmlContent.contains("<html>", ignoreCase = true)) {
        // If no head tag, inject after html tag
        htmlContent.replace(
            Regex("<html[^>]*>", RegexOption.IGNORE_CASE),
            "$0<head>$customStyles</head>"
        )
    } else {
        // Wrap entire content
        "<html><head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">$customStyles</head><body>$htmlContent</body></html>"
    }
}
