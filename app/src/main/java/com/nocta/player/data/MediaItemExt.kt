package com.nocta.player.data

import android.os.Bundle
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

private const val EXTRA_ID = "nocta.songId"
private const val EXTRA_ALBUM_ID = "nocta.albumId"
private const val EXTRA_FOLDER = "nocta.folder"
private const val EXTRA_ADDED = "nocta.addedAt"
private const val EXTRA_MIME = "nocta.mime"
private const val EXTRA_DURATION = "nocta.duration"

fun Song.toMediaItem(): MediaItem {
    val metadata = MediaMetadata.Builder()
        .setTitle(title)
        .setArtist(displayArtist)
        .setAlbumTitle(displayAlbum)
        .setArtworkUri(
            Uri.parse("content://media/external/audio/albumart/$albumId")
        )
        .setIsBrowsable(false)
        .setIsPlayable(true)
        .setExtras(
            Bundle().apply {
                putLong(EXTRA_ID, id)
                putLong(EXTRA_ALBUM_ID, albumId)
                putString(EXTRA_FOLDER, folder)
                putLong(EXTRA_ADDED, addedAt)
                putString(EXTRA_MIME, mimeType)
                putLong(EXTRA_DURATION, durationMs)
            }
        )
        .build()

    return MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(uri)
        .setMediaMetadata(metadata)
        .build()
}

fun MediaItem.toSongOrNull(): Song? {
    val metadata = mediaMetadata
    val extras = metadata.extras
    val id = extras?.getLong(EXTRA_ID)?.takeIf { it > 0 }
        ?: mediaId.toLongOrNull()
        ?: return null

    val title = metadata.title?.toString()?.takeIf { it.isNotBlank() } ?: "Unknown title"
    val artist = metadata.artist?.toString().orEmpty()
    val album = metadata.albumTitle?.toString().orEmpty()

    return Song(
        id = id,
        uri = localConfiguration?.uri ?: Uri.EMPTY,
        title = title,
        artist = artist,
        album = album,
        albumId = extras?.getLong(EXTRA_ALBUM_ID) ?: 0L,
        durationMs = extras?.getLong(EXTRA_DURATION, 0L) ?: 0L,
        folder = extras?.getString(EXTRA_FOLDER).orEmpty(),
        addedAt = extras?.getLong(EXTRA_ADDED) ?: 0L,
        mimeType = extras?.getString(EXTRA_MIME) ?: "audio/mpeg"
    )
}
