package com.pranayharjai7.gallery.ui.viewer

import app.cash.turbine.test
import com.pranayharjai7.gallery.domain.model.MediaItem
import com.pranayharjai7.gallery.domain.usecase.GetAllMediaUseCase
import com.pranayharjai7.gallery.domain.usecase.GetFavoritesUseCase
import com.pranayharjai7.gallery.domain.usecase.MoveToTrashUseCase
import com.pranayharjai7.gallery.domain.usecase.ToggleFavoriteUseCase
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

class ViewerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val getAllMedia: GetAllMediaUseCase = mockk()
    private val getFavorites: GetFavoritesUseCase = mockk()
    private val moveToTrash: MoveToTrashUseCase = mockk(relaxed = true)
    private val toggleFavorite: ToggleFavoriteUseCase = mockk(relaxed = true)

    private fun buildVm() = ViewerViewModel(getAllMedia, getFavorites, moveToTrash, toggleFavorite)

    private fun fakeItem(id: Long) = MediaItem(
        id = id,
        uri = mockk(relaxed = true),
        name = "img_$id.jpg",
        dateTaken = 0L,
        size = 0L,
        width = 0,
        height = 0,
        duration = null,
        mimeType = "image/jpeg",
        bucketId = 1L,
        bucketName = "Camera",
        location = null
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { getFavorites() } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadMedia sets correct startIndex`() = runTest {
        val items = listOf(fakeItem(1L), fakeItem(2L))
        every { getAllMedia() } returns flowOf(items)

        val vm = buildVm()
        vm.uiState.test {
            vm.loadMedia(startMediaId = 2L)
            awaitItem() // initial loading state
            val loaded = awaitItem()
            assertEquals(1, loaded.currentIndex)
            assertFalse(loaded.isLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `loadMedia falls back to index 0 when id not found`() = runTest {
        val items = listOf(fakeItem(1L))
        every { getAllMedia() } returns flowOf(items)

        val vm = buildVm()
        vm.uiState.test {
            vm.loadMedia(startMediaId = 999L)
            awaitItem()
            val loaded = awaitItem()
            assertEquals(0, loaded.currentIndex)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `toggleOverlays flips visibility`() = runTest {
        every { getAllMedia() } returns flowOf(emptyList())

        val vm = buildVm()
        assertTrue(vm.uiState.value.overlaysVisible)
        vm.toggleOverlays()
        assertFalse(vm.uiState.value.overlaysVisible)
        vm.toggleOverlays()
        assertTrue(vm.uiState.value.overlaysVisible)
    }

    @Test
    fun `onPageChanged updates currentIndex`() = runTest {
        every { getAllMedia() } returns flowOf(emptyList())

        val vm = buildVm()
        vm.onPageChanged(3)
        assertEquals(3, vm.uiState.value.currentIndex)
    }

    @Test
    fun `toggleFavoriteItem adds to favoriteIds when not present`() = runTest {
        every { getAllMedia() } returns flowOf(emptyList())

        val vm = buildVm()
        vm.toggleFavoriteItem(42L)
        assertTrue(42L in vm.uiState.value.favoriteIds)
    }

    @Test
    fun `toggleFavoriteItem removes from favoriteIds when already present`() = runTest {
        every { getAllMedia() } returns flowOf(emptyList())

        val vm = buildVm()
        vm.toggleFavoriteItem(42L) // add
        vm.toggleFavoriteItem(42L) // remove
        assertFalse(42L in vm.uiState.value.favoriteIds)
    }

    @Test
    fun `deleteCurrentItem calls moveToTrash with current item`() = runTest {
        val item = fakeItem(7L)
        every { getAllMedia() } returns flowOf(listOf(item))

        val vm = buildVm()
        vm.uiState.test {
            vm.loadMedia(startMediaId = 7L)
            awaitItem()
            awaitItem() // loaded state
            vm.deleteCurrentItem()
            testDispatcher.scheduler.advanceUntilIdle()
            cancelAndIgnoreRemainingEvents()
        }
        coVerify { moveToTrash(item) }
    }
}
