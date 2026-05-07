package com.gallery.ui.viewer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gallery.domain.model.MediaItem
import com.gallery.domain.usecase.GetAllMediaUseCase
import com.gallery.domain.usecase.MoveToTrashUseCase
import com.gallery.domain.usecase.ToggleFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ViewerUiState(
    val isLoading: Boolean = true,
    val items: List<MediaItem> = emptyList(),
    val currentIndex: Int = 0,
    val overlaysVisible: Boolean = true,
    val favoriteIds: Set<Long> = emptySet(),
    val error: String? = null
)

@HiltViewModel
class ViewerViewModel @Inject constructor(
    private val getAllMedia: GetAllMediaUseCase,
    private val moveToTrash: MoveToTrashUseCase,
    private val toggleFavorite: ToggleFavoriteUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ViewerUiState())
    val uiState: StateFlow<ViewerUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    fun loadMedia(startMediaId: Long) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            getAllMedia()
                .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
                .collect { items ->
                    val index = items.indexOfFirst { it.id == startMediaId }.coerceAtLeast(0)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            items = items,
                            currentIndex = index
                        )
                    }
                }
        }
    }

    fun onPageChanged(index: Int) = _uiState.update { it.copy(currentIndex = index) }

    fun toggleOverlays() = _uiState.update { it.copy(overlaysVisible = !it.overlaysVisible) }

    fun toggleFavoriteItem(id: Long) {
        _uiState.update { state ->
            val newFavIds = if (id in state.favoriteIds) state.favoriteIds - id else state.favoriteIds + id
            state.copy(favoriteIds = newFavIds)
        }
        viewModelScope.launch { toggleFavorite(id) }
    }

    fun deleteCurrentItem() {
        val item = _uiState.value.items.getOrNull(_uiState.value.currentIndex) ?: return
        viewModelScope.launch { moveToTrash(item) }
    }
}
