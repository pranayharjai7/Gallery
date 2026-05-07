package com.gallery.ui.photos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gallery.domain.model.MediaItem
import com.gallery.domain.usecase.GetAllMediaUseCase
import com.gallery.domain.usecase.GetOnThisDayUseCase
import com.gallery.domain.usecase.MoveToTrashUseCase
import com.gallery.domain.usecase.ToggleFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.LinkedHashMap
import java.util.Locale
import javax.inject.Inject

data class IndexedMediaItem(
    val item: MediaItem,
    val isFeatured: Boolean
)

data class PhotosUiState(
    val isLoading: Boolean = true,
    val groupedMedia: Map<String, List<IndexedMediaItem>> = emptyMap(),
    val memoriesItems: List<MediaItem> = emptyList(),
    val selectedIds: Set<Long> = emptySet(),
    val error: String? = null
)

@HiltViewModel
class PhotosViewModel @Inject constructor(
    private val getAllMedia: GetAllMediaUseCase,
    private val moveToTrash: MoveToTrashUseCase,
    private val toggleFavorite: ToggleFavoriteUseCase,
    private val getOnThisDay: GetOnThisDayUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PhotosUiState())
    val uiState: StateFlow<PhotosUiState> = _uiState.asStateFlow()

    init {
        loadMedia()
        loadMemories()
    }

    private fun loadMedia() {
        viewModelScope.launch {
            getAllMedia().collect { items ->
                _uiState.update {
                    it.copy(isLoading = false, groupedMedia = groupByDate(items))
                }
            }
        }
    }

    private fun loadMemories() {
        val today = Calendar.getInstance()
        val monthDay = (today.get(Calendar.MONTH) + 1) * 100 + today.get(Calendar.DAY_OF_MONTH)
        viewModelScope.launch {
            getOnThisDay(monthDay).collect { items ->
                if (items.size >= 3) {
                    _uiState.update { it.copy(memoriesItems = items) }
                }
            }
        }
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
        val ids = _uiState.value.selectedIds
        viewModelScope.launch {
            _uiState.value.groupedMedia.values
                .flatten()
                .filter { it.item.id in ids }
                .forEach { moveToTrash(it.item) }
            clearSelection()
        }
    }

    fun toggleFavoriteItem(id: Long) {
        viewModelScope.launch { toggleFavorite(id) }
    }

    private fun groupByDate(items: List<MediaItem>): Map<String, List<IndexedMediaItem>> {
        val now = System.currentTimeMillis()
        val today = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val yesterday = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }

        val groups = LinkedHashMap<String, MutableList<MediaItem>>()
        items.forEach { item ->
            val cal = Calendar.getInstance().apply { timeInMillis = item.dateTaken }
            val key = when {
                cal.after(today) || isSameDay(cal, today) -> "TODAY"
                isSameDay(cal, yesterday) -> "YESTERDAY"
                else -> SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                    .format(cal.time).uppercase(Locale.getDefault())
            }
            groups.getOrPut(key) { mutableListOf() }.add(item)
        }

        return groups.mapValues { (_, list) ->
            list.mapIndexed { index, item -> IndexedMediaItem(item = item, isFeatured = index % 8 == 7) }
        }
    }

    private fun isSameDay(a: Calendar, b: Calendar): Boolean =
        a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
            a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
}
