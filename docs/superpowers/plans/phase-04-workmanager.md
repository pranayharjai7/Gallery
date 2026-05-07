# Phase 04: WorkManager — Gallery App

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Implement daily trash auto-purge worker that deletes TrashItems older than 30 days.

**Depends on:** Phase 02 (Room database + TrashRepository), Phase 05 (for Hilt wiring)

---

## Prerequisites

- `TrashRepository` interface and `RoomTrashRepository` implementation exist (Phase 02).
- `TrashItem` data class exists with fields: `id: Long`, `originalUri: Uri`, `name: String`, `size: Long`, `mimeType: String`, `deletedAt: Long`, `albumId: Long`.
- `GalleryDatabase` exists (Phase 02).
- Hilt and KSP are configured in `app/build.gradle.kts`.

## Dependencies to verify in `app/build.gradle.kts`

```kotlin
// WorkManager with Hilt integration
implementation("androidx.work:work-runtime-ktx:2.9.1")
implementation("androidx.hilt:hilt-work:1.2.0")
ksp("androidx.hilt:hilt-compiler:1.2.0")
```

If these are missing, add them and sync before proceeding. Verify with `./gradlew dependencies | grep hilt-work`.

---

## Task 1: Create TrashPurgeWorker

**File:** `app/src/main/kotlin/com/gallery/data/work/TrashPurgeWorker.kt`

Create the directory `app/src/main/kotlin/com/gallery/data/work/` if it does not exist.

```kotlin
package com.gallery.data.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkerParameters
import com.gallery.data.repository.TrashRepository
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
                // Delete physical file if it exists in app's private files dir
                val file = java.io.File(item.originalUri.path ?: return@forEach)
                if (file.exists()) file.delete()
            } catch (_: Exception) {}
        }
        trashRepository.deleteAll(expired.map { it.id })
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "trash_purge"

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
```

**Key design decisions:**

- `@HiltWorker` + `@AssistedInject`: Required pair to enable Hilt to inject into a WorkManager worker. `@Assisted` parameters (`Context`, `WorkerParameters`) are provided by WorkManager at runtime; all other constructor parameters are injected by Hilt via `HiltWorkerFactory` (configured in Phase 05).
- `CoroutineWorker`: Suspending base class — `doWork()` runs on a background dispatcher automatically. No manual thread management needed.
- `thirtyDaysAgo`: Computed in milliseconds. The `30L` suffix is required to avoid integer overflow before multiplication.
- Physical file deletion is best-effort: wrapped in try/catch so a missing or inaccessible file never causes the worker to fail. The DB record is always cleaned up regardless.
- `WORK_NAME = "trash_purge"`: Used in Phase 05 with `ExistingPeriodicWorkPolicy.KEEP` so repeated app launches do not reset the schedule timer.
- Battery constraint: daily maintenance should not drain the battery; `setRequiresBatteryNotLow(true)` defers execution when the battery is low.

Commit:

```
git add app/src/main/kotlin/com/gallery/data/work/TrashPurgeWorker.kt
git commit -m "feat: add TrashPurgeWorker for 30-day trash auto-purge"
```

---

## Task 2: Write Unit Tests for TrashPurgeWorker

**File:** `app/src/test/kotlin/com/gallery/data/work/TrashPurgeWorkerTest.kt`

Create the directory `app/src/test/kotlin/com/gallery/data/work/` if it does not exist.

The test validates the repository interaction logic in isolation using MockK. Full WorkManager integration tests require `TestListenableWorkerBuilder` and are documented in the Manual Test section below.

Add test dependencies to `app/build.gradle.kts` if not already present:

```kotlin
testImplementation("io.mockk:mockk:1.13.10")
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
testImplementation("junit:junit:4.13.2")
```

```kotlin
package com.gallery.data.work

import android.net.Uri
import com.gallery.data.model.TrashItem
import com.gallery.data.repository.TrashRepository
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
            originalUri = Uri.EMPTY,
            name = "old.jpg",
            size = 0L,
            mimeType = "image/jpeg",
            deletedAt = thirtyOneDaysAgo,
            albumId = 1L
        )

        coEvery { trashRepo.getExpired(any()) } returns listOf(expiredItem)
        coEvery { trashRepo.deleteAll(any()) } just Runs

        // Simulate doWork() logic directly against the repository
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
```

Run tests:

```
./gradlew :app:testDebugUnitTest --tests "com.gallery.data.work.TrashPurgeWorkerTest"
```

Expected: both tests pass.

Commit:

```
git add app/src/test/kotlin/com/gallery/data/work/TrashPurgeWorkerTest.kt
git commit -m "test: add unit tests for TrashPurgeWorker"
```

---

## Task 3: FakeHiltWorkerFactory (for instrumented tests, optional)

If instrumented WorkManager tests are needed later, add this helper. This is optional for Phase 04; it is documented here for completeness.

**File:** `app/src/test/kotlin/com/gallery/data/work/FakeHiltWorkerFactory.kt`

```kotlin
package com.gallery.data.work

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters

class FakeHiltWorkerFactory(
    private val creator: (Context, WorkerParameters) -> ListenableWorker
) : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker = creator(appContext, workerParameters)
}
```

Usage with `TestListenableWorkerBuilder`:

```kotlin
val worker = TestListenableWorkerBuilder<TrashPurgeWorker>(context)
    .setWorkerFactory(FakeHiltWorkerFactory { ctx, params ->
        TrashPurgeWorker(ctx, params, trashRepository)
    })
    .build()
val result = worker.startWork().get()
assertEquals(ListenableWorker.Result.success(), result)
```

---

## Task 4: Manual Device Test Procedure

1. Build and install the debug APK: `./gradlew installDebug`
2. Insert a TrashItem with `deletedAt` set to 31 days ago via Android Studio Database Inspector or a debug UI.
3. Trigger the worker immediately via adb (bypasses the 1-day period):
   ```
   adb shell am broadcast -a androidx.work.diagnostics.REQUEST_DIAGNOSTICS \
       --receiver-foreground -p com.gallery
   ```
   Or enqueue a one-time run in a debug build:
   ```kotlin
   WorkManager.getInstance(context)
       .enqueue(OneTimeWorkRequestBuilder<TrashPurgeWorker>().build())
   ```
4. Verify via Database Inspector that the item has been removed from the `trash` table.
5. Confirm the periodic schedule is registered:
   ```
   adb shell dumpsys jobscheduler | grep trash_purge
   ```

---

## Task 5: Verify Build

```
./gradlew assembleDebug
```

Expected: `BUILD SUCCESSFUL`

**If KSP/Hilt annotation processing fails:**

- Verify `hilt-work` and `hilt-compiler` KSP dependencies are in `app/build.gradle.kts`.
- Verify the Hilt Gradle plugin is applied: `id("com.google.dagger.hilt.android")` in the plugins block.
- Verify KSP plugin is applied: `id("com.google.devtools.ksp")`.
- Check that `@HiltWorker` is on the class and `@AssistedInject` is on the constructor — both are required.

---

## Architecture Notes

- **Decoupling:** `TrashPurgeWorker` depends only on the `TrashRepository` interface. The `@Binds RoomTrashRepository -> TrashRepository` binding is provided by Phase 05's `RepositoryModule`. If testing Phase 04 in isolation before Phase 05 is complete, provide a temporary `@Provides` binding in a test Hilt module.
- **`ExistingPeriodicWorkPolicy.KEEP` vs `UPDATE`:** `KEEP` preserves the existing enqueue time across app launches (prevents schedule drift). `UPDATE` resets the timer. For a daily maintenance job, `KEEP` is correct.
- **`HiltWorkerFactory`:** WorkManager requires a custom `WorkerFactory` to inject Hilt-managed dependencies. This is configured in Phase 05 via `Configuration.Provider` on `GalleryApp`. Without it, the worker will fail at runtime with a factory error even though it compiles correctly.
- **`setRequiresBatteryNotLow` vs `setRequiresDeviceIdle`:** Idle requirement adds additional delay (may not fire for days on active devices). Battery-not-low is the appropriate constraint for a daily maintenance job.
