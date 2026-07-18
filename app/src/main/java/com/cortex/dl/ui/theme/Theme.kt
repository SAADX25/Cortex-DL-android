package com.cortex.dl.ui.theme

import android.os.Build
import android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalView

private val CortexColorScheme = darkColorScheme(
    primary = CortexCyan,
    onPrimary = CortexTextPrimary,
    secondary = CortexBlue,
    background = CortexDarkBackground,
    surface = CortexDarkBackground,
    onBackground = CortexTextPrimary,
    onSurface = CortexTextPrimary,
    outline = CortexSurfaceBorder
)

@Composable
fun CortexTheme(
    content: @Composable () -> Unit,
) {
    val view = LocalView.current

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.windowInsetsController?.setSystemBarsAppearance(
                0, // 0 clears light status bar, so text is light (for dark theme)
                APPEARANCE_LIGHT_STATUS_BARS,
            )
        }
    }

    MaterialTheme(
        colorScheme = CortexColorScheme,
        typography = Typography,
        content = content,
    )
}
