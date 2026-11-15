package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val VeloraDarkColorScheme = darkColorScheme(
    primary = VeloraCrimson,
    onPrimary = Color.White,
    primaryContainer = VeloraCrimsonDark,
    onPrimaryContainer = Color.White,
    secondary = VeloraCoral,
    onSecondary = Color.White,
    secondaryContainer = VeloraSurfaceBright,
    onSecondaryContainer = VeloraOnSurface,
    tertiary = VeloraGold,
    onTertiary = Color.Black,
    background = VeloraBackground,
    onBackground = VeloraOnBackground,
    surface = VeloraSurface,
    onSurface = VeloraOnSurface,
    surfaceVariant = VeloraSurfaceElevated,
    onSurfaceVariant = VeloraOnSurfaceMuted,
    outline = VeloraOutline,
    error = VeloraError,
    onError = Color.Black
)

val VeloraShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun VeloraTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = VeloraDarkColorScheme,
        typography = VeloraTypography,
        shapes = VeloraShapes,
        content = content
    )
}
