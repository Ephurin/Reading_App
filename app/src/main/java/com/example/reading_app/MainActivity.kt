package com.example.reading_app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.reading_app.model.BookType
import com.example.reading_app.ui.components.BottomNavigationBar
import com.example.reading_app.ui.navigate.BottomNavItem
import com.example.reading_app.ui.screens.*
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
                    ReadingApp()
                }
            }
        }
    }
}

@Composable
fun ReadingApp() {
    val rootNavController = rememberNavController()

    NavHost(navController = rootNavController, startDestination = "login") {
        composable("login") {
            LoginScreen(
                onLoginClick = {
                    rootNavController.navigate("main") { 
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    rootNavController.navigate("register")
                },
                onSkipClick = {
                    rootNavController.navigate("main") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        composable("register") {
            RegisterScreen(
                onRegisterClick = { rootNavController.popBackStack() },
                onNavigateBack = { rootNavController.popBackStack() }
            )
        }
        composable("main") {
            MainScreen(rootNavController = rootNavController)
        }
    }
}

@Composable
fun MainScreen(rootNavController: NavHostController) {
    val mainNavController = rememberNavController()
    val readerViewModel: ReaderViewModel = viewModel()

    Scaffold(
        bottomBar = {
            val currentRoute = mainNavController.currentBackStackEntryAsState().value?.destination?.route
            if (currentRoute?.startsWith("pdf_reader") != true &&
                currentRoute?.startsWith("epub_reader") != true) {
                BottomNavigationBar(navController = mainNavController)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = mainNavController,
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
                        readerViewModel.selectBookForReading(book)
                        when (book.type) {
                            BookType.PDF -> mainNavController.navigate("pdf_reader")
                            BookType.EPUB -> mainNavController.navigate("epub_reader")
                        }
                    }
                )
            }
            composable(BottomNavItem.Search.route) {
                SearchScreen()
            }
            composable(BottomNavItem.Profile.route) {
                ProfileScreen(onLogoutClick = {
                    rootNavController.navigate("login") {
                        popUpTo("main") { inclusive = true }
                    }
                })
            }

            composable("pdf_reader") {
                val currentBook = readerViewModel.currentBook
                if (currentBook != null) {
                    PdfReaderScreen(
                        bookTitle = currentBook.title,
                        filePath = currentBook.filePath,
                        onBackClick = { 
                            readerViewModel.clearCurrentBook()
                            mainNavController.popBackStack() 
                        }
                    )
                } else {
                    mainNavController.popBackStack()
                }
            }

            composable("epub_reader") {
                val currentBook = readerViewModel.currentBook
                if (currentBook != null) {
                    EpubReaderScreen(
                        bookTitle = currentBook.title,
                        filePath = currentBook.filePath,
                        onBackClick = { 
                            readerViewModel.clearCurrentBook()
                            mainNavController.popBackStack() 
                        }
                    )
                } else {
                    mainNavController.popBackStack()
                }
            }
        }
    }
}
