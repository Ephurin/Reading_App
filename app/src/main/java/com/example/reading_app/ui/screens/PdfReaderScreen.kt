package com.example.reading_app.ui.screens

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfReaderScreen(
    bookTitle: String,
    filePath: String,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var currentPage by remember { mutableIntStateOf(0) }
    var totalPages by remember { mutableIntStateOf(0) }
    var currentBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var pdfRenderer by remember { mutableStateOf<PdfRenderer?>(null) }
    var scale by remember { mutableFloatStateOf(1f) }
    var showGoToPageDialog by remember { mutableStateOf(false) }
    var pageInputText by remember { mutableStateOf("") }
    
    // Force light mode for PDF reader
    val lightColorScheme = lightColorScheme()

    DisposableEffect(filePath) {
        val job = scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val file = File(filePath)
                    
                    if (!file.exists()) {
                        isLoading = false
                        return@withContext
                    }

                    // Open PDF
                    val fileDescriptor = ParcelFileDescriptor.open(
                        file,
                        ParcelFileDescriptor.MODE_READ_ONLY
                    )
                    val renderer = PdfRenderer(fileDescriptor)
                    pdfRenderer = renderer
                    totalPages = renderer.pageCount

                    // Render first page
                    val page = renderer.openPage(0)
                    val bitmap = Bitmap.createBitmap(
                        page.width * 2,
                        page.height * 2,
                        Bitmap.Config.ARGB_8888
                    )
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    currentBitmap = bitmap
                    page.close()
                    isLoading = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                isLoading = false
            }
        }

        onDispose {
            job.cancel()
            pdfRenderer?.close()
        }
    }

    fun renderPage(pageIndex: Int) {
        if (pageIndex < 0 || pageIndex >= totalPages) return
        
        scope.launch {
            withContext(Dispatchers.IO) {
                try {
                    pdfRenderer?.let { renderer ->
                        val page = renderer.openPage(pageIndex)
                        val bitmap = Bitmap.createBitmap(
                            page.width * 2,
                            page.height * 2,
                            Bitmap.Config.ARGB_8888
                        )
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        currentBitmap = bitmap
                        currentPage = pageIndex
                        page.close()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
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
                        if (totalPages > 0) {
                            TextButton(
                                onClick = { showGoToPageDialog = true },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(
                                    text = "Page ${currentPage + 1} of $totalPages",
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = lightColorScheme.primaryContainer,
                    titleContentColor = lightColorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            if (totalPages > 0) {
                NavigationBar {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { renderPage(currentPage - 1) },
                            enabled = currentPage > 0
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.NavigateBefore,
                                contentDescription = "Previous Page"
                            )
                        }
                        
                        TextButton(
                            onClick = { showGoToPageDialog = true }
                        ) {
                            Text(
                                text = "${currentPage + 1} / $totalPages",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        
                        IconButton(
                            onClick = { renderPage(currentPage + 1) },
                            enabled = currentPage < totalPages - 1
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                                contentDescription = "Next Page"
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
                .background(Color.White)  // Force white background for PDF
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
                currentBitmap != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .pointerInput(Unit) {
                                detectTransformGestures { _, _, zoom, _ ->
                                    scale = (scale * zoom).coerceIn(0.5f, 3f)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = currentBitmap!!.asImageBitmap(),
                            contentDescription = "PDF Page",
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale
                                )
                                .padding(16.dp),
                            contentScale = ContentScale.FillWidth
                        )
                    }
                }
                else -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Failed to load PDF")
                    }
                }
            }
        }
    }
    
    // Go to Page Dialog
    if (showGoToPageDialog) {
        AlertDialog(
            onDismissRequest = { showGoToPageDialog = false },
            title = { Text("Go to Page") },
            text = {
                Column {
                    Text(
                        text = "Enter page number (1-$totalPages)",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = pageInputText,
                        onValueChange = { pageInputText = it },
                        label = { Text("Page number") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Go
                        ),
                        keyboardActions = KeyboardActions(
                            onGo = {
                                val pageNumber = pageInputText.toIntOrNull()
                                if (pageNumber != null && pageNumber in 1..totalPages) {
                                    renderPage(pageNumber - 1)
                                    showGoToPageDialog = false
                                    pageInputText = ""
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
                        val pageNumber = pageInputText.toIntOrNull()
                        if (pageNumber != null && pageNumber in 1..totalPages) {
                            renderPage(pageNumber - 1)
                            showGoToPageDialog = false
                            pageInputText = ""
                        }
                    }
                ) {
                    Text("Go")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showGoToPageDialog = false
                        pageInputText = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
