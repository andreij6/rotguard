package com.rotguard.app.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.rotguard.app.storage.ThemeMode

private fun paletteToLightScheme(p: VerdantPalette) = lightColorScheme(
    primary = p.mossAccent,
    onPrimary = p.mossBtnInk,
    primaryContainer = p.sageTint,
    onPrimaryContainer = p.ink,
    secondary = p.terracotta,
    onSecondary = p.card,
    secondaryContainer = p.warmCard,
    onSecondaryContainer = p.ink,
    tertiary = p.sunGold,
    onTertiary = p.canopy,
    background = p.canvas,
    onBackground = p.ink,
    surface = p.card,
    onSurface = p.ink,
    surfaceVariant = p.warmCard,
    onSurfaceVariant = p.inkMuted,
    outline = p.cardBorder,
    outlineVariant = p.rule,
    error = p.terracotta,
    onError = p.card,
)

private fun paletteToDarkScheme(p: VerdantPalette) = darkColorScheme(
    primary = p.mossAccent,
    onPrimary = p.canvas,
    primaryContainer = p.sageTint,
    onPrimaryContainer = p.ink,
    secondary = p.terracotta,
    onSecondary = p.canvas,
    secondaryContainer = p.warmCard,
    onSecondaryContainer = p.ink,
    tertiary = p.sunGold,
    onTertiary = p.canopy,
    background = p.canvas,
    onBackground = p.ink,
    surface = p.card,
    onSurface = p.ink,
    surfaceVariant = p.warmCard,
    onSurfaceVariant = p.inkMuted,
    outline = p.cardBorder,
    outlineVariant = p.rule,
    error = p.terracotta,
    onError = p.ink,
)

private val VerdantTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.4).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.3).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.2).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp,
        lineHeight = 25.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.5.sp,
        lineHeight = 21.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.6.sp,
        fontFeatureSettings = "tnum",
    ),
)

@Composable
fun RotGuardTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val useDark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val palette = if (useDark) DarkVerdantPalette else LightVerdantPalette
    val colorScheme = if (useDark) paletteToDarkScheme(palette) else paletteToLightScheme(palette)

    CompositionLocalProvider(LocalVerdantPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = VerdantTypography,
            content = content,
        )
    }
}
