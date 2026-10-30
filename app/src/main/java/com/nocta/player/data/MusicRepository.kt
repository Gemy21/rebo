package com.nocta.player.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MusicRepository(private val context: Context) {

    private val albumArtUri: Uri = Uri.parse("content://media/external/audio/albumart")

    suspend fun loadSongs(): List<Song> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<Song>()

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.IS_MUSIC
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND " +
            "${MediaStore.Audio.Media.DURATION} > 10000"

        runCatching {
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                "${MediaStore.Audio.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val trackIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
                val yearIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
                val dataIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val addedIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val mimeIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idIdx)
                    val title = cursor.getString(titleIdx)?.trim().orEmpty()
                    if (title.isBlank()) continue

                    val artist = cursor.getString(artistIdx)
                        ?.replace("<unknown>", "")
                        ?.trim()
                        .orEmpty()

                    val album = cursor.getString(albumIdx)?.trim().orEmpty()
                    val albumId = cursor.getLong(albumIdIdx)
                    val path = cursor.getString(dataIdx).orEmpty()
                    val folder = path.substringBeforeLast('/', "")

                    songs += Song(
                        id = id,
                        uri = ContentUris.withAppendedId(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            id
                        ),
                        title = title,
                        artist = artist,
                        album = album,
                        albumId = albumId,
                        durationMs = cursor.getLong(durationIdx),
                        trackNumber = cursor.getInt(trackIdx) % 1000,
                        year = cursor.getInt(yearIdx),
                        folder = folder,
                        addedAt = cursor.getLong(addedIdx) * 1000L,
                        mimeType = cursor.getString(mimeIdx) ?: "audio/mpeg"
                    )
                }
            }
        }

        songs
            .distinctBy { "${it.title.lowercase().trim()}|${it.artist.lowercase().trim()}" }
            .sortedWith(
                compareByDescending<Song> { it.addedAt }.thenBy { it.title.lowercase() }
            )
    }

    fun albumArtUri(albumId: Long): Uri =
        ContentUris.withAppendedId(albumArtUri, albumId)

    fun toAlbums(songs: List<Song>): List<Album> {
        return songs
            .groupBy { "${it.album.lowercase().trim()}|${it.artist.lowercase().trim()}" }
            .map { (key, group) ->
                val ordered = group.sortedBy { it.trackNumber }
                Album(
                    key = key,
                    name = ordered.first().displayAlbum,
                    artist = ordered.first().displayArtist,
                    songs = ordered,
                    year = ordered.first().year
                )
            }
            .sortedBy { it.name.lowercase() }
    }
}
