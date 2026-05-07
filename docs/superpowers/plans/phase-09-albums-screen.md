# Phase 09: Albums Screen — Gallery App

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Albums tab with 3-col grid, Smart + My Albums sections; Album Detail screen showing media grid.

**Depends on:** Phase 06, Phase 07, Phase 01 use cases

---

## Overview

The Albums screen is a tab that shows all albums on the device divided into two sections: **Smart Albums** (system-generated, e.g. Favorites, Videos, Screenshots) and **My Albums** (user-created buckets). Albums are displayed in a 3-column grid. Tapping an album navigates to an **Album Detail** screen showing all media in that album in a uniform 3-column grid with multi-select support.

---

## File Structure

```
app/src/main/kotlin/com/gallery/ui/albums/
├── AlbumsViewModel.kt          (includes AlbumsUiState)
├── AlbumsScreen.kt             (includes AlbumCard private composable)
├── AlbumDetailViewModel.kt     (includes AlbumDetailUiState)
└── AlbumDetailScreen.kt

app/src/test/kotlin/com/gallery/ui/albums/
├── AlbumsViewModelTest.kt
└── AlbumDetailViewModelTest.kt
```

---

## Dependencies / Imports

- `GetAlbumsUseCase` — Phase 01
- `GetAlbumMediaUseCase` — Phase 01
- `MoveToTrashUseCase` — Phase 01
- `Album`, `AlbumType` — Phase 01 shared types
- `MediaItem`, `isVideo` — Phase 01 shared types
- `MediaThumbnail`, `EmptyState`, `LoadingState`, `SelectionActionBar` — Phase 06 common UI
- `coil.compose.AsyncImage` — Coil dependency (already in project)

---

## Task 1: AlbumsViewModel + Test

### 1a. AlbumsViewModel

**File:** `app/src/main/kotlin/com/gallery/ui/albums/AlbumsViewModel.kt`

```kotlin
package com.gallery.ui.albums

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gallery.domain.model.Album
import com.gallery.domain.model.AlbumType
import com.gallery.domain.usecase.GetAlbumsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AlbumsUiState(
    val isLoading: Boolean = true,
    val smartAlbums: List<Album> = emptyList(),
    val myAlbums: List<Album> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class AlbumsViewModel @Inject constructor(
    private val getAlbums: GetAlbumsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlbumsUiState())
    val uiState: StateFlow<AlbumsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getAlbums().collect { albums ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        smartAlbums = albums.filter { a -> a.type == AlbumType.SMART },
                        myAlbums = albums.filter { a -> a.type == AlbumType.CUSTOM }
                    )
                }
            }
        }
    }
}
```

### 1b. AlbumsViewModel Unit Tests

**File:** `app/src/test/kotlin/com/gallery/ui/albums/AlbumsViewModelTest.kt`

```kotlin
package com.gallery.ui.albums

import android.net.Uri
import app.cash.turbine.test
import com.gallery.domain.model.Album
import com.gallery.domain.model.AlbumType
import com.gallery.domain.usecase.GetAlbumsUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AlbumsViewModelTest {

    private lateinit var getAlbums: GetAlbumsUseCase

    private val smart1 = Album(
        id = "smart_favorites",
        name = "Favorites",
        coverUri = Uri.EMPTY,
        count = 10,
        type = AlbumType.SMART
    )
    private val custom1 = Album(
        id = "custom_vacation",
        name = "Vacation",
        coverUri = Uri.EMPTY,
        count = 5,
        type = AlbumType.CUSTOM
    )
    private val custom2 = Album(
        id = "custom_work",
        name = "Work",
        coverUri = Uri.EMPTY,
        count = 3,
        type = AlbumType.CUSTOM
    )

    @Before
    fun setUp() {
        getAlbums = mockk()
    }

    // -----------------------------------------------------------------------
    // Smart / Custom split
    // -----------------------------------------------------------------------

    @Test
    fun `splits smart and custom albums into separate lists`() = runTest {
        every { getAlbums() } returns flowOf(listOf(smart1, custom1, custom2))

        val vm = AlbumsViewModel(getAlbums)

        vm.uiState.test {
            val loading = awaitItem()
            assertTrue(loading.isLoading)

            val loaded = awaitItem()
            assertFalse(loaded.isLoading)
            assertEquals(listOf(smart1), loaded.smartAlbums)
            assertEquals(listOf(custom1, custom2), loaded.myAlbums)
            assertNull(loaded.error)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `empty list produces empty smart and custom lists`() = runTest {
        every { getAlbums() } returns flowOf(emptyList())

        val vm = AlbumsViewModel(getAlbums)

        vm.uiState.test {
            val loading = awaitItem()
            assertTrue(loading.isLoading)

            val loaded = awaitItem()
            assertFalse(loaded.isLoading)
            assertTrue(loaded.smartAlbums.isEmpty())
            assertTrue(loaded.myAlbums.isEmpty())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `only smart albums — myAlbums is empty`() = runTest {
        every { getAlbums() } returns flowOf(listOf(smart1))

        val vm = AlbumsViewModel(getAlbums)

        vm.uiState.test {
            awaitItem() // loading
            val loaded = awaitItem()
            assertEquals(listOf(smart1), loaded.smartAlbums)
            assertTrue(loaded.myAlbums.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `only custom albums — smartAlbums is empty`() = runTest {
        every { getAlbums() } returns flowOf(listOf(custom1, custom2))

        val vm = AlbumsViewModel(getAlbums)

        vm.uiState.test {
            awaitItem() // loading
            val loaded = awaitItem()
            assertTrue(loaded.smartAlbums.isEmpty())
            assertEquals(listOf(custom1, custom2), loaded.myAlbums)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
```

- [ ] **Run test: FAIL** — Create test file first; verify compile error (no `AlbumsViewModel` yet).
- [ ] **Implement** `AlbumsViewModel.kt` with the code above.
- [ ] **Run test: PASS** — All 4 tests green.
- [ ] **Git commit:**
  ```
  git add app/src/main/kotlin/com/gallery/ui/albums/AlbumsViewModel.kt \
          app/src/test/kotlin/com/gallery/ui/albums/AlbumsViewModelTest.kt
  git commit -m "feat(albums): add AlbumsViewModel with smart/custom split and full unit tests"
  ```

---

## Task 2: AlbumsScreen

**File:** `app/src/main/kotlin/com/gallery/ui/albums/AlbumsScreen.kt`

```kotlin
package com.gallery.ui.albums

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.item
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.gallery.domain.model.Album
import com.gallery.ui.common.EmptyState
import com.gallery.ui.common.LoadingState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumsScreen(
    onAlbumClick: (String) -> Unit,
    viewModel: AlbumsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Albums") }) }
    ) { padding ->
        when {
            uiState.isLoading -> LoadingState(Modifier.padding(padding))

            uiState.smartAlbums.isEmpty() && uiState.myAlbums.isEmpty() ->
                EmptyState(
                    message = "No albums yet",
                    modifier = Modifier.padding(padding)
                )

            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (uiState.smartAlbums.isNotEmpty()) {
                    item(span = { GridItemSpan(3) }) {
                        Text(
                            text = "Smart Albums",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(uiState.smartAlbums, key = { it.id }) { album ->
                        AlbumCard(album = album, onClick = onAlbumClick)
                    }
                }

                if (uiState.myAlbums.isNotEmpty()) {
                    item(span = { GridItemSpan(3) }) {
                        Text(
                            text = "My Albums",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(uiState.myAlbums, key = { it.id }) { album ->
                        AlbumCard(album = album, onClick = onAlbumClick)
                    }
                }
            }
        }
    }
}

@Composable
private fun AlbumCard(album: Album, onClick: (String) -> Unit) {
    Column(modifier = Modifier.clickable { onClick(album.id) }) {
        AsyncImage(
            model = album.coverUri,
            contentDescription = album.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = album.name,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "${album.count}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
```

- [ ] **Implement** `AlbumsScreen.kt` with the code above.
- [ ] Verify it compiles against `AlbumsViewModel`, `EmptyState`, `LoadingState`, and Coil `AsyncImage`.
- [ ] **Git commit:**
  ```
  git add app/src/main/kotlin/com/gallery/ui/albums/AlbumsScreen.kt
  git commit -m "feat(albums): add AlbumsScreen with Smart Albums + My Albums sections in 3-col grid"
  ```

---

## Task 3: AlbumDetailViewModel + Test

### 3a. AlbumDetailViewModel

**File:** `app/src/main/kotlin/com/gallery/ui/albums/AlbumDetailViewModel.kt`

```kotlin
package com.gallery.ui.albums

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gallery.domain.model.MediaItem
import com.gallery.domain.usecase.GetAlbumMediaUseCase
import com.gallery.domain.usecase.MoveToTrashUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AlbumDetailUiState(
    val isLoading: Boolean = true,
    val albumName: String = "",
    val items: List<MediaItem> = emptyList(),
    val selectedIds: Set<Long> = emptySet()
)

@HiltViewModel
class AlbumDetailViewModel @Inject constructor(
    private val getAlbumMedia: GetAlbumMediaUseCase,
    private val moveToTrash: MoveToTrashUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlbumDetailUiState())
    val uiState: StateFlow<AlbumDetailUiState> = _uiState.asStateFlow()

    fun loadAlbum(albumId: String) {
        viewModelScope.launch {
            getAlbumMedia(albumId.toLong()).collect { items ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        albumName = items.firstOrNull()?.bucketName ?: "",
                        items = items
                    )
                }
            }
        }
    }

    fun toggleSelection(id: Long) {
        _uiState.update { state ->
            val newSet = if (id in state.selectedIds) {
                state.selectedIds - id
            } else {
                state.selectedIds + id
            }
            state.copy(selectedIds = newSet)
        }
    }

    fun clearSelection() = _uiState.update { it.copy(selectedIds = emptySet()) }

    fun selectAll(ids: List<Long>) = _uiState.update { it.copy(selectedIds = ids.toSet()) }

    fun deleteSelected() {
        val selectedIds = _uiState.value.selectedIds
        if (selectedIds.isEmpty()) return
        viewModelScope.launch {
            _uiState.value.items
                .filter { it.id in selectedIds }
                .forEach { moveToTrash(it) }
            clearSelection()
        }
    }
}
```

### 3b. AlbumDetailViewModel Unit Tests

**File:** `app/src/test/kotlin/com/gallery/ui/albums/AlbumDetailViewModelTest.kt`

```kotlin
package com.gallery.ui.albums

import android.net.Uri
import app.cash.turbine.test
import com.gallery.domain.model.MediaItem
import com.gallery.domain.usecase.GetAlbumMediaUseCase
import com.gallery.domain.usecase.MoveToTrashUseCase
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AlbumDetailViewModelTest {

    private lateinit var getAlbumMedia: GetAlbumMediaUseCase
    private lateinit var moveToTrash: MoveToTrashUseCase

    private val albumId = "1001"

    private fun fakeItem(id: Long, bucketName: String = "Camera") = MediaItem(
        id = id,
        uri = Uri.EMPTY,
        name = "img_$id.jpg",
        dateTaken = 1_700_000_000_000L,
        size = 1024L,
        width = 1080,
        height = 1920,
        duration = null,
        mimeType = "image/jpeg",
        bucketId = albumId.toLong(),
        bucketName = bucketName,
        location = null
    )

    @Before
    fun setUp() {
        getAlbumMedia = mockk()
        moveToTrash = mockk(relaxed = true)
    }

    private fun buildVm() = AlbumDetailViewModel(getAlbumMedia, moveToTrash)

    // -----------------------------------------------------------------------
    // loadAlbum
    // -----------------------------------------------------------------------

    @Test
    fun `loadAlbum sets albumName from first item bucketName`() = runTest {
        val items = listOf(fakeItem(1L, "Camera"), fakeItem(2L, "Camera"))
        every { getAlbumMedia(albumId.toLong()) } returns flowOf(items)

        val vm = buildVm()

        vm.uiState.test {
            val loading = awaitItem()
            assertTrue(loading.isLoading)

            vm.loadAlbum(albumId)

            val loaded = awaitItem()
            assertFalse(loaded.isLoading)
            assertEquals("Camera", loaded.albumName)
            assertEquals(items, loaded.items)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `loadAlbum with empty media sets albumName to empty string`() = runTest {
        every { getAlbumMedia(albumId.toLong()) } returns flowOf(emptyList())

        val vm = buildVm()
        vm.loadAlbum(albumId)

        vm.uiState.test {
            val loaded = awaitItem()
            assertFalse(loaded.isLoading)
            assertEquals("", loaded.albumName)
            assertTrue(loaded.items.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // -----------------------------------------------------------------------
    // Selection
    // -----------------------------------------------------------------------

    @Test
    fun `toggleSelection adds and removes ids`() = runTest {
        every { getAlbumMedia(any()) } returns flowOf(emptyList())

        val vm = buildVm()
        vm.toggleSelection(5L)
        assertTrue(5L in vm.uiState.value.selectedIds)

        vm.toggleSelection(5L)
        assertFalse(5L in vm.uiState.value.selectedIds)
    }

    @Test
    fun `clearSelection empties selectedIds`() = runTest {
        every { getAlbumMedia(any()) } returns flowOf(emptyList())

        val vm = buildVm()
        vm.toggleSelection(1L)
        vm.toggleSelection(2L)
        vm.clearSelection()

        assertTrue(vm.uiState.value.selectedIds.isEmpty())
    }

    @Test
    fun `selectAll sets all provided ids`() = runTest {
        every { getAlbumMedia(any()) } returns flowOf(emptyList())

        val vm = buildVm()
        vm.selectAll(listOf(10L, 20L, 30L))

        assertEquals(setOf(10L, 20L, 30L), vm.uiState.value.selectedIds)
    }

    // -----------------------------------------------------------------------
    // Actions
    // -----------------------------------------------------------------------

    @Test
    fun `deleteSelected calls moveToTrash for selected items only and clears selection`() = runTest {
        val items = listOf(fakeItem(1L), fakeItem(2L), fakeItem(3L))
        every { getAlbumMedia(albumId.toLong()) } returns flowOf(items)

        val vm = buildVm()
        vm.loadAlbum(albumId)

        // Wait for load
        vm.uiState.test {
            awaitItem() // loading
            awaitItem() // loaded
            cancelAndIgnoreRemainingEvents()
        }

        vm.selectAll(listOf(1L, 2L))
        vm.deleteSelected()

        coVerify(exactly = 1) { moveToTrash(items[0]) }
        coVerify(exactly = 1) { moveToTrash(items[1]) }
        coVerify(exactly = 0) { moveToTrash(items[2]) }
        assertTrue(vm.uiState.value.selectedIds.isEmpty())
    }

    @Test
    fun `deleteSelected is no-op when nothing is selected`() = runTest {
        every { getAlbumMedia(any()) } returns flowOf(emptyList())

        val vm = buildVm()
        vm.deleteSelected() // should not throw

        coVerify(exactly = 0) { moveToTrash(any()) }
    }
}
```

- [ ] **Run test: FAIL** — Create test file first; verify compile error.
- [ ] **Implement** `AlbumDetailViewModel.kt` with the code above.
- [ ] **Run test: PASS** — All tests green.
- [ ] **Git commit:**
  ```
  git add app/src/main/kotlin/com/gallery/ui/albums/AlbumDetailViewModel.kt \
          app/src/test/kotlin/com/gallery/ui/albums/AlbumDetailViewModelTest.kt
  git commit -m "feat(albums): add AlbumDetailViewModel with selection, delete and full unit tests"
  ```

---

## Task 4: AlbumDetailScreen

**File:** `app/src/main/kotlin/com/gallery/ui/albums/AlbumDetailScreen.kt`

```kotlin
package com.gallery.ui.albums

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gallery.domain.model.isVideo
import com.gallery.ui.common.EmptyState
import com.gallery.ui.common.LoadingState
import com.gallery.ui.common.MediaThumbnail
import com.gallery.ui.common.SelectionActionBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    albumId: String,
    onBack: () -> Unit,
    onMediaClick: (Long) -> Unit,
    viewModel: AlbumDetailViewModel = hiltViewModel()
) {
    // Trigger media load as soon as the screen enters composition
    LaunchedEffect(albumId) { viewModel.loadAlbum(albumId) }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isSelecting = uiState.selectedIds.isNotEmpty()

    Scaffold(
        topBar = {
            if (isSelecting) {
                SelectionActionBar(
                    selectedCount = uiState.selectedIds.size,
                    onSelectAll = { viewModel.selectAll(uiState.items.map { it.id }) },
                    onShare = { /* TODO */ },
                    onDelete = { viewModel.deleteSelected() },
                    onClear = { viewModel.clearSelection() }
                )
            } else {
                TopAppBar(
                    title = { Text(uiState.albumName) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        }
    ) { padding ->
        when {
            uiState.isLoading -> LoadingState(Modifier.padding(padding))

            uiState.items.isEmpty() -> EmptyState(
                message = "No media in this album",
                modifier = Modifier.padding(padding)
            )

            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(uiState.items, key = { it.id }) { item ->
                    MediaThumbnail(
                        uri = item.uri,
                        isVideo = item.isVideo,
                        duration = item.duration,
                        isSelected = item.id in uiState.selectedIds,
                        onClick = {
                            if (isSelecting) {
                                viewModel.toggleSelection(item.id)
                            } else {
                                onMediaClick(item.id)
                            }
                        },
                        onLongClick = { viewModel.toggleSelection(item.id) }
                    )
                }
            }
        }
    }
}
```

- [ ] **Implement** `AlbumDetailScreen.kt` with the code above.
- [ ] Verify it compiles against `AlbumDetailViewModel`, `SelectionActionBar`, `MediaThumbnail`, `EmptyState`, and `LoadingState`.
- [ ] **Git commit:**
  ```
  git add app/src/main/kotlin/com/gallery/ui/albums/AlbumDetailScreen.kt
  git commit -m "feat(albums): add AlbumDetailScreen with 3-col media grid and multi-select"
  ```

---

## Task 5: Wire Routes in NavGraph

**File:** `app/src/main/kotlin/com/gallery/ui/navigation/GalleryNavGraph.kt` (existing)

Add the albums destinations inside `NavHost { ... }`:

```kotlin
composable(Screen.Albums.route) {
    AlbumsScreen(
        onAlbumClick = { albumId ->
            navController.navigate(Screen.AlbumDetail.createRoute(albumId))
        }
    )
}

composable(
    route = Screen.AlbumDetail.route,          // e.g. "album/{albumId}"
    arguments = listOf(navArgument("albumId") { type = NavType.StringType })
) { backStackEntry ->
    val albumId = backStackEntry.arguments?.getString("albumId") ?: return@composable
    AlbumDetailScreen(
        albumId = albumId,
        onBack = { navController.popBackStack() },
        onMediaClick = { mediaId ->
            navController.navigate(Screen.Viewer.createRoute(mediaId))
        }
    )
}
```

- [ ] Open `GalleryNavGraph.kt` and add the routes above.
- [ ] Ensure the bottom navigation bar / tab row includes an "Albums" entry pointing to `Screen.Albums.route`.
- [ ] **Git commit:**
  ```
  git add app/src/main/kotlin/com/gallery/ui/navigation/GalleryNavGraph.kt
  git commit -m "feat(navigation): register Albums and AlbumDetail routes in NavGraph"
  ```

---

## Run Full Phase 09 Test Suite

```
./gradlew :app:testDebugUnitTest --tests "com.gallery.ui.albums.*"
```

Expected: all `AlbumsViewModelTest` (4 tests) and `AlbumDetailViewModelTest` (6 tests) pass.

Optional full build verification:

```
./gradlew :app:assembleDebug
```

---

## Acceptance Criteria

- [ ] `AlbumsUiState` compiles; `smartAlbums` and `myAlbums` correctly populated from `AlbumType`
- [ ] `AlbumsViewModel` collects from `GetAlbumsUseCase` in `init` block
- [ ] `AlbumsScreen` shows "Smart Albums" section header only when `smartAlbums` non-empty
- [ ] `AlbumsScreen` shows "My Albums" section header only when `myAlbums` non-empty
- [ ] `AlbumCard` thumbnail uses `AsyncImage` with `ContentScale.Crop` and `RoundedCornerShape(8.dp)`
- [ ] `AlbumDetailViewModel.loadAlbum` is triggered via `LaunchedEffect(albumId)` in screen
- [ ] Multi-select in AlbumDetail activates on long-press; `SelectionActionBar` shown
- [ ] `deleteSelected` calls `moveToTrash` once per selected item, then clears selection
- [ ] `EmptyState` shown in AlbumDetail when `items` is empty after load
- [ ] All ViewModel unit tests pass with Turbine

---

## Summary

| Task | Production file(s) | Test file(s) | Tests |
|------|--------------------|--------------|-------|
| 1 | `AlbumsViewModel.kt` | `AlbumsViewModelTest.kt` | 4 |
| 2 | `AlbumsScreen.kt` | _(UI tests — later phase)_ | — |
| 3 | `AlbumDetailViewModel.kt` | `AlbumDetailViewModelTest.kt` | 6 |
| 4 | `AlbumDetailScreen.kt` | _(UI tests — later phase)_ | — |
| 5 | `GalleryNavGraph.kt` (modified) | — | — |
