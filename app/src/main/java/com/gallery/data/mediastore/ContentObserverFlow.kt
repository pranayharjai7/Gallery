package com.gallery.data.mediastore

import android.content.ContentResolver
import android.database.ContentObserver
import android.net.Uri
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.onStart

fun ContentResolver.observeUri(uri: Uri): Flow<Unit> = callbackFlow {
    val observer = object : ContentObserver(null) {
        override fun onChange(selfChange: Boolean) {
            trySend(Unit)
        }
        override fun onChange(selfChange: Boolean, uri: Uri?) = onChange(selfChange)
    }
    registerContentObserver(uri, true, observer)
    awaitClose { unregisterContentObserver(observer) }
}.onStart { emit(Unit) }
