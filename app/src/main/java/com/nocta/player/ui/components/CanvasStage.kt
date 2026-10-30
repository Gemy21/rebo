package com.nocta.player.ui.components

import android.net.Uri
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.nocta.player.data.Song
import kotlin.math.sin

/**
 * Spotify-Canvas style stage. Plays a looping muted clip when a `<song>.mp4` sidecar
 * exists, otherwise generates a living, breathing artwork canvas.
 */
@Composable
fun CanvasStage(
    song: Song?,
    videoUri: Uri?,
    isPlaying: Boolean,
    videoEnabled: Boolean,
    motion: Boolean,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(30.dp)
    val accent = com.nocta.player.ui.theme.LocalAccent.current

    Box(
        modifier = modifier
            .graphicsLayer { this.shape = shape; clip = true }
            .background(Color(0xFF07070A))
    ) {
        if (videoEnabled && videoUri != null) {
            CanvasVideo(uri = videoUri, modifier = Modifier.fillMaxSize())
        } else {
            GenerativeCanvas(
                song = song,
                isPlaying = isPlaying,
                motion = motion,
                modifier = Modifier.fillMaxSize()
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.06f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.28f)
                        )
                    )
                )
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.22f),
                            Color.White.copy(alpha = 0.04f),
                            accent.primary.copy(alpha = 0.18f)
                        )
                    ),
                    shape = shape
                )
        )

        if (videoEnabled && videoUri != null) {
            LabelPill(
                text = "Canvas",
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            )
        }
    }
}

@Composable
private fun GenerativeCanvas(
    song: Song?,
    isPlaying: Boolean,
    motion: Boolean,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "canvas")

    val breathe by transition.animateFloat(
        initialValue = 1.02f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(11000, easing = FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "breathe"
    )

    val sheen by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(6800, easing = LinearEasing)
        ),
        label = "sheen"
    )

    val drift by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(16000, easing = FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "drift"
    )

    val animate = motion && isPlaying

    Box(modifier = modifier) {
        AlbumArt(
            song = song,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val scale = if (animate) breathe else 1.04f
                    scaleX = scale
                    scaleY = scale
                    translationX = if (animate) drift * size.width * 0.018f else 0f
                },
            shape = RectangleShape
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val phase = if (animate) sheen else 0.55f
            val bandWidth = size.width * 0.55f
            val x = -bandWidth + phase * (size.width + bandWidth * 2f)

            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.10f),
                        Color.White.copy(alpha = 0.02f),
                        Color.Transparent
                    ),
                    start = Offset(x, 0f),
                    end = Offset(x + bandWidth, size.height)
                ),
                topLeft = Offset(x, 0f),
                size = Size(bandWidth, size.height)
            )

            if (animate) {
                for (i in 0 until 12) {
                    val px = ((i * 37) % 100) / 100f * size.width
                    val progress = (phase + i * 0.11f) % 1f
                    val py = size.height * (1.05f - progress)
                    val alpha = (0.16f * sin(progress * Math.PI).toFloat()).coerceAtLeast(0f)
                    drawCircle(
                        color = Color.White.copy(alpha = alpha),
                        radius = (0.9f + (i % 3) * 0.5f).dp.toPx(),
                        center = Offset(px, py)
                    )
                }
            }
        }
    }
}

@Composable
private fun CanvasVideo(uri: Uri, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    val player = remember(uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            repeatMode = Player.REPEAT_MODE_ONE
            volume = 0f
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(uri) {
        onDispose { player.release() }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                this.player = player
            }
        },
        update = { it.player = player },
        modifier = modifier
    )
}
