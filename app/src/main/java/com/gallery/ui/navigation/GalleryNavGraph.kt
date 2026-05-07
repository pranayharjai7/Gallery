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
import com.gallery.ui.albums.AlbumDetailScreen
import com.gallery.ui.albums.AlbumsScreen
import com.gallery.ui.photos.PhotosScreen
import com.gallery.ui.search.SearchScreen
import com.gallery.ui.viewer.ViewerScreen

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
        composable(Screen.Photos.route) {
            PhotosScreen(
                onMediaClick = { mediaId -> navController.navigate(Screen.Viewer(mediaId).route) },
                onNavigateToEditor = { mediaId, _ -> navController.navigate(Screen.Viewer(mediaId).route) }
            )
        }
        composable(Screen.Albums.route) {
            AlbumsScreen(
                onAlbumClick = { albumId ->
                    navController.navigate(Screen.AlbumDetail(albumId).route)
                }
            )
        }
        composable(Screen.Search.route) {
            SearchScreen(
                onMediaClick = { mediaId ->
                    navController.navigate(Screen.Viewer(mediaId).route)
                }
            )
        }
        composable(Screen.More.route) { MoreScreen(navController) }
        composable(Screen.Trash.route) { TrashScreen(navController) }
        composable(Screen.Hidden.route) { HiddenAlbumScreen(navController) }
        composable(Screen.Settings.route) { SettingsScreen(navController) }
        composable(
            route = Screen.Viewer.ROUTE,
            arguments = listOf(navArgument("mediaId") { type = NavType.LongType })
        ) { backStackEntry ->
            val mediaId = backStackEntry.arguments!!.getLong("mediaId")
            ViewerScreen(
                mediaId = mediaId,
                onBack = { navController.popBackStack() },
                onEdit = { id, isVideo ->
                    if (isVideo) navController.navigate(Screen.VideoEdit(id).route)
                    else navController.navigate(Screen.PhotoEdit(id).route)
                }
            )
        }
        composable(
            route = Screen.AlbumDetail.ROUTE,
            arguments = listOf(navArgument("albumId") { type = NavType.StringType })
        ) { backStackEntry ->
            val albumId = backStackEntry.arguments?.getString("albumId") ?: return@composable
            AlbumDetailScreen(
                albumId = albumId,
                onBack = { navController.popBackStack() },
                onMediaClick = { mediaId ->
                    navController.navigate(Screen.Viewer(mediaId).route)
                }
            )
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
