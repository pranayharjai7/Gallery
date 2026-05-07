package com.gallery.ui.photos

import app.cash.turbine.test
import com.gallery.domain.model.MediaItem
import com.gallery.domain.usecase.GetAllMediaUseCase
import com.gallery.domain.usecase.GetOnThisDayUseCase
import com.gallery.domain.usecase.MoveToTrashUseCase
import com.gallery.domain.usecase.ToggleFavoriteUseCase
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PhotosViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var getAllMedia: GetAllMediaUseCase
    private lateinit var moveToTrash: MoveToTrashUseCase
    private lateinit var toggleFavorite: ToggleFavoriteUseCase
    private lateinit var getOnThisDay: GetOnThisDayUseCase

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        getAllMedia = mockk()
        moveToTrash = mockk(relaxed = true)
        toggleFavorite = mockk(relaxed = true)
        getOnThisDay = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildVm() = PhotosViewModel(getAllMedia, moveToTrash, toggleFavorite, getOnThisDay)

    private fun fakeItem(
        id: Long,
        dateTaken: Long = System.currentTimeMillis(),
        mimeType: String = "image/jpeg",
        bucketId: Long = 1L,
        bucketName: String = "Camera"
    ) = MediaItem(
        id = id,
        uri = mockk(relaxed = true),
        name = "photo_$id.jpg",
        dateTaken = dateTaken,
        size = 1024L,
        width = 1080,
        height = 1920,
        duration = null,
        mimeType = mimeType,
        bucketId = bucketId,
        bucketName = bucketName,
        location = null
    )

    @Test
    fun `groups today items under TODAY key`() = runTest {
        val item = fakeItem(1L, dateTaken = System.currentTimeMillis())
        every { getAllMedia() } returns flowOf(listOf(item))
        every { getOnThisDay(any()) } returns flowOf(emptyList())

        val vm = buildVm()
        vm.uiState.test {
            awaitItem()
            val loaded = awaitItem()
            assertFalse(loaded.isLoading)
            assertTrue(loaded.groupedMedia.containsKey("TODAY"))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `groups yesterday items under YESTERDAY key`() = runTest {
        val yesterdayMs = System.currentTimeMillis() - 25 * 60 * 60 * 1000L
        val item = fakeItem(2L, dateTaken = yesterdayMs)
        every { getAllMedia() } returns flowOf(listOf(item))
        every { getOnThisDay(any()) } returns flowOf(emptyList())

        val vm = buildVm()
        vm.uiState.test {
            awaitItem()
            val loaded = awaitItem()
            assertTrue(loaded.groupedMedia.containsKey("YESTERDAY"))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `groups old items under MMMM YYYY key`() = runTest {
        val oldMs = System.currentTimeMillis() - 400L * 24 * 60 * 60 * 1000L
        val item = fakeItem(3L, dateTaken = oldMs)
        every { getAllMedia() } returns flowOf(listOf(item))
        every { getOnThisDay(any()) } returns flowOf(emptyList())

        val vm = buildVm()
        vm.uiState.test {
            awaitItem()
            val loaded = awaitItem()
            val keys = loaded.groupedMedia.keys
            assertTrue(keys.none { it == "TODAY" || it == "YESTERDAY" })
            assertTrue(keys.isNotEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `every 8th item is marked isFeatured`() = runTest {
        val items = (1L..16L).map { fakeItem(it) }
        every { getAllMedia() } returns flowOf(items)
        every { getOnThisDay(any()) } returns flowOf(emptyList())

        val vm = buildVm()
        vm.uiState.test {
            awaitItem()
            val loaded = awaitItem()
            val todayItems = loaded.groupedMedia["TODAY"] ?: emptyList()
            assertEquals(16, todayItems.size)
            assertTrue(todayItems[7].isFeatured)
            assertTrue(todayItems[15].isFeatured)
            assertFalse(todayItems[0].isFeatured)
            assertFalse(todayItems[6].isFeatured)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `toggleSelection adds id when not selected`() = runTest {
        every { getAllMedia() } returns flowOf(emptyList())
        every { getOnThisDay(any()) } returns flowOf(emptyList())

        val vm = buildVm()
        vm.toggleSelection(5L)
        assertTrue(5L in vm.uiState.value.selectedIds)
    }

    @Test
    fun `toggleSelection removes id when already selected`() = runTest {
        every { getAllMedia() } returns flowOf(emptyList())
        every { getOnThisDay(any()) } returns flowOf(emptyList())

        val vm = buildVm()
        vm.toggleSelection(5L)
        vm.toggleSelection(5L)
        assertFalse(5L in vm.uiState.value.selectedIds)
    }

    @Test
    fun `clearSelection empties selectedIds`() = runTest {
        every { getAllMedia() } returns flowOf(emptyList())
        every { getOnThisDay(any()) } returns flowOf(emptyList())

        val vm = buildVm()
        vm.toggleSelection(1L); vm.toggleSelection(2L)
        vm.clearSelection()
        assertTrue(vm.uiState.value.selectedIds.isEmpty())
    }

    @Test
    fun `selectAll sets all provided ids`() = runTest {
        every { getAllMedia() } returns flowOf(emptyList())
        every { getOnThisDay(any()) } returns flowOf(emptyList())

        val vm = buildVm()
        vm.selectAll(listOf(10L, 20L, 30L))
        assertEquals(setOf(10L, 20L, 30L), vm.uiState.value.selectedIds)
    }

    @Test
    fun `memories not shown when fewer than 3 items`() = runTest {
        every { getAllMedia() } returns flowOf(emptyList())
        every { getOnThisDay(any()) } returns flowOf(listOf(fakeItem(99L), fakeItem(100L)))

        val vm = buildVm()
        vm.uiState.test {
            awaitItem()
            val state = awaitItem()
            assertTrue(state.memoriesItems.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `memories shown when 3 or more items`() = runTest {
        val memItems = (1L..5L).map { fakeItem(it) }
        every { getAllMedia() } returns flowOf(emptyList())
        every { getOnThisDay(any()) } returns flowOf(memItems)

        val vm = buildVm()
        vm.uiState.test {
            awaitItem()
            val state = awaitItem()
            assertEquals(5, state.memoriesItems.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `deleteSelected calls moveToTrash for each selected item and clears selection`() = runTest {
        val items = listOf(fakeItem(1L), fakeItem(2L), fakeItem(3L))
        every { getAllMedia() } returns flowOf(items)
        every { getOnThisDay(any()) } returns flowOf(emptyList())

        val vm = buildVm()
        vm.uiState.test {
            awaitItem(); awaitItem()
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
    fun `toggleFavoriteItem delegates to ToggleFavoriteUseCase`() = runTest {
        every { getAllMedia() } returns flowOf(emptyList())
        every { getOnThisDay(any()) } returns flowOf(emptyList())

        val vm = buildVm()
        vm.toggleFavoriteItem(42L)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { toggleFavorite(42L) }
    }
}
