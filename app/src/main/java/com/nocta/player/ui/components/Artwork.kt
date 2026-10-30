package com.nocta.player.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.nocta.player.data.ArtRequest
import com.nocta.player.data.Song
import com.nocta.player.data.formatTime
import com.nocta.player.ui.theme.LocalAccent
import com.nocta.player.ui.theme.NoctaMuted

@Composable
fun AlbumArt(
    song: Song?,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(14.dp)
) {
    val accent = LocalAccent.current
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        accent.primary.copy(alpha = 0.38f),
                        Color(0xFF131218),
                        accent.soft.copy(alpha = 0.22f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        if (song != null) {
            AsyncImage(
                model = ArtRequest(song),
                contentDescription = song.title,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.55f),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MarqueeText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    animate: Boolean = true
) {
    Text(
        text = text,
        style = style,
        color = color,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.then(
            if (animate) Modifier.basicMarquee(
                iterations = Int.MAX_VALUE,
                initialDelayMillis = 2400,
                velocity = 42.dp
            ) else Modifier
        )
    )
}

@Composable
fun SongRow(
    song: Song,
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
    showAlbum: Boolean = true,
    showArtwork: Boolean = true
) {
    val accent = LocalAccent.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (isPlaying) Color.White.copy(alpha = 0.055f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showArtwork) {
            AlbumArt(
                song = song,
                modifier = Modifier.size(52.dp),
                shape = RoundedCornerShape(13.dp)
            )
            Spacer(Modifier.width(13.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            MarqueeText(
                text = song.title,
                style = MaterialTheme.typography.titleMedium,
                color = if (isPlaying) accent.primary else Color.White,
                animate = isPlaying
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = if (showAlbum) {
                    "${song.displayArtist}  ·  ${song.displayAlbum}"
                } else {
                    song.displayArtist
                },
                style = MaterialTheme.typography.bodySmall,
                color = NoctaMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.width(10.dp))

        when {
            trailing != null -> trailing()
            isPlaying -> EqualizerBars(
                modifier = Modifier
                    .width(18.dp)
                    .height(16.dp),
                color = accent.primary,
                playing = true,
                barCount = 4
            )
            else -> Text(
                text = formatTime(song.durationMs),
                style = MaterialTheme.typography.labelMedium,
                color = NoctaMuted
            )
        }
    }
}

@Composable
fun EmptyState(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null
) {
    val accent = LocalAccent.current
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(78.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            accent.primary.copy(alpha = 0.30f),
                            accent.soft.copy(alpha = 0.14f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(30.dp)
            )
        }
        Spacer(Modifier.height(18.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = NoctaMuted,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(horizontal = 36.dp)
        )
        if (action != null) {
            Spacer(Modifier.height(18.dp))
            action()
        }
    }
}

@Composable
fun LabelPill(text: String, modifier: Modifier = Modifier, tint: Color = Color.White) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(tint.copy(alpha = 0.10f))
            .padding(horizontal = 9.dp, vertical = 4.dp)
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = tint.copy(alpha = 0.85f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
