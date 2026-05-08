package com.pranayharjai7.gallery.data.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hidden")
data class HiddenEntity(@PrimaryKey val mediaId: Long)
