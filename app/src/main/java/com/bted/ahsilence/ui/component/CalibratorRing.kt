package com.bted.ahsilence.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CalibratorRing(
    phaseDegrees: Float,
    onPhaseChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    // Resolve theme colors
    val surfaceColor = MaterialTheme.colorScheme.surface
    val gridColor = MaterialTheme.colorScheme.surfaceVariant
    val accentColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .aspectRatio(1f) // Forces a perfect square regardless of screen size
            .background(surfaceColor),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
                // Listen for touch gestures without triggering UI recompositions
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()

                        val center = Offset(size.width / 2f, size.height / 2f)
                        val touchOffset = change.position

                        // Calculate angle using trigonometry (0 degrees at 12 o'clock)
                        val dx = touchOffset.x - center.x
                        val dy = touchOffset.y - center.y
                        var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 90f

                        // Normalize to 0-360 strictly
                        if (angle < 0) angle += 360f
                        onPhaseChanged(angle)
                    }
                }
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxRadius = size.width / 2f

            // 1. Draw the Concentric Silence Grids (The "Soundwaves")
            val ringCount = 4
            for (i in 1..ringCount) {
                val radius = maxRadius * (i / ringCount.toFloat())
                drawCircle(
                    color = gridColor,
                    radius = radius,
                    center = center,
                    style = Stroke(width = 1.dp.toPx()) // Razor thin lines for minimalism
                )
            }

            // 2. Math Translation: Convert UI degrees back to Canvas Radians
            val mathAngleRads = Math.toRadians((phaseDegrees - 90f).toDouble())

            // Calculate the exact 2D coordinate of the user's phase selection
            val indicatorX = center.x + maxRadius * cos(mathAngleRads).toFloat()
            val indicatorY = center.y + maxRadius * sin(mathAngleRads).toFloat()
            val indicatorOffset = Offset(indicatorX, indicatorY)

            // 3. Draw the Phase Vector (The radar sweep line)
            drawLine(
                color = accentColor.copy(alpha = 0.5f),
                start = center,
                end = indicatorOffset,
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )

            // 4. Draw the Draggable Node (The Calibrator Knob)
            // Creates a hollow ring with a solid center dot
            drawCircle(
                color = surfaceColor, // Punch out the background
                radius = 14.dp.toPx(),
                center = indicatorOffset
            )
            drawCircle(
                color = accentColor,
                radius = 14.dp.toPx(),
                center = indicatorOffset,
                style = Stroke(width = 3.dp.toPx())
            )
            drawCircle(
                color = accentColor,
                radius = 4.dp.toPx(),
                center = indicatorOffset
            )

            // 5. Draw the Center Origin Node
            drawCircle(
                color = accentColor,
                radius = 6.dp.toPx(),
                center = center
            )
        }
    }
}
