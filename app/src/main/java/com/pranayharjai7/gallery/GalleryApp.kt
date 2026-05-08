package com.pranayharjai7.gallery

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.WorkManager
import com.pranayharjai7.gallery.data.work.TrashPurgeWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class GalleryApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        scheduleTrashPurge()
    }

    private fun scheduleTrashPurge() {
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            TrashPurgeWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            TrashPurgeWorker.buildRequest()
        )
    }
}
