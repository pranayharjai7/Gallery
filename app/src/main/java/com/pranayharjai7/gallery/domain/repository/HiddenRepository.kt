package com.pranayharjai7.gallery.domain.repository

import kotlinx.coroutines.flow.Flow

interface HiddenRepository {
    fun observeIds(): Flow<Set<Long>>
    suspend fun add(mediaId: Long)
    suspend fun remove(mediaId: Long)
}
