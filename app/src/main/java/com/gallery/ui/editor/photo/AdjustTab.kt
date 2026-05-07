package com.gallery.ui.editor.photo

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AdjustTab(
    values: AdjustValues,
    onBrightness: (Float) -> Unit,
    onContrast: (Float) -> Unit,
    onSaturation: (Float) -> Unit,
    onWarmth: (Float) -> Unit,
    onExposure: (Float) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item { AdjustSlider("Brightness", values.brightness, onBrightness) }
        item { AdjustSlider("Contrast", values.contrast, onContrast) }
        item { AdjustSlider("Saturation", values.saturation, onSaturation) }
        item { AdjustSlider("Warmth", values.warmth, onWarmth) }
        item { AdjustSlider("Exposure", values.exposure, onExposure) }
    }
}

@Composable
private fun AdjustSlider(
    label: String,
    value: Float,
    onChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row {
            Text(label, modifier = Modifier.weight(1f))
            Text("${value.toInt()}", style = MaterialTheme.typography.labelMedium)
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = -100f..100f,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
