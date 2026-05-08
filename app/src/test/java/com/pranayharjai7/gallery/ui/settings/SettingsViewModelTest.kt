package com.pranayharjai7.gallery.ui.settings

import app.cash.turbine.test
import com.pranayharjai7.gallery.data.prefs.GridSize
import com.pranayharjai7.gallery.data.prefs.ThemeMode
import com.pranayharjai7.gallery.data.prefs.UserPreferences
import com.pranayharjai7.gallery.data.prefs.UserPreferencesRepository
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

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
        coEvery { repo.setThemeMode(any()) } just Runs
        coEvery { repo.setDynamicColor(any()) } just Runs
        coEvery { repo.setGridSize(any()) } just Runs
        coEvery { repo.setSlideshowInterval(any()) } just Runs
        viewModel = SettingsViewModel(repo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is loading, then populated from repo`() = runTest {
        val localViewModel = SettingsViewModel(repo)
        localViewModel.uiState.test {
            val loading = awaitItem()
            assertTrue(loading.isLoading)
            advanceUntilIdle()
            val loaded = awaitItem()
            assertFalse(loaded.isLoading)
            assertEquals(defaultPrefs, loaded.prefs)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setThemeMode calls repository`() = runTest {
        advanceUntilIdle()
        viewModel.setThemeMode(ThemeMode.DARK)
        advanceUntilIdle()
        coVerify { repo.setThemeMode(ThemeMode.DARK) }
    }

    @Test
    fun `setDynamicColor calls repository`() = runTest {
        advanceUntilIdle()
        viewModel.setDynamicColor(false)
        advanceUntilIdle()
        coVerify { repo.setDynamicColor(false) }
    }

    @Test
    fun `setGridSize calls repository`() = runTest {
        advanceUntilIdle()
        viewModel.setGridSize(GridSize.COMPACT)
        advanceUntilIdle()
        coVerify { repo.setGridSize(GridSize.COMPACT) }
    }

    @Test
    fun `setSlideshowInterval calls repository`() = runTest {
        advanceUntilIdle()
        viewModel.setSlideshowInterval(8)
        advanceUntilIdle()
        coVerify { repo.setSlideshowInterval(8) }
    }
}
