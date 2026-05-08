package com.pranayharjai7.gallery.domain.usecase

import android.net.Uri
import com.pranayharjai7.gallery.domain.model.MediaItem
import com.pranayharjai7.gallery.domain.model.TrashItem
import com.pranayharjai7.gallery.domain.repository.TrashRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MoveToTrashUseCaseTest {

    private val trashRepo: TrashRepository = mockk(relaxed = true)

    @Test
    fun `inserts TrashItem with correct fields from MediaItem`() = runTest {
        val uri: Uri = mockk(relaxed = true)
        val item = MediaItem(
            id = 42L,
            uri = uri,
            name = "photo.jpg",
            dateTaken = 1000L,
            size = 2048L,
            width = 1920,
            height = 1080,
            duration = null,
            mimeType = "image/jpeg",
            bucketId = 7L,
            bucketName = "Camera",
            location = null
        )
        val slot = slot<TrashItem>()
        coEvery { trashRepo.insert(capture(slot)) } returns Unit

        MoveToTrashUseCase(trashRepo)(item)

        val captured = slot.captured
        assertEquals(42L, captured.id)
        assertEquals(uri, captured.originalUri)
        assertEquals("photo.jpg", captured.name)
        assertEquals(1000L, captured.dateTaken)
        assertEquals("image/jpeg", captured.mimeType)
        assertEquals(7L, captured.bucketId)
        assertTrue(captured.deletedAt > 0L)
    }

    @Test
    fun `calls trashRepo insert exactly once`() = runTest {
        val emptyUri: Uri = mockk(relaxed = true)
        val item = MediaItem(1L, emptyUri, "img.jpg", 0, 0, 0, 0, null, "image/jpeg", 1L, "cam", null)

        MoveToTrashUseCase(trashRepo)(item)

        coVerify(exactly = 1) { trashRepo.insert(any()) }
    }
}
