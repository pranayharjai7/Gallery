package com.gallery.domain.model

import android.net.Uri

data class MediaItem(
    val id: Long,
    val uri: Uri,
    val name: String,
    val dateTaken: Long,
    val size: Long,
    val width: Int,
    val height: Int,
    val duration: Long?,
    val mimeType: String,
    val bucketId: Long,
    val bucketName: String,
    val location: Pair<Double, Double>?
)

val MediaItem.isVideo get() = mimeType.startsWith("video/")
