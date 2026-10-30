package com.nocta.player.data

import android.net.Uri

/**
 * Resolves an optional looping "canvas" clip that sits next to the audio file.
 * The Spotify Importer writes `Artist - Title.mp3`; a matching
 * `Artist - Title.mp4` (or .webm/.mov/.mkv) unlocks the animated player canvas.
 */
class VideoRepository(private val sidecars: SidecarIndex) {

    private val extensions = listOf("mp4", "webm", "mov", "m4v", "mkv")

    suspend fun videoFor(song: Song): Uri? {
        val base = song.sidecarBase
        val names = buildList {
            extensions.forEach { add("$base.$it") }
            extensions.forEach { add("$base.${it.uppercase()}") }
            add("${sanitizeSidecar(song.title)}.mp4")
            add("${sanitizeSidecar(song.title)}.webm")
        }
        return sidecars.findFirst(*names.toTypedArray())
    }
}
