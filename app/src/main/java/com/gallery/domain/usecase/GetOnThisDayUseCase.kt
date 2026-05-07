package com.gallery.domain.usecase

import com.gallery.domain.model.MediaItem
import com.gallery.domain.repository.MediaRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

// monthDay is encoded as (month * 100 + day), e.g. May 7 = 507, December 25 = 1225
class GetOnThisDayUseCase @Inject constructor(
    private val mediaRepo: MediaRepository
) {
    operator fun invoke(monthDay: Int): Flow<List<MediaItem>> =
        mediaRepo.getOnThisDay(monthDay)
}
