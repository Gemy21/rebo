package com.nocta.player.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import com.nocta.player.PlayerViewModel
import com.nocta.player.data.LyricsState
import com.nocta.player.data.formatTime
import com.nocta.player.ui.components.CanvasStage
import com.nocta.player.ui.components.GlassIconButton
import com.nocta.player.ui.components.GlassSurface
import com.nocta.player.ui.components.LiquidSeekBar
import com.nocta.player.ui.components.MarqueeText
import com.nocta.player.ui.theme.AccentPalette
import com.nocta.player.ui.theme.LocalAccent
import com.nocta.player.ui.theme.NoctaMuted
import androidx.compose.runtime.collectAsState

@Composable
fun NowPlayingScreen(
    vm: PlayerViewModel,
    onClose: () -> Unit,
    onOpenLyrics: () -> Unit,
    onOpenQueue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val song by vm.current.collectAsState()
    val isPlaying by vm.isPlaying.collectAsState()
    val duration by vm.duration.collectAsState()
    val shuffle by vm.shuffle.collectAsState()
    val repeatMode by vm.repeatMode.collectAsState()
    val lyricsState by vm.lyrics.collectAsState()
    val videoUri by vm.video.collectAsState()
    val queue by vm.queue.collectAsState()
    val likedIds by vm.settings.liked.collectAsState()
    val canvasEnabled by vm.settings.canvasEnabled.collectAsState()
    val motionEnabled by vm.settings.motionEnabled.collectAsState()

    val accent = LocalAccent.current
    val liked = song?.let { it.id in likedIds } == true
    val totalDuration = if (duration > 0) duration else song?.durationMs ?: 0L

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        PlayerBackdrop(accent)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                GlassIconButton(
                    icon = Icons.Rounded.KeyboardArrowDown,
                    onClick = onClose,
                    size = 42.dp,
                    iconSize = 26.dp,
                    contentDescription = "Close player"
                )
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "PLAYING FROM LIBRARY",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.48f)
                    )
                    Text(
                        text = song?.displayAlbum ?: "Nocta",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.88f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                GlassIconButton(
                    icon = Icons.AutoMirrored.Rounded.PlaylistPlay,
                    onClick = onOpenQueue,
                    size = 42.dp,
                    iconSize = 22.dp,
                    contentDescription = "Queue"
                )
            }

            Spacer(Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CanvasStage(
                    song = song,
                    videoUri = videoUri,
                    isPlaying = isPlaying,
                    videoEnabled = canvasEnabled,
                    motion = motionEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .padding(horizontal = 4.dp)
                )
            }

            Spacer(Modifier.height(18.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    MarqueeText(
                        text = song?.title ?: "Nothing playing",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        animate = isPlaying
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = song?.displayArtist ?: "Press play to start",
                        style = MaterialTheme.typography.bodyLarge,
                        color = NoctaMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(12.dp))
                GlassIconButton(
                    icon = if (liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    onClick = { vm.toggleLikeCurrent() },
                    size = 46.dp,
                    iconSize = 22.dp,
                    tint = if (liked) accent.primary else Color.White.copy(alpha = 0.8f),
                    contentDescription = "Like"
                )
            }

            Spacer(Modifier.height(12.dp))

            PlayerProgress(vm = vm, totalDuration = totalDuration)

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassIconButton(
                    icon = Icons.Rounded.Shuffle,
                    onClick = { vm.toggleShuffle() },
                    size = 42.dp,
                    iconSize = 20.dp,
                    tint = if (shuffle) accent.primary else Color.White.copy(alpha = 0.82f),
                    contentDescription = "Shuffle"
                )

                GlassIconButton(
                    icon = Icons.Rounded.SkipPrevious,
                    onClick = { vm.previous() },
                    size = 50.dp,
                    iconSize = 27.dp,
                    contentDescription = "Previous"
                )

                PlayButton(
                    isPlaying = isPlaying,
                    onClick = { vm.playPause() }
                )

                GlassIconButton(
                    icon = Icons.Rounded.SkipNext,
                    onClick = { vm.next() },
                    size = 50.dp,
                    iconSize = 27.dp,
                    contentDescription = "Next"
                )

                GlassIconButton(
                    icon = if (repeatMode == Player.REPEAT_MODE_ONE) {
                        Icons.Rounded.RepeatOne
                    } else {
                        Icons.Rounded.Repeat
                    },
                    onClick = { vm.cycleRepeat() },
                    size = 42.dp,
                    iconSize = 20.dp,
                    tint = if (repeatMode != Player.REPEAT_MODE_OFF) {
                        accent.primary
                    } else {
                        Color.White.copy(alpha = 0.82f)
                    },
                    contentDescription = "Repeat"
                )
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ActionPill(
                    icon = Icons.Rounded.Lyrics,
                    title = "Lyrics",
                    subtitle = when (val state = lyricsState) {
                        is LyricsState.Ready -> if (state.lyrics.synced) "Synced lyrics" else "Plain text"
                        LyricsState.Loading -> "Finding the words…"
                        LyricsState.NotFound -> "No lyrics found"
                        else -> "Tap to open"
                    },
                    modifier = Modifier.weight(1f),
                    onClick = onOpenLyrics
                )
                ActionPill(
                    icon = Icons.AutoMirrored.Rounded.QueueMusic,
                    title = "Up next",
                    subtitle = "${queue.size} in queue",
                    modifier = Modifier.weight(1f),
                    onClick = onOpenQueue
                )
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

/**
 * Ambient wash instead of a full-screen blurred cover. Blurring a full-screen bitmap costs
 * a lot of GPU on mid-range phones; two cached radial gradients give the same glow for free.
 */
@Composable
private fun PlayerBackdrop(accent: AccentPalette) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawWithCache {
                val topGlow = Brush.radialGradient(
                    colors = listOf(
                        accent.primary.copy(alpha = 0.44f),
                        accent.soft.copy(alpha = 0.13f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.84f, size.height * 0.05f),
                    radius = size.maxDimension * 0.78f
                )
                val lowGlow = Brush.radialGradient(
                    colors = listOf(
                        accent.soft.copy(alpha = 0.15f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.05f, size.height * 0.66f),
                    radius = size.maxDimension * 0.62f
                )
                onDrawBehind {
                    drawRect(Color.Black)
                    drawRect(topGlow)
                    drawRect(lowGlow)
                }
            }
    )
}

/** Seek bar + timestamps. Scoped so the 4 Hz position tick never recomposes the whole player. */
@Composable
private fun PlayerProgress(vm: PlayerViewModel, totalDuration: Long) {
    val position by vm.position.collectAsState()
    val progress = if (totalDuration > 0) {
        (position.toFloat() / totalDuration).coerceIn(0f, 1f)
    } else 0f

    LiquidSeekBar(
        progress = progress,
        onSeek = { fraction ->
            if (totalDuration > 0) vm.seekTo((fraction * totalDuration).toLong())
        }
    )

    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = formatTime(position),
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.62f)
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = "-${formatTime((totalDuration - position).coerceAtLeast(0))}",
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.42f)
        )
    }
}

@Composable
private fun PlayButton(isPlaying: Boolean, onClick: () -> Unit) {
    val accent = LocalAccent.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.93f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 520f),
        label = "playScale"
    )

    Box(
        modifier = Modifier
            .size(76.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = 22.dp,
                shape = CircleShape,
                ambientColor = accent.primary,
                spotColor = accent.primary
            )
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(accent.primary, accent.soft)
                )
            )
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            contentDescription = if (isPlaying) "Pause" else "Play",
            tint = Color.Black,
            modifier = Modifier.size(34.dp)
        )
    }
}

@Composable
private fun ActionPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    GlassSurface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.1.sp),
                    color = NoctaMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
