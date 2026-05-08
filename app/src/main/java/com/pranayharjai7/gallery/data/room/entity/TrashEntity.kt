package com.pranayharjai7.gallery.data.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.pranayharjai7.gallery.domain.model.TrashItem

@Entity(tableName = "trash")
data class TrashEntity(
    @PrimaryKey val id: Long,
    val originalPath: String,
    val originalUri: String,
    val name: String,
    val dateTaken: Long,
    val mimeType: String,
    val deletedAt: Long,
    val bucketId: Long
)

fun TrashEntity.toTrashItem(): TrashItem = TrashItem(
    id = id,
    originalUri = android.net.Uri.parse(originalUri),
    name = name,
    dateTaken = dateTaken,
    mimeType = mimeType,
    deletedAt = deletedAt,
    bucketId = bucketId
)

fun TrashItem.toEntity(path: String): TrashEntity = TrashEntity(
    id = id,
    originalPath = path,
    originalUri = originalUri.toString(),
    name = name,
    dateTaken = dateTaken,
    mimeType = mimeType,
    deletedAt = deletedAt,
    bucketId = bucketId
)
