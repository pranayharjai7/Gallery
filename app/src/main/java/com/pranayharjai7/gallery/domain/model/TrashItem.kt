package com.pranayharjai7.gallery.domain.model

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
private const val TRASH_RETENTION_DAYS = 30
private const val MS_PER_DAY = 86_400_000L

val TrashItem.daysUntilPurge: Int
    get() = TRASH_RETENTION_DAYS - ((System.currentTimeMillis() - deletedAt) / MS_PER_DAY).toInt()
