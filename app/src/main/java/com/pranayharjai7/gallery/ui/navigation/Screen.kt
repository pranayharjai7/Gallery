package com.pranayharjai7.gallery.ui.navigation

sealed class Screen(val route: String) {
    object Photos : Screen("photos")
    object Albums : Screen("albums")
    object Search : Screen("search")
    object More : Screen("more")
    object Trash : Screen("trash")
    object Hidden : Screen("hidden")
    object Settings : Screen("settings")
    data class Viewer(val mediaId: Long) : Screen("viewer/$mediaId") {
        companion object { const val ROUTE = "viewer/{mediaId}" }
    }
    data class AlbumDetail(val albumId: String) : Screen("album/$albumId") {
        companion object { const val ROUTE = "album/{albumId}" }
    }
    data class PhotoEdit(val mediaId: Long) : Screen("edit/photo/$mediaId") {
        companion object { const val ROUTE = "edit/photo/{mediaId}" }
    }
    data class VideoEdit(val mediaId: Long) : Screen("edit/video/$mediaId") {
        companion object { const val ROUTE = "edit/video/{mediaId}" }
    }
    data class Slideshow(val date: String) : Screen("slideshow/$date") {
        companion object { const val ROUTE = "slideshow/{date}" }
    }
}
