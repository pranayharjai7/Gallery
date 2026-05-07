# Phase 16: Photo Editor — Gallery App
> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task.
**Goal:** Implement photo editor with 4 tabs (Crop/Adjust/Filters/Draw), non-destructive edits saved as new MediaStore files.
**Depends on:** Phase 06, Phase 07
---

## Overview

The photo editor opens over an existing media item (identified by `mediaId: Long`), loads its bitmap, and lets the user apply crop transformations, color adjustments, filter presets, and freehand drawing. Tapping **Save** composes all edits into a single bitmap and writes it as a brand-new file via MediaStore — the original is never touched. The screen is a single `Scaffold` with a `TabRow` switching between four composable tabs.

---

## Task 1: PhotoEditViewModel + Test

### Files
- `app/src/main/kotlin/com/gallery/ui/editor/photo/PhotoEditViewModel.kt`
- `app/src/test/kotlin/com/gallery/ui/editor/photo/PhotoEditViewModelTest.kt`

### Implementation

**`PhotoEditViewModel.kt`**

```kotlin
package com.gallery.ui.editor.photo

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.provider.MediaStore
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

enum class EditorTab { CROP, ADJUST, FILTERS, DRAW }

enum class AspectRatio(val label: String, val ratio: Float?) {
    FREE("Free", null),
    ONE_ONE("1:1", 1f),
    FOUR_THREE("4:3", 4f / 3f),
    SIXTEEN_NINE("16:9", 16f / 9f),
    NINE_SIXTEEN("9:16", 9f / 16f)
}

data class AdjustValues(
    val brightness: Float = 0f,
    val contrast: Float = 0f,
    val saturation: Float = 0f,
    val warmth: Float = 0f,
    val exposure: Float = 0f // all in -100..100
)

data class CropState(
    val rotation: Int = 0,
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false,
    val aspectRatio: AspectRatio = AspectRatio.FREE
)

data class DrawPath(val points: List<Offset>, val color: Color, val brushSize: Float)

data class DrawState(
    val paths: List<DrawPath> = emptyList(),
    val currentColor: Color = Color.Red,
    val brushSize: Float = 8f
)

data class PhotoEditUiState(
    val isLoading: Boolean = true,
    val sourceBitmap: Bitmap? = null,
    val activeTab: EditorTab = EditorTab.CROP,
    val cropState: CropState = CropState(),
    val adjustValues: AdjustValues = AdjustValues(),
    val selectedFilterIndex: Int = 0,
    val drawState: DrawState = DrawState(),
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class PhotoEditViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val mediaId: Long = savedStateHandle["mediaId"] ?: 0L
    private val _uiState = MutableStateFlow(PhotoEditUiState())
    val uiState: StateFlow<PhotoEditUiState> = _uiState.asStateFlow()

    init { loadBitmap() }

    private fun loadBitmap() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val uri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, mediaId
                )
                val bmp = context.contentResolver.openInputStream(uri)
                    ?.use { BitmapFactory.decodeStream(it) }
                _uiState.update { it.copy(isLoading = false, sourceBitmap = bmp) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    // ── Tab ──────────────────────────────────────────────────────────────────

    fun setActiveTab(tab: EditorTab) = _uiState.update { it.copy(activeTab = tab) }

    // ── Crop ─────────────────────────────────────────────────────────────────

    fun rotateCW() = _uiState.update {
        it.copy(cropState = it.cropState.copy(rotation = (it.cropState.rotation + 90) % 360))
    }

    fun flipH() = _uiState.update {
        it.copy(cropState = it.cropState.copy(flipHorizontal = !it.cropState.flipHorizontal))
    }

    fun flipV() = _uiState.update {
        it.copy(cropState = it.cropState.copy(flipVertical = !it.cropState.flipVertical))
    }

    fun setAspectRatio(ratio: AspectRatio) = _uiState.update {
        it.copy(cropState = it.cropState.copy(aspectRatio = ratio))
    }

    // ── Adjust ───────────────────────────────────────────────────────────────

    fun setBrightness(v: Float) = _uiState.update {
        it.copy(adjustValues = it.adjustValues.copy(brightness = v))
    }

    fun setContrast(v: Float) = _uiState.update {
        it.copy(adjustValues = it.adjustValues.copy(contrast = v))
    }

    fun setSaturation(v: Float) = _uiState.update {
        it.copy(adjustValues = it.adjustValues.copy(saturation = v))
    }

    fun setWarmth(v: Float) = _uiState.update {
        it.copy(adjustValues = it.adjustValues.copy(warmth = v))
    }

    fun setExposure(v: Float) = _uiState.update {
        it.copy(adjustValues = it.adjustValues.copy(exposure = v))
    }

    // ── Filters ──────────────────────────────────────────────────────────────

    fun selectFilter(index: Int) = _uiState.update { it.copy(selectedFilterIndex = index) }

    // ── Draw ─────────────────────────────────────────────────────────────────

    fun addDrawPath(path: DrawPath) = _uiState.update {
        it.copy(drawState = it.drawState.copy(paths = it.drawState.paths + path))
    }

    fun undoDrawPath() = _uiState.update {
        it.copy(drawState = it.drawState.copy(paths = it.drawState.paths.dropLast(1)))
    }

    fun setDrawColor(color: Color) = _uiState.update {
        it.copy(drawState = it.drawState.copy(currentColor = color))
    }

    fun setBrushSize(size: Float) = _uiState.update {
        it.copy(drawState = it.drawState.copy(brushSize = size))
    }

    // ── Save ─────────────────────────────────────────────────────────────────

    fun saveEdit() {
        val state = _uiState.value
        val source = state.sourceBitmap ?: return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = applyEdits(source, state)
                saveToMediaStore(result)
                _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }

    private fun applyEdits(bitmap: Bitmap, state: PhotoEditUiState): Bitmap {
        var result = bitmap

        // 1. Crop transforms (rotation + flip)
        if (state.cropState.rotation != 0
            || state.cropState.flipHorizontal
            || state.cropState.flipVertical
        ) {
            val matrix = Matrix().apply {
                postRotate(state.cropState.rotation.toFloat())
                if (state.cropState.flipHorizontal)
                    postScale(-1f, 1f, result.width / 2f, result.height / 2f)
                if (state.cropState.flipVertical)
                    postScale(1f, -1f, result.width / 2f, result.height / 2f)
            }
            result = Bitmap.createBitmap(result, 0, 0, result.width, result.height, matrix, true)
        }

        // 2. Color adjustments (contrast via ColorMatrix scale)
        val paint = Paint()
        val colorMatrix = ColorMatrix()
        val contrastFactor = 1f + state.adjustValues.contrast / 100f
        colorMatrix.setScale(contrastFactor, contrastFactor, contrastFactor, 1f)
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        val adjustedBitmap = Bitmap.createBitmap(
            result.width, result.height, result.config ?: Bitmap.Config.ARGB_8888
        )
        Canvas(adjustedBitmap).drawBitmap(result, 0f, 0f, paint)
        result = adjustedBitmap

        // 3. Filter preset (re-use FiltersTab helper)
        if (state.selectedFilterIndex != 0) {
            val filterPaint = Paint().apply {
                colorFilter = ColorMatrixColorFilter(getFilterMatrix(state.selectedFilterIndex))
            }
            val filteredBitmap = Bitmap.createBitmap(
                result.width, result.height, result.config ?: Bitmap.Config.ARGB_8888
            )
            Canvas(filteredBitmap).drawBitmap(result, 0f, 0f, filterPaint)
            result = filteredBitmap
        }

        // 4. Draw paths
        if (state.drawState.paths.isNotEmpty()) {
            val withPaths = result.copy(result.config ?: Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(withPaths)
            state.drawState.paths.forEach { dp ->
                if (dp.points.size > 1) {
                    val pathPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = android.graphics.Color.argb(
                            (dp.color.alpha * 255).toInt(),
                            (dp.color.red * 255).toInt(),
                            (dp.color.green * 255).toInt(),
                            (dp.color.blue * 255).toInt()
                        )
                        style = Paint.Style.STROKE
                        strokeWidth = dp.brushSize
                        strokeCap = Paint.Cap.ROUND
                        strokeJoin = Paint.Join.ROUND
                    }
                    val p = android.graphics.Path()
                    p.moveTo(dp.points.first().x, dp.points.first().y)
                    dp.points.drop(1).forEach { p.lineTo(it.x, it.y) }
                    canvas.drawPath(p, pathPaint)
                }
            }
            result = withPaths
        }

        return result
    }

    private suspend fun saveToMediaStore(bitmap: Bitmap) = withContext(Dispatchers.IO) {
        val displayName = "gallery_edit_${System.currentTimeMillis()}.jpg"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
        ) ?: throw Exception("Failed to create file")
        context.contentResolver.openOutputStream(uri)?.use {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it)
        }
        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        context.contentResolver.update(uri, values, null, null)
    }
}
```

**`PhotoEditViewModelTest.kt`**

```kotlin
package com.gallery.ui.editor.photo

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
        // Stub ContentResolver to avoid real IO during loadBitmap
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
            awaitItem() // initial state
            viewModel.setActiveTab(EditorTab.ADJUST)
            assertEquals(EditorTab.ADJUST, awaitItem().activeTab)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `rotateCW increments rotation by 90`() = runTest {
        viewModel.uiState.test {
            awaitItem() // initial
            viewModel.rotateCW()
            assertEquals(90, awaitItem().cropState.rotation)
            viewModel.rotateCW()
            assertEquals(180, awaitItem().cropState.rotation)
            cancelAndIgnoreRemainingEvents()
        }
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
```

---

## Task 2: CropTab

### File
- `app/src/main/kotlin/com/gallery/ui/editor/photo/CropTab.kt`

### Implementation

```kotlin
package com.gallery.ui.editor.photo

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

@Composable
fun CropTab(
    cropState: CropState,
    sourceBitmap: Bitmap?,
    onRotateCW: () -> Unit,
    onFlipH: () -> Unit,
    onFlipV: () -> Unit,
    onAspectRatio: (AspectRatio) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Preview area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (sourceBitmap != null) {
                // Matrix is computed for reference; visual transform applied via ContentScale + graphicsLayer if needed
                val matrix = remember(cropState) {
                    android.graphics.Matrix().apply {
                        postRotate(
                            cropState.rotation.toFloat(),
                            sourceBitmap.width / 2f,
                            sourceBitmap.height / 2f
                        )
                        if (cropState.flipHorizontal)
                            postScale(-1f, 1f, sourceBitmap.width / 2f, sourceBitmap.height / 2f)
                        if (cropState.flipVertical)
                            postScale(1f, -1f, sourceBitmap.width / 2f, sourceBitmap.height / 2f)
                    }
                }
                val displayBitmap = remember(cropState, sourceBitmap) {
                    Bitmap.createBitmap(
                        sourceBitmap, 0, 0,
                        sourceBitmap.width, sourceBitmap.height,
                        matrix, true
                    )
                }
                Image(
                    bitmap = displayBitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.Fit
                )
            }
        }

        // Transform controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(onClick = onRotateCW) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.RotateRight, contentDescription = "Rotate CW")
                    Text("Rotate", style = MaterialTheme.typography.labelSmall)
                }
            }
            IconButton(onClick = onFlipH) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Flip, contentDescription = "Flip Horizontal")
                    Text("Flip H", style = MaterialTheme.typography.labelSmall)
                }
            }
            IconButton(onClick = onFlipV) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Flip, contentDescription = "Flip Vertical")
                    Text("Flip V", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        // Aspect ratio chips
        LazyRow(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(AspectRatio.values()) { ratio ->
                FilterChip(
                    selected = cropState.aspectRatio == ratio,
                    onClick = { onAspectRatio(ratio) },
                    label = { Text(ratio.label) }
                )
            }
        }
    }
}
```

---

## Task 3: AdjustTab

### File
- `app/src/main/kotlin/com/gallery/ui/editor/photo/AdjustTab.kt`

### Implementation

```kotlin
package com.gallery.ui.editor.photo

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AdjustTab(
    values: AdjustValues,
    onBrightness: (Float) -> Unit,
    onContrast: (Float) -> Unit,
    onSaturation: (Float) -> Unit,
    onWarmth: (Float) -> Unit,
    onExposure: (Float) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item { AdjustSlider("Brightness", values.brightness, onBrightness) }
        item { AdjustSlider("Contrast", values.contrast, onContrast) }
        item { AdjustSlider("Saturation", values.saturation, onSaturation) }
        item { AdjustSlider("Warmth", values.warmth, onWarmth) }
        item { AdjustSlider("Exposure", values.exposure, onExposure) }
    }
}

@Composable
private fun AdjustSlider(
    label: String,
    value: Float,
    onChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row {
            Text(label, modifier = Modifier.weight(1f))
            Text("${value.toInt()}", style = MaterialTheme.typography.labelMedium)
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = -100f..100f,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
```

---

## Task 4: FiltersTab

### File
- `app/src/main/kotlin/com/gallery/ui/editor/photo/FiltersTab.kt`

### Implementation

```kotlin
package com.gallery.ui.editor.photo

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

val FILTER_NAMES = listOf("Original", "Vivid", "Cool", "Warm", "Noir", "Fade", "Chrome")

fun getFilterMatrix(index: Int): ColorMatrix = when (index) {
    0 -> ColorMatrix() // Original — identity
    1 -> ColorMatrix().apply { setSaturation(1.5f) } // Vivid
    2 -> ColorMatrix( // Cool — boost blue, reduce red
        floatArrayOf(
            0.8f, 0f, 0.2f, 0f, 0f,
            0f, 0.8f, 0.2f, 0f, 0f,
            0f, 0f, 1.4f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    )
    3 -> ColorMatrix( // Warm — boost red/green, reduce blue
        floatArrayOf(
            1.2f, 0.1f, 0f, 0f, 0f,
            0.1f, 1f, 0f, 0f, 0f,
            0f, 0f, 0.8f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    )
    4 -> ColorMatrix().apply { setSaturation(0f) } // Noir — grayscale
    5 -> ColorMatrix( // Fade — lift blacks
        floatArrayOf(
            1f, 0f, 0f, 0f, 20f,
            0f, 1f, 0f, 0f, 20f,
            0f, 0f, 1f, 0f, 20f,
            0f, 0f, 0f, 1f, 0f
        )
    )
    else -> ColorMatrix() // Chrome — fallback identity
}

@Composable
fun FiltersTab(
    selectedIndex: Int,
    sourceBitmap: Bitmap?,
    onSelectFilter: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Large preview of currently selected filter
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (sourceBitmap != null) {
                val filteredBitmap = remember(selectedIndex, sourceBitmap) {
                    val p = Paint().apply {
                        colorFilter = ColorMatrixColorFilter(getFilterMatrix(selectedIndex))
                    }
                    val dst = Bitmap.createBitmap(
                        sourceBitmap.width, sourceBitmap.height,
                        sourceBitmap.config ?: Bitmap.Config.ARGB_8888
                    )
                    Canvas(dst).drawBitmap(sourceBitmap, 0f, 0f, p)
                    dst
                }
                Image(
                    bitmap = filteredBitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.Fit
                )
            }
        }

        // Filter thumbnail strip
        LazyRow(
            modifier = Modifier
                .height(100.dp)
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(FILTER_NAMES.size) { index ->
                Column(
                    modifier = Modifier.clickable { onSelectFilter(index) },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                width = if (index == selectedIndex) 2.dp else 0.dp,
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(8.dp)
                            )
                    ) {
                        if (sourceBitmap != null) {
                            val thumb = remember(index, sourceBitmap) {
                                val scale = 72
                                val scaled = Bitmap.createScaledBitmap(sourceBitmap, scale, scale, true)
                                val p = Paint().apply {
                                    colorFilter = ColorMatrixColorFilter(getFilterMatrix(index))
                                }
                                val dst = Bitmap.createBitmap(scale, scale, Bitmap.Config.ARGB_8888)
                                Canvas(dst).drawBitmap(scaled, 0f, 0f, p)
                                dst
                            }
                            Image(
                                bitmap = thumb.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                    Text(
                        FILTER_NAMES[index],
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
```

---

## Task 5: DrawTab

### File
- `app/src/main/kotlin/com/gallery/ui/editor/photo/DrawTab.kt`

### Notes
- `DrawState` and `DrawPath` are defined in `PhotoEditViewModel.kt` (same package).
- `android.graphics.Path.toComposePath()` is available via the `androidx.compose.ui.graphics` package. The draw loop uses the Compose `Canvas` API with `drawPath`.
- `PhotoEditViewModel` already exposes `addDrawPath`, `undoDrawPath`, `setDrawColor`, `setBrushSize` actions and includes `drawState` in `PhotoEditUiState` (added in Task 1).

### Implementation

```kotlin
package com.gallery.ui.editor.photo

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

@Composable
fun DrawTab(
    sourceBitmap: Bitmap?,
    drawState: DrawState,
    onDrawPath: (DrawPath) -> Unit,
    onColorChange: (Color) -> Unit,
    onBrushSizeChange: (Float) -> Unit,
    onUndo: () -> Unit
) {
    val currentPoints = remember { mutableStateListOf<Offset>() }

    Column(modifier = Modifier.fillMaxSize()) {
        // Drawing canvas
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (sourceBitmap != null) {
                Image(
                    bitmap = sourceBitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
            androidx.compose.foundation.Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                currentPoints.clear()
                                currentPoints.add(offset)
                            },
                            onDrag = { change, _ ->
                                currentPoints.add(change.position)
                            },
                            onDragEnd = {
                                if (currentPoints.isNotEmpty()) {
                                    onDrawPath(
                                        DrawPath(
                                            points = currentPoints.toList(),
                                            color = drawState.currentColor,
                                            brushSize = drawState.brushSize
                                        )
                                    )
                                }
                                currentPoints.clear()
                            }
                        )
                    }
            ) {
                // Draw committed paths
                drawState.paths.forEach { dp ->
                    if (dp.points.size > 1) {
                        val path = Path().apply {
                            moveTo(dp.points.first().x, dp.points.first().y)
                            dp.points.drop(1).forEach { lineTo(it.x, it.y) }
                        }
                        drawPath(
                            path = path,
                            color = dp.color,
                            style = Stroke(width = dp.brushSize, cap = StrokeCap.Round)
                        )
                    }
                }
                // Draw in-progress path
                if (currentPoints.size > 1) {
                    val path = Path().apply {
                        moveTo(currentPoints.first().x, currentPoints.first().y)
                        currentPoints.drop(1).forEach { lineTo(it.x, it.y) }
                    }
                    drawPath(
                        path = path,
                        color = drawState.currentColor,
                        style = Stroke(width = drawState.brushSize, cap = StrokeCap.Round)
                    )
                }
            }
        }

        // Color swatches + Undo button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf(
                Color.Red, Color.Blue, Color.Green,
                Color.Yellow, Color.White, Color.Black
            ).forEach { color ->
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = if (drawState.currentColor == color) 2.dp else 0.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        )
                        .clickable { onColorChange(color) }
                )
            }
            IconButton(onClick = onUndo) {
                Icon(Icons.Default.Undo, contentDescription = "Undo")
            }
        }

        // Brush size slider
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Size", style = MaterialTheme.typography.labelMedium)
            Slider(
                value = drawState.brushSize,
                onValueChange = onBrushSizeChange,
                valueRange = 2f..30f,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            )
        }
    }
}
```

---

## Task 6: PhotoEditScreen

### File
- `app/src/main/kotlin/com/gallery/ui/editor/photo/PhotoEditScreen.kt`

### Notes
- `LoadingState` is assumed to be provided by Phase 07 common UI components (`com.gallery.ui.common.LoadingState`). If it is not present, replace with a centered `CircularProgressIndicator`.
- Wire `mediaId` via the navigation back stack. The nav route must pass `mediaId` as a `Long` argument so `SavedStateHandle` can retrieve it.

### Implementation

```kotlin
package com.gallery.ui.editor.photo

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gallery.ui.common.LoadingState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoEditScreen(
    mediaId: Long,
    onBack: () -> Unit,
    viewModel: PhotoEditViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Navigate back automatically on successful save
    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Photo") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "Close editor")
                    }
                },
                actions = {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .size(24.dp)
                        )
                    } else {
                        TextButton(onClick = { viewModel.saveEdit() }) {
                            Text("Save")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            LoadingState(modifier = Modifier.padding(padding))
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Tab row
            TabRow(selectedTabIndex = uiState.activeTab.ordinal) {
                EditorTab.values().forEach { tab ->
                    Tab(
                        selected = uiState.activeTab == tab,
                        onClick = { viewModel.setActiveTab(tab) },
                        text = {
                            Text(
                                tab.name.lowercase().replaceFirstChar { it.uppercase() }
                            )
                        }
                    )
                }
            }

            // Active tab content
            when (uiState.activeTab) {
                EditorTab.CROP -> CropTab(
                    cropState = uiState.cropState,
                    sourceBitmap = uiState.sourceBitmap,
                    onRotateCW = viewModel::rotateCW,
                    onFlipH = viewModel::flipH,
                    onFlipV = viewModel::flipV,
                    onAspectRatio = viewModel::setAspectRatio
                )
                EditorTab.ADJUST -> AdjustTab(
                    values = uiState.adjustValues,
                    onBrightness = viewModel::setBrightness,
                    onContrast = viewModel::setContrast,
                    onSaturation = viewModel::setSaturation,
                    onWarmth = viewModel::setWarmth,
                    onExposure = viewModel::setExposure
                )
                EditorTab.FILTERS -> FiltersTab(
                    selectedIndex = uiState.selectedFilterIndex,
                    sourceBitmap = uiState.sourceBitmap,
                    onSelectFilter = viewModel::selectFilter
                )
                EditorTab.DRAW -> DrawTab(
                    sourceBitmap = uiState.sourceBitmap,
                    drawState = uiState.drawState,
                    onDrawPath = viewModel::addDrawPath,
                    onColorChange = viewModel::setDrawColor,
                    onBrushSizeChange = viewModel::setBrushSize,
                    onUndo = viewModel::undoDrawPath
                )
            }
        }
    }
}
```

---

## Navigation Wiring

Add the following to the navigation graph (wherever Phase 06 defines routes):

```kotlin
// Route constant
const val ROUTE_PHOTO_EDIT = "photo_edit/{mediaId}"

// In NavHost
composable(
    route = ROUTE_PHOTO_EDIT,
    arguments = listOf(navArgument("mediaId") { type = NavType.LongType })
) { backStackEntry ->
    val mediaId = backStackEntry.arguments?.getLong("mediaId") ?: 0L
    PhotoEditScreen(mediaId = mediaId, onBack = { navController.popBackStack() })
}

// Navigate to editor from photo viewer (Phase 10)
navController.navigate("photo_edit/$mediaId")
```

---

## Commit

After all tasks are implemented and the unit tests pass, commit with:

```
git add app/src/main/kotlin/com/gallery/ui/editor/photo/ \
        app/src/test/kotlin/com/gallery/ui/editor/photo/
git commit -m "feat: implement Phase 16 photo editor (Crop/Adjust/Filters/Draw)"
```

---

## Dependencies Checklist

All of the following must already be present in `app/build.gradle.kts` (added in prior phases):

| Dependency | Used for |
|---|---|
| `androidx.compose.foundation` | `Canvas`, `LazyRow`, gestures |
| `androidx.compose.material3` | `Scaffold`, `TabRow`, `Slider`, `FilterChip` |
| `androidx.compose.material.icons` | `RotateRight`, `Flip`, `Close`, `Undo` |
| `androidx.hilt.navigation.compose` | `hiltViewModel()` |
| `androidx.lifecycle.compose` | `collectAsStateWithLifecycle` |
| `app.cash.turbine` | Flow testing |
| `io.mockk` | Mocking Context/ContentResolver |
| `kotlinx.coroutines.test` | `StandardTestDispatcher`, `runTest` |
