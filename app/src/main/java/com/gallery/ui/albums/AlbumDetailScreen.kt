package com.gallery.ui.albums

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gallery.ui.common.EmptyState
import com.gallery.ui.common.LoadingState
import com.gallery.ui.common.MediaThumbnail
import com.gallery.ui.common.SelectionActionBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    albumId: String,
    onBack: () -> Unit,
    onMediaClick: (Long) -> Unit,
    viewModel: AlbumDetailViewModel = hiltViewModel()
) {
    LaunchedEffect(albumId) { viewModel.loadAlbum(albumId) }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isSelecting = uiState.selectedIds.isNotEmpty()

    Scaffold(
        topBar = {
            if (isSelecting) {
                SelectionActionBar(
                    selectedCount = uiState.selectedIds.size,
                    onSelectAll = { viewModel.selectAll(uiState.items.map { it.id }) },
                    onShare = { },
                    onDelete = { viewModel.deleteSelected() },
                    onClear = { viewModel.clearSelection() }
                )
            } else {
                TopAppBar(
                    title = { Text(uiState.albumName) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                )
            }
        }
    ) { padding ->
        when {
            uiState.isLoading -> LoadingState(Modifier.padding(padding))
            uiState.items.isEmpty() -> EmptyState(
                title = "No media in this album",
                message = "Media saved to this album will appear here",
                modifier = Modifier.padding(padding)
            )
            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(uiState.items, key = { it.id }) { item ->
                    MediaThumbnail(
                        item = item,
                        modifier = Modifier.aspectRatio(1f),
                        isSelected = item.id in uiState.selectedIds,
                        onClick = {
                            if (isSelecting) viewModel.toggleSelection(item.id)
                            else onMediaClick(item.id)
                        },
                        onLongClick = { viewModel.toggleSelection(item.id) }
                    )
                }
            }
        }
    }
}
