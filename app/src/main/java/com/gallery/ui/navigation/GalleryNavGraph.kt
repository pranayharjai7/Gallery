package com.gallery.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.gallery.ui.albums.AlbumDetailScreen
import com.gallery.ui.albums.AlbumsScreen
import com.gallery.ui.hidden.HiddenAlbumScreen
import com.gallery.ui.more.MoreScreen
import com.gallery.ui.photos.PhotosScreen
import com.gallery.ui.search.SearchScreen
import com.gallery.ui.settings.SettingsScreen
import com.gallery.ui.trash.TrashScreen
import com.gallery.ui.editor.photo.PhotoEditScreen
import com.gallery.ui.editor.video.VideoEditScreen
import com.gallery.ui.viewer.ViewerScreen
import com.gallery.ui.memories.SlideshowScreen

@Composable
fun GalleryNavGraph(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(navController = navController, startDestination = Screen.Photos.route, modifier = modifier) {
        composable(Screen.Photos.route) {
            PhotosScreen(
                onMediaClick = { mediaId -> navController.navigate(Screen.Viewer(mediaId).route) },
                onNavigateToEditor = { mediaId, _ -> navController.navigate(Screen.Viewer(mediaId).route) },
                onNavigateToSlideshow = { navController.navigate(Screen.Slideshow("today").route) }
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
        composable(Screen.More.route) {
            MoreScreen(onNavigate = { route -> navController.navigate(route) })
        }
        composable(Screen.Trash.route) { TrashScreen(onBack = { navController.popBackStack() }) }
        composable(Screen.Hidden.route) {
            HiddenAlbumScreen(
                onBack = { navController.popBackStack() },
                onMediaClick = { mediaId -> navController.navigate(Screen.Viewer(mediaId).route) }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
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
            PhotoEditScreen(mediaId = mediaId, onBack = { navController.popBackStack() })
        }
        composable(
            route = Screen.VideoEdit.ROUTE,
            arguments = listOf(navArgument("mediaId") { type = NavType.LongType })
        ) { backStackEntry ->
            val mediaId = backStackEntry.arguments!!.getLong("mediaId")
            VideoEditScreen(mediaId = mediaId, onBack = { navController.popBackStack() })
        }
        composable(
            route = Screen.Slideshow.ROUTE,
            arguments = listOf(navArgument("date") { type = NavType.StringType })
        ) {
            SlideshowScreen(onBack = { navController.popBackStack() })
        }
    }
}
