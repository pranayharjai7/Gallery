package com.gallery.ui.editor.video

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import androidx.annotation.OptIn
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.ScaleAndRotateTransformation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VideoEditUiState(
    val isLoading: Boolean = true,
    val videoUri: Uri? = null,
    val durationMs: Long = 0L,
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = 0L,
    val isMuted: Boolean = false,
    val rotation: Int = 0,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: String? = null
)

@OptIn(UnstableApi::class)
@HiltViewModel
class VideoEditViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val mediaId: Long = savedStateHandle["mediaId"] ?: 0L
    private val _uiState = MutableStateFlow(VideoEditUiState())
    val uiState: StateFlow<VideoEditUiState> = _uiState.asStateFlow()

    init { loadVideo() }

    private fun loadVideo() {
        viewModelScope.launch(Dispatchers.IO) {
            val uri = ContentUris.withAppendedId(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI, mediaId
            )
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                val duration = retriever
                    .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull() ?: 0L
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        videoUri = uri,
                        durationMs = duration,
                        trimEndMs = duration
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            } finally {
                retriever.release()
            }
        }
    }

    fun setTrimStart(ms: Long) =
        _uiState.update { it.copy(trimStartMs = ms.coerceIn(0L, it.trimEndMs - 1000L)) }

    fun setTrimEnd(ms: Long) =
        _uiState.update { it.copy(trimEndMs = ms.coerceIn(it.trimStartMs + 1000L, it.durationMs)) }

    fun toggleMute() = _uiState.update { it.copy(isMuted = !it.isMuted) }

    fun rotateCW() = _uiState.update { it.copy(rotation = (it.rotation + 90) % 360) }

    fun saveEdit() {
        val state = _uiState.value
        val sourceUri = state.videoUri ?: return
        _uiState.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            try {
                val outputUri = createOutputUri()

                val clippedMediaItem = MediaItem.Builder()
                    .setUri(sourceUri)
                    .setClippingConfiguration(
                        MediaItem.ClippingConfiguration.Builder()
                            .setStartPositionMs(state.trimStartMs)
                            .setEndPositionMs(state.trimEndMs)
                            .build()
                    )
                    .build()

                val videoEffects = if (state.rotation != 0) {
                    listOf(
                        ScaleAndRotateTransformation.Builder()
                            .setRotationDegrees(state.rotation.toFloat())
                            .build()
                    )
                } else {
                    emptyList()
                }
                val effects = Effects(emptyList(), videoEffects)

                val editedMediaItem = EditedMediaItem.Builder(clippedMediaItem)
                    .setRemoveAudio(state.isMuted)
                    .setEffects(effects)
                    .build()

                val transformer = Transformer.Builder(context).build()

                transformer.addListener(object : Transformer.Listener {
                    override fun onCompleted(
                        composition: Composition,
                        exportResult: ExportResult
                    ) {
                        val values = ContentValues().apply {
                            put(MediaStore.Video.Media.IS_PENDING, 0)
                        }
                        context.contentResolver.update(outputUri, values, null, null)
                        viewModelScope.launch {
                            _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
                        }
                    }

                    override fun onError(
                        composition: Composition,
                        exportResult: ExportResult,
                        exportException: ExportException
                    ) {
                        context.contentResolver.delete(outputUri, null, null)
                        viewModelScope.launch {
                            _uiState.update {
                                it.copy(isSaving = false, error = exportException.message)
                            }
                        }
                    }
                })

                transformer.start(editedMediaItem, outputUri.toString())

            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }

    private fun createOutputUri(): Uri {
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, "gallery_edit_${System.currentTimeMillis()}.mp4")
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/Gallery Edits")
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }
        return context.contentResolver.insert(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values
        ) ?: throw Exception("Failed to create MediaStore output entry")
    }
}
