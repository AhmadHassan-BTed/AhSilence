package com.bted.ahsilence.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bted.ahsilence.domain.model.AcousticState
import com.bted.ahsilence.ui.component.CalibratorRing
import com.bted.ahsilence.ui.component.GainSlider

// ==========================================
// 1. THE UI CONTROLLER
// ==========================================

@Composable
fun DashboardScreen(
    state: AcousticState,
    onPhaseChanged: (Float) -> Unit,
    onAmplitudeChanged: (Float) -> Unit,
    onTogglePower: () -> Unit
) {
    // Local UI State: Toggles between Friendly Mode and Technical Mode
    var isProMode by remember { mutableStateOf(false) }

    // Resolve theme colors from MaterialTheme (defined once in Color.kt / Theme.kt)
    val bgOledBlack = MaterialTheme.colorScheme.background
    val surfaceDark = MaterialTheme.colorScheme.surface
    val gridBorder = MaterialTheme.colorScheme.surfaceVariant
    val textMuted = MaterialTheme.colorScheme.onSurfaceVariant
    val textActive = MaterialTheme.colorScheme.onBackground
    val accentNeon = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgOledBlack)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // --- TOP BAR: PRO TOGGLE ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { isProMode = !isProMode }
                    .border(
                        1.dp,
                        if (isProMode) accentNeon.copy(alpha = 0.5f) else Color.Transparent,
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "PRO",
                    color = if (isProMode) accentNeon else textMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- TOP SECTION: STATUS READOUT ---
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = if (state.isEmitting) {
                    if (isProMode) "ACTIVE HUM CANCELLATION" else "SILENCING ACTIVE"
                } else {
                    if (isProMode) "SYSTEM IDLE" else "READY"
                },
                color = if (state.isEmitting) accentNeon else textMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                fontFamily = FontFamily.Monospace
            )

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = if (state.detectedFrequencyHz > 0) {
                        if (isProMode) String.format("%.1f", state.detectedFrequencyHz) else "MAX"
                    } else {
                        "--"
                    },
                    color = textActive,
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = (-2).sp
                )

                if (isProMode) {
                    Text(
                        text = " Hz",
                        color = textMuted,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Light,
                        modifier = Modifier.padding(bottom = 12.dp, start = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        // --- MIDDLE SECTION: STUDIO RACK PANELS ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Panel 1: Vertical Slider
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(0.35f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(surfaceDark)
                    .border(1.dp, gridBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isProMode) "GAIN" else "VOLUME",
                        color = textMuted,
                        fontSize = 10.sp,
                        letterSpacing = 2.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    GainSlider(
                        amplitudePercentage = state.amplitudePercentage,
                        onAmplitudeChanged = onAmplitudeChanged,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Hide exact percentage from regular users
                    Text(
                        text = if (isProMode) "${state.amplitudePercentage.toInt()}%" else "",
                        color = textActive,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Panel 2: Phase Radar Calibrator
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(0.65f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(surfaceDark)
                    .border(1.dp, gridBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isProMode) "PHASE SHIFT" else "FINE TUNE",
                            color = textMuted,
                            fontSize = 10.sp,
                            letterSpacing = 2.sp,
                            fontFamily = FontFamily.Monospace
                        )

                        // Hide exact degrees from regular users
                        Text(
                            text = if (isProMode) "${state.phaseDegrees.toInt()}°" else "",
                            color = textActive,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    CalibratorRing(
                        phaseDegrees = state.phaseDegrees,
                        onPhaseChanged = onPhaseChanged,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // --- BOTTOM SECTION: POWER TOGGLE ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (state.isEmitting) accentNeon.copy(alpha = 0.1f) else Color.Transparent)
                .border(
                    BorderStroke(1.dp, if (state.isEmitting) accentNeon else gridBorder),
                    RoundedCornerShape(8.dp)
                )
                .clickable { onTogglePower() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (state.isEmitting) {
                    if (isProMode) "TERMINATE ENGINE" else "STOP"
                } else {
                    if (isProMode) "ENGAGE NULLING" else "START SILENCE"
                },
                color = if (state.isEmitting) accentNeon else textMuted,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
