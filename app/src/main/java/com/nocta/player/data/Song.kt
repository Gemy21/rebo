package com.nocta.player.data

import android.net.Uri

/**
 * A single playable track discovered in the device's MediaStore.
 *
 * Tracks produced by the Spotify Importer live in `Music/SpotifyImporter` and carry
 * embedded artwork + optional `.lrc` / `.mp4` sidecars next to the audio file.
 */
data class Song(
    val id: Long,
    val uri: Uri,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val trackNumber: Int = 0,
    val year: Int = 0,
    val folder: String = "",
    val addedAt: Long = 0L,
    val mimeType: String = "audio/mpeg"
) {
    val isImported: Boolean
        get() = folder.contains("SpotifyImporter", ignoreCase = true)

    val displayArtist: String
        get() = artist.ifBlank { "Unknown artist" }

    val displayAlbum: String
        get() = album.ifBlank { "Single" }

    /** Stable, human-readable sidecar base name (mirrors the importer's file naming). */
    val sidecarBase: String
        get() = sanitizeSidecar("$artist - $title")

    val cacheKey: String
        get() = "${artist.trim().lowercase()}|${title.trim().lowercase()}"
}

data class Album(
    val key: String,
    val name: String,
    val artist: String,
    val songs: List<Song>,
    val year: Int = 0
) {
    val first: Song get() = songs.first()
    val durationMs: Long get() = songs.sumOf { it.durationMs }
}

fun sanitizeSidecar(value: String): String = value
    .replace(Regex("[\\\\/:*?\"<>|]"), "_")
    .replace(Regex("\\s+"), " ")
    .trim()
    .take(120)
    .ifBlank { "track" }

fun formatTime(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
