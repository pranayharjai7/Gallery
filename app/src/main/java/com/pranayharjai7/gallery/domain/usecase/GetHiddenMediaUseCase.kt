package com.pranayharjai7.gallery.domain.usecase

import com.pranayharjai7.gallery.domain.model.MediaItem
import com.pranayharjai7.gallery.domain.repository.HiddenRepository
import com.pranayharjai7.gallery.domain.repository.MediaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class GetHiddenMediaUseCase @Inject constructor(
    private val mediaRepo: MediaRepository,
    private val hiddenRepo: HiddenRepository
) {
    operator fun invoke(): Flow<List<MediaItem>> =
        hiddenRepo.observeIds().combine(mediaRepo.observeAll()) { ids, all ->
            all.filter { it.id in ids }
        }
}
