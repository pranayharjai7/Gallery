package com.gallery.domain.usecase

import com.gallery.domain.repository.TrashRepository
import javax.inject.Inject

class PurgeAllTrashUseCase @Inject constructor(
    private val trashRepo: TrashRepository
) {
    suspend operator fun invoke(ids: List<Long>) {
        trashRepo.deleteAll(ids)
    }
}
