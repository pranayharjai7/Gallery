package com.gallery.domain.model

import android.net.Uri

data class TrashItem(
    val id: Long,
    val originalUri: Uri,
    val name: String,
    val dateTaken: Long,
    val mimeType: String,
    val deletedAt: Long,
    val bucketId: Long
)

val TrashItem.isVideo get() = mimeType.startsWith("video/")
val TrashItem.daysUntilPurge get() = 30 - ((System.currentTimeMillis() - deletedAt) / 86400000).toInt()
