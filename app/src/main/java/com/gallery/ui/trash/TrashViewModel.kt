package com.gallery.ui.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gallery.domain.model.TrashItem
import com.gallery.domain.usecase.GetTrashUseCase
import com.gallery.domain.usecase.PurgeTrashItemUseCase
import com.gallery.domain.usecase.RestoreFromTrashUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TrashUiState(
    val isLoading: Boolean = true,
    val items: List<TrashItem> = emptyList(),
    val selectedIds: Set<Long> = emptySet(),
    val error: String? = null
)

@HiltViewModel
class TrashViewModel @Inject constructor(
    private val getTrash: GetTrashUseCase,
    private val restore: RestoreFromTrashUseCase,
    private val purge: PurgeTrashItemUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrashUiState())
    val uiState: StateFlow<TrashUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getTrash()
                .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.message ?: "Unknown error") } }
                .collect { items ->
                    _uiState.update { it.copy(isLoading = false, items = items) }
                }
        }
    }

    fun restoreItem(item: TrashItem) {
        viewModelScope.launch { restore(item) }
    }

    fun purgeItem(id: Long) {
        viewModelScope.launch { purge(id) }
    }

    fun toggleSelection(id: Long) {
        _uiState.update { state ->
            val newIds = if (id in state.selectedIds) state.selectedIds - id else state.selectedIds + id
            state.copy(selectedIds = newIds)
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedIds = emptySet()) }
    }

    fun purgeSelected() {
        val snapshot = _uiState.value
        val ids = snapshot.selectedIds.toSet()
        viewModelScope.launch {
            ids.forEach { purge(it) }
            clearSelection()
        }
    }

    fun restoreSelected() {
        val snapshot = _uiState.value
        val ids = snapshot.selectedIds.toSet()
        viewModelScope.launch {
            snapshot.items.filter { it.id in ids }.forEach { restore(it) }
            clearSelection()
        }
    }

    fun emptyTrash() {
        val ids = _uiState.value.items.map { it.id }
        viewModelScope.launch {
            ids.forEach { purge(it) }
        }
    }
}
