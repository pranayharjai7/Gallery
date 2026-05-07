# Phase 2: Room Data Layer — Gallery App

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans`.

**Goal:** Implement Room database with entities, DAOs, and repository implementations for Trash, Hidden, and Favorites. Includes instrumented DAO tests.

**Depends on:** Phase 0 (project bootstrap), Phase 1 (domain interfaces).

---

## Task 2.1: Room Entities and Mappers

### Files
- `app/src/main/kotlin/com/gallery/data/room/entity/TrashEntity.kt`
- `app/src/main/kotlin/com/gallery/data/room/entity/HiddenEntity.kt`
- `app/src/main/kotlin/com/gallery/data/room/entity/FavoriteEntity.kt`

### Steps

- [ ] Create the directory `app/src/main/kotlin/com/gallery/data/room/entity/`
- [ ] Create `TrashEntity.kt` with the following content:

```kotlin
package com.gallery.data.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.gallery.domain.model.TrashItem

@Entity(tableName = "trash")
data class TrashEntity(
    @PrimaryKey val id: Long,
    val originalPath: String,
    val originalUri: String,
    val name: String,
    val dateTaken: Long,
    val mimeType: String,
    val deletedAt: Long,
    val bucketId: Long
)

fun TrashEntity.toTrashItem(): TrashItem = TrashItem(
    id = id,
    originalUri = android.net.Uri.parse(originalUri),
    name = name,
    dateTaken = dateTaken,
    mimeType = mimeType,
    deletedAt = deletedAt,
    bucketId = bucketId
)

fun TrashItem.toEntity(path: String): TrashEntity = TrashEntity(
    id = id,
    originalPath = path,
    originalUri = originalUri.toString(),
    name = name,
    dateTaken = dateTaken,
    mimeType = mimeType,
    deletedAt = deletedAt,
    bucketId = bucketId
)
```

- [ ] Create `HiddenEntity.kt` with the following content:

```kotlin
package com.gallery.data.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hidden")
data class HiddenEntity(@PrimaryKey val mediaId: Long)
```

- [ ] Create `FavoriteEntity.kt` with the following content:

```kotlin
package com.gallery.data.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(@PrimaryKey val mediaId: Long)
```

- [ ] Verify all three files compile with `./gradlew :app:compileDebugKotlin`
- [ ] Git commit:
  ```
  git add app/src/main/kotlin/com/gallery/data/room/entity/
  git commit -m "feat(data): add Room entities for Trash, Hidden, Favorites with mappers"
  ```

---

## Task 2.2: TrashDao and Instrumented Tests

### Files
- `app/src/main/kotlin/com/gallery/data/room/dao/TrashDao.kt`
- `app/src/androidTest/kotlin/com/gallery/data/room/TrashDaoTest.kt`

### Steps

- [ ] Create the directory `app/src/main/kotlin/com/gallery/data/room/dao/`
- [ ] Create `TrashDao.kt` with the following content:

```kotlin
package com.gallery.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gallery.data.room.entity.TrashEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrashDao {

    @Query("SELECT * FROM trash ORDER BY deletedAt DESC")
    fun observeAll(): Flow<List<TrashEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: TrashEntity)

    @Query("DELETE FROM trash WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM trash WHERE deletedAt < :before")
    suspend fun getExpired(before: Long): List<TrashEntity>

    @Query("DELETE FROM trash WHERE id IN (:ids)")
    suspend fun deleteAll(ids: List<Long>)
}
```

- [ ] Create the directory `app/src/androidTest/kotlin/com/gallery/data/room/`
- [ ] Create `TrashDaoTest.kt` with the following content:

```kotlin
package com.gallery.data.room

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.gallery.data.room.entity.TrashEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrashDaoTest {

    private lateinit var database: GalleryDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, GalleryDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun buildEntity(
        id: Long = 1L,
        deletedAt: Long = System.currentTimeMillis()
    ) = TrashEntity(
        id = id,
        originalPath = "/storage/emulated/0/DCIM/photo_$id.jpg",
        originalUri = "content://media/external/images/media/$id",
        name = "photo_$id.jpg",
        dateTaken = 1_000_000L,
        mimeType = "image/jpeg",
        deletedAt = deletedAt,
        bucketId = 100L
    )

    @Test
    fun insertAndObserveAll_emitsItem() = runTest {
        val entity = buildEntity(id = 1L)

        database.trashDao().observeAll().test {
            assertEquals(emptyList<TrashEntity>(), awaitItem())

            database.trashDao().insert(entity)

            val items = awaitItem()
            assertEquals(1, items.size)
            assertEquals(entity, items.first())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun delete_removesItem() = runTest {
        val entity = buildEntity(id = 2L)
        database.trashDao().insert(entity)

        database.trashDao().observeAll().test {
            val before = awaitItem()
            assertEquals(1, before.size)

            database.trashDao().delete(entity.id)

            val after = awaitItem()
            assertTrue(after.isEmpty())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getExpired_returnsOnlyOldItems() = runTest {
        val oldEntity = buildEntity(id = 3L, deletedAt = 1_000L)
        val newEntity = buildEntity(id = 4L, deletedAt = 9_000_000_000L)
        database.trashDao().insert(oldEntity)
        database.trashDao().insert(newEntity)

        val threshold = 5_000L
        val expired = database.trashDao().getExpired(before = threshold)

        assertEquals(1, expired.size)
        assertEquals(oldEntity.id, expired.first().id)
    }

    @Test
    fun deleteAll_removesMultipleItems() = runTest {
        val e1 = buildEntity(id = 5L)
        val e2 = buildEntity(id = 6L)
        val e3 = buildEntity(id = 7L)
        database.trashDao().insert(e1)
        database.trashDao().insert(e2)
        database.trashDao().insert(e3)

        database.trashDao().deleteAll(listOf(e1.id, e2.id))

        database.trashDao().observeAll().test {
            val remaining = awaitItem()
            assertEquals(1, remaining.size)
            assertEquals(e3.id, remaining.first().id)

            cancelAndIgnoreRemainingEvents()
        }
    }
}
```

- [ ] Run test (expect FAIL — `GalleryDatabase` does not exist yet):
  ```
  ./gradlew :app:connectedDebugAndroidTest --tests "com.gallery.data.room.TrashDaoTest"
  ```
- [ ] Proceed to Task 2.3 and 2.4 (define remaining DAOs), then Task 2.5 (GalleryDatabase), then return here
- [ ] Run test (expect PASS after GalleryDatabase is in place):
  ```
  ./gradlew :app:connectedDebugAndroidTest --tests "com.gallery.data.room.TrashDaoTest"
  ```
- [ ] Git commit:
  ```
  git add app/src/main/kotlin/com/gallery/data/room/dao/TrashDao.kt
  git add app/src/androidTest/kotlin/com/gallery/data/room/TrashDaoTest.kt
  git commit -m "feat(data): add TrashDao and instrumented tests"
  ```

---

## Task 2.3: HiddenDao and Instrumented Tests

### Files
- `app/src/main/kotlin/com/gallery/data/room/dao/HiddenDao.kt`
- `app/src/androidTest/kotlin/com/gallery/data/room/HiddenDaoTest.kt`

### Steps

- [ ] Create `HiddenDao.kt` with the following content:

```kotlin
package com.gallery.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gallery.data.room.entity.HiddenEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HiddenDao {

    @Query("SELECT mediaId FROM hidden")
    fun observeIds(): Flow<List<Long>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: HiddenEntity)

    @Query("DELETE FROM hidden WHERE mediaId = :mediaId")
    suspend fun deleteById(mediaId: Long)

    @Query("SELECT COUNT(*) > 0 FROM hidden WHERE mediaId = :mediaId")
    suspend fun contains(mediaId: Long): Boolean
}
```

- [ ] Create `HiddenDaoTest.kt` with the following content:

```kotlin
package com.gallery.data.room

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.gallery.data.room.entity.HiddenEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HiddenDaoTest {

    private lateinit var database: GalleryDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, GalleryDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun add_and_observeIds_emitsCorrectSet() = runTest {
        database.hiddenDao().observeIds().test {
            assertEquals(emptyList<Long>(), awaitItem())

            database.hiddenDao().insert(HiddenEntity(mediaId = 10L))
            database.hiddenDao().insert(HiddenEntity(mediaId = 20L))

            // Consume intermediate emission if any
            var latest = awaitItem()
            if (latest.size < 2) latest = awaitItem()

            assertEquals(setOf(10L, 20L), latest.toSet())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun remove_deletesId() = runTest {
        database.hiddenDao().insert(HiddenEntity(mediaId = 30L))

        database.hiddenDao().observeIds().test {
            val before = awaitItem()
            assertTrue(before.contains(30L))

            database.hiddenDao().deleteById(30L)

            val after = awaitItem()
            assertFalse(after.contains(30L))

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun contains_returnsTrueWhenPresent() = runTest {
        database.hiddenDao().insert(HiddenEntity(mediaId = 40L))
        assertTrue(database.hiddenDao().contains(40L))
    }

    @Test
    fun contains_returnsFalseWhenAbsent() = runTest {
        assertFalse(database.hiddenDao().contains(999L))
    }

    @Test
    fun observeIds_emitsEmptyInitially() = runTest {
        database.hiddenDao().observeIds().test {
            assertEquals(emptyList<Long>(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
```

- [ ] Run test (expect FAIL — `GalleryDatabase` does not exist yet):
  ```
  ./gradlew :app:connectedDebugAndroidTest --tests "com.gallery.data.room.HiddenDaoTest"
  ```
- [ ] After Task 2.5 is complete, run test (expect PASS):
  ```
  ./gradlew :app:connectedDebugAndroidTest --tests "com.gallery.data.room.HiddenDaoTest"
  ```
- [ ] Git commit:
  ```
  git add app/src/main/kotlin/com/gallery/data/room/dao/HiddenDao.kt
  git add app/src/androidTest/kotlin/com/gallery/data/room/HiddenDaoTest.kt
  git commit -m "feat(data): add HiddenDao and instrumented tests"
  ```

---

## Task 2.4: FavoritesDao and Instrumented Tests

### Files
- `app/src/main/kotlin/com/gallery/data/room/dao/FavoritesDao.kt`
- `app/src/androidTest/kotlin/com/gallery/data/room/FavoritesDaoTest.kt`

### Steps

- [ ] Create `FavoritesDao.kt` with the following content:

```kotlin
package com.gallery.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gallery.data.room.entity.FavoriteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoritesDao {

    @Query("SELECT mediaId FROM favorites")
    fun observeIds(): Flow<List<Long>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE mediaId = :mediaId")
    suspend fun deleteById(mediaId: Long)

    @Query("SELECT COUNT(*) > 0 FROM favorites WHERE mediaId = :mediaId")
    suspend fun contains(mediaId: Long): Boolean
}
```

- [ ] Create `FavoritesDaoTest.kt` with the following content:

```kotlin
package com.gallery.data.room

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.gallery.data.room.entity.FavoriteEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FavoritesDaoTest {

    private lateinit var database: GalleryDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, GalleryDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    // Helper that mirrors RoomFavoritesRepository.toggle logic for direct DAO testing
    private suspend fun toggle(mediaId: Long) {
        if (database.favoritesDao().contains(mediaId)) {
            database.favoritesDao().deleteById(mediaId)
        } else {
            database.favoritesDao().insert(FavoriteEntity(mediaId = mediaId))
        }
    }

    @Test
    fun toggle_addsWhenAbsent() = runTest {
        assertFalse(database.favoritesDao().contains(50L))
        toggle(50L)
        assertTrue(database.favoritesDao().contains(50L))
    }

    @Test
    fun toggle_removesWhenPresent() = runTest {
        database.favoritesDao().insert(FavoriteEntity(mediaId = 60L))
        assertTrue(database.favoritesDao().contains(60L))

        toggle(60L)

        assertFalse(database.favoritesDao().contains(60L))
    }

    @Test
    fun toggle_doubleToggle_leavesAbsent() = runTest {
        toggle(70L)
        toggle(70L)
        assertFalse(database.favoritesDao().contains(70L))
    }

    @Test
    fun observeIds_emitsCorrectSet() = runTest {
        database.favoritesDao().observeIds().test {
            assertEquals(emptyList<Long>(), awaitItem())

            database.favoritesDao().insert(FavoriteEntity(mediaId = 80L))
            database.favoritesDao().insert(FavoriteEntity(mediaId = 90L))

            var latest = awaitItem()
            if (latest.size < 2) latest = awaitItem()

            assertEquals(setOf(80L, 90L), latest.toSet())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeIds_emitsEmptyInitially() = runTest {
        database.favoritesDao().observeIds().test {
            assertEquals(emptyList<Long>(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
```

- [ ] Run test (expect FAIL — `GalleryDatabase` does not exist yet):
  ```
  ./gradlew :app:connectedDebugAndroidTest --tests "com.gallery.data.room.FavoritesDaoTest"
  ```
- [ ] After Task 2.5 is complete, run test (expect PASS):
  ```
  ./gradlew :app:connectedDebugAndroidTest --tests "com.gallery.data.room.FavoritesDaoTest"
  ```
- [ ] Git commit:
  ```
  git add app/src/main/kotlin/com/gallery/data/room/dao/FavoritesDao.kt
  git add app/src/androidTest/kotlin/com/gallery/data/room/FavoritesDaoTest.kt
  git commit -m "feat(data): add FavoritesDao and instrumented tests"
  ```

---

## Task 2.5: GalleryDatabase

### File
- `app/src/main/kotlin/com/gallery/data/room/GalleryDatabase.kt`

### Notes
- The `HiddenRepository` for the Hidden Album feature will eventually use a separate encrypted Room database (`EncryptedHiddenDatabase`) covered in Phase 14. For now, `HiddenRepository` uses the regular `GalleryDatabase`.
- `exportSchema = false` suppresses the schema export warning for now; set to `true` and configure `room.schemaLocation` if migration history is needed later.

### Steps

- [ ] Create `GalleryDatabase.kt` with the following content:

```kotlin
package com.gallery.data.room

import androidx.room.Database
import androidx.room.RoomDatabase
import com.gallery.data.room.dao.FavoritesDao
import com.gallery.data.room.dao.HiddenDao
import com.gallery.data.room.dao.TrashDao
import com.gallery.data.room.entity.FavoriteEntity
import com.gallery.data.room.entity.HiddenEntity
import com.gallery.data.room.entity.TrashEntity

@Database(
    entities = [
        TrashEntity::class,
        HiddenEntity::class,
        FavoriteEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class GalleryDatabase : RoomDatabase() {

    abstract fun trashDao(): TrashDao
    abstract fun hiddenDao(): HiddenDao
    abstract fun favoritesDao(): FavoritesDao

    companion object {
        const val DATABASE_NAME = "gallery.db"
    }
}
```

- [ ] Verify compilation: `./gradlew :app:compileDebugKotlin`
- [ ] Run all DAO tests that were written in Tasks 2.2–2.4 (expect PASS for all):
  ```
  ./gradlew :app:connectedDebugAndroidTest --tests "com.gallery.data.room.*"
  ```
- [ ] Git commit:
  ```
  git add app/src/main/kotlin/com/gallery/data/room/GalleryDatabase.kt
  git commit -m "feat(data): add GalleryDatabase wiring TrashDao, HiddenDao, FavoritesDao"
  ```

---

## Task 2.6: Room Repository Implementations

### Files
- `app/src/main/kotlin/com/gallery/data/room/RoomTrashRepository.kt`
- `app/src/main/kotlin/com/gallery/data/room/RoomHiddenRepository.kt`
- `app/src/main/kotlin/com/gallery/data/room/RoomFavoritesRepository.kt`

### Steps

- [ ] Create `RoomTrashRepository.kt` with the following content:

```kotlin
package com.gallery.data.room

import com.gallery.data.room.dao.TrashDao
import com.gallery.data.room.entity.TrashEntity
import com.gallery.data.room.entity.toTrashItem
import com.gallery.data.room.entity.toEntity
import com.gallery.domain.model.TrashItem
import com.gallery.domain.repository.TrashRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RoomTrashRepository @Inject constructor(
    private val trashDao: TrashDao
) : TrashRepository {

    override fun observeAll(): Flow<List<TrashItem>> =
        trashDao.observeAll().map { entities -> entities.map { it.toTrashItem() } }

    override suspend fun insert(item: TrashItem) {
        // originalPath is derived from the URI's path segment for local files
        val path = item.originalUri.path.orEmpty()
        trashDao.insert(item.toEntity(path))
    }

    override suspend fun delete(id: Long) {
        trashDao.delete(id)
    }

    override suspend fun getExpired(before: Long): List<TrashItem> =
        trashDao.getExpired(before).map { it.toTrashItem() }

    override suspend fun deleteAll(ids: List<Long>) {
        trashDao.deleteAll(ids)
    }
}
```

- [ ] Create `RoomHiddenRepository.kt` with the following content:

```kotlin
package com.gallery.data.room

import com.gallery.data.room.dao.HiddenDao
import com.gallery.data.room.entity.HiddenEntity
import com.gallery.domain.repository.HiddenRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RoomHiddenRepository @Inject constructor(
    private val hiddenDao: HiddenDao
) : HiddenRepository {

    override fun observeIds(): Flow<Set<Long>> =
        hiddenDao.observeIds().map { it.toSet() }

    override suspend fun add(mediaId: Long) {
        hiddenDao.insert(HiddenEntity(mediaId = mediaId))
    }

    override suspend fun remove(mediaId: Long) {
        hiddenDao.deleteById(mediaId)
    }
}
```

- [ ] Create `RoomFavoritesRepository.kt` with the following content:

```kotlin
package com.gallery.data.room

import com.gallery.data.room.dao.FavoritesDao
import com.gallery.data.room.entity.FavoriteEntity
import com.gallery.domain.repository.FavoritesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RoomFavoritesRepository @Inject constructor(
    private val favoritesDao: FavoritesDao
) : FavoritesRepository {

    override fun observeIds(): Flow<Set<Long>> =
        favoritesDao.observeIds().map { it.toSet() }

    override suspend fun toggle(mediaId: Long) {
        if (favoritesDao.contains(mediaId)) {
            favoritesDao.deleteById(mediaId)
        } else {
            favoritesDao.insert(FavoriteEntity(mediaId = mediaId))
        }
    }

    override suspend fun contains(mediaId: Long): Boolean =
        favoritesDao.contains(mediaId)
}
```

- [ ] Verify compilation: `./gradlew :app:compileDebugKotlin`
- [ ] Run all instrumented tests to confirm nothing is broken:
  ```
  ./gradlew :app:connectedDebugAndroidTest --tests "com.gallery.data.room.*"
  ```
- [ ] Git commit:
  ```
  git add app/src/main/kotlin/com/gallery/data/room/RoomTrashRepository.kt
  git add app/src/main/kotlin/com/gallery/data/room/RoomHiddenRepository.kt
  git add app/src/main/kotlin/com/gallery/data/room/RoomFavoritesRepository.kt
  git commit -m "feat(data): implement RoomTrashRepository, RoomHiddenRepository, RoomFavoritesRepository"
  ```

---

## Task 2.7: Hilt Database Module

### File
- `app/src/main/kotlin/com/gallery/di/DatabaseModule.kt`

### Notes
- Hilt 2.51.1 is used for dependency injection.
- The module is installed in `SingletonComponent` so the database and DAOs are application-scoped singletons.
- Repository bindings (`@Binds`) should live in a separate `RepositoryModule` so they can be replaced in tests; they are included here for completeness.

### Steps

- [ ] Create `DatabaseModule.kt` with the following content:

```kotlin
package com.gallery.di

import android.content.Context
import androidx.room.Room
import com.gallery.data.room.GalleryDatabase
import com.gallery.data.room.GalleryDatabase.Companion.DATABASE_NAME
import com.gallery.data.room.RoomFavoritesRepository
import com.gallery.data.room.RoomHiddenRepository
import com.gallery.data.room.RoomTrashRepository
import com.gallery.data.room.dao.FavoritesDao
import com.gallery.data.room.dao.HiddenDao
import com.gallery.data.room.dao.TrashDao
import com.gallery.domain.repository.FavoritesRepository
import com.gallery.domain.repository.HiddenRepository
import com.gallery.domain.repository.TrashRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideGalleryDatabase(
        @ApplicationContext context: Context
    ): GalleryDatabase = Room.databaseBuilder(
        context,
        GalleryDatabase::class.java,
        DATABASE_NAME
    ).build()

    @Provides
    fun provideTrashDao(db: GalleryDatabase): TrashDao = db.trashDao()

    @Provides
    fun provideHiddenDao(db: GalleryDatabase): HiddenDao = db.hiddenDao()

    @Provides
    fun provideFavoritesDao(db: GalleryDatabase): FavoritesDao = db.favoritesDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTrashRepository(impl: RoomTrashRepository): TrashRepository

    @Binds
    @Singleton
    abstract fun bindHiddenRepository(impl: RoomHiddenRepository): HiddenRepository

    @Binds
    @Singleton
    abstract fun bindFavoritesRepository(impl: RoomFavoritesRepository): FavoritesRepository
}
```

- [ ] Verify Hilt code generation compiles: `./gradlew :app:kaptDebugKotlin` (or `./gradlew :app:kspDebugKotlin` if using KSP)
- [ ] Run all instrumented tests one final time to confirm full green suite:
  ```
  ./gradlew :app:connectedDebugAndroidTest --tests "com.gallery.data.room.*"
  ```
- [ ] Git commit:
  ```
  git add app/src/main/kotlin/com/gallery/di/DatabaseModule.kt
  git commit -m "feat(di): add Hilt DatabaseModule and RepositoryModule bindings for Room layer"
  ```

---

## Dependencies to verify in `app/build.gradle.kts`

Ensure the following are present (versions match project baseline):

```kotlin
// Room
implementation("androidx.room:room-runtime:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")
ksp("androidx.room:room-compiler:2.6.1")  // or kapt if not using KSP

// Coroutines
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

// Hilt
implementation("com.google.dagger:hilt-android:2.51.1")
ksp("com.google.dagger:hilt-compiler:2.51.1")  // or kapt

// Test — instrumented
androidTestImplementation("androidx.test.ext:junit:1.1.5")
androidTestImplementation("androidx.test:core-ktx:1.5.0")
androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
androidTestImplementation("app.cash.turbine:turbine:1.1.0")
```

---

## Full test run command

```
./gradlew :app:connectedDebugAndroidTest --tests "com.gallery.data.room.*"
```

Expected passing tests after all tasks are complete:

| Test class | Tests |
|---|---|
| `TrashDaoTest` | `insertAndObserveAll_emitsItem`, `delete_removesItem`, `getExpired_returnsOnlyOldItems`, `deleteAll_removesMultipleItems` |
| `HiddenDaoTest` | `add_and_observeIds_emitsCorrectSet`, `remove_deletesId`, `contains_returnsTrueWhenPresent`, `contains_returnsFalseWhenAbsent`, `observeIds_emitsEmptyInitially` |
| `FavoritesDaoTest` | `toggle_addsWhenAbsent`, `toggle_removesWhenPresent`, `toggle_doubleToggle_leavesAbsent`, `observeIds_emitsCorrectSet`, `observeIds_emitsEmptyInitially` |
