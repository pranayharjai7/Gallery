package com.pranayharjai7.gallery.domain.usecase

import com.pranayharjai7.gallery.domain.model.TrashItem
import com.pranayharjai7.gallery.domain.repository.TrashRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTrashUseCase @Inject constructor(
    private val trashRepo: TrashRepository
) {
    operator fun invoke(): Flow<List<TrashItem>> = trashRepo.observeAll()
}
