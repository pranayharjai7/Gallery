# Phase 18: On This Day Memories — Gallery App

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Dismissible "On This Day" card at top of Photos grid + full-screen slideshow viewer.

**Depends on:** Phase 08 (PhotosScreen already loads `memoriesItems`), Phase 07 (common UI components), Phase 01 (`GetOnThisDayUseCase`)

---

## Overview

The Memories feature surfaces photos and videos taken on the same calendar date (month + day) in a previous year. The Photos screen already exposes `memoriesItems` from `PhotosViewModel` (Phase 08). This phase adds two new composables:

1. **MemoriesCard** — a dismissible hero banner at the top of the Photos grid showing a mini photo strip with a dark scrim overlay, title, and item count. Tapping it navigates to the slideshow.
2. **SlideshowScreen** — a full-screen auto-advancing slideshow with fade transitions between items, a back button, and a current/total counter.

No new ViewModel is strictly required; `SlideshowScreen` can re-use the existing `PhotosViewModel` instance already on the back stack. A dedicated `SlideshowViewModel` is also described as an alternative for teams that prefer stricter encapsulation.

---

## File Structure

```
app/src/main/kotlin/com/gallery/ui/memories/
├── MemoriesCard.kt
└── SlideshowScreen.kt
```

---

## Dependencies to Verify in `build.gradle.kts`

```kotlin
// Coil — already present from Phase 07
implementation("io.coil-kt.coil3:coil-compose:<version>")

// Compose Animation — already included via BOM
implementation("androidx.compose.animation:animation")

// Lifecycle
implementation("androidx.lifecycle:lifecycle-runtime-compose:<version>")

// Hilt Navigation Compose
implementation("androidx.hilt:hilt-navigation-compose:<version>")
```

---

## Task 1: MemoriesCard

**File:** `app/src/main/kotlin/com/gallery/ui/memories/MemoriesCard.kt`

A stateless composable that requires at least one item to render. It shows the first three media items as a horizontal photo strip with a semi-transparent dark scrim on top, title text at the bottom-left, and a close (dismiss) button at the top-right.

```kotlin
package com.gallery.ui.memories

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.gallery.domain.model.MediaItem

@Composable
fun MemoriesCard(
    items: List<MediaItem>,
    onDismiss: () -> Unit,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Do not render anything if there are no memories
    if (items.isEmpty()) return

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable(onClick = onTap),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box {
            // ── Background: first 3 items in a horizontal strip ─────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                items.take(3).forEach { item ->
                    AsyncImage(
                        model = item.uri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            }

            // ── Dark scrim over the strip ────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(Color.Black.copy(alpha = 0.4f))
            )

            // ── Title and item count ─────────────────────────────────────────
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            ) {
                Text(
                    text = "On This Day",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Text(
                    text = "${items.size} ${if (items.size == 1) "memory" else "memories"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }

            // ── Dismiss button ───────────────────────────────────────────────
            IconButton(
                modifier = Modifier.align(Alignment.TopEnd),
                onClick = onDismiss
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss memories",
                    tint = Color.White
                )
            }
        }
    }
}
```

**Integration in PhotosScreen (Phase 08 update):**

`PhotosScreen.kt` already has a `LazyVerticalGrid`. Insert the `MemoriesCard` as the first item using a full-span item before the media grid items:

```kotlin
// Inside LazyVerticalGrid content block, before the media items:
if (uiState.memoriesItems.isNotEmpty() && !uiState.memoriesDismissed) {
    item(span = { GridItemSpan(maxLineSpan) }) {
        MemoriesCard(
            items = uiState.memoriesItems,
            onDismiss = { viewModel.dismissMemories() },
            onTap = { navController.navigate("slideshow") }
        )
    }
}
```

Add `memoriesDismissed: Boolean = false` to `PhotosUiState` and a `dismissMemories()` function to `PhotosViewModel`:

```kotlin
fun dismissMemories() = _uiState.update { it.copy(memoriesDismissed = true) }
```

The dismissed state is in-memory only (resets on app restart), which matches the intended UX: the card reappears fresh each day the app is launched.

Commit message: `feat(memories): add MemoriesCard composable and wire into PhotosScreen`

---

## Task 2: SlideshowScreen

**File:** `app/src/main/kotlin/com/gallery/ui/memories/SlideshowScreen.kt`

A full-screen auto-advancing slideshow. Images cross-fade every 4 seconds using `AnimatedContent`. The user can navigate back at any time via the top-left back button. A counter in the top-right shows the current position.

```kotlin
package com.gallery.ui.memories

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.gallery.ui.photos.PhotosViewModel
import kotlinx.coroutines.delay

private const val SLIDESHOW_INTERVAL_MS = 4_000L

@Composable
fun SlideshowScreen(
    onBack: () -> Unit,
    viewModel: PhotosViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val items = uiState.memoriesItems

    // If there are no memories, go back immediately
    if (items.isEmpty()) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    var currentIndex by remember { mutableIntStateOf(0) }

    // Auto-advance the slideshow
    LaunchedEffect(Unit) {
        while (true) {
            delay(SLIDESHOW_INTERVAL_MS)
            currentIndex = (currentIndex + 1) % items.size
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // ── Animated image ─────────────────────────────────────────────────
        AnimatedContent(
            targetState = currentIndex,
            transitionSpec = {
                fadeIn(animationSpec = tween(durationMillis = 800)) togetherWith
                        fadeOut(animationSpec = tween(durationMillis = 800))
            },
            label = "slideshow_transition"
        ) { idx ->
            AsyncImage(
                model = items[idx].uri,
                contentDescription = items[idx].name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }

        // ── Back button ────────────────────────────────────────────────────
        IconButton(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp),
            onClick = onBack
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }

        // ── Item counter ───────────────────────────────────────────────────
        Text(
            text = "${currentIndex + 1} / ${items.size}",
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            color = Color.White,
            style = MaterialTheme.typography.labelLarge
        )
    }
}
```

**ViewModel approach — two options:**

| Approach | Pros | Cons |
|---|---|---|
| **Reuse `PhotosViewModel`** (shown above) | No new ViewModel needed; `memoriesItems` is already loaded; Hilt provides the same instance while both destinations share the back stack | Couples the slideshow to `PhotosViewModel`; harder to unit-test in isolation |
| **Dedicated `SlideshowViewModel`** | Clean separation; can re-query memories by date argument; easier to test | Requires a new ViewModel class, an additional `GetOnThisDayUseCase` call, and a `date` nav argument |

The shared-ViewModel approach is recommended for simplicity. If a `SlideshowViewModel` is preferred, it should:
1. Accept a `date: String` argument via `SavedStateHandle`
2. Inject `GetOnThisDayUseCase` and collect its flow into `StateFlow<List<MediaItem>>`
3. Be registered in the `slideshow/{date}` route

---

## Navigation Wiring

Register both routes in the NavHost (e.g., `AppNavigation.kt`):

```kotlin
// Slideshow — shares the PhotosViewModel instance from the back stack
composable(route = "slideshow") {
    SlideshowScreen(
        onBack = { navController.popBackStack() }
    )
}
```

The `onTap` lambda in `MemoriesCard` integration (Task 1) navigates to `"slideshow"`.

---

## Acceptance Criteria

- [ ] `MemoriesCard` does not render when `memoriesItems` is empty
- [ ] `MemoriesCard` shows up to three preview thumbnails; if there are fewer than three items, only those items fill the strip
- [ ] The item count label reads "1 memory" (singular) or "N memories" (plural)
- [ ] Tapping the dismiss `X` button hides the card for the remainder of the app session; it does not persist across restarts
- [ ] Tapping the card body navigates to `SlideshowScreen`
- [ ] `SlideshowScreen` auto-advances to the next item every 4 seconds with a cross-fade animation
- [ ] The index counter at the top-right updates correctly as items advance
- [ ] When the slideshow reaches the last item it wraps back to the first
- [ ] If `memoriesItems` is empty when `SlideshowScreen` opens, it immediately calls `onBack`
- [ ] The back button returns to the Photos screen
