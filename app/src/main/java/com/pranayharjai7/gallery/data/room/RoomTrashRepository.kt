package com.pranayharjai7.gallery.data.room

import com.pranayharjai7.gallery.data.room.dao.TrashDao
import com.pranayharjai7.gallery.data.room.entity.toEntity
import com.pranayharjai7.gallery.data.room.entity.toTrashItem
import com.pranayharjai7.gallery.domain.model.TrashItem
import com.pranayharjai7.gallery.domain.repository.TrashRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RoomTrashRepository @Inject constructor(
    private val trashDao: TrashDao
) : TrashRepository {

    override fun observeAll(): Flow<List<TrashItem>> =
        trashDao.observeAll().map { entities -> entities.map { it.toTrashItem() } }

    override suspend fun insert(item: TrashItem) {
        trashDao.insert(item.toEntity(item.originalUri.toString()))
    }

    override suspend fun delete(id: Long) {
        trashDao.delete(id)
    }

    override suspend fun getExpired(before: Long): List<TrashItem> =
        trashDao.getExpired(before).map { it.toTrashItem() }

    override suspend fun deleteAll(ids: List<Long>) {
        trashDao.deleteAll(ids)
    }
}
