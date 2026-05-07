package com.gallery.ui.editor.photo

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

@Composable
fun CropTab(
    cropState: CropState,
    sourceBitmap: Bitmap?,
    onRotateCW: () -> Unit,
    onFlipH: () -> Unit,
    onFlipV: () -> Unit,
    onAspectRatio: (AspectRatio) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (sourceBitmap != null) {
                val matrix = remember(cropState) {
                    android.graphics.Matrix().apply {
                        postRotate(
                            cropState.rotation.toFloat(),
                            sourceBitmap.width / 2f,
                            sourceBitmap.height / 2f
                        )
                        if (cropState.flipHorizontal)
                            postScale(-1f, 1f, sourceBitmap.width / 2f, sourceBitmap.height / 2f)
                        if (cropState.flipVertical)
                            postScale(1f, -1f, sourceBitmap.width / 2f, sourceBitmap.height / 2f)
                    }
                }
                val displayBitmap = remember(cropState, sourceBitmap) {
                    Bitmap.createBitmap(
                        sourceBitmap, 0, 0,
                        sourceBitmap.width, sourceBitmap.height,
                        matrix, true
                    )
                }
                Image(
                    bitmap = displayBitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.Fit
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(onClick = onRotateCW) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.RotateRight, contentDescription = "Rotate CW")
                    Text("Rotate", style = MaterialTheme.typography.labelSmall)
                }
            }
            IconButton(onClick = onFlipH) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Flip, contentDescription = "Flip Horizontal")
                    Text("Flip H", style = MaterialTheme.typography.labelSmall)
                }
            }
            IconButton(onClick = onFlipV) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Flip, contentDescription = "Flip Vertical")
                    Text("Flip V", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        LazyRow(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(AspectRatio.entries.toList()) { ratio ->
                FilterChip(
                    selected = cropState.aspectRatio == ratio,
                    onClick = { onAspectRatio(ratio) },
                    label = { Text(ratio.label) }
                )
            }
        }
    }
}
