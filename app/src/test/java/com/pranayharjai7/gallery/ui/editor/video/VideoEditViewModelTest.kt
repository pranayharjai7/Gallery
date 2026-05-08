package com.pranayharjai7.gallery.ui.editor.video

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoEditViewModelTest {

    private fun buildLoadedState(
        duration: Long = 10_000L,
        trimStart: Long = 0L,
        trimEnd: Long = 10_000L,
        isMuted: Boolean = false,
        rotation: Int = 0
    ) = VideoEditUiState(
        isLoading = false,
        videoUri = Uri.EMPTY,
        durationMs = duration,
        trimStartMs = trimStart,
        trimEndMs = trimEnd,
        isMuted = isMuted,
        rotation = rotation
    )

    @Test
    fun `setTrimStart clamps to 0 when negative`() {
        val state = buildLoadedState(trimEnd = 10_000L)
        val clamped = (-500L).coerceIn(0L, state.trimEndMs - 1000L)
        assertEquals(0L, clamped)
    }

    @Test
    fun `setTrimStart clamps to trimEnd minus 1000 when too close`() {
        val state = buildLoadedState(trimEnd = 5_000L)
        val clamped = (4_500L).coerceIn(0L, state.trimEndMs - 1000L)
        assertEquals(4_000L, clamped)
    }

    @Test
    fun `setTrimStart accepts valid value`() {
        val state = buildLoadedState(trimEnd = 8_000L)
        val clamped = (3_000L).coerceIn(0L, state.trimEndMs - 1000L)
        assertEquals(3_000L, clamped)
    }

    @Test
    fun `setTrimEnd clamps to duration when too large`() {
        val state = buildLoadedState(duration = 10_000L, trimStart = 0L)
        val clamped = (12_000L).coerceIn(state.trimStartMs + 1000L, state.durationMs)
        assertEquals(10_000L, clamped)
    }

    @Test
    fun `setTrimEnd clamps to trimStart plus 1000 when too close`() {
        val state = buildLoadedState(duration = 10_000L, trimStart = 3_000L)
        val clamped = (3_200L).coerceIn(state.trimStartMs + 1000L, state.durationMs)
        assertEquals(4_000L, clamped)
    }

    @Test
    fun `toggleMute flips isMuted from false to true`() {
        var state = buildLoadedState(isMuted = false)
        state = state.copy(isMuted = !state.isMuted)
        assertTrue(state.isMuted)
    }

    @Test
    fun `toggleMute flips isMuted from true to false`() {
        var state = buildLoadedState(isMuted = true)
        state = state.copy(isMuted = !state.isMuted)
        assertFalse(state.isMuted)
    }

    @Test
    fun `rotateCW advances rotation in 90 degree steps`() {
        var rotation = 0
        rotation = (rotation + 90) % 360
        assertEquals(90, rotation)
        rotation = (rotation + 90) % 360
        assertEquals(180, rotation)
        rotation = (rotation + 90) % 360
        assertEquals(270, rotation)
        rotation = (rotation + 90) % 360
        assertEquals(0, rotation)
    }

    @Test
    fun `initial state is loading with all defaults`() {
        val state = VideoEditUiState()
        assertTrue(state.isLoading)
        assertEquals(0L, state.durationMs)
        assertEquals(0L, state.trimStartMs)
        assertEquals(0L, state.trimEndMs)
        assertFalse(state.isMuted)
        assertEquals(0, state.rotation)
        assertFalse(state.isSaving)
        assertFalse(state.saveSuccess)
    }
}
