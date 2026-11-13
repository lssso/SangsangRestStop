package com.sangsangreststop.app.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = SangsangRestStopBlue,
    secondary = Color(0xFFAAC7FF),
    tertiary = Color(0xFFFFB2C8),
    background = Color(0xFF10213A),
    surface = Color(0xFF172A45),
    onPrimary = Color.White,
    onBackground = Color(0xFFF1F5FC),
    onSurface = Color(0xFFF1F5FC),
)

private val LightColorScheme = lightColorScheme(
    primary = SangsangRestStopBlue,
    secondary = Color(0xFF55B97A),
    tertiary = Color(0xFFF56D9A),
    background = SangsangRestStopCream,
    surface = SangsangRestStopSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = SangsangRestStopNavy,
    onSurface = SangsangRestStopNavy,
    outline = SangsangRestStopOutline,
)

@Composable
fun SangsangRestStopTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
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
        content = content,
    )
}
