package com.example.reading_app.ui.navigate

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    object Home : BottomNavItem("home", "Trang chủ", Icons.Default.Home)
    object BookStore : BottomNavItem("store", "Cửa hàng", Icons.Default.Store)
    object Library : BottomNavItem("library", "Thư viện", Icons.Default.LibraryBooks)
    object Search : BottomNavItem("search", "Tìm kiếm", Icons.Default.Search)
    object Profile : BottomNavItem("profile", "Cá nhân", Icons.Default.Person)
}
