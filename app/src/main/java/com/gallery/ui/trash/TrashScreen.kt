package com.gallery.ui.trash

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.gallery.domain.model.TrashItem
import com.gallery.domain.model.daysUntilPurge
import com.gallery.ui.common.EmptyState
import com.gallery.ui.common.LoadingState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TrashScreen(
    onBack: () -> Unit,
    viewModel: TrashViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isSelecting = uiState.selectedIds.isNotEmpty()
    var showEmptyTrashDialog by remember { mutableStateOf(false) }

    if (showEmptyTrashDialog) {
        AlertDialog(
            onDismissRequest = { showEmptyTrashDialog = false },
            title = { Text("Empty Trash") },
            text = { Text("Permanently delete all ${uiState.items.size} items from trash? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.emptyTrash()
                    showEmptyTrashDialog = false
                }) {
                    Text("Delete All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyTrashDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            if (isSelecting) {
                TopAppBar(
                    title = { Text("${uiState.selectedIds.size} selected") },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear selection")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.restoreSelected() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Restore selected")
                        }
                        IconButton(onClick = { viewModel.purgeSelected() }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete selected permanently")
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = { Text("Trash") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        if (uiState.items.isNotEmpty()) {
                            IconButton(onClick = { showEmptyTrashDialog = true }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Empty trash")
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> LoadingState(modifier = Modifier.padding(innerPadding))
            uiState.items.isEmpty() -> EmptyState(
                title = "Trash",
                message = "Trash is empty",
                modifier = Modifier.padding(innerPadding)
            )
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(uiState.items, key = { it.id }) { item ->
                        TrashItemThumbnail(
                            item = item,
                            isSelected = item.id in uiState.selectedIds,
                            isSelecting = isSelecting,
                            onToggleSelection = { viewModel.toggleSelection(item.id) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TrashItemThumbnail(
    item: TrashItem,
    isSelected: Boolean,
    isSelecting: Boolean,
    onToggleSelection: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(4.dp))
            .combinedClickable(
                onClick = { if (isSelecting) onToggleSelection() },
                onLongClick = onToggleSelection
            )
    ) {
        AsyncImage(
            model = item.originalUri,
            contentDescription = item.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
            )
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
            )
        }

        val days = item.daysUntilPurge
        val badgeColor = when {
            days <= 3 -> Color(0xFFB00020)
            days <= 7 -> Color(0xFFE65100)
            else -> Color.Black.copy(alpha = 0.7f)
        }
        Text(
            text = "${days}d",
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(4.dp)
                .background(badgeColor, RoundedCornerShape(2.dp))
                .padding(horizontal = 4.dp, vertical = 2.dp),
            color = Color.White,
            style = MaterialTheme.typography.labelSmall
        )
    }
}
