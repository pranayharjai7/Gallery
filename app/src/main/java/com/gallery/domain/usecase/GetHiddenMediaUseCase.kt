package com.gallery.domain.usecase

import com.gallery.domain.model.MediaItem
import com.gallery.domain.repository.HiddenRepository
import com.gallery.domain.repository.MediaRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetHiddenMediaUseCase @Inject constructor(
    private val mediaRepo: MediaRepository,
    private val hiddenRepo: HiddenRepository
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<List<MediaItem>> =
        hiddenRepo.observeIds().flatMapLatest { ids ->
            mediaRepo.observeAll().map { all -> all.filter { it.id in ids } }
        }
}
