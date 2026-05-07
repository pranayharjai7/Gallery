package com.gallery.ui.albums

import app.cash.turbine.test
import com.gallery.domain.model.MediaItem
import com.gallery.domain.usecase.GetAlbumMediaUseCase
import com.gallery.domain.usecase.MoveToTrashUseCase
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

class AlbumDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var getAlbumMedia: GetAlbumMediaUseCase
    private lateinit var moveToTrash: MoveToTrashUseCase

    private val albumId = "1001"

    private fun fakeItem(id: Long, bucketName: String = "Camera") = MediaItem(
        id = id,
        uri = mockk(relaxed = true),
        name = "img_$id.jpg",
        dateTaken = 1_700_000_000_000L,
        size = 1024L,
        width = 1080,
        height = 1920,
        duration = null,
        mimeType = "image/jpeg",
        bucketId = albumId.toLong(),
        bucketName = bucketName,
        location = null
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        getAlbumMedia = mockk()
        moveToTrash = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildVm() = AlbumDetailViewModel(getAlbumMedia, moveToTrash)

    @Test
    fun `loadAlbum sets albumName from first item bucketName`() = runTest {
        val items = listOf(fakeItem(1L, "Camera"), fakeItem(2L, "Camera"))
        every { getAlbumMedia(albumId.toLong()) } returns flowOf(items)

        val vm = buildVm()

        vm.uiState.test {
            val loading = awaitItem()
            assertTrue(loading.isLoading)

            vm.loadAlbum(albumId)

            val loaded = awaitItem()
            assertFalse(loaded.isLoading)
            assertEquals("Camera", loaded.albumName)
            assertEquals(items, loaded.items)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `loadAlbum with empty media sets albumName to empty string`() = runTest {
        every { getAlbumMedia(albumId.toLong()) } returns flowOf(emptyList())

        val vm = buildVm()

        vm.uiState.test {
            awaitItem() // initial loading state

            vm.loadAlbum(albumId)

            val loaded = awaitItem()
            assertFalse(loaded.isLoading)
            assertEquals("", loaded.albumName)
            assertTrue(loaded.items.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `toggleSelection adds and removes ids`() = runTest {
        every { getAlbumMedia(any()) } returns flowOf(emptyList())

        val vm = buildVm()
        vm.toggleSelection(5L)
        assertTrue(5L in vm.uiState.value.selectedIds)

        vm.toggleSelection(5L)
        assertFalse(5L in vm.uiState.value.selectedIds)
    }

    @Test
    fun `clearSelection empties selectedIds`() = runTest {
        every { getAlbumMedia(any()) } returns flowOf(emptyList())

        val vm = buildVm()
        vm.toggleSelection(1L)
        vm.toggleSelection(2L)
        vm.clearSelection()

        assertTrue(vm.uiState.value.selectedIds.isEmpty())
    }

    @Test
    fun `selectAll sets all provided ids`() = runTest {
        every { getAlbumMedia(any()) } returns flowOf(emptyList())

        val vm = buildVm()
        vm.selectAll(listOf(10L, 20L, 30L))

        assertEquals(setOf(10L, 20L, 30L), vm.uiState.value.selectedIds)
    }

    @Test
    fun `deleteSelected calls moveToTrash for selected items only and clears selection`() = runTest {
        val items = listOf(fakeItem(1L), fakeItem(2L), fakeItem(3L))
        every { getAlbumMedia(albumId.toLong()) } returns flowOf(items)

        val vm = buildVm()
        vm.loadAlbum(albumId)

        vm.uiState.test {
            awaitItem() // loading
            awaitItem() // loaded
            cancelAndIgnoreRemainingEvents()
        }

        vm.selectAll(listOf(1L, 2L))
        vm.deleteSelected()

        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { moveToTrash(items[0]) }
        coVerify(exactly = 1) { moveToTrash(items[1]) }
        coVerify(exactly = 0) { moveToTrash(items[2]) }
        assertTrue(vm.uiState.value.selectedIds.isEmpty())
    }

    @Test
    fun `deleteSelected is no-op when nothing is selected`() = runTest {
        every { getAlbumMedia(any()) } returns flowOf(emptyList())

        val vm = buildVm()
        vm.deleteSelected()

        coVerify(exactly = 0) { moveToTrash(any()) }
    }
}
