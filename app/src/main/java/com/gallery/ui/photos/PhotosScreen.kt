package com.gallery.ui.photos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gallery.ui.common.EmptyState
import com.gallery.ui.common.LoadingState
import com.gallery.ui.common.MediaThumbnail
import com.gallery.ui.common.SelectionActionBar
import com.gallery.ui.common.shareMedia
import com.gallery.ui.memories.MemoriesCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotosScreen(
    onMediaClick: (Long) -> Unit,
    onNavigateToEditor: (Long, Boolean) -> Unit,
    onNavigateToSlideshow: () -> Unit = {},
    viewModel: PhotosViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isSelecting = uiState.selectedIds.isNotEmpty()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            if (isSelecting) {
                SelectionActionBar(
                    selectedCount = uiState.selectedIds.size,
                    onSelectAll = {
                        viewModel.selectAll(
                            uiState.groupedMedia.values.flatten().map { it.item.id }
                        )
                    },
                    onShare = {
                        val items = uiState.groupedMedia.values.flatten()
                            .filter { it.item.id in uiState.selectedIds }
                            .map { it.item }
                        context.shareMedia(items)
                        viewModel.clearSelection()
                    },
                    onMoveToHidden = { viewModel.addSelectedToHidden() },
                    onDelete = { viewModel.deleteSelected() },
                    onClear = { viewModel.clearSelection() }
                )
            } else {
                TopAppBar(title = { Text("Photos") })
            }
        }
    ) { padding ->
        when {
            uiState.isLoading -> LoadingState(modifier = Modifier.padding(padding))
            uiState.groupedMedia.isEmpty() -> EmptyState(
                title = "No photos yet",
                message = "Photos from your device will appear here",
                modifier = Modifier.padding(padding)
            )
            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(4.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                if (uiState.memoriesItems.isNotEmpty() && !uiState.memoriesDismissed) {
                    item(span = { GridItemSpan(3) }) {
                        MemoriesCard(
                            items = uiState.memoriesItems,
                            onDismiss = { viewModel.dismissMemories() },
                            onTap = onNavigateToSlideshow
                        )
                    }
                }

                uiState.groupedMedia.forEach { (dateLabel, items) ->
                    item(span = { GridItemSpan(3) }) {
                        Text(
                            text = dateLabel,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    items(
                        items = items,
                        key = { it.item.id },
                        span = { indexed -> if (indexed.isFeatured) GridItemSpan(2) else GridItemSpan(1) }
                    ) { indexed ->
                        MediaThumbnail(
                            item = indexed.item,
                            isSelected = indexed.item.id in uiState.selectedIds,
                            modifier = Modifier.aspectRatio(1f),
                            onClick = {
                                if (isSelecting) viewModel.toggleSelection(indexed.item.id)
                                else onMediaClick(indexed.item.id)
                            },
                            onLongClick = { viewModel.toggleSelection(indexed.item.id) }
                        )
                    }
                }
            }
        }
    }
}
