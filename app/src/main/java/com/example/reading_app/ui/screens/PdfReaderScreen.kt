package com.example.reading_app.ui.screens

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Note
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.reading_app.model.AnnotationPoint
import com.example.reading_app.model.AnnotationPosition
import com.example.reading_app.model.AnnotationType
import com.example.reading_app.model.Bookmark
import com.example.reading_app.model.DrawingPath
import com.example.reading_app.model.PdfAnnotation
import com.example.reading_app.ui.components.AddNoteDialog
import com.example.reading_app.utils.BookmarkManager
import com.example.reading_app.utils.PdfAnnotationManager
import com.example.reading_app.viewmodel.ReaderViewModel
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
    onBackClick: () -> Unit,
    readerViewModel: ReaderViewModel
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
    var bookmarks by remember { mutableStateOf<List<Bookmark>>(emptyList()) }
    var isCurrentPageBookmarked by remember { mutableStateOf(false) }
    var showBookmarksDialog by remember { mutableStateOf(false) }
    
    // Annotation-related states
    var annotations by remember { mutableStateOf<List<PdfAnnotation>>(emptyList()) }
    var isDrawingMode by remember { mutableStateOf(false) }
    var isNoteMode by remember { mutableStateOf(false) }
    var currentDrawingPath by remember { mutableStateOf<MutableList<Offset>>(mutableListOf()) }
    var allDrawingPaths by remember { mutableStateOf<List<DrawingPath>>(emptyList()) }
    var drawingColor by remember { mutableStateOf("#000000") }
    var showNoteDialog by remember { mutableStateOf(false) }
    var notePosition by remember { mutableStateOf<Offset?>(null) }
    var showAnnotationsMenu by remember { mutableStateOf(false) }
    
    // Force light mode for PDF reader
    val lightColorScheme = lightColorScheme()
    
    // Load bookmarks for this book
    LaunchedEffect(filePath) {
        bookmarks = BookmarkManager.getBookmarksForBook(context, filePath)
    }
    
    // Load annotations for current page
    LaunchedEffect(filePath, currentPage) {
        annotations = PdfAnnotationManager.getAnnotationsForPage(context, filePath, currentPage)
        // Load drawing paths from annotations
        allDrawingPaths = annotations.filter { it.type == AnnotationType.DRAWING }
            .flatMap { it.drawingPaths }
    }
    
    // Check if current page is bookmarked
    LaunchedEffect(currentPage, bookmarks) {
        isCurrentPageBookmarked = BookmarkManager.isBookmarked(context, filePath, currentPage)
    }

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
                        
                        // Update progress in ViewModel
                        readerViewModel.updateBookProgress(filePath, pageIndex + 1, totalPages)
                        
                        // Reset drawing and annotation states when changing pages
                        isDrawingMode = false
                        isNoteMode = false
                        currentDrawingPath.clear()
                        notePosition = null
                        
                        // Load annotations for new page
                        annotations = PdfAnnotationManager.getAnnotationsForPage(context, filePath, pageIndex)
                        allDrawingPaths = annotations.filter { it.type == AnnotationType.DRAWING }
                            .flatMap { it.drawingPaths }
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
                actions = {
                    // Drawing mode button
                    IconButton(
                        onClick = {
                            isDrawingMode = !isDrawingMode
                            isNoteMode = false
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Draw,
                            contentDescription = "Drawing Mode",
                            tint = if (isDrawingMode) lightColorScheme.primary else lightColorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Note mode button
                    IconButton(
                        onClick = {
                            isNoteMode = !isNoteMode
                            isDrawingMode = false
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Note,
                            contentDescription = "Add Note",
                            tint = if (isNoteMode) lightColorScheme.primary else lightColorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Bookmark button
                    IconButton(
                        onClick = {
                            scope.launch {
                                if (isCurrentPageBookmarked) {
                                    // Remove bookmark
                                    val bookmark = bookmarks.find { it.pageOrChapter == currentPage }
                                    bookmark?.let {
                                        BookmarkManager.deleteBookmark(context, it)
                                        bookmarks = BookmarkManager.getBookmarksForBook(context, filePath)
                                    }
                                } else {
                                    // Add bookmark
                                    val bookmark = Bookmark(
                                        bookFilePath = filePath,
                                        pageOrChapter = currentPage,
                                        title = "Page ${currentPage + 1}"
                                    )
                                    BookmarkManager.saveBookmark(context, bookmark)
                                    bookmarks = BookmarkManager.getBookmarksForBook(context, filePath)
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isCurrentPageBookmarked) 
                                Icons.Default.Bookmark 
                            else 
                                Icons.Default.BookmarkBorder,
                            contentDescription = if (isCurrentPageBookmarked) 
                                "Remove Bookmark" 
                            else 
                                "Add Bookmark"
                        )
                    }
                    
                    // Show bookmarks list button
                    if (bookmarks.isNotEmpty()) {
                        IconButton(onClick = { showBookmarksDialog = true }) {
                            Badge(
                                containerColor = lightColorScheme.primary
                            ) {
                                Text("${bookmarks.size}")
                            }
                        }
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
                    ) {
                        // PDF Image
                        Image(
                            bitmap = currentBitmap!!.asImageBitmap(),
                            contentDescription = "PDF Page",
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale
                                )
                                .pointerInput(isDrawingMode, isNoteMode) {
                                    if (isDrawingMode) {
                                        // Drawing mode
                                        detectDragGestures(
                                            onDragStart = { offset ->
                                                currentDrawingPath.clear()
                                                currentDrawingPath.add(offset)
                                            },
                                            onDrag = { change, _ ->
                                                currentDrawingPath.add(change.position)
                                            },
                                            onDragEnd = {
                                                // Save the drawing path
                                                if (currentDrawingPath.isNotEmpty()) {
                                                    val path = DrawingPath(
                                                        points = currentDrawingPath.map {
                                                            AnnotationPoint(it.x, it.y)
                                                        },
                                                        color = drawingColor,
                                                        strokeWidth = 5f
                                                    )
                                                    allDrawingPaths = allDrawingPaths + path
                                                    
                                                    // Save to storage
                                                    scope.launch {
                                                        val annotation = PdfAnnotation(
                                                            bookFilePath = filePath,
                                                            pageNumber = currentPage,
                                                            type = AnnotationType.DRAWING,
                                                            color = drawingColor,
                                                            drawingPaths = listOf(path)
                                                        )
                                                        PdfAnnotationManager.saveAnnotation(context, annotation)
                                                        annotations = PdfAnnotationManager.getAnnotationsForPage(context, filePath, currentPage)
                                                    }
                                                    
                                                    currentDrawingPath.clear()
                                                }
                                            }
                                        )
                                    } else if (isNoteMode) {
                                        // Note mode - tap to add note
                                        detectTapGestures { offset ->
                                            notePosition = offset
                                            showNoteDialog = true
                                        }
                                    } else {
                                        // Normal mode - zoom
                                        detectTransformGestures { _, _, zoom, _ ->
                                            scale = (scale * zoom).coerceIn(0.5f, 3f)
                                        }
                                    }
                                },
                            contentScale = ContentScale.FillWidth
                        )
                        
                        // Drawing overlay
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(currentBitmap!!.width.toFloat() / currentBitmap!!.height)
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale
                                )
                        ) {
                            // Draw saved paths
                            allDrawingPaths.forEach { drawingPath ->
                                if (drawingPath.points.size > 1) {
                                    val path = Path()
                                    path.moveTo(drawingPath.points[0].x, drawingPath.points[0].y)
                                    for (i in 1 until drawingPath.points.size) {
                                        path.lineTo(drawingPath.points[i].x, drawingPath.points[i].y)
                                    }
                                    drawPath(
                                        path = path,
                                        color = Color(android.graphics.Color.parseColor(drawingPath.color)),
                                        style = Stroke(
                                            width = drawingPath.strokeWidth,
                                            cap = StrokeCap.Round,
                                            join = StrokeJoin.Round
                                        )
                                    )
                                }
                            }
                            
                            // Draw current path being drawn
                            if (currentDrawingPath.size > 1) {
                                val path = Path()
                                path.moveTo(currentDrawingPath[0].x, currentDrawingPath[0].y)
                                for (i in 1 until currentDrawingPath.size) {
                                    path.lineTo(currentDrawingPath[i].x, currentDrawingPath[i].y)
                                }
                                drawPath(
                                    path = path,
                                    color = Color(android.graphics.Color.parseColor(drawingColor)),
                                    style = Stroke(
                                        width = 5f,
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                )
                            }
                            
                            // Draw note indicators
                            annotations.filter { it.type == AnnotationType.NOTE }.forEach { annotation ->
                                annotation.position?.let { pos ->
                                    drawCircle(
                                        color = Color(android.graphics.Color.parseColor(annotation.color)),
                                        radius = 15f,
                                        center = Offset(pos.x, pos.y)
                                    )
                                }
                            }
                        }
                    }
                    
                    // Drawing toolbar
                    if (isDrawingMode) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.BottomEnd
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = lightColorScheme.surface
                                ),
                                elevation = CardDefaults.cardElevation(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Color options
                                    listOf("#000000", "#FF0000", "#0000FF", "#00FF00", "#FFA500").forEach { color ->
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .background(
                                                    Color(android.graphics.Color.parseColor(color)),
                                                    shape = MaterialTheme.shapes.small
                                                )
                                                .pointerInput(Unit) {
                                                    detectTapGestures {
                                                        drawingColor = color
                                                    }
                                                }
                                        )
                                    }
                                    
                                    // Clear button
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                // Delete all drawings from current page
                                                annotations.filter { it.type == AnnotationType.DRAWING }.forEach {
                                                    PdfAnnotationManager.deleteAnnotation(context, it.id)
                                                }
                                                allDrawingPaths = emptyList()
                                                annotations = PdfAnnotationManager.getAnnotationsForPage(context, filePath, currentPage)
                                            }
                                        }
                                    ) {
                                        Text("Clear")
                                    }
                                }
                            }
                        }
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
    
    // Note Dialog
    if (showNoteDialog) {
        AddNoteDialog(
            initialNote = "",
            onSave = { note ->
                notePosition?.let { pos ->
                    scope.launch {
                        val annotation = PdfAnnotation(
                            bookFilePath = filePath,
                            pageNumber = currentPage,
                            type = AnnotationType.NOTE,
                            note = note,
                            color = "#FFFF00",
                            position = AnnotationPosition(pos.x, pos.y)
                        )
                        PdfAnnotationManager.saveAnnotation(context, annotation)
                        annotations = PdfAnnotationManager.getAnnotationsForPage(context, filePath, currentPage)
                    }
                }
                notePosition = null
            },
            onDismiss = {
                showNoteDialog = false
                notePosition = null
            }
        )
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
                                    renderPage(bookmark.pageOrChapter)
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
                                            text = "Page ${bookmark.pageOrChapter + 1}",
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
