package com.pranayharjai7.gallery.ui.search

import app.cash.turbine.test
import com.pranayharjai7.gallery.domain.model.MediaItem
import com.pranayharjai7.gallery.domain.usecase.AddToHiddenUseCase
import com.pranayharjai7.gallery.domain.usecase.MoveToTrashUseCase
import com.pranayharjai7.gallery.domain.usecase.SearchMediaUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val searchMedia: SearchMediaUseCase = mockk()
    private val moveToTrash: MoveToTrashUseCase = mockk(relaxed = true)
    private val addToHidden: AddToHiddenUseCase = mockk()

    private fun fakeItem(id: Long, name: String = "item_$id", mimeType: String = "image/jpeg", durationMs: Long? = null) = MediaItem(
        id = id,
        uri = mockk(relaxed = true),
        name = name,
        dateTaken = 0L,
        size = 0L,
        width = 0,
        height = 0,
        duration = durationMs ?: if (mimeType.startsWith("video")) 5000L else null,
        mimeType = mimeType,
        bucketId = 1L,
        bucketName = "Camera",
        location = null
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coEvery { addToHidden(any()) } just Runs
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildVm() = SearchViewModel(searchMedia, moveToTrash, addToHidden)

    @Test
    fun `onQueryChange debounces and returns results`() = runTest {
        val item = fakeItem(1L)
        every { searchMedia("sunset") } returns flowOf(listOf(item))

        val vm = buildVm()
        vm.onQueryChange("sunset")
        advanceUntilIdle()

        assertEquals(listOf(item), vm.uiState.value.results)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `onQueryChange with blank resets results and loading`() = runTest {
        val vm = buildVm()
        vm.onQueryChange("   ")

        assertEquals(emptyList<MediaItem>(), vm.uiState.value.results)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `onQueryChange sets isLoading true before debounce resolves`() = runTest {
        every { searchMedia("cat") } returns flowOf(emptyList())

        val vm = buildVm()
        vm.uiState.test {
            vm.onQueryChange("cat")
            advanceTimeBy(100)
            val loadingState = expectMostRecentItem()
            assertTrue(loadingState.isLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `filter VIDEOS shows only video items`() = runTest {
        val photo = fakeItem(1L, "photo.jpg", "image/jpeg")
        val video = fakeItem(2L, "video.mp4", "video/mp4", durationMs = 5000L)
        every { searchMedia("media") } returns flowOf(listOf(photo, video))

        val vm = buildVm()
        vm.onQueryChange("media")
        advanceUntilIdle()
        vm.onFilterChange(MediaFilter.VIDEOS)

        assertEquals(listOf(video), vm.uiState.value.results)
    }

    @Test
    fun `filter PHOTOS removes video items from results`() = runTest {
        val photo = fakeItem(1L, mimeType = "image/jpeg")
        val video = fakeItem(2L, mimeType = "video/mp4")
        every { searchMedia("media") } returns flowOf(listOf(photo, video))

        val vm = buildVm()
        vm.onQueryChange("media")
        advanceUntilIdle()
        vm.onFilterChange(MediaFilter.PHOTOS)

        assertEquals(listOf(photo), vm.uiState.value.results)
    }

    @Test
    fun `filter VIDEOS removes photo items from results`() = runTest {
        val photo = fakeItem(1L, mimeType = "image/jpeg")
        val video = fakeItem(2L, mimeType = "video/mp4")
        every { searchMedia("media") } returns flowOf(listOf(photo, video))

        val vm = buildVm()
        vm.onQueryChange("media")
        advanceUntilIdle()
        vm.onFilterChange(MediaFilter.VIDEOS)

        assertEquals(listOf(video), vm.uiState.value.results)
    }

    @Test
    fun `rapid query changes cancel previous job and only last query fires`() = runTest {
        val items = listOf(fakeItem(3L))
        every { searchMedia("l") } returns flowOf(emptyList())
        every { searchMedia("la") } returns flowOf(emptyList())
        every { searchMedia("lake") } returns flowOf(items)

        val vm = buildVm()
        vm.onQueryChange("l")
        advanceTimeBy(100)
        vm.onQueryChange("la")
        advanceTimeBy(100)
        vm.onQueryChange("lake")
        advanceUntilIdle()

        assertEquals(items, vm.uiState.value.results)
    }

    @Test
    fun `filter change from PHOTOS to VIDEOS shows correct results`() = runTest {
        val photo = fakeItem(1L, mimeType = "image/jpeg")
        val video = fakeItem(2L, mimeType = "video/mp4")
        every { searchMedia("media") } returns flowOf(listOf(photo, video))

        val vm = buildVm()
        vm.onQueryChange("media")
        advanceUntilIdle()
        vm.onFilterChange(MediaFilter.PHOTOS)
        vm.onFilterChange(MediaFilter.VIDEOS)

        assertEquals(listOf(video), vm.uiState.value.results)
    }

    @Test
    fun `onQueryChange emits error state when searchMedia throws`() = runTest {
        every { searchMedia("fail") } returns flow { throw RuntimeException("network error") }

        val vm = buildVm()
        vm.onQueryChange("fail")
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isLoading)
        assertEquals("network error", vm.uiState.value.error)
    }

    @Test
    fun `starting a new query clears previous error`() = runTest {
        every { searchMedia("fail") } returns flow { throw RuntimeException("oops") }
        every { searchMedia("ok") } returns flowOf(emptyList())

        val vm = buildVm()
        vm.onQueryChange("fail")
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.error)

        vm.onQueryChange("ok")
        // After starting new query, error should be cleared immediately (before debounce)
        assertTrue(vm.uiState.value.error == null)
    }

    @Test
    fun `addSelectedToHidden calls AddToHiddenUseCase for each selected id`() = runTest {
        val items = listOf(fakeItem(1L), fakeItem(2L), fakeItem(3L))
        every { searchMedia("test") } returns flowOf(items)

        val vm = buildVm()
        vm.onQueryChange("test")
        advanceUntilIdle()

        vm.toggleSelection(1L)
        vm.toggleSelection(3L)
        vm.addSelectedToHidden()
        advanceUntilIdle()

        coVerify(exactly = 1) { addToHidden(1L) }
        coVerify(exactly = 1) { addToHidden(3L) }
        assertTrue(vm.uiState.value.selectedIds.isEmpty())
    }
}
