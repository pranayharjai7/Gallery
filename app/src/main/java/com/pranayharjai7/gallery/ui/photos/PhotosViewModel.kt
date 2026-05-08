package com.pranayharjai7.gallery.ui.photos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pranayharjai7.gallery.domain.model.MediaItem
import com.pranayharjai7.gallery.domain.usecase.AddToHiddenUseCase
import com.pranayharjai7.gallery.domain.usecase.GetAllMediaUseCase
import com.pranayharjai7.gallery.domain.usecase.MoveToTrashUseCase
import com.pranayharjai7.gallery.domain.usecase.ToggleFavoriteUseCase
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
    val isFeatured: Boolean,
    val isCompanion: Boolean = false
)

data class PhotosUiState(
    val isLoading: Boolean = true,
    val groupedMedia: Map<String, List<IndexedMediaItem>> = emptyMap(),
    val memoriesItems: List<MediaItem> = emptyList(),
    val memoriesDismissed: Boolean = false,
    val selectedIds: Set<Long> = emptySet(),
    val error: String? = null
)

@HiltViewModel
class PhotosViewModel @Inject constructor(
    private val getAllMedia: GetAllMediaUseCase,
    private val moveToTrash: MoveToTrashUseCase,
    private val toggleFavorite: ToggleFavoriteUseCase,
    private val addToHidden: AddToHiddenUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PhotosUiState())
    val uiState: StateFlow<PhotosUiState> = _uiState.asStateFlow()

    init {
        loadMedia()
    }

    private fun loadMedia() {
        viewModelScope.launch {
            getAllMedia().collect { items ->
                val memories = deriveMemories(items)
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        groupedMedia = groupByDate(items),
                        memoriesItems = if (!state.memoriesDismissed) memories else state.memoriesItems
                    )
                }
            }
        }
    }

    private fun deriveMemories(items: List<MediaItem>): List<MediaItem> {
        val today = Calendar.getInstance()
        val month = today.get(Calendar.MONTH) + 1
        val day   = today.get(Calendar.DAY_OF_MONTH)
        val thisYear = today.get(Calendar.YEAR)
        val matches = items.filter { item ->
            val cal = Calendar.getInstance().apply { timeInMillis = item.dateTaken }
            cal.get(Calendar.YEAR) != thisYear &&
                cal.get(Calendar.MONTH) + 1 == month &&
                cal.get(Calendar.DAY_OF_MONTH) == day
        }
        return if (matches.size >= 3) matches else emptyList()
    }

    fun toggleSelection(id: Long) {
        _uiState.update { state ->
            val newSet = if (id in state.selectedIds) state.selectedIds - id else state.selectedIds + id
            state.copy(selectedIds = newSet)
        }
    }

    fun clearSelection() = _uiState.update { it.copy(selectedIds = emptySet()) }

    fun dismissMemories() = _uiState.update { it.copy(memoriesItems = emptyList(), memoriesDismissed = true) }

    fun selectAll(allIds: List<Long>) = _uiState.update { it.copy(selectedIds = allIds.toSet()) }

    fun deleteSelected() {
        viewModelScope.launch {
            val snapshot = _uiState.value
            val ids = snapshot.selectedIds
            snapshot.groupedMedia.values
                .flatten()
                .filter { it.item.id in ids }
                .forEach { moveToTrash(it.item) }
            clearSelection()
        }
    }

    fun toggleFavoriteItem(id: Long) {
        viewModelScope.launch { toggleFavorite(id) }
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

    private fun groupByDate(items: List<MediaItem>): Map<String, List<IndexedMediaItem>> {
        val now = System.currentTimeMillis()
        val today = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val yesterday = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }

        val groups = LinkedHashMap<String, MutableList<MediaItem>>()
        val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        items.forEach { item ->
            val cal = Calendar.getInstance().apply { timeInMillis = item.dateTaken }
            val key = when {
                cal.after(today) || isSameDay(cal, today) -> "TODAY"
                isSameDay(cal, yesterday) -> "YESTERDAY"
                else -> monthYearFormat.format(cal.time).uppercase(Locale.getDefault())
            }
            groups.getOrPut(key) { mutableListOf() }.add(item)
        }

        return groups.mapValues { (_, list) ->
            list.mapIndexed { index, item ->
                IndexedMediaItem(
                    item = item,
                    isFeatured = index % 8 == 7,
                    isCompanion = index % 8 == 6
                )
            }
        }
    }

    private fun isSameDay(a: Calendar, b: Calendar): Boolean =
        a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
            a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
}
