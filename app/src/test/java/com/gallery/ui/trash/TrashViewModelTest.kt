package com.gallery.ui.trash

import android.net.Uri
import app.cash.turbine.test
import com.gallery.domain.model.TrashItem
import com.gallery.domain.usecase.GetTrashUseCase
import com.gallery.domain.usecase.PurgeAllTrashUseCase
import com.gallery.domain.usecase.PurgeTrashItemUseCase
import com.gallery.domain.usecase.RestoreFromTrashUseCase
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TrashViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var getTrash: GetTrashUseCase
    private lateinit var restore: RestoreFromTrashUseCase
    private lateinit var purge: PurgeTrashItemUseCase
    private lateinit var purgeAll: PurgeAllTrashUseCase

    private fun fakeTrashItem(id: Long, mimeType: String = "image/jpeg") = TrashItem(
        id = id,
        originalUri = mockk<Uri>(relaxed = true),
        name = "item_$id.${if (mimeType.startsWith("video")) "mp4" else "jpg"}",
        dateTaken = System.currentTimeMillis() - 86_400_000L,
        mimeType = mimeType,
        deletedAt = System.currentTimeMillis() - 86_400_000L,
        bucketId = 100L
    )

    private val item1 = fakeTrashItem(1L, "image/jpeg")
    private val item2 = fakeTrashItem(2L, "video/mp4")

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        getTrash = mockk()
        restore = mockk()
        purge = mockk()
        purgeAll = mockk()
        every { getTrash() } returns flowOf(listOf(item1, item2))
        coEvery { restore(any()) } just Runs
        coEvery { purge(any()) } just Runs
        coEvery { purgeAll(any()) } just Runs
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel() = TrashViewModel(getTrash, restore, purge, purgeAll)

    @Test
    fun `initial state is loading, then items are loaded`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        viewModel.uiState.test {
            val loading = awaitItem()
            assertTrue(loading.isLoading)
            assertTrue(loading.items.isEmpty())
            testDispatcher.scheduler.advanceUntilIdle()
            val loaded = awaitItem()
            assertFalse(loaded.isLoading)
            assertEquals(listOf(item1, item2), loaded.items)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `restoreItem calls restore use case`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.restoreItem(item1)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { restore(item1) }
    }

    @Test
    fun `purgeItem calls purge use case with correct id`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.purgeItem(item1.id)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { purge(item1.id) }
    }

    @Test
    fun `toggleSelection adds id when not selected`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.toggleSelection(item1.id)
        assertTrue(item1.id in viewModel.uiState.value.selectedIds)
    }

    @Test
    fun `toggleSelection removes id when already selected`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.toggleSelection(item1.id)
        viewModel.toggleSelection(item1.id)
        assertFalse(item1.id in viewModel.uiState.value.selectedIds)
    }

    @Test
    fun `clearSelection empties selectedIds`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.toggleSelection(item1.id)
        viewModel.toggleSelection(item2.id)
        viewModel.clearSelection()
        assertTrue(viewModel.uiState.value.selectedIds.isEmpty())
    }

    @Test
    fun `purgeSelected purges all selected ids then clears selection`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.toggleSelection(item1.id)
        viewModel.toggleSelection(item2.id)
        viewModel.purgeSelected()
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { purgeAll(any()) }
        assertTrue(viewModel.uiState.value.selectedIds.isEmpty())
    }

    @Test
    fun `restoreSelected restores all selected items then clears selection`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.toggleSelection(item1.id)
        viewModel.restoreSelected()
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { restore(item1) }
        assertTrue(viewModel.uiState.value.selectedIds.isEmpty())
    }

    @Test
    fun `emptyTrash purges all items`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.emptyTrash()
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { purgeAll(any()) }
    }
}
