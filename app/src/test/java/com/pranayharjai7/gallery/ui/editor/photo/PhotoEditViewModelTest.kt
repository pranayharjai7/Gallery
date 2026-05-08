package com.pranayharjai7.gallery.ui.editor.photo

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PhotoEditViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var context: Context
    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var viewModel: PhotoEditViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        context = mockk(relaxed = true)
        every { context.contentResolver } returns mockk(relaxed = true)
        savedStateHandle = SavedStateHandle(mapOf("mediaId" to 42L))
        viewModel = PhotoEditViewModel(context, savedStateHandle)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `setActiveTab changes activeTab`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            viewModel.setActiveTab(EditorTab.ADJUST)
            assertEquals(EditorTab.ADJUST, awaitItem().activeTab)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `rotateCW increments rotation by 90`() = runTest {
        viewModel.rotateCW()
        assertEquals(90, viewModel.uiState.value.cropState.rotation)
        viewModel.rotateCW()
        assertEquals(180, viewModel.uiState.value.cropState.rotation)
    }

    @Test
    fun `rotateCW wraps at 360`() = runTest {
        repeat(4) { viewModel.rotateCW() }
        assertEquals(0, viewModel.uiState.value.cropState.rotation)
    }

    @Test
    fun `flipH toggles flipHorizontal`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            viewModel.flipH()
            assertTrue(awaitItem().cropState.flipHorizontal)
            viewModel.flipH()
            assertTrue(!awaitItem().cropState.flipHorizontal)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setBrightness updates adjustValues brightness`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            viewModel.setBrightness(50f)
            assertEquals(50f, awaitItem().adjustValues.brightness)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `selectFilter updates selectedFilterIndex`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            viewModel.selectFilter(3)
            assertEquals(3, awaitItem().selectedFilterIndex)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `addDrawPath appends to drawState paths`() = runTest {
        val path = DrawPath(
            points = listOf(androidx.compose.ui.geometry.Offset(0f, 0f)),
            color = androidx.compose.ui.graphics.Color.Red,
            brushSize = 8f
        )
        viewModel.uiState.test {
            awaitItem()
            viewModel.addDrawPath(path)
            assertEquals(1, awaitItem().drawState.paths.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `undoDrawPath removes last path`() = runTest {
        val path = DrawPath(
            points = listOf(androidx.compose.ui.geometry.Offset(0f, 0f)),
            color = androidx.compose.ui.graphics.Color.Red,
            brushSize = 8f
        )
        viewModel.addDrawPath(path)
        viewModel.undoDrawPath()
        assertTrue(viewModel.uiState.value.drawState.paths.isEmpty())
    }
}
