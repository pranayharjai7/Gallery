# Phase 15: Multi-Select (Share + Move to Album) — Gallery App

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Complete the multi-select action bar: wire up Share via system intent and Move to Hidden Album. Add to Photos, Albums Detail, and Search screens.

**Depends on:** Phase 08, Phase 09, Phase 11, Phase 14

---

## Overview

Phase 08 introduced multi-select UI scaffolding with a `SelectionActionBar`, but the Share action was left as a `// TODO` and there was no "Move to Hidden" action. This phase completes those two capabilities:

1. **Share** — builds a system `ACTION_SEND` / `ACTION_SEND_MULTIPLE` intent and launches the system chooser. A single helper extension on `Context` handles both the single-item and multi-item cases and sets the correct MIME type.

2. **Move to Hidden** — adds an optional `onMoveToHidden` icon to `SelectionActionBar`. When tapped, selected items are hidden via `AddToHiddenUseCase` and selection is cleared. The action is wired into `PhotosScreen` and `AlbumDetailScreen`.

The `SearchScreen` multi-select is also brought up to parity with `PhotosScreen` (share + move to hidden).

---

## File Structure

```
app/src/main/kotlin/com/gallery/ui/common/
└── ShareUtils.kt                         (new)

app/src/main/kotlin/com/gallery/ui/common/
└── SelectionActionBar.kt                 (modified — add onMoveToHidden param)

app/src/main/kotlin/com/gallery/ui/photos/
└── PhotosViewModel.kt                    (modified — inject AddToHiddenUseCase)
└── PhotosScreen.kt                       (modified — wire share + onMoveToHidden)

app/src/main/kotlin/com/gallery/ui/albums/
└── AlbumDetailScreen.kt                  (modified — wire share + onMoveToHidden)

app/src/main/kotlin/com/gallery/ui/search/
└── SearchScreen.kt                       (modified — wire share + onMoveToHidden)
└── SearchViewModel.kt                    (modified — inject AddToHiddenUseCase)

app/src/test/kotlin/com/gallery/ui/photos/
└── PhotosViewModelTest.kt                (modified — add addSelectedToHidden tests)

app/src/test/kotlin/com/gallery/ui/search/
└── SearchViewModelTest.kt                (modified — add addSelectedToHidden tests)
```

---

## Dependencies / Imports

- `AddToHiddenUseCase` — Phase 01 / Phase 14
- `MediaItem` — Phase 01 shared types
- `SelectionActionBar` — Phase 07 common UI (modified in this phase)
- `PhotosViewModel`, `PhotosScreen` — Phase 08
- `AlbumDetailScreen` (and its ViewModel) — Phase 09
- `SearchScreen`, `SearchViewModel` — Phase 11

---

## Task 1: Share Intent Helper

**File:** `app/src/main/kotlin/com/gallery/ui/common/ShareUtils.kt`

```kotlin
package com.gallery.ui.common

import android.content.Context
import android.content.Intent
import com.gallery.domain.model.MediaItem

/**
 * Launches the system share chooser for one or more [MediaItem]s.
 *
 * - Single item: uses ACTION_SEND with the item's exact MIME type.
 * - Multiple items: uses ACTION_SEND_MULTIPLE. If all items share the same
 *   MIME type (e.g., all "image/jpeg"), that type is used; otherwise "*\/*"
 *   is used so the chooser is not filtered to a single media type.
 */
fun Context.shareMedia(items: List<MediaItem>) {
    if (items.isEmpty()) return

    if (items.size == 1) {
        val item = items.first()
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = item.mimeType
            putExtra(Intent.EXTRA_STREAM, item.uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Share"))
    } else {
        val commonMime = if (items.all { it.mimeType == items.first().mimeType }) {
            items.first().mimeType
        } else {
            "*/*"
        }
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = commonMime
            putParcelableArrayListExtra(
                Intent.EXTRA_STREAM,
                ArrayList(items.map { it.uri })
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Share ${items.size} items"))
    }
}
```

Note: `shareMedia` is a pure intent-building utility. It cannot be unit-tested without instrumented tests (requires `Context.startActivity`). Validate manually by running the app and tapping Share with 1 and N items selected.

**Manual test plan:**

| Scenario | Expected |
|----------|----------|
| Select 1 image, tap Share | System chooser opens with `ACTION_SEND`, `image/jpeg` |
| Select 1 video, tap Share | System chooser opens with `ACTION_SEND`, `video/mp4` |
| Select mixed images, tap Share | System chooser opens with `ACTION_SEND_MULTIPLE`, common MIME type |
| Select image + video, tap Share | System chooser opens with `ACTION_SEND_MULTIPLE`, `*/*` |
| Selection cleared after share chooser launched | `selectedIds` is empty |

- [ ] **Implement** `ShareUtils.kt` with the code above.
- [ ] **Git commit:**
  ```
  git add app/src/main/kotlin/com/gallery/ui/common/ShareUtils.kt
  git commit -m "feat(share): add shareMedia Context extension — single and multi-item share intent"
  ```

---

## Task 2: Add `onMoveToHidden` to SelectionActionBar

**File:** `app/src/main/kotlin/com/gallery/ui/common/SelectionActionBar.kt` (existing)

Add the optional `onMoveToHidden` parameter. When non-null, a `VisibilityOff` icon button is rendered between the Share and Delete actions.

```kotlin
@Composable
fun SelectionActionBar(
    selectedCount: Int,
    onSelectAll: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onMoveToHidden: (() -> Unit)? = null,   // NEW — null = action not shown
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Existing TopAppBar / Row layout — add the following block after the
    // Share icon button and before the Delete icon button:
    if (onMoveToHidden != null) {
        IconButton(onClick = onMoveToHidden) {
            Icon(
                imageVector = Icons.Default.VisibilityOff,
                contentDescription = "Move to Hidden Album"
            )
        }
    }
    // ... rest unchanged
}
```

The parameter is nullable (`(() -> Unit)?`) and defaults to `null` so that all existing call-sites (Albums, Viewer, etc.) compile without changes.

- [ ] Open `SelectionActionBar.kt`, add the `onMoveToHidden` parameter, and insert the conditional `IconButton`.
- [ ] Verify existing call-sites still compile (no parameter is required since it defaults to `null`).
- [ ] **Git commit:**
  ```
  git add app/src/main/kotlin/com/gallery/ui/common/SelectionActionBar.kt
  git commit -m "feat(selection): add optional onMoveToHidden icon to SelectionActionBar"
  ```

---

## Task 3: Update PhotosViewModel — inject AddToHiddenUseCase

**File:** `app/src/main/kotlin/com/gallery/ui/photos/PhotosViewModel.kt` (existing)

Inject `AddToHiddenUseCase` into the constructor and add `addSelectedToHidden()`:

```kotlin
@HiltViewModel
class PhotosViewModel @Inject constructor(
    private val getAllMedia: GetAllMediaUseCase,
    private val moveToTrash: MoveToTrashUseCase,
    private val toggleFavorite: ToggleFavoriteUseCase,
    private val getOnThisDay: GetOnThisDayUseCase,
    private val addToHidden: AddToHiddenUseCase          // NEW
) : ViewModel() {
    // ... existing code unchanged ...

    // NEW
    fun addSelectedToHidden() {
        val ids = _uiState.value.selectedIds
        viewModelScope.launch {
            ids.forEach { addToHidden(it) }
            clearSelection()
        }
    }
}
```

**Tests to add in `PhotosViewModelTest.kt`:**

```kotlin
// In setUp(), add:
private lateinit var addToHidden: AddToHiddenUseCase

// In setUp() body:
addToHidden = mockk(relaxed = true)

// Update buildVm():
private fun buildVm() = PhotosViewModel(getAllMedia, moveToTrash, toggleFavorite, getOnThisDay, addToHidden)

// New test:
@Test
fun `addSelectedToHidden calls AddToHiddenUseCase for each selected id and clears selection`() = runTest {
    every { getAllMedia() } returns flowOf(emptyList())
    every { getOnThisDay(any()) } returns flowOf(emptyList())

    val vm = buildVm()
    vm.toggleSelection(1L)
    vm.toggleSelection(3L)
    vm.addSelectedToHidden()

    coVerify(exactly = 1) { addToHidden(1L) }
    coVerify(exactly = 1) { addToHidden(3L) }
    assertTrue(vm.uiState.value.selectedIds.isEmpty())
}
```

- [ ] **Run test: FAIL** — Add the new test; verify it fails (method does not exist yet).
- [ ] **Implement** the constructor change and `addSelectedToHidden()` in `PhotosViewModel.kt`.
- [ ] **Run test: PASS** — All tests green (including existing ones, which must still pass).
- [ ] **Git commit:**
  ```
  git add app/src/main/kotlin/com/gallery/ui/photos/PhotosViewModel.kt \
          app/src/test/kotlin/com/gallery/ui/photos/PhotosViewModelTest.kt
  git commit -m "feat(photos): inject AddToHiddenUseCase, add addSelectedToHidden action and test"
  ```

---

## Task 4: Wire Share and Move to Hidden in PhotosScreen

**File:** `app/src/main/kotlin/com/gallery/ui/photos/PhotosScreen.kt` (existing)

Replace the `// TODO: build share Intent` comment and add `onMoveToHidden`:

```kotlin
@Composable
fun PhotosScreen(
    onMediaClick: (Long) -> Unit,
    onNavigateToEditor: (Long, Boolean) -> Unit,
    viewModel: PhotosViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isSelecting = uiState.selectedIds.isNotEmpty()
    val context = LocalContext.current          // NEW

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
                    onShare = {                                              // UPDATED
                        val selectedItems = uiState.groupedMedia.values
                            .flatten()
                            .filter { it.item.id in uiState.selectedIds }
                            .map { it.item }
                        context.shareMedia(selectedItems)
                        viewModel.clearSelection()
                    },
                    onDelete = { viewModel.deleteSelected() },
                    onMoveToHidden = { viewModel.addSelectedToHidden() },   // NEW
                    onClear = { viewModel.clearSelection() }
                )
            } else {
                TopAppBar(title = { Text("Photos") })
            }
        }
    ) { padding ->
        // ... rest of content unchanged ...
    }
}
```

- [ ] Open `PhotosScreen.kt` and apply the changes above.
- [ ] Ensure `LocalContext.current` is imported and `shareMedia` is imported from `com.gallery.ui.common`.
- [ ] **Git commit:**
  ```
  git add app/src/main/kotlin/com/gallery/ui/photos/PhotosScreen.kt
  git commit -m "feat(photos): wire share intent and move-to-hidden in PhotosScreen multi-select bar"
  ```

---

## Task 5: Update SearchViewModel — inject AddToHiddenUseCase

**File:** `app/src/main/kotlin/com/gallery/ui/search/SearchViewModel.kt` (existing)

Inject `AddToHiddenUseCase` into the constructor and add `addSelectedToHidden()`, following the same pattern as `PhotosViewModel`:

```kotlin
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchMedia: SearchMediaUseCase,
    private val moveToTrash: MoveToTrashUseCase,
    private val addToHidden: AddToHiddenUseCase          // NEW
) : ViewModel() {
    // ... existing code unchanged ...

    // NEW
    fun addSelectedToHidden() {
        val ids = _uiState.value.selectedIds
        viewModelScope.launch {
            ids.forEach { addToHidden(it) }
            clearSelection()
        }
    }
}
```

**Tests to add in `SearchViewModelTest.kt`:**

```kotlin
// In setUp(), add:
private lateinit var addToHidden: AddToHiddenUseCase

// In setUp() body:
addToHidden = mockk(relaxed = true)

// Update buildVm():
private fun buildVm() = SearchViewModel(searchMedia, moveToTrash, addToHidden)

// New test:
@Test
fun `addSelectedToHidden calls AddToHiddenUseCase for each selected id and clears selection`() = runTest {
    every { searchMedia(any()) } returns flowOf(emptyList())

    val vm = buildVm()
    vm.toggleSelection(2L)
    vm.toggleSelection(4L)
    vm.addSelectedToHidden()

    coVerify(exactly = 1) { addToHidden(2L) }
    coVerify(exactly = 1) { addToHidden(4L) }
    assertTrue(vm.uiState.value.selectedIds.isEmpty())
}
```

- [ ] **Run test: FAIL** — Add the new test; verify it fails.
- [ ] **Implement** the constructor change and `addSelectedToHidden()` in `SearchViewModel.kt`.
- [ ] **Run test: PASS** — All tests green.
- [ ] **Git commit:**
  ```
  git add app/src/main/kotlin/com/gallery/ui/search/SearchViewModel.kt \
          app/src/test/kotlin/com/gallery/ui/search/SearchViewModelTest.kt
  git commit -m "feat(search): inject AddToHiddenUseCase, add addSelectedToHidden action and test"
  ```

---

## Task 6: Wire Share and Move to Hidden in SearchScreen and AlbumDetailScreen

### 6a. SearchScreen

**File:** `app/src/main/kotlin/com/gallery/ui/search/SearchScreen.kt` (existing)

Apply the same pattern as `PhotosScreen`:

```kotlin
val context = LocalContext.current

// In SelectionActionBar call:
onShare = {
    val selectedItems = uiState.results
        .filter { it.id in uiState.selectedIds }
    context.shareMedia(selectedItems)
    viewModel.clearSelection()
},
onMoveToHidden = { viewModel.addSelectedToHidden() },
```

### 6b. AlbumDetailScreen

**File:** `app/src/main/kotlin/com/gallery/ui/albums/AlbumDetailScreen.kt` (existing)

The `AlbumDetailScreen` uses its own ViewModel. If `AlbumDetailViewModel` does not already have `addSelectedToHidden()`, inject `AddToHiddenUseCase` and add it following the same pattern (same constructor injection + coroutine loop + `clearSelection()`).

Then update the `SelectionActionBar` call:

```kotlin
val context = LocalContext.current

// In SelectionActionBar call:
onShare = {
    val selectedItems = uiState.items
        .filter { it.id in uiState.selectedIds }
    context.shareMedia(selectedItems)
    viewModel.clearSelection()
},
onMoveToHidden = { viewModel.addSelectedToHidden() },
```

- [ ] Open `SearchScreen.kt` and apply the share + onMoveToHidden wiring.
- [ ] Open `AlbumDetailScreen.kt` (and its ViewModel if needed) and apply the same.
- [ ] Verify all screens compile.
- [ ] **Git commit:**
  ```
  git add app/src/main/kotlin/com/gallery/ui/search/SearchScreen.kt \
          app/src/main/kotlin/com/gallery/ui/albums/AlbumDetailScreen.kt
  git commit -m "feat(multi-select): wire share and move-to-hidden in Search and AlbumDetail screens"
  ```

---

## Run Full Phase 15 Test Suite

```
./gradlew :app:testDebugUnitTest --tests "com.gallery.ui.photos.*" \
                                  --tests "com.gallery.ui.search.*"
```

Expected: all existing tests pass, plus the two new `addSelectedToHidden` tests.

---

## Acceptance Criteria

- [ ] `shareMedia` launches `ACTION_SEND` for a single item and `ACTION_SEND_MULTIPLE` for multiple items
- [ ] `shareMedia` uses the exact MIME type when all items share the same type, `*/*` otherwise
- [ ] `FLAG_GRANT_READ_URI_PERMISSION` is set on all share intents
- [ ] `SelectionActionBar` compiles without changes at all existing call-sites (parameter defaults to `null`)
- [ ] `SelectionActionBar` shows the "Move to Hidden Album" icon button only when `onMoveToHidden != null`
- [ ] `PhotosViewModel.addSelectedToHidden()` calls `AddToHiddenUseCase` for every selected ID and clears selection
- [ ] `SearchViewModel.addSelectedToHidden()` calls `AddToHiddenUseCase` for every selected ID and clears selection
- [ ] `AlbumDetailViewModel` (or equivalent) also exposes `addSelectedToHidden()`
- [ ] Photos, Search, and AlbumDetail screens each pass `onMoveToHidden` to `SelectionActionBar`
- [ ] Selection is cleared after Share is triggered in all three screens
- [ ] All ViewModel unit tests pass (existing + new)
- [ ] No direct use of `HiddenRepository` inside any Screen composable (always goes through ViewModel)

---

## Summary

| Task | Production file(s) | Test file(s) | Tests |
|------|--------------------|--------------|-------|
| 1 | `ShareUtils.kt` (new) | _(manual tests only)_ | — |
| 2 | `SelectionActionBar.kt` (modified) | — | — |
| 3 | `PhotosViewModel.kt` (modified) | `PhotosViewModelTest.kt` (modified) | +1 |
| 4 | `PhotosScreen.kt` (modified) | _(UI tests — later phase)_ | — |
| 5 | `SearchViewModel.kt` (modified) | `SearchViewModelTest.kt` (modified) | +1 |
| 6 | `SearchScreen.kt`, `AlbumDetailScreen.kt` (modified) | _(UI tests — later phase)_ | — |
