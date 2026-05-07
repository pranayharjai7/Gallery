package com.gallery.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

@Composable
private fun PhotosScreen(navController: NavHostController) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("TODO: PhotosScreen") }
}

@Composable
private fun AlbumsScreen(navController: NavHostController) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("TODO: AlbumsScreen") }
}

@Composable
private fun SearchScreen(navController: NavHostController) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("TODO: SearchScreen") }
}

@Composable
private fun MoreScreen(navController: NavHostController) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("TODO: MoreScreen") }
}

@Composable
private fun TrashScreen(navController: NavHostController) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("TODO: TrashScreen") }
}

@Composable
private fun HiddenAlbumScreen(navController: NavHostController) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("TODO: HiddenAlbumScreen") }
}

@Composable
private fun SettingsScreen(navController: NavHostController) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("TODO: SettingsScreen") }
}

@Composable
private fun ViewerScreen(mediaId: Long, navController: NavHostController) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("TODO: ViewerScreen (mediaId=$mediaId)") }
}

@Composable
private fun AlbumDetailScreen(albumId: String, navController: NavHostController) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("TODO: AlbumDetailScreen (albumId=$albumId)") }
}

@Composable
private fun PhotoEditScreen(mediaId: Long, navController: NavHostController) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("TODO: PhotoEditScreen (mediaId=$mediaId)") }
}

@Composable
private fun VideoEditScreen(mediaId: Long, navController: NavHostController) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("TODO: VideoEditScreen (mediaId=$mediaId)") }
}

@Composable
private fun SlideshowScreen(date: String, navController: NavHostController) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("TODO: SlideshowScreen (date=$date)") }
}

@Composable
fun GalleryNavGraph(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(navController = navController, startDestination = Screen.Photos.route, modifier = modifier) {
        composable(Screen.Photos.route) { PhotosScreen(navController) }
        composable(Screen.Albums.route) { AlbumsScreen(navController) }
        composable(Screen.Search.route) { SearchScreen(navController) }
        composable(Screen.More.route) { MoreScreen(navController) }
        composable(Screen.Trash.route) { TrashScreen(navController) }
        composable(Screen.Hidden.route) { HiddenAlbumScreen(navController) }
        composable(Screen.Settings.route) { SettingsScreen(navController) }
        composable(
            route = Screen.Viewer.ROUTE,
            arguments = listOf(navArgument("mediaId") { type = NavType.LongType })
        ) { backStackEntry ->
            val mediaId = backStackEntry.arguments!!.getLong("mediaId")
            ViewerScreen(mediaId = mediaId, navController = navController)
        }
        composable(Screen.AlbumDetail.ROUTE) { backStackEntry ->
            val albumId = backStackEntry.arguments?.getString("albumId") ?: return@composable
            AlbumDetailScreen(albumId = albumId, navController = navController)
        }
        composable(
            route = Screen.PhotoEdit.ROUTE,
            arguments = listOf(navArgument("mediaId") { type = NavType.LongType })
        ) { backStackEntry ->
            val mediaId = backStackEntry.arguments!!.getLong("mediaId")
            PhotoEditScreen(mediaId = mediaId, navController = navController)
        }
        composable(
            route = Screen.VideoEdit.ROUTE,
            arguments = listOf(navArgument("mediaId") { type = NavType.LongType })
        ) { backStackEntry ->
            val mediaId = backStackEntry.arguments!!.getLong("mediaId")
            VideoEditScreen(mediaId = mediaId, navController = navController)
        }
        composable(Screen.Slideshow.ROUTE) { backStackEntry ->
            val date = backStackEntry.arguments?.getString("date") ?: return@composable
            SlideshowScreen(date = date, navController = navController)
        }
    }
}
