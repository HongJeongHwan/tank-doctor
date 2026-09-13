package io.github.hongjeonghwan.tankdoctor.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import io.github.hongjeonghwan.tankdoctor.data.Level
import io.github.hongjeonghwan.tankdoctor.data.Severity

private val LightColors = lightColorScheme(
    primary = Color(0xFF00696D),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF9CF1F4),
    onPrimaryContainer = Color(0xFF002021),
    secondary = Color(0xFF4A6364),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCCE8E8),
    onSecondaryContainer = Color(0xFF051F20),
    tertiary = Color(0xFF4C5F7C),
    background = Color(0xFFF4FBFA),
    onBackground = Color(0xFF161D1D),
    surface = Color(0xFFF4FBFA),
    onSurface = Color(0xFF161D1D),
    surfaceVariant = Color(0xFFDAE4E4),
    onSurfaceVariant = Color(0xFF3F4948),
    surfaceContainerLow = Color(0xFFEEF5F4),
    surfaceContainer = Color(0xFFE8EFEE),
    surfaceContainerHigh = Color(0xFFE3E9E9),
    outline = Color(0xFF6F7979),
    outlineVariant = Color(0xFFBEC9C8),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF80D4D8),
    onPrimary = Color(0xFF003739),
    primaryContainer = Color(0xFF004F52),
    onPrimaryContainer = Color(0xFF9CF1F4),
    secondary = Color(0xFFB0CCCC),
    onSecondary = Color(0xFF1B3435),
    secondaryContainer = Color(0xFF324B4C),
    onSecondaryContainer = Color(0xFFCCE8E8),
    tertiary = Color(0xFFB4C8E9),
    background = Color(0xFF0E1515),
    onBackground = Color(0xFFDDE4E3),
    surface = Color(0xFF0E1515),
    onSurface = Color(0xFFDDE4E3),
    surfaceVariant = Color(0xFF3F4948),
    onSurfaceVariant = Color(0xFFBEC9C8),
    surfaceContainerLow = Color(0xFF161D1D),
    surfaceContainer = Color(0xFF1A2121),
    surfaceContainerHigh = Color(0xFF252B2B),
    outline = Color(0xFF889392),
    outlineVariant = Color(0xFF3F4948),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

@Composable
fun TankDoctorTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}

@Composable
fun Level.color(): Color {
    val dark = isSystemInDarkTheme()
    return when (this) {
        Level.GOOD -> if (dark) Color(0xFF7DD88A) else Color(0xFF2E7D32)
        Level.CAUTION -> if (dark) Color(0xFFFFC266) else Color(0xFFB86E00)
        Level.DANGER -> if (dark) Color(0xFFFF8A80) else Color(0xFFC62828)
        Level.UNKNOWN -> MaterialTheme.colorScheme.outline
    }
}

@Composable
fun Severity.color(): Color = when (this) {
    Severity.HIGH -> Level.DANGER.color()
    Severity.MEDIUM -> Level.CAUTION.color()
    Severity.LOW -> MaterialTheme.colorScheme.secondary
}
