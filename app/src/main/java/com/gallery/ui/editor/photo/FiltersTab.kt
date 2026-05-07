package com.gallery.ui.editor.photo

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

val FILTER_NAMES = listOf("Original", "Vivid", "Cool", "Warm", "Noir", "Fade", "Chrome")

fun getFilterMatrix(index: Int): ColorMatrix = when (index) {
    0 -> ColorMatrix()
    1 -> ColorMatrix().apply { setSaturation(1.5f) }
    2 -> ColorMatrix(
        floatArrayOf(
            0.8f, 0f, 0.2f, 0f, 0f,
            0f, 0.8f, 0.2f, 0f, 0f,
            0f, 0f, 1.4f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    )
    3 -> ColorMatrix(
        floatArrayOf(
            1.2f, 0.1f, 0f, 0f, 0f,
            0.1f, 1f, 0f, 0f, 0f,
            0f, 0f, 0.8f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    )
    4 -> ColorMatrix().apply { setSaturation(0f) }
    5 -> ColorMatrix(
        floatArrayOf(
            1f, 0f, 0f, 0f, 20f,
            0f, 1f, 0f, 0f, 20f,
            0f, 0f, 1f, 0f, 20f,
            0f, 0f, 0f, 1f, 0f
        )
    )
    else -> ColorMatrix()
}

@Composable
fun FiltersTab(
    selectedIndex: Int,
    sourceBitmap: Bitmap?,
    onSelectFilter: (Int) -> Unit
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
                val filteredBitmap = remember(selectedIndex, sourceBitmap) {
                    val p = Paint().apply {
                        colorFilter = ColorMatrixColorFilter(getFilterMatrix(selectedIndex))
                    }
                    val dst = Bitmap.createBitmap(
                        sourceBitmap.width, sourceBitmap.height,
                        sourceBitmap.config ?: Bitmap.Config.ARGB_8888
                    )
                    Canvas(dst).drawBitmap(sourceBitmap, 0f, 0f, p)
                    dst
                }
                Image(
                    bitmap = filteredBitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.Fit
                )
            }
        }

        LazyRow(
            modifier = Modifier
                .height(100.dp)
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(FILTER_NAMES.size) { index ->
                Column(
                    modifier = Modifier.clickable { onSelectFilter(index) },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                width = if (index == selectedIndex) 2.dp else 0.dp,
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(8.dp)
                            )
                    ) {
                        if (sourceBitmap != null) {
                            val thumb = remember(index, sourceBitmap) {
                                val scale = 72
                                val scaled = Bitmap.createScaledBitmap(sourceBitmap, scale, scale, true)
                                val p = Paint().apply {
                                    colorFilter = ColorMatrixColorFilter(getFilterMatrix(index))
                                }
                                val dst = Bitmap.createBitmap(scale, scale, Bitmap.Config.ARGB_8888)
                                Canvas(dst).drawBitmap(scaled, 0f, 0f, p)
                                dst
                            }
                            Image(
                                bitmap = thumb.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                    Text(
                        FILTER_NAMES[index],
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
