# Phase 3: MediaStore Data Layer — Gallery App

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans`.

**Goal:** Implement reactive MediaStore repositories that query photos/videos from device storage and rebuild smart albums, all backed by ContentObserver-wrapped Flows.

**Depends on:** Phase 0, Phase 1.

---

## Overview

This phase wires the Android MediaStore API into the app's domain layer. Three files are produced:

| File | Purpose |
|---|---|
| `ContentObserverFlow.kt` | Extension that turns a `ContentObserver` registration into a cold `Flow<Unit>` |
| `MediaStoreMediaRepository.kt` | `MediaRepository` implementation backed by `MediaStore.Files` |
| `MediaStoreAlbumRepository.kt` | `AlbumRepository` implementation that derives albums from live media data |

No instrumented tests are written for this phase. MediaStore queries require a real device with media on it; unit-testable logic (sorting, grouping, smart-album classification) can be extracted later when a `FakeMediaRepository` exists. A note is left at the bottom of this plan.

---

## Task 3.1 — ContentObserver Flow helper

**File:** `app/src/main/kotlin/com/gallery/data/mediastore/ContentObserverFlow.kt`

- [ ] Create the file `app/src/main/kotlin/com/gallery/data/mediastore/ContentObserverFlow.kt` with the following content:

```kotlin
package com.gallery.data.mediastore

import android.content.ContentResolver
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.onStart

/**
 * Returns a [Flow] that emits [Unit] once immediately (via [onStart]) and then again
 * every time the given [uri] changes in the [ContentResolver].
 *
 * The [ContentObserver] is automatically unregistered when the downstream collector
 * cancels or the flow scope ends.
 */
fun ContentResolver.observeUri(uri: Uri): Flow<Unit> = callbackFlow {
    val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            trySend(Unit)
        }
    }
    registerContentObserver(uri, true, observer)
    awaitClose { unregisterContentObserver(observer) }
}.onStart { emit(Unit) }
```

- [ ] Confirm the package declaration matches the directory structure (`com.gallery.data.mediastore`).

- [ ] Stage and commit:

```
git add app/src/main/kotlin/com/gallery/data/mediastore/ContentObserverFlow.kt
git commit -m "feat(data): add ContentObserver Flow helper"
```

---

## Task 3.2 — MediaStoreMediaRepository

**File:** `app/src/main/kotlin/com/gallery/data/mediastore/MediaStoreMediaRepository.kt`

### 3.2.1 — Skeleton and dependencies

- [ ] Create the file with the correct package, imports, and `@Singleton` / `@Inject` annotations. The class receives `@ApplicationContext context: Context` via constructor injection (Hilt).

### 3.2.2 — Projection constant

- [ ] Define a private `PROJECTION` array at the top of the file containing exactly these column names:

```kotlin
private val PROJECTION = arrayOf(
    MediaStore.Files.FileColumns._ID,
    MediaStore.Files.FileColumns.DISPLAY_NAME,
    MediaStore.Files.FileColumns.DATE_TAKEN,
    MediaStore.Files.FileColumns.SIZE,
    MediaStore.Files.FileColumns.WIDTH,
    MediaStore.Files.FileColumns.HEIGHT,
    MediaStore.Files.FileColumns.DURATION,
    MediaStore.Files.FileColumns.MIME_TYPE,
    MediaStore.Files.FileColumns.BUCKET_ID,
    MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME,
    MediaStore.Files.FileColumns.LATITUDE,
    MediaStore.Files.FileColumns.LONGITUDE,
)
```

### 3.2.3 — `queryMedia` private function

- [ ] Implement `queryMedia(selection: String?, selectionArgs: Array<String>?): List<MediaItem>`. This function:
  - Calls `context.contentResolver.query(...)` with `MEDIA_URI`, `PROJECTION`, `selection`, `selectionArgs`, and `"${MediaStore.Files.FileColumns.DATE_TAKEN} DESC"` as the sort order.
  - Iterates the cursor with `use { cursor -> ... }` to guarantee cursor closure.
  - Maps each row to a `MediaItem`, reading each column by index for performance.
  - Handles nullable `LATITUDE` / `LONGITUDE`: if either is `null` or `0.0` the `location` field is set to `null`; otherwise it is `Pair(lat, lon)`.
  - Returns an empty list when the cursor is `null`.

### 3.2.4 — `observeAll`

- [ ] Implement `observeAll()`:

```kotlin
override fun observeAll(): Flow<List<MediaItem>> =
    context.contentResolver.observeUri(MEDIA_URI)
        .map {
            queryMedia(
                selection = "${MediaStore.Files.FileColumns.MIME_TYPE} LIKE 'image/%'" +
                    " OR ${MediaStore.Files.FileColumns.MIME_TYPE} LIKE 'video/%'",
                selectionArgs = null,
            )
        }
        .flowOn(Dispatchers.IO)
```

### 3.2.5 — `observeByBucket`

- [ ] Implement `observeByBucket(bucketId: Long)`:

```kotlin
override fun observeByBucket(bucketId: Long): Flow<List<MediaItem>> =
    context.contentResolver.observeUri(MEDIA_URI)
        .map {
            queryMedia(
                selection = "(${MediaStore.Files.FileColumns.MIME_TYPE} LIKE 'image/%'" +
                    " OR ${MediaStore.Files.FileColumns.MIME_TYPE} LIKE 'video/%')" +
                    " AND ${MediaStore.Files.FileColumns.BUCKET_ID} = ?",
                selectionArgs = arrayOf(bucketId.toString()),
            )
        }
        .flowOn(Dispatchers.IO)
```

### 3.2.6 — `search`

- [ ] Implement `search(query: String)`:

```kotlin
override fun search(query: String): Flow<List<MediaItem>> =
    context.contentResolver.observeUri(MEDIA_URI)
        .map {
            queryMedia(
                selection = "(${MediaStore.Files.FileColumns.MIME_TYPE} LIKE 'image/%'" +
                    " OR ${MediaStore.Files.FileColumns.MIME_TYPE} LIKE 'video/%')" +
                    " AND ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ?",
                selectionArgs = arrayOf("%$query%"),
            )
        }
        .flowOn(Dispatchers.IO)
```

### 3.2.7 — `getById`

- [ ] Implement `getById(id: Long)`:

```kotlin
override suspend fun getById(id: Long): MediaItem? = withContext(Dispatchers.IO) {
    queryMedia(
        selection = "${MediaStore.Files.FileColumns._ID} = ?",
        selectionArgs = arrayOf(id.toString()),
    ).firstOrNull()
}
```

### 3.2.8 — `getOnThisDay`

- [ ] Implement `getOnThisDay(monthDay: Int)`. The parameter encodes month and day as `month * 100 + day` (e.g., May 7 = `507`). Use in-memory filtering after fetching all photo/video items because SQLite's `strftime` is not available through the MediaStore SQL surface:

```kotlin
override fun getOnThisDay(monthDay: Int): Flow<List<MediaItem>> {
    val month = monthDay / 100   // e.g. 507 / 100 = 5
    val day   = monthDay % 100   // e.g. 507 % 100 = 7
    return context.contentResolver.observeUri(MEDIA_URI)
        .map {
            val all = queryMedia(
                selection = "${MediaStore.Files.FileColumns.MIME_TYPE} LIKE 'image/%'" +
                    " OR ${MediaStore.Files.FileColumns.MIME_TYPE} LIKE 'video/%'",
                selectionArgs = null,
            )
            all.filter { item ->
                val cal = java.util.Calendar.getInstance().apply {
                    timeInMillis = item.dateTaken
                }
                cal.get(java.util.Calendar.MONTH) + 1 == month &&
                    cal.get(java.util.Calendar.DAY_OF_MONTH) == day
            }
        }
        .flowOn(Dispatchers.IO)
}
```

### 3.2.9 — Complete file

- [ ] Verify the final file looks exactly like this (copy in full):

```kotlin
package com.gallery.data.mediastore

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.gallery.domain.MediaItem
import com.gallery.domain.MediaRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private val MEDIA_URI: Uri = MediaStore.Files.getContentUri("external")

private val PROJECTION = arrayOf(
    MediaStore.Files.FileColumns._ID,
    MediaStore.Files.FileColumns.DISPLAY_NAME,
    MediaStore.Files.FileColumns.DATE_TAKEN,
    MediaStore.Files.FileColumns.SIZE,
    MediaStore.Files.FileColumns.WIDTH,
    MediaStore.Files.FileColumns.HEIGHT,
    MediaStore.Files.FileColumns.DURATION,
    MediaStore.Files.FileColumns.MIME_TYPE,
    MediaStore.Files.FileColumns.BUCKET_ID,
    MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME,
    MediaStore.Files.FileColumns.LATITUDE,
    MediaStore.Files.FileColumns.LONGITUDE,
)

@Singleton
class MediaStoreMediaRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : MediaRepository {

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    override fun observeAll(): Flow<List<MediaItem>> =
        context.contentResolver.observeUri(MEDIA_URI)
            .map {
                queryMedia(
                    selection = "${MediaStore.Files.FileColumns.MIME_TYPE} LIKE 'image/%'" +
                        " OR ${MediaStore.Files.FileColumns.MIME_TYPE} LIKE 'video/%'",
                    selectionArgs = null,
                )
            }
            .flowOn(Dispatchers.IO)

    override fun observeByBucket(bucketId: Long): Flow<List<MediaItem>> =
        context.contentResolver.observeUri(MEDIA_URI)
            .map {
                queryMedia(
                    selection = "(${MediaStore.Files.FileColumns.MIME_TYPE} LIKE 'image/%'" +
                        " OR ${MediaStore.Files.FileColumns.MIME_TYPE} LIKE 'video/%')" +
                        " AND ${MediaStore.Files.FileColumns.BUCKET_ID} = ?",
                    selectionArgs = arrayOf(bucketId.toString()),
                )
            }
            .flowOn(Dispatchers.IO)

    override fun search(query: String): Flow<List<MediaItem>> =
        context.contentResolver.observeUri(MEDIA_URI)
            .map {
                queryMedia(
                    selection = "(${MediaStore.Files.FileColumns.MIME_TYPE} LIKE 'image/%'" +
                        " OR ${MediaStore.Files.FileColumns.MIME_TYPE} LIKE 'video/%')" +
                        " AND ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ?",
                    selectionArgs = arrayOf("%$query%"),
                )
            }
            .flowOn(Dispatchers.IO)

    override suspend fun getById(id: Long): MediaItem? = withContext(Dispatchers.IO) {
        queryMedia(
            selection = "${MediaStore.Files.FileColumns._ID} = ?",
            selectionArgs = arrayOf(id.toString()),
        ).firstOrNull()
    }

    override fun getOnThisDay(monthDay: Int): Flow<List<MediaItem>> {
        val month = monthDay / 100
        val day   = monthDay % 100
        return context.contentResolver.observeUri(MEDIA_URI)
            .map {
                val all = queryMedia(
                    selection = "${MediaStore.Files.FileColumns.MIME_TYPE} LIKE 'image/%'" +
                        " OR ${MediaStore.Files.FileColumns.MIME_TYPE} LIKE 'video/%'",
                    selectionArgs = null,
                )
                all.filter { item ->
                    val cal = java.util.Calendar.getInstance().apply {
                        timeInMillis = item.dateTaken
                    }
                    cal.get(java.util.Calendar.MONTH) + 1 == month &&
                        cal.get(java.util.Calendar.DAY_OF_MONTH) == day
                }
            }
            .flowOn(Dispatchers.IO)
    }

    // -------------------------------------------------------------------------
    // Internal query
    // -------------------------------------------------------------------------

    private fun queryMedia(
        selection: String?,
        selectionArgs: Array<String>?,
    ): List<MediaItem> {
        val items = mutableListOf<MediaItem>()
        context.contentResolver.query(
            MEDIA_URI,
            PROJECTION,
            selection,
            selectionArgs,
            "${MediaStore.Files.FileColumns.DATE_TAKEN} DESC",
        )?.use { cursor ->
            val idCol          = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val nameCol        = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val dateTakenCol   = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_TAKEN)
            val sizeCol        = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
            val widthCol       = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.WIDTH)
            val heightCol      = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.HEIGHT)
            val durationCol    = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DURATION)
            val mimeCol        = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
            val bucketIdCol    = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_ID)
            val bucketNameCol  = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME)
            val latCol         = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.LATITUDE)
            val lonCol         = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.LONGITUDE)

            while (cursor.moveToNext()) {
                val id       = cursor.getLong(idCol)
                val lat      = if (cursor.isNull(latCol)) null else cursor.getDouble(latCol)
                val lon      = if (cursor.isNull(lonCol)) null else cursor.getDouble(lonCol)
                val location = if (lat != null && lon != null && (lat != 0.0 || lon != 0.0)) {
                    Pair(lat, lon)
                } else {
                    null
                }
                val durationRaw = if (cursor.isNull(durationCol)) null else cursor.getLong(durationCol)

                items += MediaItem(
                    id          = id,
                    uri         = ContentUris.withAppendedId(MEDIA_URI, id),
                    name        = cursor.getString(nameCol).orEmpty(),
                    dateTaken   = cursor.getLong(dateTakenCol),
                    size        = cursor.getLong(sizeCol),
                    width       = cursor.getInt(widthCol),
                    height      = cursor.getInt(heightCol),
                    duration    = durationRaw,
                    mimeType    = cursor.getString(mimeCol).orEmpty(),
                    bucketId    = cursor.getLong(bucketIdCol),
                    bucketName  = cursor.getString(bucketNameCol).orEmpty(),
                    location    = location,
                )
            }
        }
        return items
    }
}
```

- [ ] Stage and commit:

```
git add app/src/main/kotlin/com/gallery/data/mediastore/MediaStoreMediaRepository.kt
git commit -m "feat(data): implement MediaStoreMediaRepository"
```

---

## Task 3.3 — MediaStoreAlbumRepository

**File:** `app/src/main/kotlin/com/gallery/data/mediastore/MediaStoreAlbumRepository.kt`

### 3.3.1 — Smart album definitions

- [ ] Define a private sealed class or data class hierarchy for smart album specs. Use a simple list of `SmartAlbumSpec` data classes holding the album's stable string id, display name, and a predicate `(MediaItem) -> Boolean`:

```kotlin
private data class SmartAlbumSpec(
    val id: String,
    val name: String,
    val matches: (MediaItem) -> Boolean,
)

private val SMART_ALBUM_SPECS = listOf(
    SmartAlbumSpec("smart_camera",      "Camera")      { it.bucketName.contains("Camera",      ignoreCase = true) },
    SmartAlbumSpec("smart_screenshots", "Screenshots") { it.bucketName.contains("Screenshots", ignoreCase = true) },
    SmartAlbumSpec("smart_downloads",   "Downloads")   { it.bucketName.contains("Download",    ignoreCase = true) },
    SmartAlbumSpec("smart_videos",      "Videos")      { it.mimeType.startsWith("video/") },
    SmartAlbumSpec("smart_slowmo",      "Slow Motion") { it.bucketName.contains("slow",        ignoreCase = true) },
    SmartAlbumSpec("smart_whatsapp",    "WhatsApp")    { it.bucketName.contains("WhatsApp",    ignoreCase = true) },
)
```

### 3.3.2 — `observeAlbums` implementation

- [ ] Implement `observeAlbums(hiddenIds: Set<Long>)`. The function:
  1. Calls `mediaRepository.observeAll()` so it shares the same `ContentObserver` trigger.
  2. Maps each emission to a list of `Album` objects using a pure `buildAlbums(items, hiddenIds)` helper function.
  3. Applies `.flowOn(Dispatchers.Default)` because the grouping work is CPU-bound, not I/O-bound.

```kotlin
override fun observeAlbums(hiddenIds: Set<Long>): Flow<List<Album>> =
    mediaRepository.observeAll()
        .map { items -> buildAlbums(items, hiddenIds) }
        .flowOn(Dispatchers.Default)
```

### 3.3.3 — `buildAlbums` helper

- [ ] Implement the private `buildAlbums(allItems: List<MediaItem>, hiddenIds: Set<Long>): List<Album>` function with the following steps:

  1. Filter out hidden items: `val visibleItems = allItems.filter { it.id !in hiddenIds }`
  2. Build SMART albums from `SMART_ALBUM_SPECS`. For each spec, filter `visibleItems` by `spec.matches`. Skip if empty. The cover is the first item (already sorted by `DATE_TAKEN DESC` from the repository).
  3. Group `visibleItems` by `bucketId` to build CUSTOM albums. Album name = `bucketName`. Cover = first item in the group. Sort custom albums by name ascending.
  4. Concatenate smart albums before custom albums.
  5. Return the concatenated list.

### 3.3.4 — Complete file

- [ ] Verify the final file looks exactly like this (copy in full):

```kotlin
package com.gallery.data.mediastore

import com.gallery.domain.Album
import com.gallery.domain.AlbumRepository
import com.gallery.domain.AlbumType
import com.gallery.domain.MediaItem
import com.gallery.domain.MediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

// ---------------------------------------------------------------------------
// Smart album specifications
// ---------------------------------------------------------------------------

private data class SmartAlbumSpec(
    val id: String,
    val name: String,
    val matches: (MediaItem) -> Boolean,
)

private val SMART_ALBUM_SPECS = listOf(
    SmartAlbumSpec("smart_camera",      "Camera")      { it.bucketName.contains("Camera",      ignoreCase = true) },
    SmartAlbumSpec("smart_screenshots", "Screenshots") { it.bucketName.contains("Screenshots", ignoreCase = true) },
    SmartAlbumSpec("smart_downloads",   "Downloads")   { it.bucketName.contains("Download",    ignoreCase = true) },
    SmartAlbumSpec("smart_videos",      "Videos")      { it.mimeType.startsWith("video/") },
    SmartAlbumSpec("smart_slowmo",      "Slow Motion") { it.bucketName.contains("slow",        ignoreCase = true) },
    SmartAlbumSpec("smart_whatsapp",    "WhatsApp")    { it.bucketName.contains("WhatsApp",    ignoreCase = true) },
)

// ---------------------------------------------------------------------------
// Repository
// ---------------------------------------------------------------------------

@Singleton
class MediaStoreAlbumRepository @Inject constructor(
    private val mediaRepository: MediaRepository,
) : AlbumRepository {

    override fun observeAlbums(hiddenIds: Set<Long>): Flow<List<Album>> =
        mediaRepository.observeAll()
            .map { items -> buildAlbums(items, hiddenIds) }
            .flowOn(Dispatchers.Default)

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private fun buildAlbums(
        allItems: List<MediaItem>,
        hiddenIds: Set<Long>,
    ): List<Album> {
        val visibleItems = allItems.filter { it.id !in hiddenIds }

        // --- Smart albums ---------------------------------------------------
        val smartAlbums = SMART_ALBUM_SPECS.mapNotNull { spec ->
            val matching = visibleItems.filter(spec.matches)
            if (matching.isEmpty()) return@mapNotNull null
            Album(
                id       = spec.id,
                name     = spec.name,
                coverUri = matching.first().uri,
                count    = matching.size,
                type     = AlbumType.SMART,
            )
        }

        // --- Custom albums (one per bucket) ---------------------------------
        val customAlbums = visibleItems
            .groupBy { it.bucketId }
            .mapNotNull { (bucketId, items) ->
                if (items.isEmpty()) return@mapNotNull null
                Album(
                    id       = bucketId.toString(),
                    name     = items.first().bucketName,
                    coverUri = items.first().uri,   // list is already DATE_TAKEN DESC
                    count    = items.size,
                    type     = AlbumType.CUSTOM,
                )
            }
            .sortedBy { it.name }

        return smartAlbums + customAlbums
    }
}
```

- [ ] Stage and commit:

```
git add app/src/main/kotlin/com/gallery/data/mediastore/MediaStoreAlbumRepository.kt
git commit -m "feat(data): implement MediaStoreAlbumRepository with smart albums"
```

---

## Task 3.4 — Hilt bindings

**File:** `app/src/main/kotlin/com/gallery/di/RepositoryModule.kt`

- [ ] Create (or extend) a Hilt `@Module` that binds the concrete implementations to their interfaces:

```kotlin
package com.gallery.di

import com.gallery.data.mediastore.MediaStoreAlbumRepository
import com.gallery.data.mediastore.MediaStoreMediaRepository
import com.gallery.domain.AlbumRepository
import com.gallery.domain.MediaRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMediaRepository(
        impl: MediaStoreMediaRepository,
    ): MediaRepository

    @Binds
    @Singleton
    abstract fun bindAlbumRepository(
        impl: MediaStoreAlbumRepository,
    ): AlbumRepository
}
```

- [ ] Stage and commit:

```
git add app/src/main/kotlin/com/gallery/di/RepositoryModule.kt
git commit -m "feat(di): bind MediaStore repositories in Hilt module"
```

---

## Task 3.5 — Manifest permissions

**File:** `app/src/main/AndroidManifest.xml`

- [ ] Add the required `READ_MEDIA_IMAGES` and `READ_MEDIA_VIDEO` permissions (required on API 33+; on API 31–32 `READ_EXTERNAL_STORAGE` is still used but both sets should be declared for forward compatibility):

```xml
<!-- API 33+ granular media permissions -->
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
<uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />

<!-- API 31–32 fallback -->
<uses-permission
    android:name="android.permission.READ_EXTERNAL_STORAGE"
    android:maxSdkVersion="32" />
```

- [ ] Confirm `ACCESS_MEDIA_LOCATION` is also present so that `LATITUDE` / `LONGITUDE` columns are non-null for geotagged media:

```xml
<uses-permission android:name="android.permission.ACCESS_MEDIA_LOCATION" />
```

- [ ] Stage and commit:

```
git add app/src/main/AndroidManifest.xml
git commit -m "feat(manifest): add media read and location permissions"
```

---

## Task 3.6 — Build verification

- [ ] Run `./gradlew :app:compileDebugKotlin` and confirm the output contains `BUILD SUCCESSFUL` with zero errors.
- [ ] If the build fails due to missing domain types (`MediaItem`, `Album`, `AlbumType`, `MediaRepository`, `AlbumRepository`), verify that Phase 1 is merged and the domain module is on the compile classpath.
- [ ] Stage and commit (no new files — only fix any compilation issues found):

```
git commit -m "fix(data): resolve any compilation issues in MediaStore layer" --allow-empty
```

---

## Note on instrumented tests

MediaStore integration tests require a physical device or emulator with actual media files present. They cannot be run reliably in CI without pre-seeded AVD snapshots. Therefore:

- **No `androidTest` files are created in this phase.**
- Unit-testable logic (smart album classification, `getOnThisDay` calendar arithmetic, location nullability) should be extracted into pure functions and covered by JVM unit tests in a later refactoring phase once a `FakeMediaRepository` is available.
- Manual verification checklist for on-device smoke testing:
  - [ ] Launch app — all photos and videos appear in the main grid.
  - [ ] Navigate to an album — only that bucket's items appear.
  - [ ] Search for a filename substring — results update live.
  - [ ] "On This Day" screen shows items from the same calendar day in past years.
  - [ ] Camera Roll, Screenshots, Downloads, Videos smart albums appear and have correct counts.
  - [ ] Hiding an album (passing its `bucketId` in `hiddenIds`) removes it from the album list.
  - [ ] Adding a new photo from another app causes the grid to refresh without restarting the app (ContentObserver trigger test).
