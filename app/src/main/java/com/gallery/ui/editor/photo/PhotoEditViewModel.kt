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
    val exposure: Float = 0f
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

    fun setActiveTab(tab: EditorTab) = _uiState.update { it.copy(activeTab = tab) }

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

    fun selectFilter(index: Int) = _uiState.update { it.copy(selectedFilterIndex = index) }

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
