package com.pranayharjai7.gallery.ui.albums

import android.net.Uri
import app.cash.turbine.test
import com.pranayharjai7.gallery.domain.model.Album
import com.pranayharjai7.gallery.domain.model.AlbumType
import com.pranayharjai7.gallery.domain.usecase.GetAlbumsUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue

class AlbumsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var getAlbums: GetAlbumsUseCase

    private fun fakeAlbum(id: String, name: String, type: AlbumType) = Album(
        id = id,
        name = name,
        coverUri = mockk(relaxed = true),  // mockk<Uri> to avoid Android JVM issues
        count = 5,
        type = type
    )

    private val smart1 = fakeAlbum("smart_favorites", "Favorites", AlbumType.SMART)
    private val custom1 = fakeAlbum("custom_vacation", "Vacation", AlbumType.CUSTOM)
    private val custom2 = fakeAlbum("custom_work", "Work", AlbumType.CUSTOM)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        getAlbums = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `splits smart and custom albums into separate lists`() = runTest {
        every { getAlbums() } returns flowOf(listOf(smart1, custom1, custom2))

        val vm = AlbumsViewModel(getAlbums)

        vm.uiState.test {
            val loading = awaitItem()
            assertTrue(loading.isLoading)

            val loaded = awaitItem()
            assertFalse(loaded.isLoading)
            assertEquals(listOf(smart1), loaded.smartAlbums)
            assertEquals(listOf(custom1, custom2), loaded.myAlbums)
            assertNull(loaded.error)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `empty list produces empty smart and custom lists`() = runTest {
        every { getAlbums() } returns flowOf(emptyList())

        val vm = AlbumsViewModel(getAlbums)

        vm.uiState.test {
            awaitItem() // loading

            val loaded = awaitItem()
            assertFalse(loaded.isLoading)
            assertTrue(loaded.smartAlbums.isEmpty())
            assertTrue(loaded.myAlbums.isEmpty())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `only smart albums — myAlbums is empty`() = runTest {
        every { getAlbums() } returns flowOf(listOf(smart1))

        val vm = AlbumsViewModel(getAlbums)

        vm.uiState.test {
            awaitItem() // loading
            val loaded = awaitItem()
            assertEquals(listOf(smart1), loaded.smartAlbums)
            assertTrue(loaded.myAlbums.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `only custom albums — smartAlbums is empty`() = runTest {
        every { getAlbums() } returns flowOf(listOf(custom1, custom2))

        val vm = AlbumsViewModel(getAlbums)

        vm.uiState.test {
            awaitItem() // loading
            val loaded = awaitItem()
            assertTrue(loaded.smartAlbums.isEmpty())
            assertEquals(listOf(custom1, custom2), loaded.myAlbums)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
