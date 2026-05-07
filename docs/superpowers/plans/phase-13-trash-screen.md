# Phase 13: Trash Screen — Gallery App
> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Trash screen listing soft-deleted items with days-remaining badges, restore, and permanent delete.

**Depends on:** Phase 06, Phase 07, Phase 01 use cases (`GetTrashUseCase`, `RestoreFromTrashUseCase`, `PurgeTrashItemUseCase`)

---

## Overview

The Trash screen shows all soft-deleted media in a 3-column grid. Each thumbnail has a countdown badge showing how many days remain before the item is auto-purged (items are purged after 30 days). Users can long-press to enter multi-selection mode, then restore or permanently delete selected items. A top-bar "Empty Trash" action purges all items at once.

### Shared types reference (from Phase 01)

```kotlin
data class TrashItem(
    val id: Long,
    val originalUri: Uri,
    val name: String,
    val dateTaken: Long,
    val mimeType: String,
    val deletedAt: Long,
    val bucketId: Long
)

val TrashItem.isVideo get() = mimeType.startsWith("video/")
val TrashItem.daysUntilPurge get() =
    30 - ((System.currentTimeMillis() - deletedAt) / 86_400_000L).toInt()

class GetTrashUseCase(val trashRepo: TrashRepository) {
    operator fun invoke(): Flow<List<TrashItem>>
}
class RestoreFromTrashUseCase(val trashRepo: TrashRepository) {
    suspend operator fun invoke(item: TrashItem)
}
class PurgeTrashItemUseCase(val trashRepo: TrashRepository) {
    suspend operator fun invoke(id: Long)
}
```

---

## Task 1: TrashViewModel + unit tests

**Files to create:**
- `app/src/main/kotlin/com/gallery/ui/trash/TrashViewModel.kt`
- `app/src/test/kotlin/com/gallery/ui/trash/TrashViewModelTest.kt`

### `TrashViewModel.kt`

```kotlin
package com.gallery.ui.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gallery.domain.GetTrashUseCase
import com.gallery.domain.PurgeTrashItemUseCase
import com.gallery.domain.RestoreFromTrashUseCase
import com.gallery.domain.TrashItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TrashUiState(
    val isLoading: Boolean = true,
    val items: List<TrashItem> = emptyList(),
    val selectedIds: Set<Long> = emptySet(),
    val error: String? = null
)

@HiltViewModel
class TrashViewModel @Inject constructor(
    private val getTrash: GetTrashUseCase,
    private val restore: RestoreFromTrashUseCase,
    private val purge: PurgeTrashItemUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrashUiState())
    val uiState: StateFlow<TrashUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getTrash().collect { items ->
                _uiState.update { it.copy(isLoading = false, items = items) }
            }
        }
    }

    fun restoreItem(item: TrashItem) {
        viewModelScope.launch { restore(item) }
    }

    fun purgeItem(id: Long) {
        viewModelScope.launch { purge(id) }
    }

    fun toggleSelection(id: Long) {
        _uiState.update { state ->
            val newIds = if (id in state.selectedIds) state.selectedIds - id else state.selectedIds + id
            state.copy(selectedIds = newIds)
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedIds = emptySet()) }
    }

    fun purgeSelected() {
        val ids = _uiState.value.selectedIds.toSet()
        viewModelScope.launch {
            ids.forEach { purge(it) }
            clearSelection()
        }
    }

    fun restoreSelected() {
        val ids = _uiState.value.selectedIds.toSet()
        viewModelScope.launch {
            _uiState.value.items
                .filter { it.id in ids }
                .forEach { restore(it) }
            clearSelection()
        }
    }

    fun emptyTrash() {
        val ids = _uiState.value.items.map { it.id }
        viewModelScope.launch {
            ids.forEach { purge(it) }
        }
    }
}
```

### `TrashViewModelTest.kt`

```kotlin
package com.gallery.ui.trash

import android.net.Uri
import app.cash.turbine.test
import com.gallery.domain.GetTrashUseCase
import com.gallery.domain.PurgeTrashItemUseCase
import com.gallery.domain.RestoreFromTrashUseCase
import com.gallery.domain.TrashItem
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TrashViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var getTrash: GetTrashUseCase
    private lateinit var restore: RestoreFromTrashUseCase
    private lateinit var purge: PurgeTrashItemUseCase
    private lateinit var viewModel: TrashViewModel

    private val item1 = TrashItem(
        id = 1L,
        originalUri = Uri.parse("content://media/1"),
        name = "IMG_001.jpg",
        dateTaken = System.currentTimeMillis() - 86_400_000L,
        mimeType = "image/jpeg",
        deletedAt = System.currentTimeMillis() - 86_400_000L,
        bucketId = 100L
    )
    private val item2 = TrashItem(
        id = 2L,
        originalUri = Uri.parse("content://media/2"),
        name = "VID_002.mp4",
        dateTaken = System.currentTimeMillis() - 172_800_000L,
        mimeType = "video/mp4",
        deletedAt = System.currentTimeMillis() - 172_800_000L,
        bucketId = 100L
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        getTrash = mockk()
        restore = mockk()
        purge = mockk()
        every { getTrash() } returns flowOf(listOf(item1, item2))
        coEvery { restore(any()) } returns Unit
        coEvery { purge(any()) } returns Unit
        viewModel = TrashViewModel(getTrash, restore, purge)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is loading, then items are loaded`() = runTest {
        viewModel.uiState.test {
            val loading = awaitItem()
            assertTrue(loading.isLoading)
            assertTrue(loading.items.isEmpty())

            testDispatcher.scheduler.advanceUntilIdle()

            val loaded = awaitItem()
            assertFalse(loaded.isLoading)
            assertEquals(listOf(item1, item2), loaded.items)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `restoreItem calls restore use case`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.restoreItem(item1)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { restore(item1) }
    }

    @Test
    fun `purgeItem calls purge use case with correct id`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.purgeItem(item1.id)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { purge(item1.id) }
    }

    @Test
    fun `toggleSelection adds id when not selected`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.toggleSelection(item1.id)
        assertTrue(item1.id in viewModel.uiState.value.selectedIds)
    }

    @Test
    fun `toggleSelection removes id when already selected`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.toggleSelection(item1.id)
        viewModel.toggleSelection(item1.id)
        assertFalse(item1.id in viewModel.uiState.value.selectedIds)
    }

    @Test
    fun `clearSelection empties selectedIds`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.toggleSelection(item1.id)
        viewModel.toggleSelection(item2.id)
        viewModel.clearSelection()
        assertTrue(viewModel.uiState.value.selectedIds.isEmpty())
    }

    @Test
    fun `purgeSelected purges all selected ids then clears selection`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.toggleSelection(item1.id)
        viewModel.toggleSelection(item2.id)
        viewModel.purgeSelected()
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { purge(item1.id) }
        coVerify { purge(item2.id) }
        assertTrue(viewModel.uiState.value.selectedIds.isEmpty())
    }

    @Test
    fun `restoreSelected restores all selected items then clears selection`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.toggleSelection(item1.id)
        viewModel.restoreSelected()
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { restore(item1) }
        assertTrue(viewModel.uiState.value.selectedIds.isEmpty())
    }

    @Test
    fun `emptyTrash purges all items`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.emptyTrash()
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { purge(item1.id) }
        coVerify { purge(item2.id) }
    }
}
```

---

## Task 2: TrashScreen composable + navigation wiring

**Files to create / modify:**
- `app/src/main/kotlin/com/gallery/ui/trash/TrashScreen.kt` (create)
- `app/src/main/kotlin/com/gallery/ui/navigation/GalleryNavGraph.kt` (modify — uncomment the trash route stub added in Phase 12)

### `TrashScreen.kt`

```kotlin
package com.gallery.ui.trash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gallery.domain.daysUntilPurge
import com.gallery.domain.isVideo
import com.gallery.ui.common.EmptyState
import com.gallery.ui.common.LoadingState
import com.gallery.ui.common.MediaThumbnail

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    onBack: () -> Unit,
    viewModel: TrashViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isSelecting = uiState.selectedIds.isNotEmpty()
    var showConfirmEmptyDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            if (isSelecting) {
                TopAppBar(
                    title = { Text("${uiState.selectedIds.size} selected") },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear selection")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.restoreSelected() }) {
                            Icon(Icons.Default.RestoreFromTrash, contentDescription = "Restore selected")
                        }
                        IconButton(onClick = { viewModel.purgeSelected() }) {
                            Icon(Icons.Default.DeleteForever, contentDescription = "Delete permanently")
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = { Text("Trash") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        if (uiState.items.isNotEmpty()) {
                            TextButton(onClick = { showConfirmEmptyDialog = true }) {
                                Text("Empty Trash")
                            }
                        }
                    }
                )
            }
        }
    ) { padding ->
        when {
            uiState.isLoading -> LoadingState(Modifier.padding(padding))

            uiState.items.isEmpty() -> EmptyState(
                message = "Trash is empty",
                icon = Icons.Default.Delete,
                modifier = Modifier.padding(padding)
            )

            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(uiState.items, key = { it.id }) { item ->
                    Box {
                        MediaThumbnail(
                            uri = item.originalUri,
                            isVideo = item.isVideo,
                            duration = null,
                            isSelected = item.id in uiState.selectedIds,
                            onClick = {
                                if (isSelecting) viewModel.toggleSelection(item.id)
                            },
                            onLongClick = { viewModel.toggleSelection(item.id) }
                        )

                        // Days-remaining countdown badge
                        val days = item.daysUntilPurge
                        val badgeColor = when {
                            days <= 3 -> Color(0xFFB00020)   // urgent red
                            days <= 7 -> Color(0xFFE65100)   // warning orange
                            else -> Color.Black.copy(alpha = 0.7f)
                        }
                        Text(
                            text = "${days}d",
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(4.dp)
                                .background(badgeColor, RoundedCornerShape(2.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }

    // ── Confirm "Empty Trash" dialog ──────────────────────────────────────
    if (showConfirmEmptyDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmEmptyDialog = false },
            title = { Text("Empty Trash?") },
            text = { Text("All ${uiState.items.size} items will be permanently deleted and cannot be recovered.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.emptyTrash()
                        showConfirmEmptyDialog = false
                    }
                ) { Text("Delete All", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmEmptyDialog = false }) { Text("Cancel") }
            }
        )
    }
}
```

### Navigation changes (in `GalleryNavGraph.kt`)

Remove or replace the stub comment added in Phase 12 and wire up the real composable:

```kotlin
composable("trash") {
    TrashScreen(onBack = { navController.popBackStack() })
}
```

---

## Commit

After all tasks pass:

```
git add app/src/main/kotlin/com/gallery/ui/trash/ \
        app/src/main/kotlin/com/gallery/ui/navigation/GalleryNavGraph.kt \
        app/src/test/kotlin/com/gallery/ui/trash/
git commit -m "feat: add Trash screen with restore and permanent delete (Phase 13)"
```

---

## Acceptance criteria

1. All `TrashViewModelTest` tests pass.
2. `TrashViewModel.uiState` starts with `isLoading = true` and transitions to `isLoading = false` after `GetTrashUseCase` emits.
3. `toggleSelection` adds an id on first call and removes it on second call for the same id.
4. `clearSelection` sets `selectedIds` to an empty set.
5. `purgeSelected` calls `PurgeTrashItemUseCase` for every selected id and then clears the selection.
6. `restoreSelected` calls `RestoreFromTrashUseCase` only for the items whose ids are in `selectedIds`, then clears selection.
7. `emptyTrash` calls `PurgeTrashItemUseCase` for every item currently in `items`.
8. `TrashScreen` shows `LoadingState` while loading, `EmptyState` when items list is empty, and the grid otherwise.
9. Each grid item shows a countdown badge displaying `daysUntilPurge` days. Badge turns orange at ≤ 7 days and red at ≤ 3 days.
10. Long-pressing a thumbnail enters selection mode; tapping in selection mode toggles the item. Back-arrow / close-icon exits selection mode.
11. In selection mode, the top bar shows "N selected" with restore and delete-forever icon buttons.
12. Out of selection mode, the top bar shows an "Empty Trash" text button (only when items exist), which triggers a confirmation dialog before purging.
13. The `"trash"` route is registered in `GalleryNavGraph` and navigated to from `MoreScreen`.
14. The app builds without warnings on `./gradlew assembleDebug`.
