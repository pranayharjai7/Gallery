package com.gallery.domain.usecase

import com.gallery.domain.model.MediaItem
import com.gallery.domain.model.SmartAlbumFilter
import com.gallery.domain.repository.HiddenRepository
import com.gallery.domain.repository.MediaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class GetSmartAlbumMediaUseCase @Inject constructor(
    private val mediaRepo: MediaRepository,
    private val hiddenRepo: HiddenRepository
) {
    operator fun invoke(albumId: String): Flow<List<MediaItem>> =
        hiddenRepo.observeIds().combine(mediaRepo.observeAll()) { hidden, items ->
            items.filter { it.id !in hidden && SmartAlbumFilter.matches(it, albumId) }
        }
}
