# Phase 7: Common UI Components — Gallery App

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` or `superpowers:executing-plans`.

**Goal:** Build reusable Composables used across multiple screens: MediaThumbnail, EmptyState, LoadingState, and SelectionOverlay.

**Depends on:** Phase 6.

---

## Task 7.1: MediaThumbnail

**File:** `app/src/main/kotlin/com/gallery/ui/common/MediaThumbnail.kt`

- [ ] Create the file `app/src/main/kotlin/com/gallery/ui/common/MediaThumbnail.kt` with the following content:

```kotlin
package com.gallery.ui.common

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.gallery.data.model.MediaItem

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaThumbnail(
    item: MediaItem,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .clip(RoundedCornerShape(2.dp))
    ) {
        AsyncImage(
            model = item.uri,
            contentDescription = item.name,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        if (item.isVideo) {
            // semi-transparent scrim
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.2f)))
            // play icon centered
            Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.align(Alignment.Center).size(28.dp))
            // duration badge bottom-end
            item.duration?.let { ms ->
                val seconds = ms / 1000
                val text = if (seconds >= 3600) "%d:%02d:%02d".format(seconds/3600, (seconds%3600)/60, seconds%60)
                           else "%d:%02d".format(seconds/60, seconds%60)
                Text(text, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp)
                        .background(Color.Black.copy(0.5f), RoundedCornerShape(3.dp)).padding(horizontal = 4.dp, vertical = 1.dp))
            }
        }
        if (isSelected) {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)))
            Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.TopStart).padding(4.dp).size(20.dp))
        }
    }
}
```

- [ ] Verify the file was created successfully and compiles without errors by reviewing imports and usages of `MediaItem`, `AsyncImage`, `combinedClickable`, and Material 3 components.
- [ ] Run `./gradlew :app:compileDebugKotlin` and confirm no errors in `MediaThumbnail.kt`.
- [ ] Commit: `git add app/src/main/kotlin/com/gallery/ui/common/MediaThumbnail.kt && git commit -m "feat: add MediaThumbnail composable with video overlay and selection state"`

---

## Task 7.2: EmptyState

**File:** `app/src/main/kotlin/com/gallery/ui/common/EmptyState.kt`

- [ ] Create the file `app/src/main/kotlin/com/gallery/ui/common/EmptyState.kt` with the following content:

```kotlin
package com.gallery.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun EmptyState(
    icon: ImageVector = Icons.Default.PhotoLibrary,
    title: String,
    message: String,
    action: Pair<String, () -> Unit>? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 32.dp))
        action?.let { (label, onClick) ->
            Spacer(Modifier.height(24.dp))
            Button(onClick = onClick) { Text(label) }
        }
    }
}
```

- [ ] Verify the file was created successfully. Confirm `ImageVector`, `Pair<String, () -> Unit>`, and all Material 3 imports resolve correctly.
- [ ] Run `./gradlew :app:compileDebugKotlin` and confirm no errors in `EmptyState.kt`.
- [ ] Commit: `git add app/src/main/kotlin/com/gallery/ui/common/EmptyState.kt && git commit -m "feat: add EmptyState composable with optional action button"`

---

## Task 7.3: LoadingState

**File:** `app/src/main/kotlin/com/gallery/ui/common/LoadingState.kt`

- [ ] Create the file `app/src/main/kotlin/com/gallery/ui/common/LoadingState.kt` with the following content:

```kotlin
package com.gallery.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
```

- [ ] Verify the file was created successfully. Confirm `CircularProgressIndicator` is imported from `androidx.compose.material3`.
- [ ] Run `./gradlew :app:compileDebugKotlin` and confirm no errors in `LoadingState.kt`.
- [ ] Commit: `git add app/src/main/kotlin/com/gallery/ui/common/LoadingState.kt && git commit -m "feat: add LoadingState composable"`

---

## Task 7.4: DateHeader

**File:** `app/src/main/kotlin/com/gallery/ui/common/DateHeader.kt`

- [ ] Create the file `app/src/main/kotlin/com/gallery/ui/common/DateHeader.kt` with the following content:

```kotlin
package com.gallery.ui.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun DateHeader(dateLabel: String, modifier: Modifier = Modifier) {
    Text(
        text = dateLabel,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(horizontal = 12.dp, vertical = 8.dp)
    )
}

fun formatDateHeader(timestamp: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
    val today = Calendar.getInstance()
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    return when {
        cal.isSameDay(today) -> "Today"
        cal.isSameDay(yesterday) -> "Yesterday"
        cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) ->
            SimpleDateFormat("MMMM d", Locale.getDefault()).format(Date(timestamp))
        else -> SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}

private fun Calendar.isSameDay(other: Calendar) =
    get(Calendar.YEAR) == other.get(Calendar.YEAR) && get(Calendar.DAY_OF_YEAR) == other.get(Calendar.DAY_OF_YEAR)
```

- [ ] Verify the file was created successfully. Confirm `Calendar`, `SimpleDateFormat`, `Date`, and `Locale` are imported from `java.*`. Confirm `DateHeader` uses `MaterialTheme.typography.labelLarge`.
- [ ] Run `./gradlew :app:compileDebugKotlin` and confirm no errors in `DateHeader.kt`.
- [ ] Commit: `git add app/src/main/kotlin/com/gallery/ui/common/DateHeader.kt && git commit -m "feat: add DateHeader composable and formatDateHeader utility"`

---

## Task 7.5: SelectionActionBar

**File:** `app/src/main/kotlin/com/gallery/ui/common/SelectionActionBar.kt`

- [ ] Create the file `app/src/main/kotlin/com/gallery/ui/common/SelectionActionBar.kt` with the following content:

```kotlin
package com.gallery.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionActionBar(
    selectedCount: Int,
    onSelectAll: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = { Text("$selectedCount selected") },
        navigationIcon = { IconButton(onClick = onClear) { Icon(Icons.Default.Close, "Clear selection") } },
        actions = {
            IconButton(onClick = onSelectAll) { Icon(Icons.Default.SelectAll, "Select all") }
            IconButton(onClick = onShare) { Icon(Icons.Default.Share, "Share") }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Delete") }
        },
        modifier = modifier,
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    )
}
```

- [ ] Verify the file was created successfully. Confirm all four `Icons.Default.*` references (`Close`, `SelectAll`, `Share`, `Delete`) resolve. Confirm `@OptIn(ExperimentalMaterial3Api::class)` is present since `TopAppBar` requires it.
- [ ] Run `./gradlew :app:compileDebugKotlin` and confirm no errors in `SelectionActionBar.kt`.
- [ ] Commit: `git add app/src/main/kotlin/com/gallery/ui/common/SelectionActionBar.kt && git commit -m "feat: add SelectionActionBar composable for multi-select mode"`

---

## Phase 7 Completion Checklist

- [ ] All five files exist under `app/src/main/kotlin/com/gallery/ui/common/`:
  - `MediaThumbnail.kt`
  - `EmptyState.kt`
  - `LoadingState.kt`
  - `DateHeader.kt`
  - `SelectionActionBar.kt`
- [ ] Full project compiles: `./gradlew :app:assembleDebug` succeeds with no errors.
- [ ] Each composable has been committed in its own git commit with a descriptive message.
- [ ] No placeholder comments (`TODO`, `// ...`) remain in any of the five files.
