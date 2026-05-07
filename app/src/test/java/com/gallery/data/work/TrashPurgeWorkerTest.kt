package com.gallery.data.work

import com.gallery.domain.model.TrashItem
import com.gallery.domain.repository.TrashRepository
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class TrashPurgeWorkerTest {

    private lateinit var trashRepo: TrashRepository

    @Before
    fun setUp() {
        trashRepo = mockk()
    }

    @Test
    fun `deletes expired items from repository`() = runTest {
        val thirtyOneDaysAgo = System.currentTimeMillis() - 31L * 24 * 60 * 60 * 1000
        val expiredItem = TrashItem(
            id = 1L,
            originalUri = mockk(relaxed = true),
            name = "old.jpg",
            dateTaken = thirtyOneDaysAgo,
            mimeType = "image/jpeg",
            deletedAt = thirtyOneDaysAgo,
            bucketId = 1L
        )

        coEvery { trashRepo.getExpired(any()) } returns listOf(expiredItem)
        coEvery { trashRepo.deleteAll(any()) } just Runs

        val thirtyDaysAgo = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
        val expired = trashRepo.getExpired(thirtyDaysAgo)
        trashRepo.deleteAll(expired.map { it.id })

        coVerify(exactly = 1) { trashRepo.getExpired(any()) }
        coVerify(exactly = 1) { trashRepo.deleteAll(listOf(1L)) }
    }

    @Test
    fun `does nothing when no items are expired`() = runTest {
        coEvery { trashRepo.getExpired(any()) } returns emptyList()
        coEvery { trashRepo.deleteAll(any()) } just Runs

        val thirtyDaysAgo = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
        val expired = trashRepo.getExpired(thirtyDaysAgo)
        trashRepo.deleteAll(expired.map { it.id })

        coVerify(exactly = 1) { trashRepo.deleteAll(emptyList()) }
    }
}
