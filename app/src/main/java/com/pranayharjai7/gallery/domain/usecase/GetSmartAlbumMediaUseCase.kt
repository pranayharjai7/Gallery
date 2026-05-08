package com.pranayharjai7.gallery.domain.usecase

import com.pranayharjai7.gallery.domain.model.MediaItem
import com.pranayharjai7.gallery.domain.model.SmartAlbumFilter
import com.pranayharjai7.gallery.domain.repository.HiddenRepository
import com.pranayharjai7.gallery.domain.repository.MediaRepository
import com.pranayharjai7.gallery.domain.repository.TrashRepository
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
