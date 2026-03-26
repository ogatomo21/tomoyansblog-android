package net.ogatomo.tomoyansblog.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF7086BD),
    onPrimary = Color.White,
    secondary = Color(0xFF7086BD),
    onSecondary = Color.White,
    tertiary = Color(0xFF237AEB),
    background = Color(0xFFF7F9FC),
    onBackground = Color(0xFF101828),
    surface = Color.White,
    onSurface = Color(0xFF101828),
    surfaceVariant = Color(0xFFE8EEF5),
    onSurfaceVariant = Color(0xFF4A5B6D)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF435071),
    onPrimary = Color.White,
    secondary = Color(0xFFFFB784),
    onSecondary = Color.Black,
    tertiary = Color(0xFF237AEB),
    background = Color(0xFF0E1720),
    onBackground = Color(0xFFF3F6FB),
    surface = Color(0xFF15212D),
    onSurface = Color(0xFFF3F6FB),
    surfaceVariant = Color(0xFF203142),
    onSurfaceVariant = Color(0xFFBDCAD7)
)

@Composable
fun TomoyansBlogTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
