package com.gallery.domain.repository

import com.gallery.domain.model.MediaItem
import kotlinx.coroutines.flow.Flow

interface MediaRepository {
    fun observeAll(): Flow<List<MediaItem>>
    fun observeByBucket(bucketId: Long): Flow<List<MediaItem>>
    fun search(query: String): Flow<List<MediaItem>>
    suspend fun getById(id: Long): MediaItem?
    fun getOnThisDay(monthDay: Int): Flow<List<MediaItem>>
}
