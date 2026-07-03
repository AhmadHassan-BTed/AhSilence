package com.bted.ahsilence.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Map our custom studio colors directly into the Material 3 Dark Scheme
private val StudioDarkColorScheme = darkColorScheme(
    primary = AmberAccent,
    onPrimary = OledBlack,
    secondary = AmberDimmed,
    background = OledBlack,
    surface = StudioSurface,
    onBackground = TextActive,
    onSurface = TextActive,
    surfaceVariant = GridLines,
    onSurfaceVariant = TextMuted
)

@Composable
fun AhSilenceTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = StudioDarkColorScheme
    val view = LocalView.current

    // This block seamlessly colors the Android Status Bar to match your OLED black background,
    // ensuring the immersive, full-screen hardware look isn't broken by a gray OS bar.
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()

            val windowInsetsController = WindowCompat.getInsetsController(window, view)
            windowInsetsController.isAppearanceLightStatusBars = false
            windowInsetsController.isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}