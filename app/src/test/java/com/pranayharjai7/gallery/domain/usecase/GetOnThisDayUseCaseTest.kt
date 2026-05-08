package com.pranayharjai7.gallery.domain.usecase

import android.net.Uri
import com.pranayharjai7.gallery.domain.model.MediaItem
import com.pranayharjai7.gallery.domain.repository.MediaRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

private val EMPTY_URI: Uri = mockk(relaxed = true)

class GetOnThisDayUseCaseTest {

    private val mediaRepo: MediaRepository = mockk()

    @Test
    fun `delegates to mediaRepo getOnThisDay with correct monthDay`() = runTest {
        val monthDay = 507 // May 7
        val expected = listOf(
            MediaItem(1L, EMPTY_URI, "memory.jpg", 0, 0, 0, 0, null, "image/jpeg", 1L, "cam", null)
        )
        every { mediaRepo.getOnThisDay(monthDay) } returns flowOf(expected)

        val result = GetOnThisDayUseCase(mediaRepo)(monthDay).first()

        assertEquals(expected, result)
        verify(exactly = 1) { mediaRepo.getOnThisDay(monthDay) }
    }

    @Test
    fun `passes monthDay value unchanged to repository`() = runTest {
        val monthDay = 1225 // December 25
        every { mediaRepo.getOnThisDay(monthDay) } returns flowOf(emptyList())

        GetOnThisDayUseCase(mediaRepo)(monthDay).first()

        verify(exactly = 1) { mediaRepo.getOnThisDay(1225) }
    }

    @Test
    fun `returns empty list when no memories exist for date`() = runTest {
        val monthDay = 101 // January 1
        every { mediaRepo.getOnThisDay(monthDay) } returns flowOf(emptyList())

        val result = GetOnThisDayUseCase(mediaRepo)(monthDay).first()

        assertEquals(emptyList<MediaItem>(), result)
    }
}
