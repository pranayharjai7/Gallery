package com.gallery.ui.common

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun DateHeader(dateLabel: String, modifier: Modifier = Modifier) {
    Text(
        text = dateLabel,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(horizontal = 12.dp, vertical = 8.dp)
    )
}

fun formatDateHeader(timestamp: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
    val today = Calendar.getInstance()
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    return when {
        cal.isSameDay(today) -> "Today"
        cal.isSameDay(yesterday) -> "Yesterday"
        cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) ->
            SimpleDateFormat("MMMM d", Locale.getDefault()).format(Date(timestamp))
        else -> SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}

private fun Calendar.isSameDay(other: Calendar) =
    get(Calendar.YEAR) == other.get(Calendar.YEAR) && get(Calendar.DAY_OF_YEAR) == other.get(Calendar.DAY_OF_YEAR)
