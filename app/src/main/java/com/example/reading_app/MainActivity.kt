package com.example.reading_app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.reading_app.ui.components.BottomNavigationBar
import com.example.reading_app.ui.navigate.BottomNavItem
import com.example.reading_app.ui.screens.BookStoreScreen
import com.example.reading_app.ui.screens.EpubReaderScreen
import com.example.reading_app.ui.screens.HomeScreen
import com.example.reading_app.ui.screens.LibraryScreen
import com.example.reading_app.ui.screens.PdfReaderScreen
import com.example.reading_app.ui.screens.SearchScreen
import com.example.reading_app.ui.theme.Reading_AppTheme
import com.example.reading_app.viewmodel.ReaderViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Reading_AppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val readerViewModel: ReaderViewModel = viewModel()

    androidx.compose.material3.Scaffold(
        bottomBar = {
            val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
            // Hide bottom nav when in reader screens
            if (currentRoute?.startsWith("pdf_reader") != true && 
                currentRoute?.startsWith("epub_reader") != true) {
                BottomNavigationBar(navController = navController)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(BottomNavItem.Home.route) {
                HomeScreen()
            }
            composable(BottomNavItem.BookStore.route) {
                BookStoreScreen()
            }
            composable(BottomNavItem.Library.route) {
                LibraryScreen(
                    onBookSelected = { book ->
                        // Store selected book in ViewModel
                        readerViewModel.selectBookForReading(book)
                        when (book.type) {
                            com.example.reading_app.model.BookType.PDF -> {
                                navController.navigate("pdf_reader")
                            }
                            com.example.reading_app.model.BookType.EPUB -> {
                                navController.navigate("epub_reader")
                            }
                        }
                    },
                    readerViewModel = readerViewModel
                )
            }
            composable(BottomNavItem.Search.route) {
                SearchScreen()
            }
            
            // PDF Reader Screen
            composable(route = "pdf_reader") {
                val currentBook = readerViewModel.currentBook
                if (currentBook != null) {
                    PdfReaderScreen(
                        bookTitle = currentBook.title,
                        filePath = currentBook.filePath,
                        onBackClick = { 
                            readerViewModel.clearCurrentBook()
                            navController.popBackStack() 
                        }
                    )
                } else {
                    // Fallback if no book selected
                    navController.popBackStack()
                }
            }
            
            // EPUB Reader Screen
            composable(route = "epub_reader") {
                val currentBook = readerViewModel.currentBook
                if (currentBook != null) {
                    EpubReaderScreen(
                        bookTitle = currentBook.title,
                        filePath = currentBook.filePath,
                        onBackClick = { 
                            readerViewModel.clearCurrentBook()
                            navController.popBackStack() 
                        }
                    )
                } else {
                    // Fallback if no book selected
                    navController.popBackStack()
                }
            }
        }
    }
}


