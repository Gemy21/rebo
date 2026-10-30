package com.nocta.player.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nocta.player.PlayerViewModel
import com.nocta.player.data.LyricsState
import com.nocta.player.data.formatTime
import com.nocta.player.ui.components.AlbumArt
import com.nocta.player.ui.components.GlassIconButton
import com.nocta.player.ui.components.GlassSurface
import com.nocta.player.ui.components.LabelPill
import com.nocta.player.ui.components.LiquidSeekBar
import com.nocta.player.ui.components.LyricsPanel
import com.nocta.player.ui.components.LyricsStatus
import com.nocta.player.ui.theme.AccentPalette
import com.nocta.player.ui.theme.LocalAccent
import com.nocta.player.ui.theme.NoctaMuted

@Composable
fun LyricsScreen(
    vm: PlayerViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val song by vm.current.collectAsState()
    val lyricsState by vm.lyrics.collectAsState()
    val duration by vm.duration.collectAsState()
    val karaoke by vm.settings.karaokeFill.collectAsState()

    val accent = LocalAccent.current
    val totalDuration = if (duration > 0) duration else song?.durationMs ?: 0L

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        LyricsBackdrop(accent)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassIconButton(
                    icon = Icons.Rounded.KeyboardArrowDown,
                    onClick = onClose,
                    size = 42.dp,
                    iconSize = 26.dp,
                    contentDescription = "Close lyrics"
                )
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "LYRICS",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.55f)
                    )
                    Text(
                        text = song?.title ?: "Nothing playing",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.9f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                LabelPill(
                    text = when (val state = lyricsState) {
                        is LyricsState.Ready -> if (state.lyrics.synced) "Synced" else "Plain"
                        LyricsState.Loading -> "Matching"
                        else -> "Offline"
                    },
                    tint = accent.primary
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AlbumArt(
                    song = song,
                    modifier = Modifier.size(46.dp),
                    shape = RoundedCornerShape(13.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song?.displayArtist ?: "—",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Text(
                        text = song?.displayAlbum ?: "Nocta",
                        style = MaterialTheme.typography.labelMedium,
                        color = NoctaMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            LyricsBody(
                vm = vm,
                lyricsState = lyricsState,
                karaoke = karaoke,
                totalDuration = totalDuration,
                accent = accent
            )
        }
    }
}

/**
 * Everything below the header. The playhead only invalidates this subtree, so the header
 * artwork and title are not recomposed four times a second.
 */
@Composable
private fun ColumnScope.LyricsBody(
    vm: PlayerViewModel,
    lyricsState: LyricsState,
    karaoke: Boolean,
    totalDuration: Long,
    accent: AccentPalette
) {
    val position by vm.position.collectAsState()
    val isPlaying by vm.isPlaying.collectAsState()

    when (val state = lyricsState) {
        is LyricsState.Ready -> LyricsPanel(
            lyrics = state.lyrics,
            positionMs = position,
            onSeek = { vm.seekTo(it) },
            baseSize = 27.sp,
            karaoke = karaoke,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )

        else -> Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            LyricsStatus(
                state = state,
                onRetry = { vm.retryLyrics() }
            )
        }
    }

    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        shape = RoundedCornerShape(24.dp),
        darkness = 0.24f
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            LiquidSeekBar(
                progress = if (totalDuration > 0) {
                    (position.toFloat() / totalDuration).coerceIn(0f, 1f)
                } else 0f,
                onSeek = { fraction ->
                    if (totalDuration > 0) vm.seekTo((fraction * totalDuration).toLong())
                }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatTime(position),
                    style = MaterialTheme.typography.labelSmall,
                    color = NoctaMuted
                )
                Spacer(Modifier.weight(1f))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassIconButton(
                        icon = Icons.Rounded.SkipPrevious,
                        onClick = { vm.previous() },
                        size = 38.dp,
                        iconSize = 20.dp,
                        contentDescription = "Previous"
                    )
                    GlassIconButton(
                        icon = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        onClick = { vm.playPause() },
                        size = 46.dp,
                        iconSize = 24.dp,
                        tint = accent.primary,
                        contentDescription = "Play or pause"
                    )
                    GlassIconButton(
                        icon = Icons.Rounded.SkipNext,
                        onClick = { vm.next() },
                        size = 38.dp,
                        iconSize = 20.dp,
                        contentDescription = "Next"
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = "-${formatTime((totalDuration - position).coerceAtLeast(0))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = NoctaMuted
                )
            }
        }
    }
}

@Composable
private fun LyricsBackdrop(accent: AccentPalette) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawWithCache {
                val glow = Brush.radialGradient(
                    colors = listOf(
                        accent.primary.copy(alpha = 0.36f),
                        accent.soft.copy(alpha = 0.10f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.86f, size.height * 0.04f),
                    radius = size.maxDimension * 0.72f
                )
                val floor = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.55f),
                        Color.Black.copy(alpha = 0.92f)
                    ),
                    startY = size.height * 0.45f,
                    endY = size.height
                )
                onDrawBehind {
                    drawRect(Color.Black)
                    drawRect(glow)
                    drawRect(floor)
                }
            }
    )
}
