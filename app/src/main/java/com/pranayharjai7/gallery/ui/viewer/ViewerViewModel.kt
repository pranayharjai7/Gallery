package com.pranayharjai7.gallery.ui.viewer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pranayharjai7.gallery.domain.model.MediaItem
import com.pranayharjai7.gallery.domain.usecase.GetAllMediaUseCase
import com.pranayharjai7.gallery.domain.usecase.GetFavoritesUseCase
import com.pranayharjai7.gallery.domain.usecase.MoveToTrashUseCase
import com.pranayharjai7.gallery.domain.usecase.ToggleFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
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
    private val getFavorites: GetFavoritesUseCase,
    private val moveToTrash: MoveToTrashUseCase,
    private val toggleFavorite: ToggleFavoriteUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ViewerUiState())
    val uiState: StateFlow<ViewerUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    fun loadMedia(startMediaId: Long) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            combine(getAllMedia(), getFavorites()) { items, favItems ->
                Pair(items, favItems)
            }
                .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
                .collect { (items, favItems) ->
                    _uiState.update { state ->
                        val targetIndex = if (state.isLoading) {
                            // First load: find the startMediaId position
                            items.indexOfFirst { it.id == startMediaId }.coerceAtLeast(0)
                        } else {
                            // Subsequent emissions: keep current position, clamp to valid range
                            state.currentIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))
                        }
                        state.copy(
                            isLoading = false,
                            items = items,
                            currentIndex = targetIndex,
                            favoriteIds = favItems.map { it.id }.toSet()
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
