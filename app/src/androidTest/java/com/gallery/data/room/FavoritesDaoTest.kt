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
    fun tearDown() { database.close() }

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
        toggle(60L)
        assertFalse(database.favoritesDao().contains(60L))
    }

    @Test
    fun toggle_doubleToggle_leavesAbsent() = runTest {
        toggle(70L); toggle(70L)
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
