package com.gallery.data.room

import androidx.room.Database
import androidx.room.RoomDatabase
import com.gallery.data.room.dao.FavoritesDao
import com.gallery.data.room.dao.HiddenDao
import com.gallery.data.room.dao.TrashDao
import com.gallery.data.room.entity.FavoriteEntity
import com.gallery.data.room.entity.HiddenEntity
import com.gallery.data.room.entity.TrashEntity

@Database(
    entities = [
        TrashEntity::class,
        HiddenEntity::class,
        FavoriteEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class GalleryDatabase : RoomDatabase() {

    abstract fun trashDao(): TrashDao
    abstract fun hiddenDao(): HiddenDao
    abstract fun favoritesDao(): FavoritesDao

    companion object {
        const val DATABASE_NAME = "gallery.db"
    }
}
