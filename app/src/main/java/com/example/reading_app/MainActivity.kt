package com.example.reading_app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.reading_app.model.BookType
import com.example.reading_app.ui.screens.EpubReaderScreen
import com.example.reading_app.ui.screens.HomeScreen
import com.example.reading_app.ui.screens.PdfReaderScreen
import com.example.reading_app.ui.theme.Reading_AppTheme
import com.example.reading_app.viewmodel.ReaderViewModel
import android.net.Uri

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Reading_AppTheme {
                ReaderApp()
            }
        }
    }
}

@Composable
fun ReaderApp(viewModel: ReaderViewModel = viewModel()) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                viewModel = viewModel,
                onBookSelected = { book ->
                    // Encode both title and filePath to handle special characters
                    val encodedTitle = Uri.encode(book.title)
                    val encodedPath = Uri.encode(book.filePath)
                    when (book.type) {
                        BookType.PDF -> navController.navigate("pdf/$encodedTitle/$encodedPath")
                        BookType.EPUB -> navController.navigate("epub/$encodedTitle/$encodedPath")
                    }
                }
            )
        }

        composable(
            route = "pdf/{title}/{filePath}",
            arguments = listOf(
                navArgument("title") { 
                    type = NavType.StringType
                    nullable = false
                },
                navArgument("filePath") { 
                    type = NavType.StringType
                    nullable = false
                }
            )
        ) { backStackEntry ->
            val title = Uri.decode(backStackEntry.arguments?.getString("title") ?: "")
            val filePath = Uri.decode(backStackEntry.arguments?.getString("filePath") ?: "")

            PdfReaderScreen(
                bookTitle = title,
                filePath = filePath,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = "epub/{title}/{filePath}",
            arguments = listOf(
                navArgument("title") { 
                    type = NavType.StringType
                    nullable = false
                },
                navArgument("filePath") { 
                    type = NavType.StringType
                    nullable = false
                }
            )
        ) { backStackEntry ->
            val title = Uri.decode(backStackEntry.arguments?.getString("title") ?: "")
            val filePath = Uri.decode(backStackEntry.arguments?.getString("filePath") ?: "")

            EpubReaderScreen(
                bookTitle = title,
                filePath = filePath,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}