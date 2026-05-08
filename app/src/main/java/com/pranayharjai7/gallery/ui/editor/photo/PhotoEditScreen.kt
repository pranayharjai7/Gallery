package com.pranayharjai7.gallery.ui.editor.photo

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pranayharjai7.gallery.ui.common.LoadingState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoEditScreen(
    mediaId: Long,
    onBack: () -> Unit,
    viewModel: PhotoEditViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Photo") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "Close editor")
                    }
                },
                actions = {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .size(24.dp)
                        )
                    } else {
                        TextButton(onClick = { viewModel.saveEdit() }) {
                            Text("Save")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            LoadingState(modifier = Modifier.padding(padding))
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            TabRow(selectedTabIndex = uiState.activeTab.ordinal) {
                EditorTab.entries.forEach { tab ->
                    Tab(
                        selected = uiState.activeTab == tab,
                        onClick = { viewModel.setActiveTab(tab) },
                        text = {
                            Text(
                                tab.name.lowercase().replaceFirstChar { it.uppercase() }
                            )
                        }
                    )
                }
            }

            when (uiState.activeTab) {
                EditorTab.CROP -> CropTab(
                    cropState = uiState.cropState,
                    sourceBitmap = uiState.sourceBitmap,
                    onRotateCW = viewModel::rotateCW,
                    onFlipH = viewModel::flipH,
                    onFlipV = viewModel::flipV,
                    onAspectRatio = viewModel::setAspectRatio
                )
                EditorTab.ADJUST -> AdjustTab(
                    values = uiState.adjustValues,
                    onBrightness = viewModel::setBrightness,
                    onContrast = viewModel::setContrast,
                    onSaturation = viewModel::setSaturation,
                    onWarmth = viewModel::setWarmth,
                    onExposure = viewModel::setExposure
                )
                EditorTab.FILTERS -> FiltersTab(
                    selectedIndex = uiState.selectedFilterIndex,
                    sourceBitmap = uiState.sourceBitmap,
                    onSelectFilter = viewModel::selectFilter
                )
                EditorTab.DRAW -> DrawTab(
                    sourceBitmap = uiState.sourceBitmap,
                    drawState = uiState.drawState,
                    onDrawPath = viewModel::addDrawPath,
                    onColorChange = viewModel::setDrawColor,
                    onBrushSizeChange = viewModel::setBrushSize,
                    onUndo = viewModel::undoDrawPath
                )
            }
        }
    }
}
