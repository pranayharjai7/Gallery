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
        val expired = database.trashDao().getExpired(before = 5_000L)
        assertEquals(1, expired.size)
        assertEquals(oldEntity.id, expired.first().id)
    }

    @Test
    fun deleteAll_removesMultipleItems() = runTest {
        val e1 = buildEntity(id = 5L); val e2 = buildEntity(id = 6L); val e3 = buildEntity(id = 7L)
        database.trashDao().insert(e1); database.trashDao().insert(e2); database.trashDao().insert(e3)
        database.trashDao().deleteAll(listOf(e1.id, e2.id))
        database.trashDao().observeAll().test {
            val remaining = awaitItem()
            assertEquals(1, remaining.size)
            assertEquals(e3.id, remaining.first().id)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
