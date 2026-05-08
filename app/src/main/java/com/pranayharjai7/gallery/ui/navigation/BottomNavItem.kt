package com.pranayharjai7.gallery.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector

data class BottomNavItem(val screen: Screen, val label: String, val icon: ImageVector)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Photos, "Photos", Icons.Default.Favorite),
    BottomNavItem(Screen.Albums, "Albums", Icons.AutoMirrored.Filled.List),
    BottomNavItem(Screen.Search, "Search", Icons.Default.Search),
    BottomNavItem(Screen.More, "More", Icons.Default.MoreVert)
)

val bottomNavRoutes: Set<String> = bottomNavItems.map { it.screen.route }.toHashSet()
