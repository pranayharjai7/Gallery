package com.gallery.data.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkerParameters
import com.gallery.domain.repository.TrashRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class TrashPurgeWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val trashRepository: TrashRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val thirtyDaysAgo = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
        val expired = trashRepository.getExpired(thirtyDaysAgo)
        expired.forEach { item ->
            try {
                val file = java.io.File(item.originalUri.path ?: return@forEach)
                if (file.exists()) file.delete()
            } catch (_: Exception) {}
        }
        trashRepository.deleteAll(expired.map { it.id })
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "trash_purge"

        // TODO(Phase 05): GalleryApp must implement Configuration.Provider and supply
        // HiltWorkerFactory; without it this worker will fail at runtime with a factory error.
        fun buildRequest(): PeriodicWorkRequest =
            PeriodicWorkRequestBuilder<TrashPurgeWorker>(1, TimeUnit.DAYS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(true)
                        .build()
                )
                .build()
    }
}
