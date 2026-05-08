package com.pranayharjai7.gallery.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pranayharjai7.gallery.data.room.entity.HiddenEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HiddenDao {

    @Query("SELECT mediaId FROM hidden")
    fun observeIds(): Flow<List<Long>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: HiddenEntity)

    @Query("DELETE FROM hidden WHERE mediaId = :mediaId")
    suspend fun deleteById(mediaId: Long)

    @Query("SELECT COUNT(*) > 0 FROM hidden WHERE mediaId = :mediaId")
    suspend fun contains(mediaId: Long): Boolean
}
