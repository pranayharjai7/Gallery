# Phase 6: Theme, Navigation & MainActivity — Gallery App

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` or `superpowers:executing-plans`.

**Goal:** Set up Material You theming with dynamic color, define all navigation routes, build the bottom nav scaffold, and wire MainActivity.

**Depends on:** Phase 5.

---

## Task 6.1: Theme — Color, Type, Theme

### Files to create:
- `app/src/main/kotlin/com/gallery/ui/theme/Color.kt`
- `app/src/main/kotlin/com/gallery/ui/theme/Type.kt`
- `app/src/main/kotlin/com/gallery/ui/theme/Theme.kt`

### Steps

- [ ] Create `app/src/main/kotlin/com/gallery/ui/theme/Color.kt` with the fallback palette (Material You dynamic color overrides these at runtime on API 31+):

```kotlin
package com.gallery.ui.theme

import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF); val PurpleGrey80 = Color(0xFFCCC2DC); val Pink80 = Color(0xFFEFB8C8)
val Purple40 = Color(0xFF6650A4); val PurpleGrey40 = Color(0xFF625B71); val Pink40 = Color(0xFF7D5260)
```

- [ ] Create `app/src/main/kotlin/com/gallery/ui/theme/Type.kt` defining `Typography` using the default Material 3 type scale (no custom fonts required):

```kotlin
package com.gallery.ui.theme

import androidx.compose.material3.Typography

val Typography = Typography()
```

- [ ] Create `app/src/main/kotlin/com/gallery/ui/theme/Theme.kt` with the `GalleryTheme` composable that uses dynamic color on API 31+ and falls back to the static palette below:

```kotlin
package com.gallery.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun GalleryTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        if (darkTheme) darkColorScheme(primary = Purple80, secondary = PurpleGrey80, tertiary = Pink80)
        else lightColorScheme(primary = Purple40, secondary = PurpleGrey40, tertiary = Pink40)
    }
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
```

- [ ] Commit:

```
git add app/src/main/kotlin/com/gallery/ui/theme/
git commit -m "feat: add Material You GalleryTheme with dynamic color and fallback palette"
```

---

## Task 6.2: Navigation Destinations

### File to create:
- `app/src/main/kotlin/com/gallery/ui/navigation/Screen.kt`

### Steps

- [ ] Create `app/src/main/kotlin/com/gallery/ui/navigation/Screen.kt` with a sealed class covering all bottom-tab and stack routes:

```kotlin
package com.gallery.ui.navigation

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
```

- [ ] Commit:

```
git add app/src/main/kotlin/com/gallery/ui/navigation/Screen.kt
git commit -m "feat: add Screen sealed class with all navigation routes"
```

---

## Task 6.3: Bottom Nav Items

### File to create:
- `app/src/main/kotlin/com/gallery/ui/navigation/BottomNavItem.kt`

### Steps

- [ ] Create `app/src/main/kotlin/com/gallery/ui/navigation/BottomNavItem.kt` defining the data class and the four bottom-tab entries:

```kotlin
package com.gallery.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector

data class BottomNavItem(val screen: Screen, val label: String, val icon: ImageVector)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Photos, "Photos", Icons.Default.Image),
    BottomNavItem(Screen.Albums, "Albums", Icons.Default.PhotoLibrary),
    BottomNavItem(Screen.Search, "Search", Icons.Default.Search),
    BottomNavItem(Screen.More, "More", Icons.Default.MoreHoriz)
)
```

- [ ] Commit:

```
git add app/src/main/kotlin/com/gallery/ui/navigation/BottomNavItem.kt
git commit -m "feat: add BottomNavItem definitions for four bottom tabs"
```

---

## Task 6.4: NavGraph

### File to create:
- `app/src/main/kotlin/com/gallery/ui/navigation/GalleryNavGraph.kt`

### Steps

- [ ] For each screen not yet implemented (AlbumsScreen, SearchScreen, MoreScreen, TrashScreen, HiddenAlbumScreen, SettingsScreen, ViewerScreen, AlbumDetailScreen, PhotoEditScreen, VideoEditScreen, SlideshowScreen), add a private stub composable in this file using `Box(Modifier.fillMaxSize()) { Text("TODO: <ScreenName>") }`. PhotosScreen is assumed to already exist from Phase 5; if it does not, add a stub for it as well.

- [ ] Create `app/src/main/kotlin/com/gallery/ui/navigation/GalleryNavGraph.kt` with the complete `NavHost` wiring all routes:

```kotlin
package com.gallery.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

// ---------------------------------------------------------------------------
// Stub composables — replace with real implementations in later phases
// ---------------------------------------------------------------------------

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

// ---------------------------------------------------------------------------
// NavGraph
// ---------------------------------------------------------------------------

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
        composable(Screen.Viewer.ROUTE) { backStackEntry ->
            val mediaId = backStackEntry.arguments?.getString("mediaId")?.toLongOrNull() ?: return@composable
            ViewerScreen(mediaId = mediaId, navController = navController)
        }
        composable(Screen.AlbumDetail.ROUTE) { backStackEntry ->
            val albumId = backStackEntry.arguments?.getString("albumId") ?: return@composable
            AlbumDetailScreen(albumId = albumId, navController = navController)
        }
        composable(Screen.PhotoEdit.ROUTE) { backStackEntry ->
            val mediaId = backStackEntry.arguments?.getString("mediaId")?.toLongOrNull() ?: return@composable
            PhotoEditScreen(mediaId = mediaId, navController = navController)
        }
        composable(Screen.VideoEdit.ROUTE) { backStackEntry ->
            val mediaId = backStackEntry.arguments?.getString("mediaId")?.toLongOrNull() ?: return@composable
            VideoEditScreen(mediaId = mediaId, navController = navController)
        }
        composable(Screen.Slideshow.ROUTE) { backStackEntry ->
            val date = backStackEntry.arguments?.getString("date") ?: return@composable
            SlideshowScreen(date = date, navController = navController)
        }
    }
}
```

> **Note on `PhotosScreen`:** The call `PhotosScreen(navController)` must resolve to the real composable delivered in Phase 5. Verify the correct package import is present at the top of this file. If PhotosScreen was not yet implemented, add a stub identical in shape to the others above.

- [ ] Commit:

```
git add app/src/main/kotlin/com/gallery/ui/navigation/GalleryNavGraph.kt
git commit -m "feat: add GalleryNavGraph with all routes and TODO stub screens"
```

---

## Task 6.5: Main Scaffold with Bottom Nav

### File to create:
- `app/src/main/kotlin/com/gallery/ui/GalleryScaffold.kt`

### Steps

- [ ] Create `app/src/main/kotlin/com/gallery/ui/GalleryScaffold.kt` with the `GalleryScaffold` composable that hosts `GalleryNavGraph` inside a `Scaffold` and conditionally shows the `NavigationBar` for the four bottom-tab routes:

```kotlin
package com.gallery.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.gallery.ui.navigation.GalleryNavGraph
import com.gallery.ui.navigation.bottomNavItems

@Composable
fun GalleryScaffold() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in bottomNavItems.map { it.screen.route }
    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.screen.route,
                            onClick = { navController.navigate(item.screen.route) { launchSingleTop = true; restoreState = true; popUpTo(navController.graph.findStartDestination().id) { saveState = true } } },
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
```

- [ ] Commit:

```
git add app/src/main/kotlin/com/gallery/ui/GalleryScaffold.kt
git commit -m "feat: add GalleryScaffold with bottom NavigationBar and conditional visibility"
```

---

## Task 6.6: Update MainActivity

### File to update:
- `app/src/main/kotlin/com/gallery/MainActivity.kt`

### Steps

- [ ] Open `app/src/main/kotlin/com/gallery/MainActivity.kt` and replace its entire content with:

```kotlin
package com.gallery

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.gallery.ui.GalleryScaffold
import com.gallery.ui.theme.GalleryTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { GalleryTheme { GalleryScaffold() } }
    }
}
```

- [ ] Confirm `@AndroidEntryPoint` is present (required for Hilt injection into the activity and any `@HiltViewModel` in the back stack).
- [ ] Confirm `enableEdgeToEdge()` is called before `setContent` so system bars are transparent and the Scaffold's `innerPadding` handles insets correctly.

- [ ] Commit:

```
git add app/src/main/kotlin/com/gallery/MainActivity.kt
git commit -m "feat: wire MainActivity to GalleryTheme + GalleryScaffold"
```

---

## Task 6.7: Verify Build and Run

### Steps

- [ ] Run a clean debug build to verify compilation:

```
./gradlew :app:assembleDebug
```

Expected: `BUILD SUCCESSFUL` with no errors. Resolve any unresolved reference errors by checking imports in `GalleryNavGraph.kt` (especially `PhotosScreen`) and `GalleryScaffold.kt`.

- [ ] Install on a connected device or emulator:

```
./gradlew :app:installDebug
```

Expected: app installs successfully.

- [ ] Launch the app and verify:
  - [ ] Bottom navigation bar is visible with four tabs: Photos, Albums, Search, More.
  - [ ] Tapping each tab navigates to the corresponding screen showing its "TODO" placeholder text.
  - [ ] Back-stack state is preserved when switching tabs (back button / gesture returns to the previous tab's state, not the start destination).
  - [ ] On a device running Android 12+ (API 31+), the app adopts the system dynamic color scheme (Material You wallpaper-based colors visible in the nav bar selected indicator and other tinted surfaces).
  - [ ] On a device running Android 11 or below, the app falls back to the static purple palette defined in `Color.kt`.
  - [ ] Edge-to-edge rendering is active: content renders behind the navigation bar, and the Scaffold's `innerPadding` prevents content from being obscured.

- [ ] Commit if any fixup changes were required during verification:

```
git add -p
git commit -m "fix: resolve build/runtime issues found during phase 6 verification"
```

---

## Summary of files created / modified in Phase 6

| File | Action |
|---|---|
| `app/src/main/kotlin/com/gallery/ui/theme/Color.kt` | Created |
| `app/src/main/kotlin/com/gallery/ui/theme/Type.kt` | Created |
| `app/src/main/kotlin/com/gallery/ui/theme/Theme.kt` | Created |
| `app/src/main/kotlin/com/gallery/ui/navigation/Screen.kt` | Created |
| `app/src/main/kotlin/com/gallery/ui/navigation/BottomNavItem.kt` | Created |
| `app/src/main/kotlin/com/gallery/ui/navigation/GalleryNavGraph.kt` | Created |
| `app/src/main/kotlin/com/gallery/ui/GalleryScaffold.kt` | Created |
| `app/src/main/kotlin/com/gallery/MainActivity.kt` | Modified |
