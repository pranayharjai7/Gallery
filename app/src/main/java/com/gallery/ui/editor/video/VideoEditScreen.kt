package com.gallery.ui.editor.video

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
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
import com.gallery.ui.common.LoadingState
import com.gallery.ui.viewer.VideoPlayer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoEditScreen(
    mediaId: Long,
    onBack: () -> Unit,
    viewModel: VideoEditViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Video") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
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
                        TextButton(
                            onClick = { viewModel.saveEdit() },
                            enabled = !uiState.isLoading
                        ) {
                            Text("Save")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            val uri = uiState.videoUri
            if (uri != null) {
                VideoPlayer(
                    uri = uri,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
            } else {
                LoadingState(modifier = Modifier.weight(1f))
            }

            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    text = "Trim: ${uiState.trimStartMs.toTimeString()} – ${uiState.trimEndMs.toTimeString()}",
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(Modifier.height(4.dp))

                Text("Start", style = MaterialTheme.typography.labelSmall)
                Slider(
                    value = uiState.trimStartMs.toFloat(),
                    onValueChange = { viewModel.setTrimStart(it.toLong()) },
                    valueRange = 0f..uiState.durationMs.toFloat(),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("End", style = MaterialTheme.typography.labelSmall)
                Slider(
                    value = uiState.trimEndMs.toFloat(),
                    onValueChange = { viewModel.setTrimEnd(it.toLong()) },
                    valueRange = 0f..uiState.durationMs.toFloat(),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButton(onClick = { viewModel.toggleMute() }) {
                    Icon(
                        imageVector = if (uiState.isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                        contentDescription = if (uiState.isMuted) "Unmute" else "Mute"
                    )
                }
                IconButton(onClick = { viewModel.rotateCW() }) {
                    Icon(Icons.Default.RotateRight, contentDescription = "Rotate 90°")
                }
            }
        }
    }
}

private fun Long.toTimeString(): String {
    val totalSeconds = this / 1000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
