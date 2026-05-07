package com.gallery.domain.usecase

import com.gallery.domain.model.MediaItem
import com.gallery.domain.repository.HiddenRepository
import com.gallery.domain.repository.MediaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class GetAlbumMediaUseCase @Inject constructor(
    private val mediaRepo: MediaRepository,
    private val hiddenRepo: HiddenRepository
) {
    operator fun invoke(bucketId: Long): Flow<List<MediaItem>> =
        hiddenRepo.observeIds().combine(mediaRepo.observeByBucket(bucketId)) { hidden, items ->
            items.filter { it.id !in hidden }
        }
}
