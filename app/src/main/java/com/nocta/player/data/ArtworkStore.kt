package com.nocta.player.data

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.LruCache
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.drawable.toDrawable
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.key.Keyer
import coil.request.Options
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max

data class ArtColors(
    val primary: Color,
    val secondary: Color,
    val surface: Color
)

/** Coil key so artwork is cached by song id. */
data class ArtRequest(val song: Song)

class ArtKeyer : Keyer<ArtRequest> {
    override fun key(data: ArtRequest, options: Options): String = "nocta-art-${data.song.id}"
}

class ArtFetcherFactory(
    private val store: ArtworkStore,
    private val context: Context
) : Fetcher.Factory<ArtRequest> {
    override fun create(data: ArtRequest, options: Options, imageLoader: ImageLoader): Fetcher {
        return object : Fetcher {
            override suspend fun fetch(): FetchResult {
                val bitmap = store.bitmap(data.song, 640)
                    ?: throw IllegalStateException("No artwork for ${data.song.title}")
                return DrawableResult(
                    drawable = BitmapDrawable(context.resources, bitmap),
                    isSampled = false,
                    dataSource = DataSource.DISK
                )
            }
        }
    }
}

class ArtworkStore(private val context: Context) {

    private val dir = File(context.filesDir, "art").apply { mkdirs() }
    private val paletteCache = LruCache<Long, ArtColors>(96)

    fun cachedFile(song: Song): File = File(dir, "${song.id}.art")

    suspend fun colors(song: Song): ArtColors? = withContext(Dispatchers.IO) {
        paletteCache.get(song.id)?.let { return@withContext it }
        val bitmap = bitmap(song, 96) ?: return@withContext null
        val palette = runCatching {
            Palette.from(bitmap)
                .clearFilters()
                .maximumColorCount(24)
                .generate()
        }.getOrNull() ?: return@withContext null
        bitmap.recycle()

        val vibrant = palette.getVibrantColor(palette.getDominantColor(0))
        val muted = palette.getMutedColor(palette.getDarkMutedColor(0))
        val dominant = palette.getDominantColor(vibrant)

        val colors = ArtColors(
            primary = vivid(if (vibrant != 0) vibrant else dominant),
            secondary = vivid(if (muted != 0) muted else dominant, saturation = 0.34f, value = 0.55f),
            surface = vivid(dominant, saturation = 0.30f, value = 0.22f)
        )
        paletteCache.put(song.id, colors)
        colors
    }

    suspend fun bitmap(song: Song, targetSize: Int = 512): Bitmap? = withContext(Dispatchers.IO) {
        val file = cachedFile(song)
        if (file.exists()) {
            decode(file.path, targetSize)?.let { return@withContext it }
        }

        val bytes = embeddedArt(song) ?: albumArt(song) ?: return@withContext null
        runCatching { file.writeBytes(bytes) }
        decode(bytes, targetSize)
    }

    private fun embeddedArt(song: Song): ByteArray? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, song.uri)
            retriever.embeddedPicture
        } catch (t: Throwable) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun albumArt(song: Song): ByteArray? {
        val uri = ContentUris.withAppendedId(
            Uri.parse("content://media/external/audio/albumart"),
            song.albumId
        )
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        }.getOrNull()
    }

    private fun decode(path: String, target: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0) return null
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= target && bounds.outHeight / (sample * 2) >= target) {
            sample *= 2
        }
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return BitmapFactory.decodeFile(path, opts)
    }

    private fun decode(bytes: ByteArray, target: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0) return null
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= target && bounds.outHeight / (sample * 2) >= target) {
            sample *= 2
        }
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
    }

    private fun vivid(color: Int, saturation: Float = 0.46f, value: Float = 0.72f): Color {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(color, hsv)
        hsv[1] = max(hsv[1], saturation)
        hsv[2] = max(hsv[2], value)
        return Color(android.graphics.Color.HSVToColor(hsv))
    }
}

/** Fallback used until palette extraction finishes. */
val DefaultAccent = Color(0xFFA78BFA)
val DefaultAccentSoft = Color(0xFF5EEAD4)
