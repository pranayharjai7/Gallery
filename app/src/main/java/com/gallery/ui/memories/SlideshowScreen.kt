package com.gallery.ui.memories

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.gallery.ui.photos.PhotosViewModel
import kotlinx.coroutines.delay

private const val SLIDESHOW_INTERVAL_MS = 4_000L

@Composable
fun SlideshowScreen(
    onBack: () -> Unit,
    viewModel: PhotosViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val items = uiState.memoriesItems

    if (items.isEmpty()) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    var currentIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(SLIDESHOW_INTERVAL_MS)
            currentIndex = (currentIndex + 1) % items.size
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AnimatedContent(
            targetState = currentIndex,
            transitionSpec = {
                fadeIn(animationSpec = tween(durationMillis = 800)) togetherWith
                        fadeOut(animationSpec = tween(durationMillis = 800))
            },
            label = "slideshow_transition"
        ) { idx ->
            AsyncImage(
                model = items[idx].uri,
                contentDescription = items[idx].name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }

        IconButton(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp),
            onClick = onBack
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }

        Text(
            text = "${currentIndex + 1} / ${items.size}",
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            color = Color.White,
            style = MaterialTheme.typography.labelLarge
        )
    }
}
