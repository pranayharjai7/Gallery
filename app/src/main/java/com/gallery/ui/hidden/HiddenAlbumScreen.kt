package com.gallery.ui.hidden

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.gallery.ui.common.EmptyState
import com.gallery.ui.common.MediaThumbnail

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HiddenAlbumScreen(
    onBack: () -> Unit,
    viewModel: HiddenAlbumViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Trigger biometric auth when screen first loads (while still locked)
    LaunchedEffect(uiState.authState) {
        if (uiState.authState is HiddenAuthState.Locked) {
            val activity = context as? FragmentActivity ?: return@LaunchedEffect
            val helper = BiometricHelper(activity)
            when (val result = helper.authenticate()) {
                is BiometricHelper.AuthResult.Success -> viewModel.onAuthSuccess()
                is BiometricHelper.AuthResult.Error -> viewModel.onAuthError(result.message)
                is BiometricHelper.AuthResult.NotEnrolled ->
                    viewModel.onAuthError("No biometric or screen lock enrolled")
                is BiometricHelper.AuthResult.HardwareUnavailable ->
                    viewModel.onHardwareUnavailable()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val selCount = uiState.selectedIds.size
                    if (selCount > 0) Text("$selCount selected") else Text("Hidden Album")
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.selectedIds.isNotEmpty()) viewModel.clearSelection()
                        else onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.selectedIds.isNotEmpty()) {
                        IconButton(onClick = { viewModel.unhideSelected() }) {
                            Icon(Icons.Default.Delete, contentDescription = "Unhide selected")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val authState = uiState.authState) {
                is HiddenAuthState.Locked -> {
                    // Show locking indicator while waiting for biometric prompt
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Text("Authenticating...", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                is HiddenAuthState.Error -> {
                    EmptyState(
                        icon = Icons.Default.Lock,
                        title = "Authentication Failed",
                        message = authState.message,
                        action = "Try Again" to { viewModel.retryAuth() }
                    )
                }

                is HiddenAuthState.HardwareUnavailable -> {
                    EmptyState(
                        icon = Icons.Default.Lock,
                        title = "Biometric Unavailable",
                        message = "Your device does not support biometric authentication or it is not set up."
                    )
                }

                is HiddenAuthState.Unlocked -> {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    } else if (uiState.items.isEmpty()) {
                        EmptyState(
                            icon = Icons.Default.Lock,
                            title = "No Hidden Media",
                            message = "Photos and videos you hide will appear here."
                        )
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(uiState.items, key = { it.id }) { item ->
                                MediaThumbnail(
                                    item = item,
                                    modifier = Modifier.aspectRatio(1f),
                                    isSelected = item.id in uiState.selectedIds,
                                    onClick = { viewModel.toggleSelection(item.id) },
                                    onLongClick = { viewModel.toggleSelection(item.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
