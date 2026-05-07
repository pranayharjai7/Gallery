package com.gallery.domain.usecase

import com.gallery.domain.model.MediaItem
import com.gallery.domain.model.TrashItem
import com.gallery.domain.repository.TrashRepository
import javax.inject.Inject

class MoveToTrashUseCase @Inject constructor(
    private val trashRepo: TrashRepository
) {
    suspend operator fun invoke(item: MediaItem) {
        trashRepo.insert(
            TrashItem(
                id = item.id,
                originalUri = item.uri,
                name = item.name,
                dateTaken = item.dateTaken,
                mimeType = item.mimeType,
                deletedAt = System.currentTimeMillis(),
                bucketId = item.bucketId
            )
        )
    }
}
