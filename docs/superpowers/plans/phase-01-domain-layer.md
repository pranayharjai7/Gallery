# Phase 01: Domain Layer — Gallery App

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Define all domain models, repository interfaces, and use cases with full unit test coverage.
**Depends on:** Phase 00

---

## Task 1: Domain Models

**Files to create:**
- `app/src/main/kotlin/com/gallery/domain/model/MediaItem.kt`
- `app/src/main/kotlin/com/gallery/domain/model/Album.kt`
- `app/src/main/kotlin/com/gallery/domain/model/AlbumType.kt`
- `app/src/main/kotlin/com/gallery/domain/model/TrashItem.kt`

**Steps:**

- [ ] Create `app/src/main/kotlin/com/gallery/domain/model/MediaItem.kt`:

```kotlin
package com.gallery.domain.model

import android.net.Uri

data class MediaItem(
    val id: Long,
    val uri: Uri,
    val name: String,
    val dateTaken: Long,
    val size: Long,
    val width: Int,
    val height: Int,
    val duration: Long?,
    val mimeType: String,
    val bucketId: Long,
    val bucketName: String,
    val location: Pair<Double, Double>?
)

val MediaItem.isVideo get() = mimeType.startsWith("video/")
```

- [ ] Create `app/src/main/kotlin/com/gallery/domain/model/AlbumType.kt`:

```kotlin
package com.gallery.domain.model

enum class AlbumType { SMART, CUSTOM }
```

- [ ] Create `app/src/main/kotlin/com/gallery/domain/model/Album.kt`:

```kotlin
package com.gallery.domain.model

import android.net.Uri

data class Album(
    val id: String,
    val name: String,
    val coverUri: Uri,
    val count: Int,
    val type: AlbumType
)
```

- [ ] Create `app/src/main/kotlin/com/gallery/domain/model/TrashItem.kt`:

```kotlin
package com.gallery.domain.model

import android.net.Uri

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
val TrashItem.daysUntilPurge get() = 30 - ((System.currentTimeMillis() - deletedAt) / 86400000).toInt()
```

- [ ] Commit: `git add app/src/main/kotlin/com/gallery/domain/model/ && git commit -m "feat(domain): add domain models MediaItem, Album, AlbumType, TrashItem"`

> No unit tests needed for these plain data classes — they contain no business logic beyond trivially correct computed properties.

---

## Task 2: Repository Interfaces

**Files to create:**
- `app/src/main/kotlin/com/gallery/domain/repository/MediaRepository.kt`
- `app/src/main/kotlin/com/gallery/domain/repository/AlbumRepository.kt`
- `app/src/main/kotlin/com/gallery/domain/repository/TrashRepository.kt`
- `app/src/main/kotlin/com/gallery/domain/repository/HiddenRepository.kt`
- `app/src/main/kotlin/com/gallery/domain/repository/FavoritesRepository.kt`

**Steps:**

- [ ] Create `app/src/main/kotlin/com/gallery/domain/repository/MediaRepository.kt`:

```kotlin
package com.gallery.domain.repository

import com.gallery.domain.model.MediaItem
import kotlinx.coroutines.flow.Flow

interface MediaRepository {
    fun observeAll(): Flow<List<MediaItem>>
    fun observeByBucket(bucketId: Long): Flow<List<MediaItem>>
    fun search(query: String): Flow<List<MediaItem>>
    suspend fun getById(id: Long): MediaItem?
    fun getOnThisDay(monthDay: Int): Flow<List<MediaItem>>
}
```

- [ ] Create `app/src/main/kotlin/com/gallery/domain/repository/AlbumRepository.kt`:

```kotlin
package com.gallery.domain.repository

import com.gallery.domain.model.Album
import kotlinx.coroutines.flow.Flow

interface AlbumRepository {
    fun observeAlbums(hiddenIds: Set<Long>): Flow<List<Album>>
}
```

- [ ] Create `app/src/main/kotlin/com/gallery/domain/repository/TrashRepository.kt`:

```kotlin
package com.gallery.domain.repository

import com.gallery.domain.model.TrashItem
import kotlinx.coroutines.flow.Flow

interface TrashRepository {
    fun observeAll(): Flow<List<TrashItem>>
    suspend fun insert(item: TrashItem)
    suspend fun delete(id: Long)
    suspend fun deleteAll(ids: List<Long>)
    suspend fun getExpired(before: Long): List<TrashItem>
}
```

- [ ] Create `app/src/main/kotlin/com/gallery/domain/repository/HiddenRepository.kt`:

```kotlin
package com.gallery.domain.repository

import kotlinx.coroutines.flow.Flow

interface HiddenRepository {
    fun observeIds(): Flow<Set<Long>>
    suspend fun add(mediaId: Long)
    suspend fun remove(mediaId: Long)
}
```

- [ ] Create `app/src/main/kotlin/com/gallery/domain/repository/FavoritesRepository.kt`:

```kotlin
package com.gallery.domain.repository

import kotlinx.coroutines.flow.Flow

interface FavoritesRepository {
    fun observeIds(): Flow<Set<Long>>
    suspend fun toggle(mediaId: Long)
    suspend fun contains(mediaId: Long): Boolean
}
```

- [ ] Commit: `git add app/src/main/kotlin/com/gallery/domain/repository/ && git commit -m "feat(domain): add repository interfaces for media, album, trash, hidden, favorites"`

---

## Task 3: Use Cases — Media Query

**Files to create:**
- `app/src/main/kotlin/com/gallery/domain/usecase/GetAllMediaUseCase.kt`
- `app/src/main/kotlin/com/gallery/domain/usecase/GetAlbumsUseCase.kt`
- `app/src/main/kotlin/com/gallery/domain/usecase/GetAlbumMediaUseCase.kt`
- `app/src/main/kotlin/com/gallery/domain/usecase/SearchMediaUseCase.kt`
- `app/src/test/kotlin/com/gallery/domain/usecase/GetAllMediaUseCaseTest.kt`

**Test dependencies required in `app/build.gradle.kts`:**
```kotlin
testImplementation("io.mockk:mockk:1.13.10")
testImplementation("app.cash.turbine:turbine:1.1.0")
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
testImplementation("junit:junit:4.13.2")
```

**Steps:**

- [ ] Write the failing test first. Create `app/src/test/kotlin/com/gallery/domain/usecase/GetAllMediaUseCaseTest.kt`:

```kotlin
package com.gallery.domain.usecase

import android.net.Uri
import com.gallery.domain.model.MediaItem
import com.gallery.domain.repository.HiddenRepository
import com.gallery.domain.repository.MediaRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetAllMediaUseCaseTest {

    private val mediaRepo: MediaRepository = mockk()
    private val hiddenRepo: HiddenRepository = mockk()

    @Test
    fun `filters hidden items from all media`() = runTest {
        val item1 = MediaItem(1L, Uri.EMPTY, "a.jpg", 0, 0, 0, 0, null, "image/jpeg", 1L, "cam", null)
        val item2 = MediaItem(2L, Uri.EMPTY, "b.jpg", 0, 0, 0, 0, null, "image/jpeg", 1L, "cam", null)
        every { mediaRepo.observeAll() } returns flowOf(listOf(item1, item2))
        every { hiddenRepo.observeIds() } returns flowOf(setOf(2L))

        val result = GetAllMediaUseCase(mediaRepo, hiddenRepo)().first()

        assertEquals(listOf(item1), result)
    }

    @Test
    fun `returns all items when nothing is hidden`() = runTest {
        val item1 = MediaItem(1L, Uri.EMPTY, "a.jpg", 0, 0, 0, 0, null, "image/jpeg", 1L, "cam", null)
        val item2 = MediaItem(2L, Uri.EMPTY, "b.jpg", 0, 0, 0, 0, null, "image/jpeg", 1L, "cam", null)
        every { mediaRepo.observeAll() } returns flowOf(listOf(item1, item2))
        every { hiddenRepo.observeIds() } returns flowOf(emptySet())

        val result = GetAllMediaUseCase(mediaRepo, hiddenRepo)().first()

        assertEquals(listOf(item1, item2), result)
    }

    @Test
    fun `returns empty list when all items are hidden`() = runTest {
        val item1 = MediaItem(1L, Uri.EMPTY, "a.jpg", 0, 0, 0, 0, null, "image/jpeg", 1L, "cam", null)
        every { mediaRepo.observeAll() } returns flowOf(listOf(item1))
        every { hiddenRepo.observeIds() } returns flowOf(setOf(1L))

        val result = GetAllMediaUseCase(mediaRepo, hiddenRepo)().first()

        assertEquals(emptyList<MediaItem>(), result)
    }
}
```

- [ ] Run the test and confirm it FAILS (class does not exist yet): `./gradlew :app:testDebugUnitTest --tests "com.gallery.domain.usecase.GetAllMediaUseCaseTest"`

- [ ] Create `app/src/main/kotlin/com/gallery/domain/usecase/GetAllMediaUseCase.kt`:

```kotlin
package com.gallery.domain.usecase

import com.gallery.domain.model.MediaItem
import com.gallery.domain.repository.HiddenRepository
import com.gallery.domain.repository.MediaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class GetAllMediaUseCase @Inject constructor(
    private val mediaRepo: MediaRepository,
    private val hiddenRepo: HiddenRepository
) {
    operator fun invoke(): Flow<List<MediaItem>> =
        hiddenRepo.observeIds().combine(mediaRepo.observeAll()) { hidden, items ->
            items.filter { it.id !in hidden }
        }
}
```

- [ ] Create `app/src/main/kotlin/com/gallery/domain/usecase/GetAlbumsUseCase.kt`:

```kotlin
package com.gallery.domain.usecase

import com.gallery.domain.model.Album
import com.gallery.domain.repository.AlbumRepository
import com.gallery.domain.repository.HiddenRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject

class GetAlbumsUseCase @Inject constructor(
    private val albumRepo: AlbumRepository,
    private val hiddenRepo: HiddenRepository
) {
    operator fun invoke(): Flow<List<Album>> =
        hiddenRepo.observeIds().flatMapLatest { albumRepo.observeAlbums(it) }
}
```

- [ ] Create `app/src/main/kotlin/com/gallery/domain/usecase/GetAlbumMediaUseCase.kt`:

```kotlin
package com.gallery.domain.usecase

import com.gallery.domain.model.MediaItem
import com.gallery.domain.repository.HiddenRepository
import com.gallery.domain.repository.MediaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class GetAlbumMediaUseCase @Inject constructor(
    private val mediaRepo: MediaRepository,
    private val hiddenRepo: HiddenRepository
) {
    operator fun invoke(bucketId: Long): Flow<List<MediaItem>> =
        hiddenRepo.observeIds().combine(mediaRepo.observeByBucket(bucketId)) { hidden, items ->
            items.filter { it.id !in hidden }
        }
}
```

- [ ] Create `app/src/main/kotlin/com/gallery/domain/usecase/SearchMediaUseCase.kt`:

```kotlin
package com.gallery.domain.usecase

import com.gallery.domain.model.MediaItem
import com.gallery.domain.repository.MediaRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SearchMediaUseCase @Inject constructor(
    private val mediaRepo: MediaRepository
) {
    operator fun invoke(query: String): Flow<List<MediaItem>> = mediaRepo.search(query)
}
```

- [ ] Run tests and confirm they PASS: `./gradlew :app:testDebugUnitTest --tests "com.gallery.domain.usecase.GetAllMediaUseCaseTest"`

- [ ] Commit: `git add app/src/main/kotlin/com/gallery/domain/usecase/GetAllMediaUseCase.kt app/src/main/kotlin/com/gallery/domain/usecase/GetAlbumsUseCase.kt app/src/main/kotlin/com/gallery/domain/usecase/GetAlbumMediaUseCase.kt app/src/main/kotlin/com/gallery/domain/usecase/SearchMediaUseCase.kt app/src/test/kotlin/com/gallery/domain/usecase/GetAllMediaUseCaseTest.kt && git commit -m "feat(domain): add media query use cases with unit tests"`

---

## Task 4: Use Cases — Trash

**Files to create:**
- `app/src/main/kotlin/com/gallery/domain/usecase/MoveToTrashUseCase.kt`
- `app/src/main/kotlin/com/gallery/domain/usecase/RestoreFromTrashUseCase.kt`
- `app/src/main/kotlin/com/gallery/domain/usecase/GetTrashUseCase.kt`
- `app/src/main/kotlin/com/gallery/domain/usecase/PurgeTrashItemUseCase.kt`
- `app/src/test/kotlin/com/gallery/domain/usecase/MoveToTrashUseCaseTest.kt`
- `app/src/test/kotlin/com/gallery/domain/usecase/RestoreFromTrashUseCaseTest.kt`

**Steps:**

- [ ] Write the failing test for `MoveToTrashUseCase`. Create `app/src/test/kotlin/com/gallery/domain/usecase/MoveToTrashUseCaseTest.kt`:

```kotlin
package com.gallery.domain.usecase

import android.net.Uri
import com.gallery.domain.model.MediaItem
import com.gallery.domain.model.TrashItem
import com.gallery.domain.repository.TrashRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class MoveToTrashUseCaseTest {

    private val trashRepo: TrashRepository = mockk(relaxed = true)

    @Test
    fun `inserts TrashItem with correct fields from MediaItem`() = runTest {
        val item = MediaItem(
            id = 42L,
            uri = Uri.parse("content://media/42"),
            name = "photo.jpg",
            dateTaken = 1000L,
            size = 2048L,
            width = 1920,
            height = 1080,
            duration = null,
            mimeType = "image/jpeg",
            bucketId = 7L,
            bucketName = "Camera",
            location = null
        )
        val slot = slot<TrashItem>()
        coEvery { trashRepo.insert(capture(slot)) } returns Unit

        MoveToTrashUseCase(trashRepo)(item)

        val captured = slot.captured
        assertEquals(42L, captured.id)
        assertEquals(Uri.parse("content://media/42"), captured.originalUri)
        assertEquals("photo.jpg", captured.name)
        assertEquals(1000L, captured.dateTaken)
        assertEquals("image/jpeg", captured.mimeType)
        assertEquals(7L, captured.bucketId)
    }

    @Test
    fun `calls trashRepo insert exactly once`() = runTest {
        val item = MediaItem(1L, Uri.EMPTY, "img.jpg", 0, 0, 0, 0, null, "image/jpeg", 1L, "cam", null)

        MoveToTrashUseCase(trashRepo)(item)

        coVerify(exactly = 1) { trashRepo.insert(any()) }
    }
}
```

- [ ] Write the failing test for `RestoreFromTrashUseCase`. Create `app/src/test/kotlin/com/gallery/domain/usecase/RestoreFromTrashUseCaseTest.kt`:

```kotlin
package com.gallery.domain.usecase

import android.net.Uri
import com.gallery.domain.model.TrashItem
import com.gallery.domain.repository.TrashRepository
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class RestoreFromTrashUseCaseTest {

    private val trashRepo: TrashRepository = mockk(relaxed = true)

    @Test
    fun `calls trashRepo delete with correct id`() = runTest {
        val trashItem = TrashItem(
            id = 99L,
            originalUri = Uri.parse("content://media/99"),
            name = "video.mp4",
            dateTaken = 5000L,
            mimeType = "video/mp4",
            deletedAt = 9000L,
            bucketId = 3L
        )

        RestoreFromTrashUseCase(trashRepo)(trashItem)

        coVerify(exactly = 1) { trashRepo.delete(99L) }
    }
}
```

- [ ] Run the tests and confirm they FAIL (classes do not exist yet): `./gradlew :app:testDebugUnitTest --tests "com.gallery.domain.usecase.MoveToTrashUseCaseTest" --tests "com.gallery.domain.usecase.RestoreFromTrashUseCaseTest"`

- [ ] Create `app/src/main/kotlin/com/gallery/domain/usecase/MoveToTrashUseCase.kt`:

```kotlin
package com.gallery.domain.usecase

import com.gallery.domain.model.MediaItem
import com.gallery.domain.model.TrashItem
import com.gallery.domain.repository.TrashRepository
import javax.inject.Inject

class MoveToTrashUseCase @Inject constructor(
    private val trashRepo: TrashRepository
) {
    suspend operator fun invoke(item: MediaItem) {
        trashRepo.insert(
            TrashItem(
                id = item.id,
                originalUri = item.uri,
                name = item.name,
                dateTaken = item.dateTaken,
                mimeType = item.mimeType,
                deletedAt = System.currentTimeMillis(),
                bucketId = item.bucketId
            )
        )
    }
}
```

- [ ] Create `app/src/main/kotlin/com/gallery/domain/usecase/RestoreFromTrashUseCase.kt`:

```kotlin
package com.gallery.domain.usecase

// Restores an item from trash by removing its Room entry.
// The media file itself already exists at originalUri in MediaStore.
import com.gallery.domain.model.TrashItem
import com.gallery.domain.repository.TrashRepository
import javax.inject.Inject

class RestoreFromTrashUseCase @Inject constructor(
    private val trashRepo: TrashRepository
) {
    suspend operator fun invoke(trashItem: TrashItem) {
        trashRepo.delete(trashItem.id)
    }
}
```

- [ ] Create `app/src/main/kotlin/com/gallery/domain/usecase/GetTrashUseCase.kt`:

```kotlin
package com.gallery.domain.usecase

import com.gallery.domain.model.TrashItem
import com.gallery.domain.repository.TrashRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTrashUseCase @Inject constructor(
    private val trashRepo: TrashRepository
) {
    operator fun invoke(): Flow<List<TrashItem>> = trashRepo.observeAll()
}
```

- [ ] Create `app/src/main/kotlin/com/gallery/domain/usecase/PurgeTrashItemUseCase.kt`:

```kotlin
package com.gallery.domain.usecase

import com.gallery.domain.repository.TrashRepository
import javax.inject.Inject

class PurgeTrashItemUseCase @Inject constructor(
    private val trashRepo: TrashRepository
) {
    suspend operator fun invoke(id: Long) {
        trashRepo.delete(id)
    }
}
```

- [ ] Run tests and confirm they PASS: `./gradlew :app:testDebugUnitTest --tests "com.gallery.domain.usecase.MoveToTrashUseCaseTest" --tests "com.gallery.domain.usecase.RestoreFromTrashUseCaseTest"`

- [ ] Commit: `git add app/src/main/kotlin/com/gallery/domain/usecase/MoveToTrashUseCase.kt app/src/main/kotlin/com/gallery/domain/usecase/RestoreFromTrashUseCase.kt app/src/main/kotlin/com/gallery/domain/usecase/GetTrashUseCase.kt app/src/main/kotlin/com/gallery/domain/usecase/PurgeTrashItemUseCase.kt app/src/test/kotlin/com/gallery/domain/usecase/MoveToTrashUseCaseTest.kt app/src/test/kotlin/com/gallery/domain/usecase/RestoreFromTrashUseCaseTest.kt && git commit -m "feat(domain): add trash use cases with unit tests"`

---

## Task 5: Use Cases — Hidden, Favorites & Memories

**Files to create:**
- `app/src/main/kotlin/com/gallery/domain/usecase/GetHiddenMediaUseCase.kt`
- `app/src/main/kotlin/com/gallery/domain/usecase/AddToHiddenUseCase.kt`
- `app/src/main/kotlin/com/gallery/domain/usecase/RemoveFromHiddenUseCase.kt`
- `app/src/main/kotlin/com/gallery/domain/usecase/ToggleFavoriteUseCase.kt`
- `app/src/main/kotlin/com/gallery/domain/usecase/GetFavoritesUseCase.kt`
- `app/src/main/kotlin/com/gallery/domain/usecase/GetOnThisDayUseCase.kt`
- `app/src/test/kotlin/com/gallery/domain/usecase/GetOnThisDayUseCaseTest.kt`

**Steps:**

- [ ] Write the failing test first. Create `app/src/test/kotlin/com/gallery/domain/usecase/GetOnThisDayUseCaseTest.kt`:

```kotlin
package com.gallery.domain.usecase

import android.net.Uri
import com.gallery.domain.model.MediaItem
import com.gallery.domain.repository.MediaRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetOnThisDayUseCaseTest {

    private val mediaRepo: MediaRepository = mockk()

    @Test
    fun `delegates to mediaRepo getOnThisDay with correct monthDay`() = runTest {
        val monthDay = 507 // May 7
        val expected = listOf(
            MediaItem(1L, Uri.EMPTY, "memory.jpg", 0, 0, 0, 0, null, "image/jpeg", 1L, "cam", null)
        )
        every { mediaRepo.getOnThisDay(monthDay) } returns flowOf(expected)

        val result = GetOnThisDayUseCase(mediaRepo)(monthDay).first()

        assertEquals(expected, result)
        verify(exactly = 1) { mediaRepo.getOnThisDay(monthDay) }
    }

    @Test
    fun `passes monthDay value unchanged to repository`() = runTest {
        val monthDay = 1225 // December 25
        every { mediaRepo.getOnThisDay(monthDay) } returns flowOf(emptyList())

        GetOnThisDayUseCase(mediaRepo)(monthDay).first()

        verify(exactly = 1) { mediaRepo.getOnThisDay(1225) }
    }

    @Test
    fun `returns empty list when no memories exist for date`() = runTest {
        val monthDay = 101 // January 1
        every { mediaRepo.getOnThisDay(monthDay) } returns flowOf(emptyList())

        val result = GetOnThisDayUseCase(mediaRepo)(monthDay).first()

        assertEquals(emptyList<MediaItem>(), result)
    }
}
```

- [ ] Run the test and confirm it FAILS (class does not exist yet): `./gradlew :app:testDebugUnitTest --tests "com.gallery.domain.usecase.GetOnThisDayUseCaseTest"`

- [ ] Create `app/src/main/kotlin/com/gallery/domain/usecase/GetHiddenMediaUseCase.kt`:

```kotlin
package com.gallery.domain.usecase

import com.gallery.domain.model.MediaItem
import com.gallery.domain.repository.HiddenRepository
import com.gallery.domain.repository.MediaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetHiddenMediaUseCase @Inject constructor(
    private val mediaRepo: MediaRepository,
    private val hiddenRepo: HiddenRepository
) {
    operator fun invoke(): Flow<List<MediaItem>> =
        hiddenRepo.observeIds().flatMapLatest { ids ->
            mediaRepo.observeAll().map { all -> all.filter { it.id in ids } }
        }
}
```

- [ ] Create `app/src/main/kotlin/com/gallery/domain/usecase/AddToHiddenUseCase.kt`:

```kotlin
package com.gallery.domain.usecase

import com.gallery.domain.repository.HiddenRepository
import javax.inject.Inject

class AddToHiddenUseCase @Inject constructor(
    private val hiddenRepo: HiddenRepository
) {
    suspend operator fun invoke(mediaId: Long) = hiddenRepo.add(mediaId)
}
```

- [ ] Create `app/src/main/kotlin/com/gallery/domain/usecase/RemoveFromHiddenUseCase.kt`:

```kotlin
package com.gallery.domain.usecase

import com.gallery.domain.repository.HiddenRepository
import javax.inject.Inject

class RemoveFromHiddenUseCase @Inject constructor(
    private val hiddenRepo: HiddenRepository
) {
    suspend operator fun invoke(mediaId: Long) = hiddenRepo.remove(mediaId)
}
```

- [ ] Create `app/src/main/kotlin/com/gallery/domain/usecase/ToggleFavoriteUseCase.kt`:

```kotlin
package com.gallery.domain.usecase

import com.gallery.domain.repository.FavoritesRepository
import javax.inject.Inject

class ToggleFavoriteUseCase @Inject constructor(
    private val favRepo: FavoritesRepository
) {
    suspend operator fun invoke(mediaId: Long) = favRepo.toggle(mediaId)
}
```

- [ ] Create `app/src/main/kotlin/com/gallery/domain/usecase/GetFavoritesUseCase.kt`:

```kotlin
package com.gallery.domain.usecase

import com.gallery.domain.model.MediaItem
import com.gallery.domain.repository.FavoritesRepository
import com.gallery.domain.repository.MediaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetFavoritesUseCase @Inject constructor(
    private val mediaRepo: MediaRepository,
    private val favRepo: FavoritesRepository
) {
    operator fun invoke(): Flow<List<MediaItem>> =
        favRepo.observeIds().flatMapLatest { ids ->
            mediaRepo.observeAll().map { all -> all.filter { it.id in ids } }
        }
}
```

- [ ] Create `app/src/main/kotlin/com/gallery/domain/usecase/GetOnThisDayUseCase.kt`:

```kotlin
package com.gallery.domain.usecase

// monthDay is encoded as (month * 100 + day), e.g. May 7 = 507, December 25 = 1225
import com.gallery.domain.model.MediaItem
import com.gallery.domain.repository.MediaRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetOnThisDayUseCase @Inject constructor(
    private val mediaRepo: MediaRepository
) {
    operator fun invoke(monthDay: Int): Flow<List<MediaItem>> =
        mediaRepo.getOnThisDay(monthDay)
}
```

- [ ] Run tests and confirm they PASS: `./gradlew :app:testDebugUnitTest --tests "com.gallery.domain.usecase.GetOnThisDayUseCaseTest"`

- [ ] Run the full domain unit test suite and confirm all tests pass: `./gradlew :app:testDebugUnitTest`

- [ ] Commit: `git add app/src/main/kotlin/com/gallery/domain/usecase/GetHiddenMediaUseCase.kt app/src/main/kotlin/com/gallery/domain/usecase/AddToHiddenUseCase.kt app/src/main/kotlin/com/gallery/domain/usecase/RemoveFromHiddenUseCase.kt app/src/main/kotlin/com/gallery/domain/usecase/ToggleFavoriteUseCase.kt app/src/main/kotlin/com/gallery/domain/usecase/GetFavoritesUseCase.kt app/src/main/kotlin/com/gallery/domain/usecase/GetOnThisDayUseCase.kt app/src/test/kotlin/com/gallery/domain/usecase/GetOnThisDayUseCaseTest.kt && git commit -m "feat(domain): add hidden, favorites, and on-this-day use cases with unit tests"`
