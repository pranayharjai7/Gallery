package com.gallery.ui.common

import android.content.Context
import android.content.Intent
import com.gallery.domain.model.MediaItem

fun Context.shareMedia(items: List<MediaItem>) {
    if (items.isEmpty()) return
    if (items.size == 1) {
        val item = items.first()
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = item.mimeType
            putExtra(Intent.EXTRA_STREAM, item.uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Share"))
    } else {
        val commonMime = if (items.all { it.mimeType == items.first().mimeType }) {
            items.first().mimeType
        } else {
            "*/*"
        }
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = commonMime
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(items.map { it.uri }))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Share ${items.size} items"))
    }
}
