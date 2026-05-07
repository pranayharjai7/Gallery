package com.gallery.domain.usecase

import com.gallery.domain.model.MediaItem
import com.gallery.domain.repository.MediaRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SearchMediaUseCase @Inject constructor(
    private val mediaRepo: MediaRepository
) {
    operator fun invoke(query: String): Flow<List<MediaItem>> = mediaRepo.search(query)
}
