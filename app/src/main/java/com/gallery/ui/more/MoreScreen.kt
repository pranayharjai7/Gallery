package com.gallery.ui.more

import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(onNavigate: (String) -> Unit) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("More") }) }
    ) { padding ->
        LazyColumn(contentPadding = padding) {
            item {
                ListItem(
                    headlineContent = { Text("Trash") },
                    leadingContent = { Icon(Icons.Filled.Delete, contentDescription = null) },
                    trailingContent = { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null) },
                    modifier = Modifier.clickable { onNavigate("trash") }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Hidden Album") },
                    leadingContent = { Icon(Icons.Filled.Lock, contentDescription = null) },
                    trailingContent = { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null) },
                    modifier = Modifier.clickable { onNavigate("hidden") }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Settings") },
                    leadingContent = { Icon(Icons.Filled.Settings, contentDescription = null) },
                    trailingContent = { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null) },
                    modifier = Modifier.clickable { onNavigate("settings") }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("About") },
                    leadingContent = { Icon(Icons.Filled.Info, contentDescription = null) },
                    supportingContent = { Text("Gallery v1.0") },
                    modifier = Modifier.clickable { /* no-op */ }
                )
            }
        }
    }
}
