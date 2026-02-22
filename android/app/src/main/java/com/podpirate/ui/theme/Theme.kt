package com.podpirate.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val Purple = Color(0xFF9333EA)
private val PurpleLight = Color(0xFFC084FC)
private val PurpleDark = Color(0xFF7E22CE)

private val DarkColorScheme = darkColorScheme(
    primary = Purple,
    secondary = PurpleLight,
    tertiary = PurpleDark,
    background = Color(0xFF111827),
    surface = Color(0xFF1F2937),
    onBackground = Color.White,
    onSurface = Color.White,
)

private val LightColorScheme = lightColorScheme(
    primary = Purple,
    secondary = PurpleDark,
    tertiary = PurpleLight,
)

@Composable
fun PodPirateTheme(
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
        content = content,
    )
}
