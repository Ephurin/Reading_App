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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.reading_app.ui.components.BottomNavigationBar
import com.example.reading_app.ui.navigate.BottomNavItem
import com.example.reading_app.ui.screens.BookStoreScreen
import com.example.reading_app.ui.screens.HomeScreen
import com.example.reading_app.ui.screens.LibraryScreen
import com.example.reading_app.ui.screens.SearchScreen
import com.example.reading_app.ui.theme.Reading_AppTheme

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

    androidx.compose.material3.Scaffold(
        bottomBar = {
            BottomNavigationBar(navController = navController)
        }
    ) { innerPadding -> // đây là PaddingValues
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding) // ✅ thêm dòng này để tránh bị che
        ) {
            composable(BottomNavItem.Home.route) {
                HomeScreen()
            }
            composable(BottomNavItem.BookStore.route) {
                BookStoreScreen()
            }
            composable(BottomNavItem.Library.route) {
                LibraryScreen()
            }
            composable(BottomNavItem.Search.route) {
                SearchScreen()
            }
        }
    }
}


