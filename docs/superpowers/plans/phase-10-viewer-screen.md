# Phase 10: Viewer Screen — Gallery App

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Full-screen immersive media viewer with HorizontalPager, zoom, overlays, share/delete/favorite, and ExoPlayer video playback.

**Depends on:** Phase 06, Phase 07, Phase 01 use cases

---

## Overview

The Viewer Screen provides a full-screen immersive experience for browsing individual media items. Users can swipe horizontally through the entire media library, pinch-to-zoom on photos (up to 5×), watch videos via ExoPlayer, and perform actions (share, edit, favorite, delete) via animated overlay panels. A horizontal thumbnail strip at the bottom provides quick navigation context.

---

## File Structure

```
app/src/main/kotlin/com/gallery/ui/viewer/
  ViewerViewModel.kt
  ViewerScreen.kt
  VideoPlayer.kt

app/src/test/kotlin/com/gallery/ui/viewer/
  ViewerViewModelTest.kt
```

---

## Task 1: ViewerViewModel + Unit Tests

**File:** `app/src/main/kotlin/com/gallery/ui/viewer/ViewerViewModel.kt`

Create the ViewModel that manages pager state, overlay visibility, favorites tracking, and deletion.

```kotlin
package com.gallery.ui.viewer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gallery.domain.GetAllMediaUseCase
import com.gallery.domain.GetFavoritesUseCase
import com.gallery.domain.MoveToTrashUseCase
import com.gallery.domain.ToggleFavoriteUseCase
import com.gallery.model.MediaItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ViewerUiState(
    val isLoading: Boolean = true,
    val items: List<MediaItem> = emptyList(),
    val currentIndex: Int = 0,
    val overlaysVisible: Boolean = true,
    val favoriteIds: Set<Long> = emptySet(),
    val error: String? = null
)

@HiltViewModel
class ViewerViewModel @Inject constructor(
    private val getAllMedia: GetAllMediaUseCase,
    private val getFavorites: GetFavoritesUseCase,
    private val moveToTrash: MoveToTrashUseCase,
    private val toggleFavorite: ToggleFavoriteUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ViewerUiState())
    val uiState: StateFlow<ViewerUiState> = _uiState.asStateFlow()

    fun loadMedia(startMediaId: Long) {
        viewModelScope.launch {
            combine(getAllMedia(), getFavorites()) { items, favs ->
                val index = items.indexOfFirst { it.id == startMediaId }.coerceAtLeast(0)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        items = items,
                        currentIndex = index,
                        favoriteIds = favs.map { f -> f.id }.toSet()
                    )
                }
            }.collect()
        }
    }

    fun onPageChanged(index: Int) = _uiState.update { it.copy(currentIndex = index) }

    fun toggleOverlays() = _uiState.update { it.copy(overlaysVisible = !it.overlaysVisible) }

    fun toggleFavorite(id: Long) {
        viewModelScope.launch { toggleFavorite.invoke(id) }
    }

    fun deleteCurrentItem() {
        val item = _uiState.value.items.getOrNull(_uiState.value.currentIndex) ?: return
        viewModelScope.launch { moveToTrash(item) }
    }
}
```

**File:** `app/src/test/kotlin/com/gallery/ui/viewer/ViewerViewModelTest.kt`

```kotlin
package com.gallery.ui.viewer

import android.net.Uri
import app.cash.turbine.test
import com.gallery.domain.GetAllMediaUseCase
import com.gallery.domain.GetFavoritesUseCase
import com.gallery.domain.MoveToTrashUseCase
import com.gallery.domain.ToggleFavoriteUseCase
import com.gallery.model.MediaItem
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ViewerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getAllMedia: GetAllMediaUseCase = mockk()
    private val getFavorites: GetFavoritesUseCase = mockk()
    private val moveToTrash: MoveToTrashUseCase = mockk(relaxed = true)
    private val toggleFavorite: ToggleFavoriteUseCase = mockk(relaxed = true)

    private fun buildVm() = ViewerViewModel(getAllMedia, getFavorites, moveToTrash, toggleFavorite)

    @Test
    fun `loadMedia sets correct startIndex`() = runTest {
        val items = listOf(
            MediaItem(1L, Uri.EMPTY, "a.jpg", 0, 0, 0, 0, null, "image/jpeg", 1L, "", null),
            MediaItem(2L, Uri.EMPTY, "b.jpg", 0, 0, 0, 0, null, "image/jpeg", 1L, "", null)
        )
        every { getAllMedia() } returns flowOf(items)
        every { getFavorites() } returns flowOf(emptyList())

        val vm = buildVm()
        vm.uiState.test {
            vm.loadMedia(startMediaId = 2L)
            val initial = awaitItem() // initial loading state
            val loaded = awaitItem()
            assertEquals(1, loaded.currentIndex)
            assertFalse(loaded.isLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `loadMedia falls back to index 0 when id not found`() = runTest {
        val items = listOf(
            MediaItem(1L, Uri.EMPTY, "a.jpg", 0, 0, 0, 0, null, "image/jpeg", 1L, "", null)
        )
        every { getAllMedia() } returns flowOf(items)
        every { getFavorites() } returns flowOf(emptyList())

        val vm = buildVm()
        vm.uiState.test {
            vm.loadMedia(startMediaId = 999L)
            awaitItem()
            val loaded = awaitItem()
            assertEquals(0, loaded.currentIndex)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `toggleOverlays flips visibility`() = runTest {
        every { getAllMedia() } returns flowOf(emptyList())
        every { getFavorites() } returns flowOf(emptyList())

        val vm = buildVm()
        assertTrue(vm.uiState.value.overlaysVisible)
        vm.toggleOverlays()
        assertFalse(vm.uiState.value.overlaysVisible)
        vm.toggleOverlays()
        assertTrue(vm.uiState.value.overlaysVisible)
    }

    @Test
    fun `onPageChanged updates currentIndex`() = runTest {
        every { getAllMedia() } returns flowOf(emptyList())
        every { getFavorites() } returns flowOf(emptyList())

        val vm = buildVm()
        vm.onPageChanged(3)
        assertEquals(3, vm.uiState.value.currentIndex)
    }

    @Test
    fun `favoriteIds populated from getFavorites`() = runTest {
        val favs = listOf(
            MediaItem(42L, Uri.EMPTY, "fav.jpg", 0, 0, 0, 0, null, "image/jpeg", 1L, "", null)
        )
        every { getAllMedia() } returns flowOf(emptyList())
        every { getFavorites() } returns flowOf(favs)

        val vm = buildVm()
        vm.uiState.test {
            vm.loadMedia(startMediaId = 0L)
            awaitItem()
            val loaded = awaitItem()
            assertTrue(loaded.favoriteIds.contains(42L))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `deleteCurrentItem calls moveToTrash with current item`() = runTest {
        val item = MediaItem(7L, Uri.EMPTY, "del.jpg", 0, 0, 0, 0, null, "image/jpeg", 1L, "", null)
        every { getAllMedia() } returns flowOf(listOf(item))
        every { getFavorites() } returns flowOf(emptyList())

        val vm = buildVm()
        vm.uiState.test {
            vm.loadMedia(startMediaId = 7L)
            awaitItem()
            awaitItem() // wait for loaded state
            vm.deleteCurrentItem()
            cancelAndIgnoreRemainingEvents()
        }
        verify { moveToTrash.invoke(item) }
    }
}
```

Run tests:

```bash
./gradlew :app:testDebugUnitTest --tests "com.gallery.ui.viewer.*"
```

Commit message: `feat(viewer): add ViewerViewModel with loadMedia, toggleOverlays, delete, favorite + unit tests`

---

## Task 2: VideoPlayer Composable

**File:** `app/src/main/kotlin/com/gallery/ui/viewer/VideoPlayer.kt`

Wraps Media3 ExoPlayer in an `AndroidView`-based composable. The player is created once per URI via `remember(uri)`, auto-plays on load, and is released via `DisposableEffect` to prevent resource leaks.

```kotlin
package com.gallery.ui.viewer

import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

@Composable
fun VideoPlayer(uri: Uri, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val exoPlayer = remember(uri) {
        ExoPlayer.Builder(context).build().also { player ->
            player.setMediaItem(MediaItem.fromUri(uri))
            player.prepare()
            player.playWhenReady = true
        }
    }
    DisposableEffect(exoPlayer) {
        onDispose { exoPlayer.release() }
    }
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = true
            }
        },
        modifier = modifier.fillMaxSize()
    )
}
```

**Notes:**
- `remember(uri)` ensures a new player instance is created whenever the URI changes (e.g., when the pager swipes to a different video), keeping each page's player independent.
- `DisposableEffect` guarantees `exoPlayer.release()` is called when the composable leaves the composition, preventing audio/video codec leaks.
- `useController = true` shows ExoPlayer's built-in playback controls. To replace them with custom Compose controls, set to `false` and overlay your own buttons.

Commit message: `feat(viewer): add VideoPlayer composable with ExoPlayer/Media3 and DisposableEffect cleanup`

---

## Task 3: ViewerScreen

**File:** `app/src/main/kotlin/com/gallery/ui/viewer/ViewerScreen.kt`

Full-screen viewer with `HorizontalPager`, pinch-to-zoom for images (capped at 5×), animated top/bottom overlays, action buttons (share, edit, favorite, delete), and a thumbnail strip.

```kotlin
package com.gallery.ui.viewer

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.gallery.model.isVideo
import com.gallery.ui.common.EmptyState
import com.gallery.ui.common.LoadingState
import com.gallery.ui.common.MediaThumbnail
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerScreen(
    mediaId: Long,
    onBack: () -> Unit,
    onEdit: (Long, Boolean) -> Unit,
    viewModel: ViewerViewModel = hiltViewModel()
) {
    LaunchedEffect(mediaId) { viewModel.loadMedia(mediaId) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    val pagerState = rememberPagerState(
        initialPage = uiState.currentIndex,
        pageCount = { uiState.items.size }
    )

    LaunchedEffect(pagerState.currentPage) {
        viewModel.onPageChanged(pagerState.currentPage)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { viewModel.toggleOverlays() }
    ) {
        if (uiState.isLoading) {
            LoadingState()
        } else {
            // ── Pager ──────────────────────────────────────────────────────────
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val item = uiState.items[page]
                if (item.isVideo) {
                    VideoPlayer(uri = item.uri)
                } else {
                    var scale by remember { mutableFloatStateOf(1f) }
                    var offset by remember { mutableStateOf(Offset.Zero) }
                    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
                        scale = (scale * zoomChange).coerceIn(1f, 5f)
                        offset += offsetChange
                    }
                    AsyncImage(
                        model = item.uri,
                        contentDescription = item.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .transformable(transformState)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationX = offset.x
                                translationY = offset.y
                            }
                    )
                }
            }

            // ── Top overlay: counter + back ────────────────────────────────────
            AnimatedVisibility(
                visible = uiState.overlaysVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                TopAppBar(
                    title = {
                        Text(
                            text = "${uiState.currentIndex + 1} / ${uiState.items.size}",
                            color = Color.White
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Black.copy(alpha = 0.5f)
                    )
                )
            }

            // ── Bottom overlay: actions + thumbnail strip ──────────────────────
            AnimatedVisibility(
                visible = uiState.overlaysVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                val currentItem = uiState.items.getOrNull(uiState.currentIndex)
                val ctx = LocalContext.current

                Column(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(bottom = 16.dp)
                ) {
                    // Action row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Share
                        IconButton(onClick = {
                            currentItem?.let { item ->
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = item.mimeType
                                    putExtra(Intent.EXTRA_STREAM, item.uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                ctx.startActivity(Intent.createChooser(intent, "Share"))
                            }
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                        }
                        // Edit
                        IconButton(onClick = {
                            currentItem?.let { onEdit(it.id, it.isVideo) }
                        }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.White)
                        }
                        // Favorite
                        IconButton(onClick = {
                            currentItem?.let { viewModel.toggleFavorite(it.id) }
                        }) {
                            val isFav = currentItem?.id in uiState.favoriteIds
                            Icon(
                                imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (isFav) Color.Red else Color.White
                            )
                        }
                        // Delete
                        IconButton(onClick = { viewModel.deleteCurrentItem() }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
                        }
                    }

                    // Thumbnail strip
                    LazyRow(
                        modifier = Modifier
                            .height(56.dp)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(uiState.items.size) { idx ->
                            val item = uiState.items[idx]
                            AsyncImage(
                                model = item.uri,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .width(56.dp)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(2.dp))
                                    .border(
                                        width = if (idx == uiState.currentIndex) 2.dp else 0.dp,
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(2.dp)
                                    )
                                    .clickable {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(idx)
                                        }
                                    }
                            )
                        }
                    }
                }
            }
        }
    }
}
```

**Implementation notes:**
- Tap on the `Box` root toggles overlays. Child buttons consume their own touch events and do not propagate up, so tapping an action button does not also toggle the overlay.
- Pinch-to-zoom state (`scale`, `offset`) is declared with `remember` inside the pager lambda. Each page gets its own independent zoom state; navigating to a new page resets zoom automatically.
- The thumbnail strip `clickable` launches `pagerState.animateScrollToPage(idx)` inside a `rememberCoroutineScope` scope — this is required because `animateScrollToPage` is a `suspend` function.
- `AsyncImage` is from the Coil Compose library. Thumbnails are loaded lazily and cached by Coil's memory and disk cache.

Run tests:

```bash
./gradlew :app:testDebugUnitTest --tests "com.gallery.ui.viewer.*"
```

Commit message: `feat(viewer): add ViewerScreen with HorizontalPager, pinch-to-zoom, animated overlays, share/delete/favorite actions, and thumbnail strip`

---

## Dependencies to Verify in `build.gradle.kts`

```kotlin
// Media3 / ExoPlayer
implementation("androidx.media3:media3-exoplayer:1.4.0")
implementation("androidx.media3:media3-ui:1.4.0")
implementation("androidx.media3:media3-common:1.4.0")

// Compose Foundation (includes HorizontalPager)
implementation("androidx.compose.foundation:foundation") // included in BOM

// Coil for AsyncImage
implementation("io.coil-kt.coil3:coil-compose:<version>")

// Hilt Navigation Compose
implementation("androidx.hilt:hilt-navigation-compose:<version>")

// Lifecycle collectAsStateWithLifecycle
implementation("androidx.lifecycle:lifecycle-runtime-compose:<version>")

// Turbine (test)
testImplementation("app.cash.turbine:turbine:<version>")

// MockK (test)
testImplementation("io.mockk:mockk:<version>")

// Coroutines test
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:<version>")
```

---

## Navigation Wiring

Register the route in the NavHost (e.g., `AppNavigation.kt`):

```kotlin
composable(
    route = "viewer/{mediaId}",
    arguments = listOf(navArgument("mediaId") { type = NavType.LongType })
) { backStackEntry ->
    val mediaId = backStackEntry.arguments!!.getLong("mediaId")
    ViewerScreen(
        mediaId = mediaId,
        onBack = { navController.popBackStack() },
        onEdit = { id, isVideo -> navController.navigate("editor/$id?isVideo=$isVideo") }
    )
}
```

Callers navigate to the viewer with:

```kotlin
navController.navigate("viewer/${item.id}")
```

---

## Acceptance Criteria

- [ ] Swiping left/right pages through all media items via `HorizontalPager`
- [ ] Tapping anywhere (not on a button) toggles overlay visibility with fade animation
- [ ] Pinch-to-zoom on photos works up to 5× and resets when navigating to the next item
- [ ] Videos play automatically via ExoPlayer with built-in playback controls
- [ ] Share icon launches the system share sheet with the correct MIME type and URI
- [ ] Edit icon navigates to the editor passing correct `mediaId` and `isVideo` flag
- [ ] Favorite icon toggles filled/outlined state with red tint; calls `ToggleFavoriteUseCase`
- [ ] Delete icon calls `MoveToTrashUseCase` for the current item
- [ ] Thumbnail strip highlights the current item with a primary-color border
- [ ] Tapping a thumbnail scrolls the pager to that index
- [ ] Top overlay shows "current / total" counter (e.g., "3 / 47")
- [ ] All `ViewerViewModelTest` tests pass
