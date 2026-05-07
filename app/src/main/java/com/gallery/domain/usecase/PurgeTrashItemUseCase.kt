package com.gallery.domain.usecase

import com.gallery.domain.repository.TrashRepository
import javax.inject.Inject

class PurgeTrashItemUseCase @Inject constructor(
    private val trashRepo: TrashRepository
) {
    suspend operator fun invoke(id: Long) {
        trashRepo.delete(id)
    }
}
