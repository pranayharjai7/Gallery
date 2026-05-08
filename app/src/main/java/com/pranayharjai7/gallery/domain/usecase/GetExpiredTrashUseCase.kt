package com.pranayharjai7.gallery.domain.usecase

import com.pranayharjai7.gallery.domain.model.TrashItem
import com.pranayharjai7.gallery.domain.repository.TrashRepository
import javax.inject.Inject

class GetExpiredTrashUseCase @Inject constructor(
    private val trashRepo: TrashRepository
) {
    suspend operator fun invoke(before: Long): List<TrashItem> =
        trashRepo.getExpired(before)
}
