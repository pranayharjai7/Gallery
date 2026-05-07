package com.gallery.ui.common

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.request.error
import androidx.compose.ui.platform.LocalContext
import com.gallery.R
import com.gallery.domain.model.MediaItem
import com.gallery.domain.model.isVideo

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaThumbnail(
    item: MediaItem,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(2.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(item.uri)
                .crossfade(true)
                .error(R.drawable.ic_broken_image)
                .build(),
            contentDescription = item.name,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            onError = { state ->
                android.util.Log.w("MediaThumbnail", "Failed to load: ${item.uri} — ${state.result.throwable.message}")
            }
        )
        if (item.isVideo) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.2f)))
            Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.align(Alignment.Center).size(28.dp))
            item.duration?.takeIf { it > 0L }?.let { ms ->
                val seconds = ms / 1000
                val text = if (seconds >= 3600) "%d:%02d:%02d".format(seconds/3600, (seconds%3600)/60, seconds%60)
                           else "%d:%02d".format(seconds/60, seconds%60)
                Text(text, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp)
                        .background(Color.Black.copy(0.5f), RoundedCornerShape(3.dp)).padding(horizontal = 4.dp, vertical = 1.dp))
            }
        }
        if (isSelected) {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)))
            Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.TopStart).padding(4.dp).size(20.dp))
        }
    }
}
