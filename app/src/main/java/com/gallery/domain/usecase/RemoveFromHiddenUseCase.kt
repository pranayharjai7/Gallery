package com.gallery.domain.usecase

import com.gallery.domain.repository.HiddenRepository
import javax.inject.Inject

class RemoveFromHiddenUseCase @Inject constructor(
    private val hiddenRepo: HiddenRepository
) {
    suspend operator fun invoke(mediaId: Long) = hiddenRepo.remove(mediaId)
}
