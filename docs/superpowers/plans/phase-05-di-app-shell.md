# Phase 05: Dependency Injection & App Shell — Gallery App

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Wire all Hilt modules, create the Application class, configure `HiltWorkerFactory` for WorkManager, and schedule the daily trash purge.

**Depends on:** Phase 02 (Room + repositories), Phase 03 (MediaStore repositories), Phase 04 (TrashPurgeWorker)

---

## Prerequisites

- All repository interfaces and implementations exist: `TrashRepository` / `RoomTrashRepository`, `HiddenRepository` / `RoomHiddenRepository`, `FavoritesRepository` / `RoomFavoritesRepository`, `MediaRepository` / `MediaStoreMediaRepository`, `AlbumRepository` / `MediaStoreAlbumRepository`.
- `GalleryDatabase` (abstract `RoomDatabase`) exposes `trashDao()`, `hiddenDao()`, `favoritesDao()`.
- `TrashPurgeWorker` exists (Phase 04) with `WORK_NAME` and `buildRequest()`.
- `app/build.gradle.kts` includes Hilt, KSP, WorkManager, and hilt-work dependencies (see Phase 04 prerequisites).

---

## Task 1: Create DatabaseModule

**File:** `app/src/main/kotlin/com/gallery/di/DatabaseModule.kt`

Create the directory `app/src/main/kotlin/com/gallery/di/` if it does not exist.

```kotlin
package com.gallery.di

import android.content.Context
import androidx.room.Room
import com.gallery.data.local.GalleryDatabase
import com.gallery.data.local.dao.FavoritesDao
import com.gallery.data.local.dao.HiddenDao
import com.gallery.data.local.dao.TrashDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): GalleryDatabase =
        Room.databaseBuilder(context, GalleryDatabase::class.java, "gallery.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideTrashDao(db: GalleryDatabase): TrashDao = db.trashDao()

    @Provides
    fun provideHiddenDao(db: GalleryDatabase): HiddenDao = db.hiddenDao()

    @Provides
    fun provideFavoritesDao(db: GalleryDatabase): FavoritesDao = db.favoritesDao()
}
```

**Key design decisions:**

- `@Singleton` on `provideDatabase`: The `GalleryDatabase` instance must be a singleton — multiple instances pointing to the same file cause crashes. The individual DAO `@Provides` methods do NOT need `@Singleton` because each DAO is a lightweight view over the database and Room always returns the same DAO instance for a given database instance.
- `fallbackToDestructiveMigration()`: Acceptable for a development-phase app. Replace with proper `Migration` objects before production release.
- `@ApplicationContext`: Hilt qualifier that injects the application `Context`, ensuring no `Activity` context leaks into the singleton-scoped module.

Commit:

```
git add app/src/main/kotlin/com/gallery/di/DatabaseModule.kt
git commit -m "feat: add Hilt DatabaseModule wiring Room DAOs"
```

---

## Task 2: Create RepositoryModule

**File:** `app/src/main/kotlin/com/gallery/di/RepositoryModule.kt`

```kotlin
package com.gallery.di

import com.gallery.data.repository.MediaStoreAlbumRepository
import com.gallery.data.repository.MediaStoreMediaRepository
import com.gallery.data.repository.RoomFavoritesRepository
import com.gallery.data.repository.RoomHiddenRepository
import com.gallery.data.repository.RoomTrashRepository
import com.gallery.data.repository.AlbumRepository
import com.gallery.data.repository.FavoritesRepository
import com.gallery.data.repository.HiddenRepository
import com.gallery.data.repository.MediaRepository
import com.gallery.data.repository.TrashRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMediaRepository(impl: MediaStoreMediaRepository): MediaRepository

    @Binds
    @Singleton
    abstract fun bindAlbumRepository(impl: MediaStoreAlbumRepository): AlbumRepository

    @Binds
    @Singleton
    abstract fun bindTrashRepository(impl: RoomTrashRepository): TrashRepository

    @Binds
    @Singleton
    abstract fun bindHiddenRepository(impl: RoomHiddenRepository): HiddenRepository

    @Binds
    @Singleton
    abstract fun bindFavoritesRepository(impl: RoomFavoritesRepository): FavoritesRepository
}
```

**Key design decisions:**

- `abstract class` with `@Binds`: `@Binds` is more efficient than `@Provides` for interface-to-implementation mappings because it generates no extra factory code — Hilt knows at compile time exactly which class to use. `@Binds` requires an abstract function in an abstract class or interface.
- `@Singleton` on each binding: All repositories are singleton-scoped so there is exactly one `Flow` subscriber per repository across the entire app lifetime. Multiple instances would result in duplicated database subscriptions.
- Import paths: Adjust the package of each interface (`com.gallery.data.repository` shown here) to match the actual project structure. If interfaces live in a `domain` sub-package, update imports accordingly.
- Each concrete implementation class must have `@Inject constructor(...)` — otherwise Hilt cannot instantiate it. Verify this in each implementation before running the build.

Commit:

```
git add app/src/main/kotlin/com/gallery/di/RepositoryModule.kt
git commit -m "feat: add Hilt RepositoryModule binding domain interfaces to Room/MediaStore impls"
```

---

## Task 3: Create AppModule (ContentResolver + WorkManager)

**File:** `app/src/main/kotlin/com/gallery/di/AppModule.kt`

```kotlin
package com.gallery.di

import android.content.ContentResolver
import android.content.Context
import androidx.work.WorkManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideContentResolver(@ApplicationContext context: Context): ContentResolver =
        context.contentResolver

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager =
        WorkManager.getInstance(context)
}
```

**Key design decisions:**

- `ContentResolver` as a singleton: `MediaStoreMediaRepository` and `MediaStoreAlbumRepository` require a `ContentResolver`. Injecting it as a singleton avoids storing `Context` directly in the repositories, which is a lint and lifecycle best practice.
- `WorkManager` as a singleton: WorkManager itself is a singleton internally, but exposing it via Hilt allows repositories or ViewModels to receive it without calling `WorkManager.getInstance(context)` directly, making them easier to test with a mock or fake.

Commit:

```
git add app/src/main/kotlin/com/gallery/di/AppModule.kt
git commit -m "feat: add Hilt AppModule providing ContentResolver and WorkManager"
```

---

## Task 4: Create GalleryApp

**File:** `app/src/main/kotlin/com/gallery/GalleryApp.kt`

This class serves two roles:
1. `@HiltAndroidApp` triggers Hilt's code generation for the entire component hierarchy.
2. `Configuration.Provider` supplies a custom `WorkerFactory` so that `HiltWorkerFactory` can inject dependencies into workers annotated with `@HiltWorker`.

```kotlin
package com.gallery

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.WorkManager
import com.gallery.data.work.TrashPurgeWorker
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
```

**`workManagerConfiguration` vs `WorkManager.getInstance(this)` — timing requirement:**

When `Configuration.Provider` is implemented, WorkManager initializes lazily on the first call to `WorkManager.getInstance(context)`. This call must occur AFTER `super.onCreate()` so that Hilt has completed injection (populating `workerFactory`). The `scheduleTrashPurge()` call in `onCreate()` satisfies this constraint.

Do NOT call `WorkManager.getInstance(this)` before `super.onCreate()`.

**Register GalleryApp in `AndroidManifest.xml`:**

Open `app/src/main/AndroidManifest.xml` and add `android:name=".GalleryApp"` to the `<application>` element:

```xml
<application
    android:name=".GalleryApp"
    android:label="@string/app_name"
    android:icon="@mipmap/ic_launcher"
    ...>
```

**Remove the default WorkManager startup initializer:**

WorkManager ships with an `androidx.startup` `Initializer` that auto-initializes it with a default (non-custom) configuration. When a custom `Configuration.Provider` is used, this auto-initializer must be disabled or WorkManager will throw `IllegalStateException: WorkManager is already initialized` at runtime.

Inside the `<application>` block of `AndroidManifest.xml`, add:

```xml
<provider
    android:name="androidx.startup.InitializationProvider"
    android:authorities="${applicationId}.androidx-startup"
    android:exported="false"
    tools:node="remove" />
```

Ensure `xmlns:tools="http://schemas.android.com/tools"` is declared on the root `<manifest>` element:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">
```

Commit:

```
git add app/src/main/kotlin/com/gallery/GalleryApp.kt \
        app/src/main/AndroidManifest.xml
git commit -m "feat: add GalleryApp with HiltWorkerFactory and WorkManager custom config"
```

---

## Task 5: Verify Build

```
./gradlew assembleDebug
```

Expected: `BUILD SUCCESSFUL`

**Common build failures and fixes:**

| Error | Cause | Fix |
|---|---|---|
| `Cannot be provided without an @Inject constructor` | A concrete repository class (e.g. `RoomTrashRepository`) is missing `@Inject constructor` | Add `@Inject constructor` to the implementation class |
| `WorkManager is already initialized` | The `<provider tools:node="remove">` block is absent from the manifest | Add it as documented in Task 4 |
| `[Hilt] ... is missing a binding` | A `@Binds` function in `RepositoryModule` references a class that does not have `@Inject constructor` | Verify all five repository implementation constructors have `@Inject` |
| `Unresolved reference: HiltWorkerFactory` | `hilt-work` dependency is missing from `app/build.gradle.kts` | Add `implementation("androidx.hilt:hilt-work:1.2.0")` and `ksp("androidx.hilt:hilt-compiler:1.2.0")` |
| `@HiltWorker` annotation not found | `hilt-work` dependency is missing | Same as above |

Commit after successful build:

```
git commit --allow-empty -m "chore: confirm Phase 05 assembleDebug BUILD SUCCESSFUL"
```

---

## Task 6: Smoke Test on Device

1. Install: `./gradlew installDebug`
2. Launch the app. It should open without crashing.
3. Verify WorkManager scheduled the purge job:
   ```
   adb shell dumpsys jobscheduler | grep -A5 trash_purge
   ```
   Expected: a job entry for `trash_purge` with a pending execution window approximately 24 hours from now.
4. Confirm WorkManager diagnostics show no errors:
   ```
   adb shell am broadcast -a androidx.work.diagnostics.REQUEST_DIAGNOSTICS \
       --receiver-foreground -p com.gallery
   adb logcat -d | grep WorkManager
   ```

---

## Architecture Notes

- **Module split rationale:** `DatabaseModule` (Room) is `object` because it uses `@Provides` with concrete construction. `RepositoryModule` is `abstract class` because it uses `@Binds`. Hilt does not allow mixing `@Provides` (non-abstract) and `@Binds` (abstract) in the same class — use companion objects if needed, but separating them into two modules is cleaner.
- **`SingletonComponent` scope:** All modules are installed in `SingletonComponent` (application-scoped). This ensures a single database instance and single repository instance for the app's lifetime. Activity- or Fragment-scoped modules would be appropriate for UI state; they are introduced in later phases.
- **`HiltWorkerFactory` injection:** Hilt injects `HiltWorkerFactory` into `GalleryApp` via field injection (`@Inject lateinit var`). This is the standard pattern because `Application` is not created by Hilt — it cannot use constructor injection. `@HiltAndroidApp` generates the necessary injection infrastructure.
- **`ExistingPeriodicWorkPolicy.KEEP`:** Ensures the daily purge schedule is not reset every time the app starts. If `UPDATE` were used instead, the 24-hour window would restart on every cold launch, potentially preventing the worker from ever running on apps with daily usage.
