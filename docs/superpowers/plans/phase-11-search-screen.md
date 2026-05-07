# Phase 11: Search Screen — Gallery App

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Search tab with search bar, filter chips (All/Photos/Videos), and results grid.

**Depends on:** Phase 06, Phase 07, Phase 01 use cases

---

## Overview

The Search Screen provides a dedicated tab where users can search across all media by name. Results update in real time with a 300 ms debounce to avoid excessive queries. Filter chips allow narrowing results to photos only or videos only. An empty state is shown when no query is entered, and a distinct "no results" state is shown when the search yields nothing.

---

## File Structure

```
app/src/main/kotlin/com/gallery/ui/search/
  SearchViewModel.kt
  SearchScreen.kt

app/src/test/kotlin/com/gallery/ui/search/
  SearchViewModelTest.kt
```

---

## Task 1: SearchViewModel + Unit Tests

**File:** `app/src/main/kotlin/com/gallery/ui/search/SearchViewModel.kt`

Manages the search query, debounce logic, filter state, and result list.

```kotlin
package com.gallery.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gallery.domain.SearchMediaUseCase
import com.gallery.model.MediaItem
import com.gallery.model.isVideo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class MediaFilter { ALL, PHOTOS, VIDEOS }

data class SearchUiState(
    val query: String = "",
    val filter: MediaFilter = MediaFilter.ALL,
    val results: List<MediaItem> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchMedia: SearchMediaUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.update { it.copy(results = emptyList(), isLoading = false) }
            return
        }
        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            delay(300) // debounce
            searchMedia(query).collect { items ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        results = applyFilter(items, it.filter)
                    )
                }
            }
        }
    }

    fun onFilterChange(filter: MediaFilter) {
        _uiState.update { it.copy(filter = filter, results = applyFilter(it.results, filter)) }
    }

    private fun applyFilter(items: List<MediaItem>, filter: MediaFilter) = when (filter) {
        MediaFilter.ALL -> items
        MediaFilter.PHOTOS -> items.filter { !it.isVideo }
        MediaFilter.VIDEOS -> items.filter { it.isVideo }
    }
}
```

**File:** `app/src/test/kotlin/com/gallery/ui/search/SearchViewModelTest.kt`

```kotlin
package com.gallery.ui.search

import android.net.Uri
import app.cash.turbine.test
import com.gallery.domain.SearchMediaUseCase
import com.gallery.model.MediaItem
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SearchViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val searchMedia: SearchMediaUseCase = mockk()

    private fun buildVm() = SearchViewModel(searchMedia)

    @Test
    fun `onQueryChange debounces and returns results`() = runTest {
        val items = listOf(
            MediaItem(1L, Uri.EMPTY, "sunset.jpg", 0, 0, 0, 0, null, "image/jpeg", 1L, "", null)
        )
        every { searchMedia("sunset") } returns flowOf(items)

        val vm = buildVm()
        vm.onQueryChange("sunset")
        advanceTimeBy(400)

        assertEquals(items, vm.uiState.value.results)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `onQueryChange with blank resets results and loading`() = runTest {
        every { searchMedia(any()) } returns flowOf(emptyList())

        val vm = buildVm()
        vm.onQueryChange("   ")

        assertEquals(emptyList<MediaItem>(), vm.uiState.value.results)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `onQueryChange sets isLoading true before debounce resolves`() = runTest {
        every { searchMedia("cat") } returns flowOf(emptyList())

        val vm = buildVm()
        vm.uiState.test {
            vm.onQueryChange("cat")
            // After cancel of blank state update and setting isLoading = true:
            // Advance only partially (less than 300ms) to observe loading
            advanceTimeBy(100)
            val loadingState = expectMostRecentItem()
            assertTrue(loadingState.isLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `filter VIDEOS shows only video items`() = runTest {
        every { searchMedia(any()) } returns flowOf(emptyList())

        val vm = buildVm()
        vm.onFilterChange(MediaFilter.VIDEOS)

        assertEquals(MediaFilter.VIDEOS, vm.uiState.value.filter)
    }

    @Test
    fun `filter PHOTOS removes video items from results`() = runTest {
        val photo = MediaItem(1L, Uri.EMPTY, "photo.jpg", 0, 0, 0, 0, null, "image/jpeg", 1L, "", null)
        val video = MediaItem(2L, Uri.EMPTY, "video.mp4", 0, 0, 0, 0, 5000L, "video/mp4", 1L, "", null)
        every { searchMedia("media") } returns flowOf(listOf(photo, video))

        val vm = buildVm()
        vm.onQueryChange("media")
        advanceTimeBy(400)
        vm.onFilterChange(MediaFilter.PHOTOS)

        assertEquals(listOf(photo), vm.uiState.value.results)
    }

    @Test
    fun `filter VIDEOS removes photo items from results`() = runTest {
        val photo = MediaItem(1L, Uri.EMPTY, "photo.jpg", 0, 0, 0, 0, null, "image/jpeg", 1L, "", null)
        val video = MediaItem(2L, Uri.EMPTY, "video.mp4", 0, 0, 0, 0, 5000L, "video/mp4", 1L, "", null)
        every { searchMedia("media") } returns flowOf(listOf(photo, video))

        val vm = buildVm()
        vm.onQueryChange("media")
        advanceTimeBy(400)
        vm.onFilterChange(MediaFilter.VIDEOS)

        assertEquals(listOf(video), vm.uiState.value.results)
    }

    @Test
    fun `rapid query changes cancel previous job and only last query fires`() = runTest {
        val items = listOf(
            MediaItem(3L, Uri.EMPTY, "lake.jpg", 0, 0, 0, 0, null, "image/jpeg", 1L, "", null)
        )
        every { searchMedia("l") } returns flowOf(emptyList())
        every { searchMedia("la") } returns flowOf(emptyList())
        every { searchMedia("lake") } returns flowOf(items)

        val vm = buildVm()
        vm.onQueryChange("l")
        advanceTimeBy(100)
        vm.onQueryChange("la")
        advanceTimeBy(100)
        vm.onQueryChange("lake")
        advanceTimeBy(400)

        assertEquals(items, vm.uiState.value.results)
    }
}
```

Run tests:

```bash
./gradlew :app:testDebugUnitTest --tests "com.gallery.ui.search.*"
```

Commit message: `feat(search): add SearchViewModel with debounced query, MediaFilter, applyFilter + unit tests`

---

## Task 2: SearchScreen

**File:** `app/src/main/kotlin/com/gallery/ui/search/SearchScreen.kt`

Search bar, filter chip row, and a results grid that reuses the shared `MediaThumbnail` component. Shows loading, empty-query, and no-results states.

```kotlin
package com.gallery.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gallery.ui.common.EmptyState
import com.gallery.ui.common.LoadingState
import com.gallery.ui.common.MediaThumbnail

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onMediaClick: (Long) -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {

        // Search bar
        SearchBar(
            query = uiState.query,
            onQueryChange = viewModel::onQueryChange,
            onSearch = {},
            active = false,
            onActiveChange = {},
            placeholder = { Text("Search photos & videos") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {}

        // Filter chips
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MediaFilter.values().forEach { filter ->
                FilterChip(
                    selected = uiState.filter == filter,
                    onClick = { viewModel.onFilterChange(filter) },
                    label = {
                        Text(
                            filter.name
                                .lowercase()
                                .replaceFirstChar { it.uppercase() }
                        )
                    }
                )
            }
        }

        // Content area
        when {
            uiState.isLoading -> LoadingState()

            uiState.query.isBlank() -> EmptyState(
                message = "Search your photos and videos",
                icon = Icons.Default.Search
            )

            uiState.results.isEmpty() -> EmptyState(
                message = "No results for \"${uiState.query}\""
            )

            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(uiState.results, key = { it.id }) { item ->
                    MediaThumbnail(
                        uri = item.uri,
                        isVideo = item.isVideo,
                        duration = item.duration,
                        isSelected = false,
                        onClick = { onMediaClick(item.id) }
                    )
                }
            }
        }
    }
}
```

**Implementation notes:**
- `SearchBar` is the Material3 component. `active = false` keeps it in a non-expanded state (no in-place suggestions panel). If a suggestions or history panel is needed in a future phase, flip to stateful `active` management.
- `MediaFilter.values()` iterates enum values in declaration order (ALL, PHOTOS, VIDEOS), matching the desired chip order.
- `key = { it.id }` on the grid items enables stable Compose diffing so items animate correctly when the result list changes.
- `EmptyState` and `LoadingState` are shared components from Phase 07 (`com.gallery.ui.common`).
- `MediaThumbnail` is the shared thumbnail component from Phase 07, used here with `isSelected = false` since search has no selection mode.

Run tests:

```bash
./gradlew :app:testDebugUnitTest --tests "com.gallery.ui.search.*"
```

Commit message: `feat(search): add SearchScreen with SearchBar, MediaFilter chips, results grid, loading and empty states`

---

## Dependencies to Verify in `build.gradle.kts`

All dependencies for this phase should already be present from earlier phases. Verify:

```kotlin
// Material3 (SearchBar, FilterChip)
implementation("androidx.compose.material3:material3") // included in BOM

// Compose Foundation (LazyVerticalGrid)
implementation("androidx.compose.foundation:foundation") // included in BOM

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

The Search screen is typically a tab destination rather than a navigated route. Wire it in the bottom navigation tab host (e.g., `AppNavigation.kt`):

```kotlin
// Bottom nav tab
composable(route = "search") {
    SearchScreen(
        onMediaClick = { mediaId -> navController.navigate("viewer/$mediaId") }
    )
}
```

Add the corresponding bottom navigation item to the tab bar alongside the Photos and Albums tabs.

---

## Acceptance Criteria

- [ ] Typing in the search bar updates the query in `SearchUiState`
- [ ] Results do not appear until at least 300 ms after the last keystroke (debounce)
- [ ] Clearing the search bar shows the "Search your photos and videos" empty state
- [ ] A spinner/loading indicator is shown while the debounce delay is active and results are loading
- [ ] Selecting the "Photos" chip hides video items from results
- [ ] Selecting the "Videos" chip hides photo items from results
- [ ] Selecting "All" chip shows both photo and video items
- [ ] Tapping a result navigates to the Viewer screen for that media item
- [ ] Results grid uses 3 columns with 2 dp gaps between items
- [ ] "No results for ..." empty state is shown when the query yields no matches after filtering
- [ ] All `SearchViewModelTest` tests pass
