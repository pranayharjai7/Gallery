package com.gallery.domain.repository

import kotlinx.coroutines.flow.Flow

interface FavoritesRepository {
    fun observeIds(): Flow<Set<Long>>
    suspend fun toggle(mediaId: Long)
    suspend fun contains(mediaId: Long): Boolean
}
