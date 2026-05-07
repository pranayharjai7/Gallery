package com.gallery.domain.repository

import com.gallery.domain.model.Album
import kotlinx.coroutines.flow.Flow

interface AlbumRepository {
    fun observeAlbums(hiddenIds: Set<Long>): Flow<List<Album>>
}
