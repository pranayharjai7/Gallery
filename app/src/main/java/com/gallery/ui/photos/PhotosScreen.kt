package com.gallery.ui.photos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.item
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gallery.domain.model.MediaItem
import com.gallery.ui.common.EmptyState
import com.gallery.ui.common.LoadingState
import com.gallery.ui.common.MediaThumbnail
import com.gallery.ui.common.SelectionActionBar

@Composable
fun MemoriesCard(
    items: List<MediaItem>,
    onDismiss: () -> Unit,
    onTap: () -> Unit
) {
    Card(
        onClick = onTap,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        androidx.compose.foundation.layout.Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("On This Day", style = MaterialTheme.typography.titleSmall)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Dismiss memories")
                }
            }
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                items(items.take(5)) { memItem ->
                    MediaThumbnail(
                        item = memItem,
                        isSelected = false,
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        onClick = onTap,
                        onLongClick = {}
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotosScreen(
    onMediaClick: (Long) -> Unit,
    onNavigateToEditor: (Long, Boolean) -> Unit,
    viewModel: PhotosViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isSelecting = uiState.selectedIds.isNotEmpty()

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
                    onShare = {},
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
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (uiState.memoriesItems.isNotEmpty()) {
                    item(span = { GridItemSpan(3) }) {
                        MemoriesCard(
                            items = uiState.memoriesItems,
                            onDismiss = {},
                            onTap = {}
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
                            modifier = Modifier.aspectRatio(if (indexed.isFeatured) 2f else 1f),
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
