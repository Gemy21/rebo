package com.nocta.player.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nocta.player.ui.theme.LocalAccent
import kotlin.math.PI
import kotlin.math.sin

/**
 * AMOLED backdrop: pure black with two soft light pools tinted by the current artwork.
 *
 * The wash is drawn once per size/accent change (not animated frame by frame). Keeping it
 * static removes a full-screen redraw every frame, which is the difference between smooth
 * scrolling and 40 fps on modest hardware.
 */
@Composable
fun NoctaBackground(
    modifier: Modifier = Modifier,
    intensity: Float = 1f
) {
    val accent = LocalAccent.current

    Box(
        modifier = modifier.drawWithCache {
            val topWash = Brush.radialGradient(
                colors = listOf(
                    accent.primary.copy(alpha = 0.34f * intensity),
                    accent.primary.copy(alpha = 0.10f * intensity),
                    Color.Transparent
                ),
                center = Offset(size.width * 0.16f, size.height * 0.04f),
                radius = size.maxDimension * 0.78f
            )
            val bottomWash = Brush.radialGradient(
                colors = listOf(
                    accent.soft.copy(alpha = 0.20f * intensity),
                    accent.soft.copy(alpha = 0.05f * intensity),
                    Color.Transparent
                ),
                center = Offset(size.width * 0.94f, size.height * 0.84f),
                radius = size.maxDimension * 0.7f
            )
            val vignette = Brush.radialGradient(
                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f)),
                center = Offset(size.width * 0.5f, size.height * 0.40f),
                radius = size.maxDimension * 0.82f
            )

            onDrawBehind {
                drawRect(Color.Black)
                drawRect(topWash)
                drawRect(bottomWash)
                drawRect(vignette)
            }
        }
    )
}

/**
 * Frosted AMOLED glass.
 *
 * The surface is built from layered translucency so the content behind it stays visible
 * through the pane (a clear "liquid glass" look) while still reading as glass:
 *  1. a light smoked backing — alpha scales with [darkness] and leaves the backdrop visible,
 *  2. a soft white specular wash that catches light from the top-left corner,
 *  3. a hairline rim that is brightest along the top edge, fades at the sides and catches a
 *     little light again at the base.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    tint: Color = Color.White,
    intensity: Float = 1f,
    /** How much the pane tint darkens the content behind it (0f = crystal clear, 1f = smoked). */
    darkness: Float = 0.26f,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val smoked = darkness.coerceIn(0f, 1f)
    val base = modifier
        .clip(shape)
        .background(
            Brush.verticalGradient(
                colors = listOf(
                    Color.Black.copy(alpha = smoked * 0.62f),
                    Color.Black.copy(alpha = smoked * 0.88f),
                    Color.Black.copy(alpha = smoked)
                )
            )
        )
        .background(
            Brush.linearGradient(
                colors = listOf(
                    tint.copy(alpha = 0.13f * intensity),
                    tint.copy(alpha = 0.045f * intensity),
                    tint.copy(alpha = 0.010f * intensity)
                ),
                start = Offset.Zero,
                end = Offset.Infinite
            )
        )
        .border(
            border = BorderStroke(
                1.dp,
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0f to tint.copy(alpha = 0.72f * intensity),
                        0.30f to tint.copy(alpha = 0.20f * intensity),
                        0.72f to tint.copy(alpha = 0.12f * intensity),
                        1f to tint.copy(alpha = 0.42f * intensity)
                    )
                )
            ),
            shape = shape
        )
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
        .padding(contentPadding)

    Box(modifier = base, content = content)
}

@Composable
fun GlassIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    size: Dp = 44.dp,
    iconSize: Dp = 20.dp,
    tint: Color = Color.White,
    enabled: Boolean = true
) {
    GlassSurface(
        modifier = modifier.size(size),
        shape = CircleShape,
        onClick = { if (enabled) onClick() }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) tint else tint.copy(alpha = 0.35f),
            modifier = Modifier
                .align(Alignment.Center)
                .size(iconSize)
        )
    }
}

@Composable
fun GlassChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = LocalAccent.current
    val shape = CircleShape
    Box(
        modifier = modifier
            .clip(shape)
            .then(
                if (selected) {
                    Modifier.background(
                        Brush.linearGradient(
                            listOf(
                                accent.primary.copy(alpha = 0.95f),
                                accent.soft.copy(alpha = 0.75f)
                            )
                        )
                    )
                } else {
                    Modifier
                        .background(Color.White.copy(alpha = 0.055f))
                        .border(
                            1.dp,
                            Brush.linearGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.18f),
                                    Color.White.copy(alpha = 0.03f)
                                )
                            ),
                            shape
                        )
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) Color.Black else Color.White.copy(alpha = 0.82f),
            maxLines = 1
        )
    }
}

@Composable
fun EqualizerBars(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    playing: Boolean = true,
    barCount: Int = 4
) {
    val transition = rememberInfiniteTransition(label = "eq")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = LinearEasing)
        ),
        label = "eqPhase"
    )

    Canvas(modifier = modifier) {
        val barWidth = size.width / (barCount * 2f - 1f)
        val gap = barWidth * 0.85f
        for (i in 0 until barCount) {
            val wave = if (playing) {
                (sin((phase * 2 * PI + i * 1.15)).toFloat() + 1f) / 2f
            } else {
                0.24f
            }
            val height = size.height * (0.14f + 0.86f * wave)
            drawRoundRect(
                color = color,
                topLeft = Offset(i * (barWidth + gap), size.height - height),
                size = Size(barWidth, height),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}
