package com.nocta.player.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nocta.player.data.Song
import com.nocta.player.data.formatTime
import com.nocta.player.ui.theme.LocalAccent
import com.nocta.player.ui.theme.NoctaMuted

enum class MainTab(val label: String, val icon: ImageVector) {
    Library("Library", Icons.Rounded.LibraryMusic),
    Search("Search", Icons.Rounded.Search),
    Settings("Settings", Icons.Rounded.Tune)
}

@Composable
fun MiniPlayer(
    song: Song?,
    isPlaying: Boolean,
    position: State<Long>,
    duration: Long,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (song == null) return

    val accent = LocalAccent.current

    GlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp),
        shape = RoundedCornerShape(22.dp),
        tint = accent.primary,
        intensity = 1.15f,
        darkness = 0.38f,
        onClick = onOpen
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 8.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AlbumArt(
                song = song,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(Modifier.width(11.dp))

            androidx.compose.foundation.layout.Column(
                modifier = Modifier.weight(1f)
            ) {
                MarqueeText(
                    text = song.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White,
                    animate = isPlaying
                )
                Spacer(Modifier.height(1.dp))
                MarqueeText(
                    text = song.displayArtist,
                    style = MaterialTheme.typography.labelMedium,
                    color = NoctaMuted,
                    animate = false
                )
            }

            GlassIconButton(
                icon = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                onClick = onToggle,
                size = 46.dp,
                iconSize = 23.dp,
                contentDescription = if (isPlaying) "Pause" else "Play"
            )

            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onNext),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.SkipNext,
                    contentDescription = "Next",
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        MiniProgress(position = position, duration = duration, accent = accent)
    }
}

/** Only this sliver recomposes on every tick, instead of the whole mini player. */
@Composable
private fun BoxScope.MiniProgress(
    position: State<Long>,
    duration: Long,
    accent: com.nocta.player.ui.theme.AccentPalette
) {
    val fraction = if (duration > 0) {
        (position.value.toFloat() / duration).coerceIn(0f, 1f)
    } else 0f

    Box(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth(fraction)
            .height(2.dp)
            .background(
                Brush.horizontalGradient(listOf(accent.primary, accent.soft))
            )
    )
}

@Composable
fun BottomNav(
    selected: MainTab,
    onSelect: (MainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = LocalAccent.current

    GlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp),
        shape = RoundedCornerShape(26.dp),
        tint = accent.primary,
        intensity = 1.2f,
        darkness = 0.4f
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MainTab.entries.forEach { tab ->
                val active = tab == selected
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (active) accent.primary.copy(alpha = 0.18f) else Color.Transparent
                        )
                        .clickable { onSelect(tab) },
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.layout.Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            tint = if (active) accent.primary else Color.White.copy(alpha = 0.55f),
                            modifier = Modifier.size(21.dp)
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.5.sp,
                                lineHeight = 12.sp,
                                letterSpacing = 0.3.sp
                            ),
                            color = if (active) accent.primary else Color.White.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun miniDurationLabel(position: Long, duration: Long, modifier: Modifier = Modifier) {
    Text(
        text = "${formatTime(position)} / ${formatTime(duration)}",
        style = MaterialTheme.typography.labelSmall,
        color = NoctaMuted,
        modifier = modifier
    )
}

@Composable
fun FloatingToast(text: String, modifier: Modifier = Modifier) {
    GlassSurface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        intensity = 1.4f
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)
        )
    }
}
