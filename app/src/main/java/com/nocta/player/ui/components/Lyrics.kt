package com.nocta.player.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nocta.player.data.Lyrics
import com.nocta.player.data.LyricsState
import com.nocta.player.ui.theme.LocalAccent
import com.nocta.player.ui.theme.NoctaMuted
import kotlin.math.abs

@Composable
fun LyricsPanel(
    lyrics: Lyrics?,
    positionMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    baseSize: TextUnit = 25.sp,
    autoScroll: Boolean = true,
    karaoke: Boolean = true
) {
    if (lyrics == null) return

    val accent = LocalAccent.current
    val listState = rememberLazyListState()
    var viewportHeight by remember { mutableIntStateOf(0) }
    val activeIndex = if (lyrics.synced) lyrics.activeIndex(positionMs) else -1

    val density = LocalDensity.current
    val halfViewport = with(density) { (viewportHeight / 2).toDp() }

    LaunchedEffect(activeIndex, lyrics, viewportHeight) {
        if (!autoScroll || !lyrics.synced || activeIndex < 0 || viewportHeight <= 0) {
            return@LaunchedEffect
        }
        runCatching { listState.animateScrollToItem(activeIndex) }
    }

    Box(
        modifier = modifier.onSizeChanged { viewportHeight = it.height }
    ) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(
                top = (halfViewport - 40.dp).coerceAtLeast(0.dp),
                bottom = (halfViewport - 40.dp).coerceAtLeast(0.dp)
            ),
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(lyrics.lines) { index, line ->
                val isActive = index == activeIndex
                val distance = if (activeIndex >= 0) abs(index - activeIndex) else 0

                val nextTime = lyrics.lines.getOrNull(index + 1)?.timeMs ?: (line.timeMs + 4200)
                val span = (nextTime - line.timeMs).coerceAtLeast(600L)
                val rawProgress = if (lyrics.synced && isActive) {
                    ((positionMs - line.timeMs).toFloat() / span).coerceIn(0f, 1f)
                } else if (!lyrics.synced) 1f else 1f

                val animatedProgress by animateFloatAsState(
                    targetValue = if (isActive) rawProgress else 1f,
                    animationSpec = tween(240),
                    label = "lyricFill"
                )

                val alpha by animateFloatAsState(
                    targetValue = when {
                        !lyrics.synced -> 0.82f
                        isActive -> 1f
                        distance == 1 -> 0.46f
                        distance == 2 -> 0.28f
                        else -> 0.16f
                    },
                    animationSpec = tween(420),
                    label = "lyricAlpha"
                )

                val scale by animateFloatAsState(
                    targetValue = if (isActive) 1.04f else 1f,
                    animationSpec = spring(dampingRatio = 0.72f, stiffness = 320f),
                    label = "lyricScale"
                )

                LyricLineRow(
                    text = line.text,
                    active = isActive,
                    alpha = alpha,
                    scale = scale,
                    fill = if (karaoke) animatedProgress else 1f,
                    accent = accent.primary,
                    baseSize = baseSize,
                    clickable = lyrics.synced,
                    onClick = { if (lyrics.synced) onSeek(line.timeMs) }
                )
            }
        }

        // Soft edge fades keep the scrolling text liquid.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .height(90.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black, Color.Transparent)
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .height(110.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black)
                    )
                )
        )
    }
}

@Composable
private fun LyricLineRow(
    text: String,
    active: Boolean,
    alpha: Float,
    scale: Float,
    fill: Float,
    accent: Color,
    baseSize: TextUnit,
    clickable: Boolean,
    onClick: () -> Unit
) {
    val targetColor by animateColorAsState(
        targetValue = if (active) Color.White else Color.White.copy(alpha = alpha),
        animationSpec = tween(380),
        label = "lyricColor"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (clickable) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 26.dp, vertical = 9.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = TransformOrigin(0f, 0.5f)
                }
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = if (active) baseSize * 1.18f else baseSize,
                    lineHeight = (if (active) baseSize * 1.18f else baseSize) * 1.32f,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
                    textAlign = TextAlign.Start
                ),
                color = targetColor
            )
            if (active) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = baseSize * 1.18f,
                        lineHeight = baseSize * 1.18f * 1.32f,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Start
                    ),
                    color = accent,
                    modifier = Modifier.drawWithContent {
                        clipRect(right = size.width * fill) {
                            this@drawWithContent.drawContent()
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun LyricsStatus(
    state: LyricsState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = LocalAccent.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        when (state) {
            LyricsState.Loading -> ColumnCenter(
                title = "Finding the words",
                subtitle = "Matching this track with synced lyrics…",
                accent = accent.primary,
                loading = true
            )

            LyricsState.NotFound -> ColumnCenter(
                title = "No lyrics yet",
                subtitle = "Nothing matched in your sidecar files or LRCLIB.",
                accent = accent.primary,
                action = onRetry
            )

            is LyricsState.Failed -> ColumnCenter(
                title = "Lyrics unavailable",
                subtitle = state.message,
                accent = accent.primary,
                action = onRetry
            )

            else -> Unit
        }
    }
}

@Composable
private fun ColumnCenter(
    title: String,
    subtitle: String,
    accent: Color,
    loading: Boolean = false,
    action: (() -> Unit)? = null
) {
    androidx.compose.foundation.layout.Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (loading) {
            EqualizerBars(
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .width(26.dp)
                    .height(20.dp),
                color = accent,
                playing = true,
                barCount = 4
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = Color.White
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = NoctaMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp, start = 20.dp, end = 20.dp)
        )
        if (action != null) {
            Box(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .clip(RoundedCornerShape(50))
                    .background(accent.copy(alpha = 0.16f))
                    .clickable(onClick = action)
                    .padding(horizontal = 18.dp, vertical = 9.dp)
            ) {
                Text(
                    text = "Try again",
                    style = MaterialTheme.typography.labelLarge,
                    color = accent
                )
            }
        }
    }
}
