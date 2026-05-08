package com.gallery.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gallery.domain.model.MediaItem
import com.gallery.domain.model.isVideo
import com.gallery.domain.usecase.AddToHiddenUseCase
import com.gallery.domain.usecase.MoveToTrashUseCase
import com.gallery.domain.usecase.SearchMediaUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class MediaFilter { ALL, PHOTOS, VIDEOS }

data class SearchUiState(
    val query: String = "",
    val filter: MediaFilter = MediaFilter.ALL,
    val results: List<MediaItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val rawResults: List<MediaItem> = emptyList(),
    val selectedIds: Set<Long> = emptySet()
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchMedia: SearchMediaUseCase,
    private val moveToTrash: MoveToTrashUseCase,
    private val addToHidden: AddToHiddenUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query, error = null) }
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.update { it.copy(results = emptyList(), isLoading = false, rawResults = emptyList()) }
            return
        }
        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            delay(300) // debounce
            searchMedia(query)
                .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.message ?: "Unknown error") } }
                .collect { items ->
                    _uiState.update { state ->
                        state.copy(isLoading = false, results = applyFilter(items, state.filter), rawResults = items)
                    }
                }
        }
    }

    fun onFilterChange(filter: MediaFilter) {
        _uiState.update { it.copy(filter = filter, results = applyFilter(it.rawResults, filter)) }
    }

    fun toggleSelection(id: Long) {
        _uiState.update { state ->
            val newSet = if (id in state.selectedIds) state.selectedIds - id else state.selectedIds + id
            state.copy(selectedIds = newSet)
        }
    }

    fun clearSelection() = _uiState.update { it.copy(selectedIds = emptySet()) }

    fun selectAll(allIds: List<Long>) = _uiState.update { it.copy(selectedIds = allIds.toSet()) }

    fun deleteSelected() {
        viewModelScope.launch {
            val snapshot = _uiState.value
            val ids = snapshot.selectedIds
            snapshot.results
                .filter { it.id in ids }
                .forEach { moveToTrash(it) }
            clearSelection()
        }
    }

    fun addSelectedToHidden() {
        val ids = _uiState.value.selectedIds.toSet()
        viewModelScope.launch {
            try {
                ids.forEach { addToHidden(it) }
            } finally {
                _uiState.update { it.copy(selectedIds = it.selectedIds - ids) }
            }
        }
    }

    private fun applyFilter(items: List<MediaItem>, filter: MediaFilter) = when (filter) {
        MediaFilter.ALL -> items
        MediaFilter.PHOTOS -> items.filter { !it.isVideo }
        MediaFilter.VIDEOS -> items.filter { it.isVideo }
    }
}
