package com.gallery.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gallery.domain.model.MediaItem
import com.gallery.domain.model.isVideo
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
    val error: String? = null
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchMedia: SearchMediaUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var rawResults: List<MediaItem> = emptyList()

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
        searchJob?.cancel()
        if (query.isBlank()) {
            rawResults = emptyList()
            _uiState.update { it.copy(results = emptyList(), isLoading = false) }
            return
        }
        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            delay(300) // debounce
            searchMedia(query)
                .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
                .collect { items ->
                    rawResults = items
                    _uiState.update { state ->
                        state.copy(isLoading = false, results = applyFilter(items, state.filter))
                    }
                }
        }
    }

    fun onFilterChange(filter: MediaFilter) {
        _uiState.update { it.copy(filter = filter, results = applyFilter(rawResults, filter)) }
    }

    private fun applyFilter(items: List<MediaItem>, filter: MediaFilter) = when (filter) {
        MediaFilter.ALL -> items
        MediaFilter.PHOTOS -> items.filter { !it.isVideo }
        MediaFilter.VIDEOS -> items.filter { it.isVideo }
    }
}
