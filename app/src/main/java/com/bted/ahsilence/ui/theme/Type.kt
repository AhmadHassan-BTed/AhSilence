package com.bted.ahsilence.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Typography = Typography(
    // Used for the massive "48 Hz" readout.
    // Negative letter spacing pulls the numbers tightly together like a digital clock.
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Light,
        fontSize = 72.sp,
        lineHeight = 72.sp,
        letterSpacing = (-2).sp,
        color = TextActive
    ),

    // Used for primary numerical readouts in the UI panels (e.g., "45°")
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp,
        color = TextActive
    ),

    // Used for standard text and secondary readouts
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
        color = TextActive
    ),

    // Used for all technical labels ("INPUT DELAY", "GAIN", "PHASE SHIFT")
    // Extreme wide letter spacing (3.sp) creates that premium, minimalist look.
    labelSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        lineHeight = 16.sp,
        letterSpacing = 3.sp,
        color = TextMuted
    )
)