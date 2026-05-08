package com.pranayharjai7.gallery.data.mediastore

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.pranayharjai7.gallery.domain.model.MediaItem
import com.pranayharjai7.gallery.domain.repository.MediaRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private val MEDIA_URI: Uri = MediaStore.Files.getContentUri("external")

private val PROJECTION = arrayOf(
    MediaStore.Files.FileColumns._ID,
    MediaStore.Files.FileColumns.DISPLAY_NAME,
    MediaStore.Files.FileColumns.DATE_TAKEN,
    MediaStore.Files.FileColumns.SIZE,
    MediaStore.Files.FileColumns.WIDTH,
    MediaStore.Files.FileColumns.HEIGHT,
    MediaStore.Files.FileColumns.DURATION,
    MediaStore.Files.FileColumns.MIME_TYPE,
    MediaStore.Files.FileColumns.BUCKET_ID,
    MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME,
)

@Singleton
class MediaStoreMediaRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : MediaRepository {

    override fun observeAll(): Flow<List<MediaItem>> =
        context.contentResolver.observeUri(MEDIA_URI)
            .map {
                queryMedia(
                    selection = "(${MediaStore.Files.FileColumns.MIME_TYPE} LIKE 'image/%'" +
                        " OR ${MediaStore.Files.FileColumns.MIME_TYPE} LIKE 'video/%')",
                    selectionArgs = null,
                )
            }
            .flowOn(Dispatchers.IO)

    override fun observeByBucket(bucketId: Long): Flow<List<MediaItem>> =
        context.contentResolver.observeUri(MEDIA_URI)
            .map {
                queryMedia(
                    selection = "(${MediaStore.Files.FileColumns.MIME_TYPE} LIKE 'image/%'" +
                        " OR ${MediaStore.Files.FileColumns.MIME_TYPE} LIKE 'video/%')" +
                        " AND ${MediaStore.Files.FileColumns.BUCKET_ID} = ?",
                    selectionArgs = arrayOf(bucketId.toString()),
                )
            }
            .flowOn(Dispatchers.IO)

    override fun search(query: String): Flow<List<MediaItem>> =
        context.contentResolver.observeUri(MEDIA_URI)
            .map {
                queryMedia(
                    selection = "(${MediaStore.Files.FileColumns.MIME_TYPE} LIKE 'image/%'" +
                        " OR ${MediaStore.Files.FileColumns.MIME_TYPE} LIKE 'video/%')" +
                        " AND ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ?",
                    selectionArgs = arrayOf("%$query%"),
                )
            }
            .flowOn(Dispatchers.IO)

    override suspend fun getById(id: Long): MediaItem? = withContext(Dispatchers.IO) {
        queryMedia(
            selection = "${MediaStore.Files.FileColumns._ID} = ?",
            selectionArgs = arrayOf(id.toString()),
        ).firstOrNull()
    }

    override fun getOnThisDay(monthDay: Int): Flow<List<MediaItem>> {
        val month = monthDay / 100
        val day   = monthDay % 100
        return context.contentResolver.observeUri(MEDIA_URI)
            .map {
                val all = queryMedia(
                    selection = "(${MediaStore.Files.FileColumns.MIME_TYPE} LIKE 'image/%'" +
                        " OR ${MediaStore.Files.FileColumns.MIME_TYPE} LIKE 'video/%')",
                    selectionArgs = null,
                )
                all.filter { item ->
                    val cal = java.util.Calendar.getInstance().apply {
                        timeInMillis = item.dateTaken
                    }
                    cal.get(java.util.Calendar.MONTH) + 1 == month &&
                        cal.get(java.util.Calendar.DAY_OF_MONTH) == day
                }
            }
            .flowOn(Dispatchers.IO)
    }

    private fun queryMedia(
        selection: String?,
        selectionArgs: Array<String>?,
    ): List<MediaItem> {
        val items = mutableListOf<MediaItem>()
        context.contentResolver.query(
            MEDIA_URI,
            PROJECTION,
            selection,
            selectionArgs,
            "${MediaStore.Files.FileColumns.DATE_TAKEN} DESC",
        )?.use { cursor ->
            val idCol         = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val nameCol       = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val dateTakenCol  = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_TAKEN)
            val sizeCol       = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
            val widthCol      = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.WIDTH)
            val heightCol     = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.HEIGHT)
            val durationCol   = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DURATION)
            val mimeCol       = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
            val bucketIdCol   = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_ID)
            val bucketNameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val id       = cursor.getLong(idCol)

                items += MediaItem(
                    id        = id,
                    uri       = ContentUris.withAppendedId(MEDIA_URI, id),
                    name      = cursor.getString(nameCol).orEmpty(),
                    dateTaken = if (cursor.isNull(dateTakenCol)) 0L else cursor.getLong(dateTakenCol),
                    size      = cursor.getLong(sizeCol),
                    width     = cursor.getInt(widthCol),
                    height    = cursor.getInt(heightCol),
                    duration  = if (cursor.isNull(durationCol)) null else cursor.getLong(durationCol),
                    mimeType  = cursor.getString(mimeCol).orEmpty(),
                    bucketId  = cursor.getLong(bucketIdCol),
                    bucketName = cursor.getString(bucketNameCol).orEmpty(),
                    location  = null,
                )
            }
        }
        return items
    }
}
