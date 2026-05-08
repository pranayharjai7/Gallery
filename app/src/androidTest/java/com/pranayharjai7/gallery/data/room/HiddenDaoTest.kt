package com.pranayharjai7.gallery.data.room

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.pranayharjai7.gallery.data.room.entity.HiddenEntity
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
    fun tearDown() { database.close() }

    @Test
    fun add_and_observeIds_emitsCorrectSet() = runTest {
        database.hiddenDao().observeIds().test {
            assertEquals(emptyList<Long>(), awaitItem())
            database.hiddenDao().insert(HiddenEntity(mediaId = 10L))
            database.hiddenDao().insert(HiddenEntity(mediaId = 20L))
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
