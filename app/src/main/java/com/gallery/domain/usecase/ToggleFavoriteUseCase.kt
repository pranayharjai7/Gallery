package com.gallery.domain.usecase

import com.gallery.domain.repository.FavoritesRepository
import javax.inject.Inject

class ToggleFavoriteUseCase @Inject constructor(
    private val favRepo: FavoritesRepository
) {
    suspend operator fun invoke(mediaId: Long) = favRepo.toggle(mediaId)
}
