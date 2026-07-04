package com.bted.ahsilence.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
fun GainSlider(
    amplitudePercentage: Float,
    onAmplitudeChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    // Resolve theme colors
    val surfaceColor = MaterialTheme.colorScheme.surface
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val accentColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .width(64.dp)
            .fillMaxHeight()
            .background(surfaceColor),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxHeight()
                .width(48.dp)
                .padding(vertical = 32.dp)
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()

                        val viewHeight = size.height.toFloat()

                        // Y-Axis is inverted in Canvas (0 is top, height is bottom)
                        val y = change.position.y.coerceIn(0f, viewHeight)
                        val percentage = (1f - (y / viewHeight)) * 100f

                        onAmplitudeChanged(percentage)
                    }
                }
        ) {
            val trackWidth = 2.dp.toPx()
            val faderHeight = 8.dp.toPx()
            val faderWidth = 32.dp.toPx()

            val centerX = size.width / 2f
            val bottomY = size.height
            val topY = 0f

            // 1. Draw EQ Tick Marks
            val tickCount = 10
            for (i in 0..tickCount) {
                val tickY = size.height * (i / tickCount.toFloat())
                val isCenter = i == tickCount / 2

                val tickLength = if (isCenter) 24.dp.toPx() else 12.dp.toPx()
                val tickThickness = if (isCenter) 2.dp.toPx() else 1.dp.toPx()

                drawLine(
                    color = trackColor,
                    start = Offset(centerX - (tickLength / 2f), tickY),
                    end = Offset(centerX + (tickLength / 2f), tickY),
                    strokeWidth = tickThickness
                )
            }

            // 2. Draw the Main Vertical Background Track
            drawLine(
                color = trackColor,
                start = Offset(centerX, bottomY),
                end = Offset(centerX, topY),
                strokeWidth = trackWidth,
                cap = StrokeCap.Round
            )

            // 3. Map the 0-100 Float to physical Y pixels
            val percentageDecimal = (amplitudePercentage / 100f).coerceIn(0f, 1f)
            val currentY = size.height - (size.height * percentageDecimal)

            // 4. Draw the Active Filled Track
            drawLine(
                color = accentColor.copy(alpha = 0.4f),
                start = Offset(centerX, bottomY),
                end = Offset(centerX, currentY),
                strokeWidth = trackWidth,
                cap = StrokeCap.Round
            )

            // 5. Draw the Fader Knob
            drawRect(
                color = accentColor,
                topLeft = Offset(centerX - (faderWidth / 2f), currentY - (faderHeight / 2f)),
                size = Size(faderWidth, faderHeight)
            )
        }
    }
}
