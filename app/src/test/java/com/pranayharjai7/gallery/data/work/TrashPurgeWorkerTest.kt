package com.pranayharjai7.gallery.data.work

import android.content.Context
import androidx.work.WorkerParameters
import com.pranayharjai7.gallery.domain.model.TrashItem
import com.pranayharjai7.gallery.domain.repository.TrashRepository
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class TrashPurgeWorkerTest {

    private lateinit var trashRepo: TrashRepository
    private lateinit var context: Context
    private lateinit var workerParams: WorkerParameters

    @Before
    fun setUp() {
        trashRepo = mockk()
        context = mockk(relaxed = true)
        workerParams = mockk(relaxed = true)
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

        val cutoffSlot = slot<Long>()
        coEvery { trashRepo.getExpired(capture(cutoffSlot)) } returns listOf(expiredItem)
        coEvery { trashRepo.deleteAll(any()) } just Runs

        val worker = TrashPurgeWorker(context, workerParams, trashRepo)
        val result = worker.doWork()

        assertEquals(androidx.work.ListenableWorker.Result.success(), result)
        val expectedCutoff = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
        assert(kotlin.math.abs(cutoffSlot.captured - expectedCutoff) < 5_000L) {
            "Expected cutoff ~$expectedCutoff but was ${cutoffSlot.captured}"
        }
        coVerify(exactly = 1) { trashRepo.getExpired(any()) }
        coVerify(exactly = 1) { trashRepo.deleteAll(listOf(1L)) }
    }

    @Test
    fun `does nothing when no items are expired`() = runTest {
        coEvery { trashRepo.getExpired(any()) } returns emptyList()
        coEvery { trashRepo.deleteAll(any()) } just Runs

        val worker = TrashPurgeWorker(context, workerParams, trashRepo)
        val result = worker.doWork()

        assertEquals(androidx.work.ListenableWorker.Result.success(), result)
        coVerify(exactly = 1) { trashRepo.deleteAll(emptyList()) }
    }
}
