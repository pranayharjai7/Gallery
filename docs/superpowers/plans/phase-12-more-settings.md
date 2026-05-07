# Phase 12: More Screen & Settings — Gallery App
> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Implement the More tab linking to Trash, Hidden Album, and Settings; plus a Settings screen backed by DataStore.

**Depends on:** Phase 06, Phase 07

---

## Overview

This phase introduces the "More" tab (the fourth bottom-navigation destination) and a full Settings screen. Preferences are persisted via DataStore Preferences. The More screen acts as a simple menu linking to Trash, Hidden Album, Settings, and an About entry.

---

## Task 1: UserPreferences data model + DataStore repository

**Files to create:**
- `app/src/main/kotlin/com/gallery/data/prefs/UserPreferencesRepository.kt`
- `app/src/main/kotlin/com/gallery/di/PreferencesModule.kt`

### `UserPreferencesRepository.kt`

```kotlin
package com.gallery.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val gridSize: GridSize = GridSize.NORMAL,
    val slideshowInterval: Int = 4 // seconds
)

enum class ThemeMode { LIGHT, DARK, SYSTEM }
enum class GridSize(val columns: Int) { COMPACT(4), NORMAL(3), LARGE(2) }

@Singleton
class UserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val GRID_SIZE = stringPreferencesKey("grid_size")
        val SLIDESHOW_INTERVAL = intPreferencesKey("slideshow_interval")
    }

    val preferences: Flow<UserPreferences> = dataStore.data.map { prefs ->
        UserPreferences(
            themeMode = ThemeMode.valueOf(prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name),
            dynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: true,
            gridSize = GridSize.valueOf(prefs[Keys.GRID_SIZE] ?: GridSize.NORMAL.name),
            slideshowInterval = prefs[Keys.SLIDESHOW_INTERVAL] ?: 4
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) =
        dataStore.edit { it[Keys.THEME_MODE] = mode.name }

    suspend fun setDynamicColor(enabled: Boolean) =
        dataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }

    suspend fun setGridSize(size: GridSize) =
        dataStore.edit { it[Keys.GRID_SIZE] = size.name }

    suspend fun setSlideshowInterval(seconds: Int) =
        dataStore.edit { it[Keys.SLIDESHOW_INTERVAL] = seconds }
}
```

### `PreferencesModule.kt`

```kotlin
package com.gallery.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.gallery.data.prefs.UserPreferencesRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PreferencesModule {

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create {
            context.preferencesDataStoreFile("user_preferences")
        }

    @Provides
    @Singleton
    fun provideUserPreferencesRepository(
        dataStore: DataStore<Preferences>
    ): UserPreferencesRepository = UserPreferencesRepository(dataStore)
}
```

**Notes:**
- Do NOT use the `by preferencesDataStore` property delegate — it is incompatible with Hilt's singleton scope because it ties the DataStore to the Application class. Use `PreferenceDataStoreFactory.create` instead.
- The `@Singleton` on the repository ensures a single Flow collector across the app.

---

## Task 2: SettingsViewModel + unit test

**Files to create:**
- `app/src/main/kotlin/com/gallery/ui/settings/SettingsViewModel.kt`
- `app/src/test/kotlin/com/gallery/ui/settings/SettingsViewModelTest.kt`

### `SettingsViewModel.kt`

```kotlin
package com.gallery.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gallery.data.prefs.GridSize
import com.gallery.data.prefs.ThemeMode
import com.gallery.data.prefs.UserPreferences
import com.gallery.data.prefs.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val prefs: UserPreferences = UserPreferences(),
    val isLoading: Boolean = true
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefsRepo: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            prefsRepo.preferences.collect { prefs ->
                _uiState.update { it.copy(prefs = prefs, isLoading = false) }
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { prefsRepo.setThemeMode(mode) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setDynamicColor(enabled) }
    }

    fun setGridSize(size: GridSize) {
        viewModelScope.launch { prefsRepo.setGridSize(size) }
    }

    fun setSlideshowInterval(seconds: Int) {
        viewModelScope.launch { prefsRepo.setSlideshowInterval(seconds) }
    }
}
```

### `SettingsViewModelTest.kt`

```kotlin
package com.gallery.ui.settings

import app.cash.turbine.test
import com.gallery.data.prefs.GridSize
import com.gallery.data.prefs.ThemeMode
import com.gallery.data.prefs.UserPreferences
import com.gallery.data.prefs.UserPreferencesRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repo: UserPreferencesRepository
    private lateinit var viewModel: SettingsViewModel

    private val defaultPrefs = UserPreferences(
        themeMode = ThemeMode.SYSTEM,
        dynamicColor = true,
        gridSize = GridSize.NORMAL,
        slideshowInterval = 4
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repo = mockk()
        every { repo.preferences } returns flowOf(defaultPrefs)
        coEvery { repo.setThemeMode(any()) } returns Unit
        coEvery { repo.setDynamicColor(any()) } returns Unit
        coEvery { repo.setGridSize(any()) } returns Unit
        coEvery { repo.setSlideshowInterval(any()) } returns Unit
        viewModel = SettingsViewModel(repo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is loading, then populated from repo`() = runTest {
        viewModel.uiState.test {
            // initial emission still loading
            val loading = awaitItem()
            // after collecting the flow
            testDispatcher.scheduler.advanceUntilIdle()
            val loaded = awaitItem()
            assertFalse(loaded.isLoading)
            assertEquals(defaultPrefs, loaded.prefs)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setThemeMode calls repository`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.setThemeMode(ThemeMode.DARK)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { repo.setThemeMode(ThemeMode.DARK) }
    }

    @Test
    fun `setDynamicColor calls repository`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.setDynamicColor(false)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { repo.setDynamicColor(false) }
    }

    @Test
    fun `setGridSize calls repository`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.setGridSize(GridSize.COMPACT)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { repo.setGridSize(GridSize.COMPACT) }
    }

    @Test
    fun `setSlideshowInterval calls repository`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.setSlideshowInterval(8)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { repo.setSlideshowInterval(8) }
    }
}
```

---

## Task 3: SettingsScreen composable

**File to create:**
- `app/src/main/kotlin/com/gallery/ui/settings/SettingsScreen.kt`

```kotlin
package com.gallery.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gallery.data.prefs.GridSize
import com.gallery.data.prefs.ThemeMode
import com.gallery.ui.common.LoadingState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showGridDialog by remember { mutableStateOf(false) }
    var showIntervalDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            LoadingState(Modifier.padding(padding))
            return@Scaffold
        }

        LazyColumn(modifier = Modifier.padding(padding)) {

            // ── Appearance section header ──────────────────────────────────
            item {
                Text(
                    text = "Appearance",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp)
                )
            }

            // Theme mode — SingleChoiceSegmentedButtonRow
            item {
                ListItem(
                    headlineContent = { Text("Theme") },
                    trailingContent = {
                        SingleChoiceSegmentedButtonRow {
                            ThemeMode.values().forEachIndexed { index, mode ->
                                SegmentedButton(
                                    selected = uiState.prefs.themeMode == mode,
                                    onClick = { viewModel.setThemeMode(mode) },
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = ThemeMode.values().size
                                    ),
                                    label = {
                                        Text(
                                            text = when (mode) {
                                                ThemeMode.LIGHT -> "Light"
                                                ThemeMode.DARK -> "Dark"
                                                ThemeMode.SYSTEM -> "Auto"
                                            },
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                )
                            }
                        }
                    }
                )
            }

            // Dynamic Color (Android 12+ only, but always render the toggle)
            item {
                ListItem(
                    headlineContent = { Text("Dynamic Color") },
                    supportingContent = { Text("Use wallpaper colors for the theme") },
                    trailingContent = {
                        Switch(
                            checked = uiState.prefs.dynamicColor,
                            onCheckedChange = { viewModel.setDynamicColor(it) }
                        )
                    }
                )
            }

            // ── Media section header ───────────────────────────────────────
            item {
                Text(
                    text = "Media",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp)
                )
            }

            // Grid size
            item {
                ListItem(
                    headlineContent = { Text("Grid Size") },
                    supportingContent = {
                        Text(
                            uiState.prefs.gridSize.name.lowercase()
                                .replaceFirstChar { it.uppercase() }
                        )
                    },
                    modifier = Modifier.clickable { showGridDialog = true }
                )
            }

            // Slideshow interval
            item {
                ListItem(
                    headlineContent = { Text("Slideshow Interval") },
                    supportingContent = { Text("${uiState.prefs.slideshowInterval} seconds") },
                    modifier = Modifier.clickable { showIntervalDialog = true }
                )
            }
        }
    }

    // ── Grid Size dialog ──────────────────────────────────────────────────
    if (showGridDialog) {
        var selected by remember { mutableStateOf(uiState.prefs.gridSize) }
        AlertDialog(
            onDismissRequest = { showGridDialog = false },
            title = { Text("Grid Size") },
            text = {
                Column {
                    GridSize.values().forEach { size ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selected = size }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RadioButton(
                                selected = selected == size,
                                onClick = { selected = size }
                            )
                            Text(
                                text = "${size.name.lowercase().replaceFirstChar { it.uppercase() }} (${size.columns} columns)",
                                modifier = Modifier.align(androidx.compose.ui.Alignment.CenterVertically)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setGridSize(selected)
                    showGridDialog = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showGridDialog = false }) { Text("Cancel") }
            }
        )
    }

    // ── Slideshow Interval dialog ─────────────────────────────────────────
    if (showIntervalDialog) {
        val options = listOf(2, 3, 4, 5, 8, 10)
        var selected by remember { mutableStateOf(uiState.prefs.slideshowInterval) }
        AlertDialog(
            onDismissRequest = { showIntervalDialog = false },
            title = { Text("Slideshow Interval") },
            text = {
                Column {
                    options.forEach { seconds ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selected = seconds }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RadioButton(
                                selected = selected == seconds,
                                onClick = { selected = seconds }
                            )
                            Text(
                                text = "$seconds seconds",
                                modifier = Modifier.align(androidx.compose.ui.Alignment.CenterVertically)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setSlideshowInterval(selected)
                    showIntervalDialog = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showIntervalDialog = false }) { Text("Cancel") }
            }
        )
    }
}
```

---

## Task 4: MoreScreen composable + navigation wiring

**Files to create / modify:**
- `app/src/main/kotlin/com/gallery/ui/more/MoreScreen.kt` (create)
- `app/src/main/kotlin/com/gallery/ui/navigation/GalleryNavGraph.kt` (modify — add more, settings routes)

### `MoreScreen.kt`

```kotlin
package com.gallery.ui.more

import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(onNavigate: (String) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("More") })
        }
    ) { padding ->
        LazyColumn(contentPadding = padding) {

            item {
                ListItem(
                    headlineContent = { Text("Trash") },
                    leadingContent = { Icon(Icons.Default.Delete, contentDescription = null) },
                    trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                    modifier = Modifier.clickable { onNavigate("trash") }
                )
            }

            item {
                ListItem(
                    headlineContent = { Text("Hidden Album") },
                    leadingContent = { Icon(Icons.Default.Lock, contentDescription = null) },
                    trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                    modifier = Modifier.clickable { onNavigate("hidden") }
                )
            }

            item {
                ListItem(
                    headlineContent = { Text("Settings") },
                    leadingContent = { Icon(Icons.Default.Settings, contentDescription = null) },
                    trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                    modifier = Modifier.clickable { onNavigate("settings") }
                )
            }

            item {
                ListItem(
                    headlineContent = { Text("About") },
                    leadingContent = { Icon(Icons.Default.Info, contentDescription = null) },
                    supportingContent = { Text("Gallery v1.0") },
                    modifier = Modifier.clickable { /* no-op for now */ }
                )
            }
        }
    }
}
```

### Navigation changes (in `GalleryNavGraph.kt`)

Add the following composable destinations inside the existing `NavHost`:

```kotlin
// ── More tab destination (already handled as a tab) ──
composable("more") {
    MoreScreen(onNavigate = { route -> navController.navigate(route) })
}

// ── Settings ──
composable("settings") {
    SettingsScreen(onBack = { navController.popBackStack() })
}

// ── Trash ── (implemented in Phase 13)
// composable("trash") { TrashScreen(onBack = { navController.popBackStack() }) }

// ── Hidden Album ── (future phase)
// composable("hidden") { HiddenAlbumScreen(onBack = { navController.popBackStack() }) }
```

Also ensure the bottom navigation bar includes the "More" tab item pointing to the `"more"` route, e.g.:

```kotlin
BottomNavigationItem(
    selected = currentRoute == "more",
    onClick = { navController.navigate("more") { launchSingleTop = true; restoreState = true } },
    icon = { Icon(Icons.Default.MoreHoriz, contentDescription = "More") },
    label = { Text("More") }
)
```

---

## Commit

After all tasks pass:

```
git add app/src/main/kotlin/com/gallery/data/prefs/ \
        app/src/main/kotlin/com/gallery/di/PreferencesModule.kt \
        app/src/main/kotlin/com/gallery/ui/settings/ \
        app/src/main/kotlin/com/gallery/ui/more/ \
        app/src/main/kotlin/com/gallery/ui/navigation/GalleryNavGraph.kt \
        app/src/test/kotlin/com/gallery/ui/settings/
git commit -m "feat: add More screen, Settings screen, and UserPreferences DataStore (Phase 12)"
```

---

## Acceptance criteria

1. `PreferencesModule` provides a single `DataStore<Preferences>` and `UserPreferencesRepository` singleton.
2. `UserPreferencesRepository.preferences` emits `UserPreferences` with correct defaults when the DataStore is empty.
3. Each setter (`setThemeMode`, `setDynamicColor`, `setGridSize`, `setSlideshowInterval`) persists the value and the `preferences` Flow re-emits with the updated value.
4. `SettingsViewModel` exposes `uiState` that starts with `isLoading = true` and transitions to `isLoading = false` after the first emission from the repository.
5. All `SettingsViewModelTest` tests pass with MockK + Turbine.
6. `SettingsScreen` renders a `SingleChoiceSegmentedButtonRow` for theme selection, a `Switch` for dynamic color, and opens `AlertDialog`s for grid size and slideshow interval.
7. `MoreScreen` navigates to `"trash"`, `"hidden"`, and `"settings"` routes via `onNavigate`.
8. The app builds without warnings on `./gradlew assembleDebug`.
