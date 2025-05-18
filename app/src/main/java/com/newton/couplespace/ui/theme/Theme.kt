package com.newton.couplespace.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Romantic color scheme for Bonded app
private val DarkColorScheme = darkColorScheme(
    primary = RomanticPink,
    onPrimary = Color.White,
    primaryContainer = RomanticPinkLight,
    onPrimaryContainer = RomanticPinkDark,
    secondary = RomanticPurple,
    onSecondary = Color.White,
    secondaryContainer = RomanticPurpleLight,
    onSecondaryContainer = RomanticPurpleDark,
    tertiary = RomanticRed,
    onTertiary = Color.White,
    tertiaryContainer = RomanticRedLight,
    onTertiaryContainer = RomanticRedDark,
    error = ErrorRed,
    background = DarkBackground,
    onBackground = Color.White,
    surface = DarkSurface,
    onSurface = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = RomanticPink,
    onPrimary = Color.White,
    primaryContainer = RomanticPinkLight,
    onPrimaryContainer = RomanticPinkDark,
    secondary = RomanticPurple,
    onSecondary = Color.White,
    secondaryContainer = RomanticPurpleLight,
    onSecondaryContainer = RomanticPurpleDark,
    tertiary = RomanticRed,
    onTertiary = Color.White,
    tertiaryContainer = RomanticRedLight,
    onTertiaryContainer = RomanticRedDark,
    error = ErrorRed,
    background = LightBackground,
    onBackground = Color(0xFF1C1B1F),
    surface = LightSurface,
    onSurface = Color(0xFF1C1B1F)
)

@Composable
fun BondedTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Disabled by default to use our custom romantic colors
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Use WindowCompat to handle system bars
            WindowCompat.setDecorFitsSystemWindows(window, false)
            // Set the status bar color using the recommended approach without using deprecated property
            window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
            // Configure the appearance of the status bar based on the theme
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}