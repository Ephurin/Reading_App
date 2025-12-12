package com.example.reading_app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.example.reading_app.viewmodel.ThemeViewModel
import com.example.reading_app.viewmodel.BookStoreViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val themeViewModel: ThemeViewModel = viewModel()
            val useDarkTheme by themeViewModel.isDarkTheme.collectAsState()
            Reading_AppTheme(darkTheme = useDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ReadingApp(themeViewModel = themeViewModel)
                }
            }
        }
    }
}

@Composable
fun ReadingApp(themeViewModel: ThemeViewModel) {
    val rootNavController = rememberNavController()
    val bookStoreViewModel: BookStoreViewModel = viewModel()

    NavHost(navController = rootNavController, startDestination = "login") {
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    rootNavController.navigate(route = "main") { 
                        popUpTo(route = "login") { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    rootNavController.navigate(route = "register")
                }
            )
        }
        composable("register") {
            RegisterScreen(
                onRegisterSuccess = { rootNavController.popBackStack() },
                onNavigateBack = { rootNavController.popBackStack() }
            )
        }
        composable("main") {
            MainScreen(
                rootNavController = rootNavController, 
                themeViewModel = themeViewModel,
                bookStoreViewModel = bookStoreViewModel
            )
        }
    }
}

@Composable
fun MainScreen(
    rootNavController: NavHostController, 
    themeViewModel: ThemeViewModel, 
    bookStoreViewModel: BookStoreViewModel
) {
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
                HomeScreen(
                    recentBooks = readerViewModel.recentBooks,
                    onBookSelected = { book ->
                        readerViewModel.selectBookForReading(book)
                        when (book.type) {
                            BookType.PDF -> mainNavController.navigate(route = "pdf_reader")
                            BookType.EPUB -> mainNavController.navigate(route = "epub_reader")
                        }
                    },
                    onOnlineBookSelected = { onlineBook ->
                        bookStoreViewModel.select(onlineBook)
                        mainNavController.navigate("book_details")
                    },
                    bookStoreViewModel = bookStoreViewModel
                )
            }
            composable(BottomNavItem.BookStore.route) {
                BookStoreScreen(navController = mainNavController, bookStoreViewModel = bookStoreViewModel)
            }
            composable(BottomNavItem.Library.route) {
                LibraryScreen(
                    onBookSelected = { book ->
                        readerViewModel.selectBookForReading(book)
                        when (book.type) {
                            BookType.PDF -> mainNavController.navigate(route = "pdf_reader")
                            BookType.EPUB -> mainNavController.navigate(route = "epub_reader")
                        }
                    },
                    readerViewModel = readerViewModel
                )
            }
            composable(BottomNavItem.Search.route) {
                SearchScreen(bookStoreViewModel = bookStoreViewModel)
            }
            composable(BottomNavItem.Profile.route) {
                ProfileScreen(
                    onLogoutClick = {
                        rootNavController.navigate(route = "login") {
                            popUpTo(route = "main") { inclusive = true }
                        }
                    },
                    onNavigateToAccountSettings = { mainNavController.navigate("account_settings") },
                    onNavigateToPurchaseManagement = { mainNavController.navigate("purchase_management") },
                    readerViewModel = readerViewModel
                )
            }

            composable("pdf_reader") {
                val bookToOpen = readerViewModel.selectedBook ?: readerViewModel.currentBook
                if (bookToOpen != null) {
                    PdfReaderScreen(
                        bookTitle = bookToOpen.title,
                        filePath = bookToOpen.filePath,
                        onBackClick = {
                            readerViewModel.clearSelection()
                            mainNavController.navigate(route = BottomNavItem.Library.route) {
                                popUpTo(route = BottomNavItem.Library.route) { inclusive = true }
                            }
                        },
                        readerViewModel = readerViewModel
                    )
                } else {
                    mainNavController.popBackStack()
                }
            }

            composable("epub_reader") {
                val bookToOpen = readerViewModel.selectedBook ?: readerViewModel.currentBook
                if (bookToOpen != null) {
                    EpubReaderScreen(
                        bookTitle = bookToOpen.title,
                        filePath = bookToOpen.filePath,
                        onBackClick = {
                            readerViewModel.clearSelection()
                            mainNavController.navigate(route = BottomNavItem.Library.route) {
                                popUpTo(route = BottomNavItem.Library.route) { inclusive = true }
                            }
                        },
                        readerViewModel = readerViewModel
                    )
                } else {
                    mainNavController.popBackStack()
                }
            }
            composable("book_details") {
                BookDetailsScreen(navController = mainNavController, bookStoreViewModel = bookStoreViewModel, readerViewModel = readerViewModel)
            }
            composable("account_settings") {
                AccountSettingsScreen(
                    onNavigateBack = { mainNavController.popBackStack() },
                    onNavigateToEditProfile = { mainNavController.navigate("edit_profile") },
                    themeViewModel = themeViewModel
                )
            }
            composable("purchase_management") {
                PurchaseManagementScreen(onNavigateBack = { mainNavController.popBackStack() })
            }
            composable("edit_profile") {
                EditProfileScreen(onNavigateBack = { mainNavController.popBackStack() })
            }
        }
    }
}
