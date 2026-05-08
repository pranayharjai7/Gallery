package com.gallery.ui.trash

import android.app.PendingIntent
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gallery.domain.model.TrashItem
import com.gallery.domain.usecase.GetTrashUseCase
import com.gallery.domain.usecase.PurgeAllTrashUseCase
import com.gallery.domain.usecase.PurgeTrashItemUseCase
import com.gallery.domain.usecase.RestoreFromTrashUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PendingDelete(
    val intentSender: IntentSender,
    val ids: List<Long>
)

data class TrashUiState(
    val isLoading: Boolean = true,
    val items: List<TrashItem> = emptyList(),
    val selectedIds: Set<Long> = emptySet(),
    val error: String? = null,
    val pendingDelete: PendingDelete? = null
)

@HiltViewModel
class TrashViewModel @Inject constructor(
    private val getTrash: GetTrashUseCase,
    private val restore: RestoreFromTrashUseCase,
    private val purge: PurgeTrashItemUseCase,
    private val purgeAll: PurgeAllTrashUseCase,
    @ApplicationContext private val context: Context
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
        val item = _uiState.value.items.firstOrNull { it.id == id } ?: return
        requestMediaStorePurge(listOf(id), listOf(item.originalUri))
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
        val ids = snapshot.selectedIds.toList()
        val uris = snapshot.items.filter { it.id in snapshot.selectedIds }.map { it.originalUri }
        requestMediaStorePurge(ids, uris)
    }

    fun restoreSelected() {
        val snapshot = _uiState.value
        val ids = snapshot.selectedIds.toSet()
        viewModelScope.launch {
            snapshot.items.filter { it.id in ids }.forEach { restore(it) }
            _uiState.update { it.copy(selectedIds = it.selectedIds - ids) }
        }
    }

    fun emptyTrash() {
        val items = _uiState.value.items
        requestMediaStorePurge(items.map { it.id }, items.map { it.originalUri })
    }

    private fun requestMediaStorePurge(ids: List<Long>, uris: List<Uri>) {
        if (uris.isEmpty()) return
        val pendingIntent: PendingIntent = MediaStore.createDeleteRequest(context.contentResolver, uris)
        _uiState.update { it.copy(pendingDelete = PendingDelete(pendingIntent.intentSender, ids)) }
    }

    fun onDeleteConfirmed() {
        val ids = _uiState.value.pendingDelete?.ids ?: return
        viewModelScope.launch {
            purgeAll(ids)
            _uiState.update { it.copy(pendingDelete = null, selectedIds = it.selectedIds - ids.toSet()) }
        }
    }

    fun clearPendingDelete() {
        _uiState.update { it.copy(pendingDelete = null) }
    }
}
