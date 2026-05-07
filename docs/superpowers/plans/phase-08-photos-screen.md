# Phase 08: Photos Screen — Gallery App

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Implement the Photos tab with staggered grid (featured tiles every ~8 items), date headers, multi-select, and "On This Day" memories card.

**Depends on:** Phase 06, Phase 07, Phase 01 use cases

---

## Overview

The Photos screen is the primary tab of the Gallery app. It displays all media items from the device grouped by date, with a staggered grid layout where every 8th item is "featured" (spans 2 columns, OnePlus-style). At the top, an "On This Day" memories card appears when there are 3 or more historical items taken on today's date in a past year. Multi-select mode activates on long-press and shows a `SelectionActionBar`.

---

## File Structure

```
app/src/main/kotlin/com/gallery/ui/photos/
├── PhotosViewModel.kt          (includes PhotosUiState, IndexedMediaItem)
└── PhotosScreen.kt             (includes MemoriesCard composable)

app/src/test/kotlin/com/gallery/ui/photos/
└── PhotosViewModelTest.kt
```

---

## Dependencies / Imports

All use cases, shared types, and common UI components referenced in this phase are defined in earlier phases:

- `GetAllMediaUseCase` — Phase 01
- `MoveToTrashUseCase` — Phase 01
- `ToggleFavoriteUseCase` — Phase 01
- `GetOnThisDayUseCase` — Phase 07 (queries media by month+day across all years)
- `MediaItem`, `isVideo` extension — Phase 01 shared types
- `MediaThumbnail`, `EmptyState`, `LoadingState`, `SelectionActionBar` — Phase 06 common UI

---

## Task 1: PhotosViewModel + Test

### 1a. PhotosViewModel

**File:** `app/src/main/kotlin/com/gallery/ui/photos/PhotosViewModel.kt`

```kotlin
package com.gallery.ui.photos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gallery.domain.model.MediaItem
import com.gallery.domain.model.isVideo
import com.gallery.domain.usecase.GetAllMediaUseCase
import com.gallery.domain.usecase.GetOnThisDayUseCase
import com.gallery.domain.usecase.MoveToTrashUseCase
import com.gallery.domain.usecase.ToggleFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.LinkedHashMap
import java.util.Locale
import javax.inject.Inject

// --- UI Models ---

data class IndexedMediaItem(
    val item: MediaItem,
    val isFeatured: Boolean
)

data class PhotosUiState(
    val isLoading: Boolean = true,
    // key = "TODAY", "YESTERDAY", "MAY 2025", etc.
    val groupedMedia: Map<String, List<IndexedMediaItem>> = emptyMap(),
    val memoriesItems: List<MediaItem> = emptyList(),
    val selectedIds: Set<Long> = emptySet(),
    val error: String? = null
)

// --- ViewModel ---

@HiltViewModel
class PhotosViewModel @Inject constructor(
    private val getAllMedia: GetAllMediaUseCase,
    private val moveToTrash: MoveToTrashUseCase,
    private val toggleFavorite: ToggleFavoriteUseCase,
    private val getOnThisDay: GetOnThisDayUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PhotosUiState())
    val uiState: StateFlow<PhotosUiState> = _uiState.asStateFlow()

    init {
        loadMedia()
        loadMemories()
    }

    // --- Loaders ---

    private fun loadMedia() {
        viewModelScope.launch {
            getAllMedia().collect { items ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        groupedMedia = groupByDate(items)
                    )
                }
            }
        }
    }

    private fun loadMemories() {
        val today = Calendar.getInstance()
        val monthDay = (today.get(Calendar.MONTH) + 1) * 100 + today.get(Calendar.DAY_OF_MONTH)
        viewModelScope.launch {
            getOnThisDay(monthDay).collect { items ->
                // Only show memories if there are at least 3 items
                if (items.size >= 3) {
                    _uiState.update { it.copy(memoriesItems = items) }
                }
            }
        }
    }

    // --- Selection ---

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

    fun selectAll(allIds: List<Long>) = _uiState.update { it.copy(selectedIds = allIds.toSet()) }

    // --- Actions ---

    fun deleteSelected() {
        val ids = _uiState.value.selectedIds
        viewModelScope.launch {
            _uiState.value.groupedMedia.values
                .flatten()
                .filter { it.item.id in ids }
                .forEach { moveToTrash(it.item) }
            clearSelection()
        }
    }

    fun toggleFavoriteItem(id: Long) {
        viewModelScope.launch { toggleFavorite(id) }
    }

    // --- Grouping Logic ---

    /**
     * Groups a flat list of MediaItems by display date label, inserting
     * isFeatured=true for every 8th item (index % 8 == 7) within each group
     * to create the staggered/featured tile effect.
     *
     * Labels:
     *   - "TODAY"     — taken on today's date
     *   - "YESTERDAY" — taken on yesterday's date
     *   - "MMMM YYYY" — e.g., "MAY 2025" for older items
     */
    private fun groupByDate(items: List<MediaItem>): Map<String, List<IndexedMediaItem>> {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val yesterday = (today.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, -1)
        }

        val groups = LinkedHashMap<String, MutableList<MediaItem>>()

        items.forEach { item ->
            val cal = Calendar.getInstance().apply { timeInMillis = item.dateTaken }
            val key = when {
                cal.after(today) || isSameDay(cal, today) -> "TODAY"
                isSameDay(cal, yesterday) -> "YESTERDAY"
                else -> SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                    .format(cal.time)
                    .uppercase(Locale.getDefault())
            }
            groups.getOrPut(key) { mutableListOf() }.add(item)
        }

        return groups.mapValues { (_, list) ->
            list.mapIndexed { index, item ->
                IndexedMediaItem(item = item, isFeatured = index % 8 == 7)
            }
        }
    }

    private fun isSameDay(a: Calendar, b: Calendar): Boolean =
        a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
                a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
}
```

### 1b. ViewModel Unit Tests

**File:** `app/src/test/kotlin/com/gallery/ui/photos/PhotosViewModelTest.kt`

```kotlin
package com.gallery.ui.photos

import android.net.Uri
import app.cash.turbine.test
import com.gallery.domain.model.MediaItem
import com.gallery.domain.usecase.GetAllMediaUseCase
import com.gallery.domain.usecase.GetOnThisDayUseCase
import com.gallery.domain.usecase.MoveToTrashUseCase
import com.gallery.domain.usecase.ToggleFavoriteUseCase
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

class PhotosViewModelTest {

    private lateinit var getAllMedia: GetAllMediaUseCase
    private lateinit var moveToTrash: MoveToTrashUseCase
    private lateinit var toggleFavorite: ToggleFavoriteUseCase
    private lateinit var getOnThisDay: GetOnThisDayUseCase

    @Before
    fun setUp() {
        getAllMedia = mockk()
        moveToTrash = mockk(relaxed = true)
        toggleFavorite = mockk(relaxed = true)
        getOnThisDay = mockk()
    }

    private fun buildVm() = PhotosViewModel(getAllMedia, moveToTrash, toggleFavorite, getOnThisDay)

    private fun fakeItem(
        id: Long,
        dateTaken: Long = System.currentTimeMillis(),
        mimeType: String = "image/jpeg",
        bucketId: Long = 1L,
        bucketName: String = "Camera"
    ) = MediaItem(
        id = id,
        uri = Uri.EMPTY,
        name = "photo_$id.jpg",
        dateTaken = dateTaken,
        size = 1024L,
        width = 1080,
        height = 1920,
        duration = null,
        mimeType = mimeType,
        bucketId = bucketId,
        bucketName = bucketName,
        location = null
    )

    // -----------------------------------------------------------------------
    // Grouping
    // -----------------------------------------------------------------------

    @Test
    fun `groups today items under TODAY key`() = runTest {
        val item = fakeItem(1L, dateTaken = System.currentTimeMillis())
        every { getAllMedia() } returns flowOf(listOf(item))
        every { getOnThisDay(any()) } returns flowOf(emptyList())

        val vm = buildVm()

        vm.uiState.test {
            awaitItem() // initial loading state (isLoading = true)
            val loaded = awaitItem()
            assertFalse(loaded.isLoading)
            assertTrue(loaded.groupedMedia.containsKey("TODAY"))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `groups yesterday items under YESTERDAY key`() = runTest {
        val yesterdayMs = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
        val item = fakeItem(2L, dateTaken = yesterdayMs)
        every { getAllMedia() } returns flowOf(listOf(item))
        every { getOnThisDay(any()) } returns flowOf(emptyList())

        val vm = buildVm()

        vm.uiState.test {
            awaitItem()
            val loaded = awaitItem()
            assertTrue(loaded.groupedMedia.containsKey("YESTERDAY"))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `groups old items under MMMM YYYY key`() = runTest {
        // 400 days ago
        val oldMs = System.currentTimeMillis() - 400L * 24 * 60 * 60 * 1000L
        val item = fakeItem(3L, dateTaken = oldMs)
        every { getAllMedia() } returns flowOf(listOf(item))
        every { getOnThisDay(any()) } returns flowOf(emptyList())

        val vm = buildVm()

        vm.uiState.test {
            awaitItem()
            val loaded = awaitItem()
            val keys = loaded.groupedMedia.keys
            assertTrue(keys.none { it == "TODAY" || it == "YESTERDAY" })
            assertTrue(keys.isNotEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `every 8th item (index mod 8 == 7) is marked isFeatured`() = runTest {
        val items = (1L..16L).map { fakeItem(it) }
        every { getAllMedia() } returns flowOf(items)
        every { getOnThisDay(any()) } returns flowOf(emptyList())

        val vm = buildVm()

        vm.uiState.test {
            awaitItem()
            val loaded = awaitItem()
            val todayItems = loaded.groupedMedia["TODAY"] ?: emptyList()
            assertEquals(16, todayItems.size)
            assertTrue(todayItems[7].isFeatured)
            assertTrue(todayItems[15].isFeatured)
            assertFalse(todayItems[0].isFeatured)
            assertFalse(todayItems[6].isFeatured)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // -----------------------------------------------------------------------
    // Selection
    // -----------------------------------------------------------------------

    @Test
    fun `toggleSelection adds id when not selected`() = runTest {
        every { getAllMedia() } returns flowOf(emptyList())
        every { getOnThisDay(any()) } returns flowOf(emptyList())

        val vm = buildVm()
        vm.toggleSelection(5L)

        assertTrue(5L in vm.uiState.value.selectedIds)
    }

    @Test
    fun `toggleSelection removes id when already selected`() = runTest {
        every { getAllMedia() } returns flowOf(emptyList())
        every { getOnThisDay(any()) } returns flowOf(emptyList())

        val vm = buildVm()
        vm.toggleSelection(5L)
        vm.toggleSelection(5L)

        assertFalse(5L in vm.uiState.value.selectedIds)
    }

    @Test
    fun `clearSelection empties selectedIds`() = runTest {
        every { getAllMedia() } returns flowOf(emptyList())
        every { getOnThisDay(any()) } returns flowOf(emptyList())

        val vm = buildVm()
        vm.toggleSelection(1L)
        vm.toggleSelection(2L)
        vm.clearSelection()

        assertTrue(vm.uiState.value.selectedIds.isEmpty())
    }

    @Test
    fun `selectAll sets all provided ids`() = runTest {
        every { getAllMedia() } returns flowOf(emptyList())
        every { getOnThisDay(any()) } returns flowOf(emptyList())

        val vm = buildVm()
        vm.selectAll(listOf(10L, 20L, 30L))

        assertEquals(setOf(10L, 20L, 30L), vm.uiState.value.selectedIds)
    }

    // -----------------------------------------------------------------------
    // Memories
    // -----------------------------------------------------------------------

    @Test
    fun `memories not shown when fewer than 3 items`() = runTest {
        every { getAllMedia() } returns flowOf(emptyList())
        every { getOnThisDay(any()) } returns flowOf(listOf(fakeItem(99L), fakeItem(100L)))

        val vm = buildVm()

        vm.uiState.test {
            awaitItem()
            val state = awaitItem()
            assertTrue(state.memoriesItems.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `memories shown when 3 or more items`() = runTest {
        val memItems = (1L..5L).map { fakeItem(it) }
        every { getAllMedia() } returns flowOf(emptyList())
        every { getOnThisDay(any()) } returns flowOf(memItems)

        val vm = buildVm()

        vm.uiState.test {
            awaitItem()
            val state = awaitItem()
            assertEquals(5, state.memoriesItems.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // -----------------------------------------------------------------------
    // Actions
    // -----------------------------------------------------------------------

    @Test
    fun `deleteSelected calls moveToTrash for each selected item and clears selection`() = runTest {
        val items = listOf(fakeItem(1L), fakeItem(2L), fakeItem(3L))
        every { getAllMedia() } returns flowOf(items)
        every { getOnThisDay(any()) } returns flowOf(emptyList())

        val vm = buildVm()

        // Wait for state to load
        vm.uiState.test {
            awaitItem()
            awaitItem() // loaded state
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
    fun `toggleFavoriteItem delegates to ToggleFavoriteUseCase`() = runTest {
        every { getAllMedia() } returns flowOf(emptyList())
        every { getOnThisDay(any()) } returns flowOf(emptyList())

        val vm = buildVm()
        vm.toggleFavoriteItem(42L)

        coVerify { toggleFavorite(42L) }
    }
}
```

- [ ] **Run test: FAIL** — Create test file first; verify it fails to compile (no production code).
- [ ] **Implement** `PhotosViewModel.kt` with the code above.
- [ ] **Run test: PASS** — All tests green.
- [ ] **Git commit:**
  ```
  git add app/src/main/kotlin/com/gallery/ui/photos/PhotosViewModel.kt \
          app/src/test/kotlin/com/gallery/ui/photos/PhotosViewModelTest.kt
  git commit -m "feat(photos): add PhotosViewModel with groupByDate, selection, memories and full unit tests"
  ```

---

## Task 2: PhotosScreen Composable

**File:** `app/src/main/kotlin/com/gallery/ui/photos/PhotosScreen.kt`

```kotlin
package com.gallery.ui.photos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.item
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gallery.domain.model.MediaItem
import com.gallery.domain.model.isVideo
import com.gallery.ui.common.EmptyState
import com.gallery.ui.common.LoadingState
import com.gallery.ui.common.MediaThumbnail
import com.gallery.ui.common.SelectionActionBar

// ---------------------------------------------------------------------------
// Memories Card
// ---------------------------------------------------------------------------

/**
 * Displays a horizontal strip of "On This Day" memory thumbnails.
 *
 * Shown only when [items] has >= 3 entries (enforced by ViewModel).
 * [onDismiss] hides the card for the current session.
 * [onTap] navigates to a slideshow / memory detail screen.
 */
@Composable
fun MemoriesCard(
    items: List<MediaItem>,
    onDismiss: () -> Unit,
    onTap: () -> Unit
) {
    Card(
        onClick = onTap,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.padding(12.dp)
        ) {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("On This Day", style = MaterialTheme.typography.titleSmall)
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss memories"
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                items(items.take(5)) { item ->
                    MediaThumbnail(
                        uri = item.uri,
                        isVideo = item.isVideo,
                        duration = item.duration,
                        isSelected = false,
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        onClick = onTap,
                        onLongClick = {}
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Photos Screen
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotosScreen(
    onMediaClick: (Long) -> Unit,
    onNavigateToEditor: (Long, Boolean) -> Unit,
    viewModel: PhotosViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isSelecting = uiState.selectedIds.isNotEmpty()

    Scaffold(
        topBar = {
            if (isSelecting) {
                SelectionActionBar(
                    selectedCount = uiState.selectedIds.size,
                    onSelectAll = {
                        viewModel.selectAll(
                            uiState.groupedMedia.values.flatten().map { it.item.id }
                        )
                    },
                    onShare = {
                        // TODO: build share Intent for selected URIs
                    },
                    onDelete = { viewModel.deleteSelected() },
                    onClear = { viewModel.clearSelection() }
                )
            } else {
                TopAppBar(title = { Text("Photos") })
            }
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                LoadingState(modifier = Modifier.padding(padding))
            }

            uiState.groupedMedia.isEmpty() -> {
                EmptyState(
                    message = "No photos yet",
                    modifier = Modifier.padding(padding)
                )
            }

            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // --- On This Day Memories Card ---
                    if (uiState.memoriesItems.isNotEmpty()) {
                        item(span = { GridItemSpan(3) }) {
                            MemoriesCard(
                                items = uiState.memoriesItems,
                                onDismiss = { /* TODO: persist dismissal */ },
                                onTap = { /* TODO: navigate to memories slideshow */ }
                            )
                        }
                    }

                    // --- Date Groups + Media Grid ---
                    uiState.groupedMedia.forEach { (dateLabel, items) ->

                        // Date header — spans full width
                        item(span = { GridItemSpan(3) }) {
                            Text(
                                text = dateLabel,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(
                                    horizontal = 8.dp,
                                    vertical = 4.dp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Media tiles: featured items span 2 cols, normal tiles span 1
                        items(
                            items = items,
                            key = { it.item.id },
                            span = { indexed ->
                                if (indexed.isFeatured) GridItemSpan(2) else GridItemSpan(1)
                            }
                        ) { indexed ->
                            MediaThumbnail(
                                uri = indexed.item.uri,
                                isVideo = indexed.item.isVideo,
                                duration = indexed.item.duration,
                                isSelected = indexed.item.id in uiState.selectedIds,
                                modifier = Modifier.aspectRatio(
                                    if (indexed.isFeatured) 2f else 1f
                                ),
                                onClick = {
                                    if (isSelecting) {
                                        viewModel.toggleSelection(indexed.item.id)
                                    } else {
                                        onMediaClick(indexed.item.id)
                                    }
                                },
                                onLongClick = {
                                    viewModel.toggleSelection(indexed.item.id)
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

- [ ] **Implement** `PhotosScreen.kt` with the code above.
- [ ] Verify it compiles against `PhotosViewModel`, `MemoriesCard`, and all common UI components.
- [ ] **Git commit:**
  ```
  git add app/src/main/kotlin/com/gallery/ui/photos/PhotosScreen.kt
  git commit -m "feat(photos): add PhotosScreen — staggered grid, date headers, memories card, multi-select"
  ```

---

## Task 3: Wire into NavGraph

**File:** `app/src/main/kotlin/com/gallery/ui/navigation/GalleryNavGraph.kt` (existing)

Add the `photos` composable destination inside `NavHost { ... }`:

```kotlin
composable(Screen.Photos.route) {
    PhotosScreen(
        onMediaClick = { mediaId ->
            navController.navigate(Screen.Viewer.createRoute(mediaId))
        },
        onNavigateToEditor = { mediaId, isVideo ->
            navController.navigate(Screen.Editor.createRoute(mediaId, isVideo))
        }
    )
}
```

- [ ] Open `GalleryNavGraph.kt` and add the route above.
- [ ] Ensure the bottom navigation bar / tab row includes a "Photos" entry pointing to `Screen.Photos.route`.
- [ ] **Git commit:**
  ```
  git add app/src/main/kotlin/com/gallery/ui/navigation/GalleryNavGraph.kt
  git commit -m "feat(navigation): register Photos route in NavGraph"
  ```

---

## Run Full Phase 08 Test Suite

```
./gradlew :app:testDebugUnitTest --tests "com.gallery.ui.photos.*"
```

Expected: all ViewModel tests pass. `PhotosScreen` is composable-only; it will be covered by UI/screenshot tests in a later phase.

---

## Acceptance Criteria

- [ ] `PhotosUiState` and `IndexedMediaItem` data classes compile without errors
- [ ] `PhotosViewModel` initialises both `loadMedia` and `loadMemories` in `init`
- [ ] `groupByDate` correctly labels TODAY / YESTERDAY / MMMM YYYY
- [ ] Every 8th item within a group (`index % 8 == 7`) has `isFeatured = true`
- [ ] `MemoriesCard` only renders when `memoriesItems.size >= 3` (enforced in ViewModel)
- [ ] Multi-select activates on long-press; `SelectionActionBar` shown when `selectedIds` non-empty
- [ ] Featured tiles use `GridItemSpan(2)`, normal tiles use `GridItemSpan(1)`
- [ ] `LoadingState` shown while `isLoading = true`
- [ ] `EmptyState` shown when `groupedMedia` is empty after load
- [ ] All ViewModel tests pass with Turbine
- [ ] No direct use of `MediaStore` or `ContentResolver` inside ViewModel or Screen

---

## Summary

| Task | Production file(s) | Test file(s) | Tests |
|------|--------------------|--------------|-------|
| 1 | `PhotosViewModel.kt` | `PhotosViewModelTest.kt` | 11 |
| 2 | `PhotosScreen.kt` | _(UI tests — later phase)_ | — |
| 3 | `GalleryNavGraph.kt` (modified) | — | — |
