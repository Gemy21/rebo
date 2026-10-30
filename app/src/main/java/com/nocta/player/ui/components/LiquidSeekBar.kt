package com.nocta.player.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import com.nocta.player.ui.theme.LocalAccent

@Composable
fun LiquidSeekBar(
    progress: Float,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = LocalAccent.current
    var scrubbing by remember { mutableStateOf(false) }
    var scrubValue by remember { mutableFloatStateOf(progress.coerceIn(0f, 1f)) }
    var widthPx by remember { mutableFloatStateOf(1f) }

    if (!scrubbing) {
        scrubValue = progress.coerceIn(0f, 1f)
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(30.dp)
            .onSizeChanged { widthPx = it.width.toFloat().coerceAtLeast(1f) }
            .pointerInput(widthPx) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    scrubbing = true
                    scrubValue = (down.position.x / widthPx).coerceIn(0f, 1f)
                    down.consume()

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        if (!change.pressed) break
                        scrubValue = (change.position.x / widthPx).coerceIn(0f, 1f)
                        change.consume()
                    }

                    onSeek(scrubValue)
                    scrubbing = false
                }
            }
    ) {
        val centerY = size.height / 2f
        val trackHeight = 5.dp.toPx()
        val trackRadius = CornerRadius(trackHeight / 2f, trackHeight / 2f)
        val thumbX = size.width * scrubValue

        drawRoundRect(
            color = Color.White.copy(alpha = 0.10f),
            topLeft = Offset(0f, centerY - trackHeight / 2f),
            size = Size(size.width, trackHeight),
            cornerRadius = trackRadius
        )

        if (scrubValue > 0f) {
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(accent.primary, accent.soft)
                ),
                topLeft = Offset(0f, centerY - trackHeight / 2f),
                size = Size(thumbX.coerceAtLeast(trackHeight), trackHeight),
                cornerRadius = trackRadius
            )
        }

        drawCircle(
            color = accent.primary.copy(alpha = 0.26f),
            radius = if (scrubbing) 16.dp.toPx() else 12.dp.toPx(),
            center = Offset(thumbX, centerY)
        )
        drawCircle(
            color = Color.White,
            radius = if (scrubbing) 8.dp.toPx() else 6.dp.toPx(),
            center = Offset(thumbX, centerY)
        )
    }
}
