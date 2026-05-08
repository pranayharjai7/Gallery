package com.pranayharjai7.gallery.domain.usecase

import android.net.Uri
import com.pranayharjai7.gallery.domain.model.TrashItem
import com.pranayharjai7.gallery.domain.repository.TrashRepository
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class RestoreFromTrashUseCaseTest {

    private val trashRepo: TrashRepository = mockk(relaxed = true)

    @Test
    fun `calls trashRepo delete with correct id`() = runTest {
        val uri: Uri = mockk(relaxed = true)
        val trashItem = TrashItem(
            id = 99L,
            originalUri = uri,
            name = "video.mp4",
            dateTaken = 5000L,
            mimeType = "video/mp4",
            deletedAt = 9000L,
            bucketId = 3L
        )

        RestoreFromTrashUseCase(trashRepo)(trashItem)

        coVerify(exactly = 1) { trashRepo.delete(99L) }
    }
}
