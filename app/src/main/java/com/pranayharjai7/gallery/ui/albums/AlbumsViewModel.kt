package com.pranayharjai7.gallery.ui.albums

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pranayharjai7.gallery.domain.model.Album
import com.pranayharjai7.gallery.domain.model.AlbumType
import com.pranayharjai7.gallery.domain.usecase.GetAlbumsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AlbumsUiState(
    val isLoading: Boolean = true,
    val smartAlbums: List<Album> = emptyList(),
    val myAlbums: List<Album> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class AlbumsViewModel @Inject constructor(
    private val getAlbums: GetAlbumsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlbumsUiState())
    val uiState: StateFlow<AlbumsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getAlbums()
                .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
                .collect { albums ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            smartAlbums = albums.filter { a -> a.type == AlbumType.SMART },
                            myAlbums = albums.filter { a -> a.type == AlbumType.CUSTOM }
                        )
                    }
                }
        }
    }
}
