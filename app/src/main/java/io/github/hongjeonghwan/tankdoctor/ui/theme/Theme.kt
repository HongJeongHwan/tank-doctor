package io.github.hongjeonghwan.tankdoctor.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.dp
import io.github.hongjeonghwan.tankdoctor.R
import io.github.hongjeonghwan.tankdoctor.data.Level
import io.github.hongjeonghwan.tankdoctor.data.Severity

private val LightColors = lightColorScheme(
    primary = Color(0xFF0E6E6A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE3F2F0),
    onPrimaryContainer = Color(0xFF0B3B39),
    secondary = Color(0xFF4D5F5D),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE3F2F0),
    onSecondaryContainer = Color(0xFF10201F),
    tertiary = Color(0xFF9A4A0C),
    tertiaryContainer = Color(0xFFFDEEE3),
    onTertiaryContainer = Color(0xFF4A2405),
    background = Color(0xFFF4F7F6),
    onBackground = Color(0xFF10201F),
    surface = Color(0xFFF4F7F6),
    onSurface = Color(0xFF10201F),
    surfaceVariant = Color(0xFFE6EDEC),
    onSurfaceVariant = Color(0xFF5B6B69),
    // Cards sit on surfaceContainerLow, so it is the white of the design.
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFEEF3F2),
    surfaceContainerHigh = Color(0xFFE6EDEC),
    surfaceContainerHighest = Color(0xFFDFE7E5),
    outline = Color(0xFF7A8987),
    outlineVariant = Color(0xFFDCE5E3),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7FD3CC),
    onPrimary = Color(0xFF003734),
    primaryContainer = Color(0xFF183A38),
    onPrimaryContainer = Color(0xFFCDEBE8),
    secondary = Color(0xFFB0C4C1),
    onSecondary = Color(0xFF1B3432),
    secondaryContainer = Color(0xFF213634),
    onSecondaryContainer = Color(0xFFCDEBE8),
    tertiary = Color(0xFFFFB783),
    tertiaryContainer = Color(0xFF4A2A12),
    onTertiaryContainer = Color(0xFFFFDCC2),
    background = Color(0xFF0D1716),
    onBackground = Color(0xFFE2ECEA),
    surface = Color(0xFF0D1716),
    onSurface = Color(0xFFE2ECEA),
    surfaceVariant = Color(0xFF2A3A38),
    onSurfaceVariant = Color(0xFFA7B7B5),
    surfaceContainerLowest = Color(0xFF0A1312),
    surfaceContainerLow = Color(0xFF152120),
    surfaceContainer = Color(0xFF1A2726),
    surfaceContainerHigh = Color(0xFF22302F),
    surfaceContainerHighest = Color(0xFF2B3A38),
    outline = Color(0xFF889896),
    outlineVariant = Color(0xFF2A3A38),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

private val PlexSansKr = GoogleFont("IBM Plex Sans KR")

private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

// Fetched through Play services and cached; the system font shows until then, or if it cannot load.
private val AppFont = FontFamily(
    listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold)
        .map { Font(googleFont = PlexSansKr, fontProvider = provider, weight = it) }
)

private val AppTypography = Typography().run {
    fun TextStyle.app() = copy(fontFamily = AppFont)
    copy(
        displayLarge = displayLarge.app(), displayMedium = displayMedium.app(), displaySmall = displaySmall.app(),
        headlineLarge = headlineLarge.app(), headlineMedium = headlineMedium.app(), headlineSmall = headlineSmall.app(),
        titleLarge = titleLarge.app(), titleMedium = titleMedium.app(), titleSmall = titleSmall.app(),
        bodyLarge = bodyLarge.app(), bodyMedium = bodyMedium.app(), bodySmall = bodySmall.app(),
        labelLarge = labelLarge.app(), labelMedium = labelMedium.app(), labelSmall = labelSmall.app(),
    )
}

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun TankDoctorTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}

/** The deep teal of the home screen's 다음 환수 card; text on it is white. */
@Composable
fun heroColor(): Color = if (isSystemInDarkTheme()) Color(0xFF173F3C) else Color(0xFF0F4F4C)

/** Secondary text on [heroColor]. */
val HeroMuted = Color(0xFFCDEBE8)

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
