package com.pranayharjai7.gallery.di

import android.content.Context
import androidx.room.Room
import com.pranayharjai7.gallery.data.room.GalleryDatabase
import com.pranayharjai7.gallery.data.room.GalleryDatabase.Companion.DATABASE_NAME
import com.pranayharjai7.gallery.data.mediastore.MediaStoreAlbumRepository
import com.pranayharjai7.gallery.data.mediastore.MediaStoreMediaRepository
import com.pranayharjai7.gallery.data.room.RoomFavoritesRepository
import com.pranayharjai7.gallery.data.room.RoomHiddenRepository
import com.pranayharjai7.gallery.data.room.RoomTrashRepository
import com.pranayharjai7.gallery.data.room.dao.FavoritesDao
import com.pranayharjai7.gallery.data.room.dao.HiddenDao
import com.pranayharjai7.gallery.data.room.dao.TrashDao
import com.pranayharjai7.gallery.domain.repository.AlbumRepository
import com.pranayharjai7.gallery.domain.repository.FavoritesRepository
import com.pranayharjai7.gallery.domain.repository.HiddenRepository
import com.pranayharjai7.gallery.domain.repository.MediaRepository
import com.pranayharjai7.gallery.domain.repository.TrashRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideGalleryDatabase(
        @ApplicationContext context: Context
    ): GalleryDatabase = Room.databaseBuilder(
        context,
        GalleryDatabase::class.java,
        DATABASE_NAME
    ).fallbackToDestructiveMigration()
        .build()

    @Provides
    fun provideTrashDao(db: GalleryDatabase): TrashDao = db.trashDao()

    @Provides
    fun provideHiddenDao(db: GalleryDatabase): HiddenDao = db.hiddenDao()

    @Provides
    fun provideFavoritesDao(db: GalleryDatabase): FavoritesDao = db.favoritesDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTrashRepository(impl: RoomTrashRepository): TrashRepository

    @Binds
    @Singleton
    abstract fun bindHiddenRepository(impl: RoomHiddenRepository): HiddenRepository

    @Binds
    @Singleton
    abstract fun bindFavoritesRepository(impl: RoomFavoritesRepository): FavoritesRepository

    @Binds
    @Singleton
    abstract fun bindMediaRepository(impl: MediaStoreMediaRepository): MediaRepository

    @Binds
    @Singleton
    abstract fun bindAlbumRepository(impl: MediaStoreAlbumRepository): AlbumRepository
}
