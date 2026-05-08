package com.pranayharjai7.gallery.domain.repository

import com.pranayharjai7.gallery.domain.model.Album
import kotlinx.coroutines.flow.Flow

interface AlbumRepository {
    fun observeAlbums(hiddenIds: Set<Long>): Flow<List<Album>>
}
