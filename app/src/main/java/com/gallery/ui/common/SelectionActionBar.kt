package com.gallery.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionActionBar(
    selectedCount: Int,
    onSelectAll: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = { Text("$selectedCount selected") },
        navigationIcon = { IconButton(onClick = onClear) { Icon(Icons.Default.Close, "Clear selection") } },
        actions = {
            IconButton(onClick = onSelectAll) { Icon(Icons.Default.Check, "Select all") }
            IconButton(onClick = onShare) { Icon(Icons.Default.Share, "Share") }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Delete") }
        },
        modifier = modifier,
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    )
}
