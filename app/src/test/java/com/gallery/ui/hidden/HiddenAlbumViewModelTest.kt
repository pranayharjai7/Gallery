package com.gallery.ui.hidden

import android.net.Uri
import app.cash.turbine.test
import com.gallery.domain.model.MediaItem
import com.gallery.domain.usecase.GetHiddenMediaUseCase
import com.gallery.domain.usecase.RemoveFromHiddenUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
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
class HiddenAlbumViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var getHiddenMedia: GetHiddenMediaUseCase
    private lateinit var removeFromHidden: RemoveFromHiddenUseCase

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        getHiddenMedia = mockk()
        removeFromHidden = mockk()
        coEvery { removeFromHidden(any()) } just Runs
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildVm() = HiddenAlbumViewModel(getHiddenMedia, removeFromHidden)

    private fun fakeItem(id: Long) = MediaItem(
        id = id,
        uri = mockk<Uri>(relaxed = true),
        name = "hidden_$id.jpg",
        dateTaken = System.currentTimeMillis(),
        size = 1024L,
        width = 1080,
        height = 1920,
        duration = null,
        mimeType = "image/jpeg",
        bucketId = 0L,
        bucketName = "Hidden",
        location = null
    )

    @Test
    fun `initial state is Locked`() = runTest {
        every { getHiddenMedia() } returns flowOf(emptyList())
        val vm = buildVm()
        assertTrue(vm.uiState.value.authState is HiddenAuthState.Locked)
    }

    @Test
    fun `onAuthSuccess transitions to Unlocked and loads media`() = runTest {
        val items = listOf(fakeItem(1L), fakeItem(2L))
        every { getHiddenMedia() } returns flowOf(items)
        val vm = buildVm()
        vm.uiState.test {
            awaitItem() // Locked initial
            vm.onAuthSuccess()
            val loadingState = awaitItem()
            assertTrue(loadingState.authState is HiddenAuthState.Unlocked)
            assertTrue(loadingState.isLoading)
            advanceUntilIdle()
            val loadedState = awaitItem()
            assertTrue(loadedState.authState is HiddenAuthState.Unlocked)
            assertFalse(loadedState.isLoading)
            assertEquals(2, loadedState.items.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onAuthError sets Error state`() = runTest {
        every { getHiddenMedia() } returns flowOf(emptyList())
        val vm = buildVm()
        vm.onAuthError("Authentication cancelled")
        val state = vm.uiState.value
        assertTrue(state.authState is HiddenAuthState.Error)
        assertEquals("Authentication cancelled", (state.authState as HiddenAuthState.Error).message)
    }

    @Test
    fun `onHardwareUnavailable sets HardwareUnavailable state`() = runTest {
        every { getHiddenMedia() } returns flowOf(emptyList())
        val vm = buildVm()
        vm.onHardwareUnavailable()
        assertTrue(vm.uiState.value.authState is HiddenAuthState.HardwareUnavailable)
    }

    @Test
    fun `toggleSelection adds id when not selected`() = runTest {
        every { getHiddenMedia() } returns flowOf(emptyList())
        val vm = buildVm()
        vm.toggleSelection(10L)
        assertTrue(10L in vm.uiState.value.selectedIds)
    }

    @Test
    fun `toggleSelection removes id when already selected`() = runTest {
        every { getHiddenMedia() } returns flowOf(emptyList())
        val vm = buildVm()
        vm.toggleSelection(10L)
        vm.toggleSelection(10L)
        assertFalse(10L in vm.uiState.value.selectedIds)
    }

    @Test
    fun `clearSelection empties selectedIds`() = runTest {
        every { getHiddenMedia() } returns flowOf(emptyList())
        val vm = buildVm()
        vm.toggleSelection(1L)
        vm.toggleSelection(2L)
        vm.clearSelection()
        assertTrue(vm.uiState.value.selectedIds.isEmpty())
    }

    @Test
    fun `unhideSelected calls removeFromHidden for each selected id`() = runTest {
        every { getHiddenMedia() } returns flowOf(emptyList())
        val vm = buildVm()
        vm.toggleSelection(5L)
        vm.toggleSelection(7L)
        vm.unhideSelected()
        advanceUntilIdle()
        coVerify(exactly = 1) { removeFromHidden(5L) }
        coVerify(exactly = 1) { removeFromHidden(7L) }
        assertTrue(vm.uiState.value.selectedIds.isEmpty())
    }

    @Test
    fun `removeFromHiddenItem delegates to RemoveFromHiddenUseCase`() = runTest {
        every { getHiddenMedia() } returns flowOf(emptyList())
        val vm = buildVm()
        vm.removeFromHiddenItem(42L)
        advanceUntilIdle()
        coVerify { removeFromHidden(42L) }
    }
}
