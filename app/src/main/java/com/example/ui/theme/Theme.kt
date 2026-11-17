package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class HeyBkPalette(
    val isDark: Boolean,
    val railBackground: Color,
    val masterPaneBackground: Color,
    val detailPaneBackground: Color,
    val cardSurface: Color,
    val elevatedTile: Color,
    val selectedCard: Color,
    val hairline: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val accentBlue: Color,
    val accentAmber: Color,
    val accentCoral: Color,
    val accentTeal: Color,
    val accentSage: Color,
    val fabContainer: Color,
    val fabContent: Color
)

private val DarkPalette = HeyBkPalette(
    isDark = true,
    railBackground = RailSurfaceDark,
    masterPaneBackground = MasterPaneSlate,
    detailPaneBackground = ObsidianCanvas,
    cardSurface = CardCharcoal,
    elevatedTile = ElevatedTileDark,
    selectedCard = SelectedCardSlate,
    hairline = HairlineDark,
    textPrimary = TextPrimaryDark,
    textSecondary = TextSecondaryDark,
    textMuted = TextMutedDark,
    accentBlue = SkyAccent,
    accentAmber = AmberGold,
    accentCoral = CoralRed,
    accentTeal = TealPin,
    accentSage = EmeraldSage,
    fabContainer = Color(0xFF283038),
    fabContent = TextPrimaryDark
)

private val LightPalette = HeyBkPalette(
    isDark = false,
    railBackground = RailSurfaceLight,
    masterPaneBackground = MasterPanePaper,
    detailPaneBackground = PaperCanvas,
    cardSurface = CardIvory,
    elevatedTile = ElevatedTileLight,
    selectedCard = SelectedCardLight,
    hairline = HairlineLight,
    textPrimary = TextPrimaryLight,
    textSecondary = TextSecondaryLight,
    textMuted = TextMutedLight,
    accentBlue = SkyAccentDeep,
    accentAmber = Color(0xFFD49600),
    accentCoral = Color(0xFFB85335),
    accentTeal = TealPin,
    accentSage = Color(0xFF3E8E63),
    fabContainer = Color(0xFF24323F),
    fabContent = Color.White
)

val LocalHeyBkPalette = staticCompositionLocalOf { DarkPalette }

private val HeyBkDarkColorScheme = darkColorScheme(
    primary = SkyAccent,
    onPrimary = ObsidianCanvas,
    primaryContainer = SelectedCardSlate,
    onPrimaryContainer = TextPrimaryDark,
    secondary = AmberGold,
    onSecondary = ObsidianCanvas,
    secondaryContainer = ElevatedTileDark,
    onSecondaryContainer = TextPrimaryDark,
    tertiary = CoralRed,
    background = ObsidianCanvas,
    onBackground = TextPrimaryDark,
    surface = MasterPaneSlate,
    onSurface = TextPrimaryDark,
    surfaceVariant = CardCharcoal,
    onSurfaceVariant = TextSecondaryDark,
    outline = HairlineDark
)

private val HeyBkLightColorScheme = lightColorScheme(
    primary = SkyAccentDeep,
    onPrimary = Color.White,
    primaryContainer = SelectedCardLight,
    onPrimaryContainer = TextPrimaryLight,
    secondary = Color(0xFFB85335),
    onSecondary = Color.White,
    secondaryContainer = ElevatedTileLight,
    onSecondaryContainer = TextPrimaryLight,
    tertiary = TealPin,
    background = PaperCanvas,
    onBackground = TextPrimaryLight,
    surface = MasterPanePaper,
    onSurface = TextPrimaryLight,
    surfaceVariant = CardIvory,
    onSurfaceVariant = TextSecondaryLight,
    outline = HairlineLight
)

@Composable
fun HeyBkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val palette = if (darkTheme) DarkPalette else LightPalette
    val colorScheme = if (darkTheme) HeyBkDarkColorScheme else HeyBkLightColorScheme

    CompositionLocalProvider(LocalHeyBkPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = HeyBkTypography,
            content = content
        )
    }
}

fun parseColorHex(hex: String, fallback: Color = SkyAccent): Color {
    return try {
        val clean = hex.removePrefix("#")
        val parsed = clean.toLong(16)
        when (clean.length) {
            6 -> Color(0xFF000000L or parsed)
            8 -> Color(parsed)
            else -> fallback
        }
    } catch (_: Exception) {
        fallback
    }
}
