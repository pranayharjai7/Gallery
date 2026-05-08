package com.pranayharjai7.gallery.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pranayharjai7.gallery.data.room.entity.TrashEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrashDao {

    @Query("SELECT * FROM trash ORDER BY deletedAt DESC")
    fun observeAll(): Flow<List<TrashEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: TrashEntity)

    @Query("DELETE FROM trash WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM trash WHERE deletedAt < :before")
    suspend fun getExpired(before: Long): List<TrashEntity>

    @Query("DELETE FROM trash WHERE id IN (:ids)")
    suspend fun deleteAll(ids: List<Long>)
}
