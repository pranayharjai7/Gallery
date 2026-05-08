package com.gallery.domain.usecase

import android.net.Uri
import com.gallery.domain.model.MediaItem
import com.gallery.domain.repository.HiddenRepository
import com.gallery.domain.repository.MediaRepository
import com.gallery.domain.repository.TrashRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

private val EMPTY_URI: Uri = mockk(relaxed = true)

class GetAllMediaUseCaseTest {

    private val mediaRepo: MediaRepository = mockk()
    private val hiddenRepo: HiddenRepository = mockk()
    private val trashRepo: TrashRepository = mockk()

    private fun useCase() = GetAllMediaUseCase(mediaRepo, hiddenRepo, trashRepo)

    @Test
    fun `filters hidden items from all media`() = runTest {
        val item1 = MediaItem(1L, EMPTY_URI, "a.jpg", 0, 0, 0, 0, null, "image/jpeg", 1L, "cam", null)
        val item2 = MediaItem(2L, EMPTY_URI, "b.jpg", 0, 0, 0, 0, null, "image/jpeg", 1L, "cam", null)
        every { mediaRepo.observeAll() } returns flowOf(listOf(item1, item2))
        every { hiddenRepo.observeIds() } returns flowOf(setOf(2L))
        every { trashRepo.observeAll() } returns flowOf(emptyList())

        val result = useCase()().first()

        assertEquals(listOf(item1), result)
    }

    @Test
    fun `returns all items when nothing is hidden`() = runTest {
        val item1 = MediaItem(1L, EMPTY_URI, "a.jpg", 0, 0, 0, 0, null, "image/jpeg", 1L, "cam", null)
        val item2 = MediaItem(2L, EMPTY_URI, "b.jpg", 0, 0, 0, 0, null, "image/jpeg", 1L, "cam", null)
        every { mediaRepo.observeAll() } returns flowOf(listOf(item1, item2))
        every { hiddenRepo.observeIds() } returns flowOf(emptySet())
        every { trashRepo.observeAll() } returns flowOf(emptyList())

        val result = useCase()().first()

        assertEquals(listOf(item1, item2), result)
    }

    @Test
    fun `returns empty list when all items are hidden`() = runTest {
        val item1 = MediaItem(1L, EMPTY_URI, "a.jpg", 0, 0, 0, 0, null, "image/jpeg", 1L, "cam", null)
        every { mediaRepo.observeAll() } returns flowOf(listOf(item1))
        every { hiddenRepo.observeIds() } returns flowOf(setOf(1L))
        every { trashRepo.observeAll() } returns flowOf(emptyList())

        val result = useCase()().first()

        assertEquals(emptyList<MediaItem>(), result)
    }

    @Test
    fun `filters trashed items from all media`() = runTest {
        val item1 = MediaItem(1L, EMPTY_URI, "a.jpg", 0, 0, 0, 0, null, "image/jpeg", 1L, "cam", null)
        val item2 = MediaItem(2L, EMPTY_URI, "b.jpg", 0, 0, 0, 0, null, "image/jpeg", 1L, "cam", null)
        val trashItem = mockk<com.gallery.domain.model.TrashItem> { every { id } returns 2L }
        every { mediaRepo.observeAll() } returns flowOf(listOf(item1, item2))
        every { hiddenRepo.observeIds() } returns flowOf(emptySet())
        every { trashRepo.observeAll() } returns flowOf(listOf(trashItem))

        val result = useCase()().first()

        assertEquals(listOf(item1), result)
    }
}
