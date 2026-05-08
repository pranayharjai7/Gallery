package com.pranayharjai7.gallery.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.pranayharjai7.gallery.ui.navigation.GalleryNavGraph
import com.pranayharjai7.gallery.ui.navigation.bottomNavItems
import com.pranayharjai7.gallery.ui.navigation.bottomNavRoutes

@Composable
fun GalleryScaffold() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in bottomNavRoutes
    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val onClick = remember(navController, item.screen.route) {
                            {
                                navController.navigate(item.screen.route) {
                                    launchSingleTop = true
                                    restoreState = true
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                }
                            }
                        }
                        NavigationBarItem(
                            selected = currentRoute == item.screen.route,
                            onClick = onClick,
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        GalleryNavGraph(navController = navController, modifier = Modifier.padding(innerPadding))
    }
}
