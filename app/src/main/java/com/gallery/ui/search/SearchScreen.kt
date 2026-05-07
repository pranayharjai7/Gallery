package com.gallery.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onMediaClick: (Long) -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isSelecting = uiState.selectedIds.isNotEmpty()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            if (isSelecting) {
                SelectionActionBar(
                    selectedCount = uiState.selectedIds.size,
                    onSelectAll = { viewModel.selectAll(uiState.results.map { it.id }) },
                    onShare = {
                        val items = uiState.results.filter { it.id in uiState.selectedIds }
                        context.shareMedia(items)
                        viewModel.clearSelection()
                    },
                    onMoveToHidden = { viewModel.addSelectedToHidden() },
                    onDelete = { viewModel.deleteSelected() },
                    onClear = { viewModel.clearSelection() }
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {
            // Search bar
            OutlinedTextField(
                value = uiState.query,
                onValueChange = viewModel::onQueryChange,
                placeholder = { Text("Search photos & videos") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            )

            // Filter chips
            Row(
                modifier = Modifier.padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MediaFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = uiState.filter == filter,
                        onClick = { viewModel.onFilterChange(filter) },
                        label = {
                            Text(filter.name.lowercase().replaceFirstChar { it.uppercase() })
                        }
                    )
                }
            }

            // Content area
            when {
                uiState.isLoading -> LoadingState()

                uiState.error != null -> EmptyState(
                    title = "Something went wrong",
                    message = uiState.error!!
                )

                uiState.query.isBlank() -> EmptyState(
                    title = "Search your photos and videos",
                    message = "Results will appear here"
                )

                uiState.results.isEmpty() -> EmptyState(
                    title = "No results",
                    message = "No results for \"${uiState.query}\""
                )

                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(uiState.results, key = { it.id }) { item ->
                        MediaThumbnail(
                            item = item,
                            isSelected = item.id in uiState.selectedIds,
                            modifier = Modifier.aspectRatio(1f),
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
}
