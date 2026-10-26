package com.example.mini_fadfed_android.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = FadfedPrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = FadfedSecondaryIndigo,
    onPrimaryContainer = Color.White,
    secondary = FadfedSecondaryIndigo,
    onSecondary = Color.White,
    secondaryContainer = FadfedSelectedIndigoBg,
    onSecondaryContainer = FadfedSecondaryIndigo,
    tertiary = FadfedYellowAccent,
    onTertiary = FadfedTextPrimary,
    background = Color(0xFF141720),
    onBackground = Color.White,
    surface = Color(0xFF1E2230),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF282D3D),
    onSurfaceVariant = FadfedMutedBlueGrey,
    outline = Color(0xFF3B4154),
    error = FadfedErrorRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = FadfedPrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = FadfedSelectedBlueBg,
    onPrimaryContainer = FadfedSecondaryIndigo,
    secondary = FadfedSecondaryIndigo,
    onSecondary = Color.White,
    secondaryContainer = FadfedSelectedIndigoBg,
    onSecondaryContainer = FadfedSecondaryIndigo,
    tertiary = FadfedYellowAccent,
    onTertiary = FadfedTextPrimary,
    background = FadfedPrimaryBackground,
    onBackground = FadfedTextPrimary,
    surface = FadfedSurface,
    onSurface = FadfedTextPrimary,
    surfaceVariant = FadfedPrimaryBackground,
    onSurfaceVariant = FadfedMutedBlueGrey,
    outline = FadfedBorder,
    outlineVariant = Color(0xFFE8ECF2),
    error = FadfedErrorRed,
    onError = Color.White,
    errorContainer = Color(0xFFFFECEC),
    onErrorContainer = FadfedErrorRed
)

@Composable
fun MinifadfedandroidTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}