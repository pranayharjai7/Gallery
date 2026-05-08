package com.pranayharjai7.gallery.domain.usecase

import com.pranayharjai7.gallery.domain.repository.FavoritesRepository
import javax.inject.Inject

class ToggleFavoriteUseCase @Inject constructor(
    private val favRepo: FavoritesRepository
) {
    suspend operator fun invoke(mediaId: Long) = favRepo.toggle(mediaId)
}
