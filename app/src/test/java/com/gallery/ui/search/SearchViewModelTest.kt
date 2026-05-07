package com.gallery.ui.search

import app.cash.turbine.test
import com.gallery.domain.model.MediaItem
import com.gallery.domain.usecase.SearchMediaUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val searchMedia: SearchMediaUseCase = mockk()

    private fun fakeItem(id: Long, mimeType: String = "image/jpeg") = MediaItem(
        id = id,
        uri = mockk(relaxed = true),
        name = "item_$id.${if (mimeType.startsWith("video")) "mp4" else "jpg"}",
        dateTaken = 0L,
        size = 0L,
        width = 0,
        height = 0,
        duration = if (mimeType.startsWith("video")) 5000L else null,
        mimeType = mimeType,
        bucketId = 1L,
        bucketName = "Camera",
        location = null
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildVm() = SearchViewModel(searchMedia)

    @Test
    fun `onQueryChange debounces and returns results`() = runTest {
        val item = fakeItem(1L)
        every { searchMedia("sunset") } returns flowOf(listOf(item))

        val vm = buildVm()
        vm.onQueryChange("sunset")
        advanceTimeBy(400)

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
        val vm = buildVm()
        vm.onFilterChange(MediaFilter.VIDEOS)
        assertEquals(MediaFilter.VIDEOS, vm.uiState.value.filter)
    }

    @Test
    fun `filter PHOTOS removes video items from results`() = runTest {
        val photo = fakeItem(1L, "image/jpeg")
        val video = fakeItem(2L, "video/mp4")
        every { searchMedia("media") } returns flowOf(listOf(photo, video))

        val vm = buildVm()
        vm.onQueryChange("media")
        advanceTimeBy(400)
        vm.onFilterChange(MediaFilter.PHOTOS)

        assertEquals(listOf(photo), vm.uiState.value.results)
    }

    @Test
    fun `filter VIDEOS removes photo items from results`() = runTest {
        val photo = fakeItem(1L, "image/jpeg")
        val video = fakeItem(2L, "video/mp4")
        every { searchMedia("media") } returns flowOf(listOf(photo, video))

        val vm = buildVm()
        vm.onQueryChange("media")
        advanceTimeBy(400)
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
        advanceTimeBy(400)

        assertEquals(items, vm.uiState.value.results)
    }

    @Test
    fun `filter change from PHOTOS to VIDEOS shows correct results`() = runTest {
        val photo = fakeItem(1L, "image/jpeg")
        val video = fakeItem(2L, "video/mp4")
        every { searchMedia("media") } returns flowOf(listOf(photo, video))

        val vm = buildVm()
        vm.onQueryChange("media")
        advanceTimeBy(400)
        vm.onFilterChange(MediaFilter.PHOTOS)
        vm.onFilterChange(MediaFilter.VIDEOS)

        assertEquals(listOf(video), vm.uiState.value.results)
    }
}
