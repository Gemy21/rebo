package com.nocta.player.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

sealed interface LyricsState {
    data object Idle : LyricsState
    data object Loading : LyricsState
    data class Ready(val lyrics: Lyrics, val source: String) : LyricsState
    data object NotFound : LyricsState
    data class Failed(val message: String) : LyricsState
}

class LyricsRepository(
    private val context: Context,
    private val sidecars: SidecarIndex
) {

    private val client = OkHttpClient.Builder()
        .callTimeout(18, TimeUnit.SECONDS)
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    private val memory = LruCache<String, Lyrics>(72)
    private val cacheDir = File(context.filesDir, "lyrics").apply { mkdirs() }

    suspend fun lyricsFor(song: Song, forceRefresh: Boolean = false): LyricsState {
        val key = song.cacheKey

        if (!forceRefresh) {
            memory.get(key)?.let {
                return LyricsState.Ready(it, it.source)
            }
        }

        val diskCache = cacheFile(song)
        if (!forceRefresh) {
            readCache(diskCache)?.let {
                memory.put(key, it)
                return LyricsState.Ready(it, it.source)
            }
        }

        val sidecar = loadSidecar(song)
        if (sidecar != null) {
            val parsed = LrcParser.parse(sidecar, "sidecar")
            if (parsed != null) {
                memory.put(key, parsed)
                writeCache(diskCache, parsed)
                return LyricsState.Ready(parsed, "sidecar")
            }
        }

        val remote = fetchRemote(song)
        if (remote != null) {
            memory.put(key, remote)
            writeCache(diskCache, remote)
            return LyricsState.Ready(remote, "lrclib")
        }

        return LyricsState.NotFound
    }

    fun cachedAny(song: Song): Lyrics? = memory.get(song.cacheKey)

    fun clearCache() {
        memory.evictAll()
        cacheDir.listFiles()?.forEach { runCatching { it.delete() } }
    }

    private suspend fun loadSidecar(song: Song): String? = withContext(Dispatchers.IO) {
        val base = song.sidecarBase
        val names = listOf("$base.lrc", "$base.txt", "$base.LRC", "$base.TXT")

        sidecars.findFirst(*names.toTypedArray())?.let { uri ->
            readUri(uri)?.takeIf { it.isNotBlank() }?.let { return@withContext it }
        }

        readMediaStoreSidecar(names)
    }

    private fun readMediaStoreSidecar(names: List<String>): String? {
        val collection = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(MediaStore.Files.FileColumns._ID)
        for (name in names) {
            runCatching {
                context.contentResolver.query(
                    collection,
                    projection,
                    "${MediaStore.Files.FileColumns.DISPLAY_NAME} = ?",
                    arrayOf(name),
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val uri = ContentUris.withAppendedId(collection, cursor.getLong(0))
                        return readUri(uri)
                    }
                }
            }
        }
        return null
    }

    private fun readUri(uri: Uri): String? = runCatching {
        context.contentResolver.openInputStream(uri)
            ?.bufferedReader(Charsets.UTF_8)
            ?.use { it.readText() }
    }.getOrNull()

    private suspend fun fetchRemote(song: Song): Lyrics? = withContext(Dispatchers.IO) {
        val artists = artistCandidates(song.artist)
        val titles = titleCandidates(song.title)

        for (artist in artists) {
            for (title in titles) {
                val exact = lrclibGet(artist, title, song.album, song.durationMs)
                if (exact != null) return@withContext exact
            }
        }

        for (artist in artists.take(1)) {
            for (title in titles.take(1)) {
                val searched = lrclibSearch(artist, title)
                if (searched != null) return@withContext searched
            }
        }
        null
    }

    private fun lrclibGet(artist: String, title: String, album: String, durationMs: Long): Lyrics? {
        val url = buildString {
            append("https://lrclib.net/api/get?")
            append("artist_name=").append(enc(artist))
            append("&track_name=").append(enc(title))
            if (album.isNotBlank()) append("&album_name=").append(enc(album))
            if (durationMs > 0) append("&duration=").append(durationMs / 1000)
        }
        val body = get(url) ?: return null
        return parseLrclibBody(JSONObject(body), "lrclib")
    }

    private fun lrclibSearch(artist: String, title: String): Lyrics? {
        val url = buildString {
            append("https://lrclib.net/api/search?")
            append("artist_name=").append(enc(artist))
            append("&track_name=").append(enc(title))
        }
        val body = get(url) ?: return null
        val array = runCatching { org.json.JSONArray(body) }.getOrNull() ?: return null
        var fallback: Lyrics? = null
        for (i in 0 until array.length()) {
            val item = runCatching { array.getJSONObject(i) }.getOrNull() ?: continue
            val parsed = parseLrclibBody(item, "lrclib") ?: continue
            if (parsed.synced) return parsed
            if (fallback == null) fallback = parsed
        }
        return fallback
    }

    private fun get(url: String): String? {
        return runCatching {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Nocta/1.0 (local music player)")
                .header("Accept", "application/json")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                response.body?.string()
            }
        }.getOrNull()
    }

    private fun parseLrclibBody(json: JSONObject, source: String): Lyrics? {
        val syncedRaw = if (json.isNull("syncedLyrics")) null else json.optString("syncedLyrics")
        val plainRaw = if (json.isNull("plainLyrics")) null else json.optString("plainLyrics")

        if (!syncedRaw.isNullOrBlank()) {
            LrcParser.parse(syncedRaw, source)?.let { return it }
        }
        if (!plainRaw.isNullOrBlank()) {
            LrcParser.parse(plainRaw, source)?.let { return it }
        }
        return null
    }

    private fun artistCandidates(artist: String): List<String> {
        val clean = artist.trim()
        if (clean.isBlank()) return listOf("")
        val primary = clean
            .substringBefore(",")
            .substringBefore(" feat.")
            .substringBefore(" feat ")
            .substringBefore(" & ")
            .substringBefore(" x ")
            .trim()
        return listOf(clean, primary).filter { it.isNotBlank() }.distinct()
    }

    private fun titleCandidates(title: String): List<String> {
        val clean = title.trim()
        val withoutDash = clean.substringBefore(" - Remaster", clean)
            .substringBefore(" - Live", clean)
            .trim()
        val withoutParen = clean
            .replace(
                Regex(
                    """\s*[\(\[](remaster(ed)?|live|deluxe|explicit|radio edit|version)[^)\]]*[\)\]]""",
                    RegexOption.IGNORE_CASE
                ),
                ""
            )
            .trim()
        return listOf(clean, withoutDash, withoutParen)
            .filter { it.isNotBlank() }
            .distinct()
    }

    private fun enc(value: String): String = URLEncoder.encode(value, "UTF-8")

    private fun cacheFile(song: Song): File {
        val digest = MessageDigest.getInstance("MD5")
            .digest(song.cacheKey.toByteArray())
            .joinToString("") { "%02x".format(it) }
        return File(cacheDir, "$digest.json")
    }

    private fun readCache(file: File): Lyrics? {
        if (!file.exists()) return null
        val text = runCatching { file.readText() }.getOrNull() ?: return null
        val json = runCatching { JSONObject(text) }.getOrNull() ?: return null
        val raw = json.optString("raw")
        val source = json.optString("source", "cached")
        val synced = json.optBoolean("synced", false)
        if (raw.isBlank()) return null
        val parsed = LrcParser.parse(raw, source) ?: return null
        return if (synced == parsed.synced) parsed else parsed
    }

    private fun writeCache(file: File, lyrics: Lyrics) {
        runCatching {
            val raw = if (lyrics.synced) {
                lyrics.lines.joinToString("\n") { line ->
                    "[%02d:%02d.%02d]%s".format(
                        line.timeMs / 60000,
                        (line.timeMs % 60000) / 1000,
                        ((line.timeMs % 1000) / 10),
                        line.text
                    )
                }
            } else {
                lyrics.lines.joinToString("\n") { it.text }
            }
            JSONObject()
                .put("raw", raw)
                .put("source", lyrics.source)
                .put("synced", lyrics.synced)
                .toString()
                .let(file::writeText)
        }
    }
}
