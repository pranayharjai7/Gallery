package com.gallery.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.gallery.data.room.entity.FavoriteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoritesDao {

    @Query("SELECT mediaId FROM favorites")
    fun observeIds(): Flow<List<Long>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE mediaId = :mediaId")
    suspend fun deleteById(mediaId: Long)

    @Query("SELECT COUNT(*) > 0 FROM favorites WHERE mediaId = :mediaId")
    suspend fun contains(mediaId: Long): Boolean

    @Transaction
    suspend fun toggle(mediaId: Long) {
        if (contains(mediaId)) {
            deleteById(mediaId)
        } else {
            insert(FavoriteEntity(mediaId = mediaId))
        }
    }
}
