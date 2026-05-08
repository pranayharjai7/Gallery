package com.pranayharjai7.gallery.domain.usecase

import com.pranayharjai7.gallery.domain.model.Album
import com.pranayharjai7.gallery.domain.repository.AlbumRepository
import com.pranayharjai7.gallery.domain.repository.HiddenRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject

class GetAlbumsUseCase @Inject constructor(
    private val albumRepo: AlbumRepository,
    private val hiddenRepo: HiddenRepository
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<List<Album>> =
        hiddenRepo.observeIds().flatMapLatest { albumRepo.observeAlbums(it) }
}
