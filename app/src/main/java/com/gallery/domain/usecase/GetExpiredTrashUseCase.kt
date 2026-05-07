package com.gallery.domain.usecase

import com.gallery.domain.model.TrashItem
import com.gallery.domain.repository.TrashRepository
import javax.inject.Inject

class GetExpiredTrashUseCase @Inject constructor(
    private val trashRepo: TrashRepository
) {
    suspend operator fun invoke(before: Long): List<TrashItem> =
        trashRepo.getExpired(before)
}
