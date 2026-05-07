package com.gallery.data.mediastore

import com.gallery.domain.model.Album
import com.gallery.domain.model.AlbumType
import com.gallery.domain.model.MediaItem
import com.gallery.domain.repository.AlbumRepository
import com.gallery.domain.repository.MediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private data class SmartAlbumSpec(
    val id: String,
    val name: String,
    val matches: (MediaItem) -> Boolean,
)

private val SMART_ALBUM_SPECS = listOf(
    SmartAlbumSpec("smart_camera",      "Camera")      { it.bucketName.contains("Camera",      ignoreCase = true) },
    SmartAlbumSpec("smart_screenshots", "Screenshots") { it.bucketName.contains("Screenshots", ignoreCase = true) },
    SmartAlbumSpec("smart_downloads",   "Downloads")   { it.bucketName.contains("Download",    ignoreCase = true) },
    SmartAlbumSpec("smart_videos",      "Videos")      { it.mimeType.startsWith("video/") },
    SmartAlbumSpec("smart_slowmo",      "Slow Motion") { it.bucketName.contains("slow",        ignoreCase = true) },
    SmartAlbumSpec("smart_whatsapp",    "WhatsApp")    { it.bucketName.contains("WhatsApp",    ignoreCase = true) },
)

@Singleton
class MediaStoreAlbumRepository @Inject constructor(
    private val mediaRepository: MediaRepository,
) : AlbumRepository {

    override fun observeAlbums(hiddenIds: Set<Long>): Flow<List<Album>> =
        mediaRepository.observeAll()
            .map { items -> buildAlbums(items, hiddenIds) }
            .flowOn(Dispatchers.Default)

    private fun buildAlbums(
        allItems: List<MediaItem>,
        hiddenIds: Set<Long>,
    ): List<Album> {
        val visibleItems = allItems.filter { it.id !in hiddenIds }

        val smartAlbums = SMART_ALBUM_SPECS.mapNotNull { spec ->
            val matching = visibleItems.filter(spec.matches)
            if (matching.isEmpty()) return@mapNotNull null
            Album(
                id       = spec.id,
                name     = spec.name,
                coverUri = matching.first().uri,
                count    = matching.size,
                type     = AlbumType.SMART,
            )
        }

        val customAlbums = visibleItems
            .groupBy { it.bucketId }
            .mapNotNull { (_, items) ->
                if (items.isEmpty()) return@mapNotNull null
                Album(
                    id       = items.first().bucketId.toString(),
                    name     = items.first().bucketName,
                    coverUri = items.first().uri,
                    count    = items.size,
                    type     = AlbumType.CUSTOM,
                )
            }
            .sortedBy { it.name }

        return smartAlbums + customAlbums
    }
}
