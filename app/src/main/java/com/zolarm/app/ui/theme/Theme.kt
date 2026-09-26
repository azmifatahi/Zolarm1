package com.zolarm.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val ZolarmDark = darkColorScheme(
    primary = NeonCyan, onPrimary = DarkBg,
    secondary = NeonPink, onSecondary = DarkBg,
    tertiary = NeonLime, background = DarkBg, onBackground = TextPrimary,
    surface = DarkSurface, onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceHi, onSurfaceVariant = TextSecondary, outline = TextSecondary
)
private val ZolarmLight = lightColorScheme(
    primary = DeepTeal, secondary = NeonPink, background = LightBg, surface = LightSurface
)

@Composable
fun ZolarmTheme(darkTheme: Boolean = true, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) ZolarmDark else ZolarmLight,
        typography = ZolarmTypography,
        content = content
    )
}
