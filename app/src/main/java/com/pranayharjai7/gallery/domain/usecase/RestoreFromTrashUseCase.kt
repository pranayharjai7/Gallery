package com.pranayharjai7.gallery.domain.usecase

import com.pranayharjai7.gallery.domain.model.TrashItem
import com.pranayharjai7.gallery.domain.repository.TrashRepository
import javax.inject.Inject

class RestoreFromTrashUseCase @Inject constructor(
    private val trashRepo: TrashRepository
) {
    suspend operator fun invoke(trashItem: TrashItem) {
        trashRepo.delete(trashItem.id)
    }
}
