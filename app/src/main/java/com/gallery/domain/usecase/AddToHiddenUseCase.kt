package com.gallery.domain.usecase

import com.gallery.domain.repository.HiddenRepository
import javax.inject.Inject

class AddToHiddenUseCase @Inject constructor(
    private val hiddenRepo: HiddenRepository
) {
    suspend operator fun invoke(mediaId: Long) = hiddenRepo.add(mediaId)
}
