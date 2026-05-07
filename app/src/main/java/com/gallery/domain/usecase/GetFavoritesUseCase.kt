package com.gallery.domain.usecase

import com.gallery.domain.model.MediaItem
import com.gallery.domain.repository.FavoritesRepository
import com.gallery.domain.repository.MediaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class GetFavoritesUseCase @Inject constructor(
    private val mediaRepo: MediaRepository,
    private val favRepo: FavoritesRepository
) {
    operator fun invoke(): Flow<List<MediaItem>> =
        favRepo.observeIds().combine(mediaRepo.observeAll()) { ids, all ->
            all.filter { it.id in ids }
        }
}
