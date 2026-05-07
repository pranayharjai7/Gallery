package com.gallery.domain.repository

import com.gallery.domain.model.TrashItem
import kotlinx.coroutines.flow.Flow

interface TrashRepository {
    fun observeAll(): Flow<List<TrashItem>>
    suspend fun insert(item: TrashItem)
    suspend fun delete(id: Long)
    suspend fun deleteAll(ids: List<Long>)
    suspend fun getExpired(before: Long): List<TrashItem>
}
