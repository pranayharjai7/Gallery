package com.gallery.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gallery.data.prefs.GridSize
import com.gallery.data.prefs.ThemeMode
import com.gallery.ui.common.LoadingState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showGridDialog by remember { mutableStateOf(false) }
    var showIntervalDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            LoadingState(Modifier.padding(padding))
            return@Scaffold
        }

        LazyColumn(modifier = Modifier.padding(padding)) {
            item {
                Text(
                    text = "Appearance",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp)
                )
            }

            item {
                ListItem(
                    headlineContent = { Text("Theme") },
                    trailingContent = {
                        SingleChoiceSegmentedButtonRow {
                            ThemeMode.entries.forEachIndexed { index, mode ->
                                SegmentedButton(
                                    selected = uiState.prefs.themeMode == mode,
                                    onClick = { viewModel.setThemeMode(mode) },
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = ThemeMode.entries.size
                                    ),
                                    label = {
                                        Text(
                                            text = when (mode) {
                                                ThemeMode.LIGHT -> "Light"
                                                ThemeMode.DARK -> "Dark"
                                                ThemeMode.SYSTEM -> "Auto"
                                            },
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                )
                            }
                        }
                    }
                )
            }

            item {
                ListItem(
                    headlineContent = { Text("Dynamic Color") },
                    supportingContent = { Text("Use wallpaper colors for the theme") },
                    trailingContent = {
                        Switch(
                            checked = uiState.prefs.dynamicColor,
                            onCheckedChange = { viewModel.setDynamicColor(it) }
                        )
                    }
                )
            }

            item {
                Text(
                    text = "Media",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp)
                )
            }

            item {
                ListItem(
                    headlineContent = { Text("Grid Size") },
                    supportingContent = {
                        Text(uiState.prefs.gridSize.name.lowercase().replaceFirstChar { it.uppercase() })
                    },
                    modifier = Modifier.clickable { showGridDialog = true }
                )
            }

            item {
                ListItem(
                    headlineContent = { Text("Slideshow Interval") },
                    supportingContent = { Text("${uiState.prefs.slideshowInterval} seconds") },
                    modifier = Modifier.clickable { showIntervalDialog = true }
                )
            }
        }
    }

    if (showGridDialog) {
        var selected by remember { mutableStateOf(uiState.prefs.gridSize) }
        AlertDialog(
            onDismissRequest = { showGridDialog = false },
            title = { Text("Grid Size") },
            text = {
                Column {
                    GridSize.entries.forEach { size ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selected = size }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RadioButton(selected = selected == size, onClick = { selected = size })
                            Text(
                                text = "${size.name.lowercase().replaceFirstChar { it.uppercase() }} (${size.columns} columns)",
                                modifier = Modifier.align(Alignment.CenterVertically)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.setGridSize(selected); showGridDialog = false }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showGridDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showIntervalDialog) {
        val options = listOf(2, 3, 4, 5, 8, 10)
        var selected by remember { mutableStateOf(uiState.prefs.slideshowInterval) }
        AlertDialog(
            onDismissRequest = { showIntervalDialog = false },
            title = { Text("Slideshow Interval") },
            text = {
                Column {
                    options.forEach { seconds ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selected = seconds }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RadioButton(selected = selected == seconds, onClick = { selected = seconds })
                            Text(
                                text = "$seconds seconds",
                                modifier = Modifier.align(Alignment.CenterVertically)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.setSlideshowInterval(selected); showIntervalDialog = false }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showIntervalDialog = false }) { Text("Cancel") }
            }
        )
    }
}
