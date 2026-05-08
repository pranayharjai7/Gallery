package com.gallery.domain.model

object SmartAlbumFilter {
    fun matches(item: MediaItem, albumId: String): Boolean = when (albumId) {
        "smart_camera"      -> item.bucketName.contains("Camera",      ignoreCase = true)
        "smart_screenshots" -> item.bucketName.contains("Screenshots", ignoreCase = true)
        "smart_downloads"   -> item.bucketName.contains("Download",    ignoreCase = true)
        "smart_videos"      -> item.mimeType.startsWith("video/")
        "smart_slowmo"      -> item.bucketName.contains("slow",        ignoreCase = true)
        "smart_whatsapp"    -> item.bucketName.contains("WhatsApp",    ignoreCase = true)
        else -> false
    }

    fun nameFor(albumId: String): String = when (albumId) {
        "smart_camera"      -> "Camera"
        "smart_screenshots" -> "Screenshots"
        "smart_downloads"   -> "Downloads"
        "smart_videos"      -> "Videos"
        "smart_slowmo"      -> "Slow Motion"
        "smart_whatsapp"    -> "WhatsApp"
        else -> albumId
    }
}
