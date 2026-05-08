package com.gallery.domain.usecase

import com.gallery.domain.model.MediaItem
import com.gallery.domain.model.SmartAlbumFilter
import com.gallery.domain.repository.HiddenRepository
import com.gallery.domain.repository.MediaRepository
import com.gallery.domain.repository.TrashRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class GetSmartAlbumMediaUseCase @Inject constructor(
    private val mediaRepo: MediaRepository,
    private val hiddenRepo: HiddenRepository,
    private val trashRepo: TrashRepository
) {
    operator fun invoke(albumId: String): Flow<List<MediaItem>> =
        combine(mediaRepo.observeAll(), hiddenRepo.observeIds(), trashRepo.observeAll()) { items, hidden, trashed ->
            val trashedIds = trashed.map { it.id }.toSet()
            items.filter { it.id !in hidden && it.id !in trashedIds && SmartAlbumFilter.matches(it, albumId) }
        }
}
