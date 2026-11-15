package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val RankForgeDarkColorScheme = darkColorScheme(
    primary = ForgeGold,
    onPrimary = Color(0xFF1F1300),
    primaryContainer = ForgeGoldContainerDark,
    onPrimaryContainer = ForgeGoldBright,
    secondary = ElectricCyan,
    onSecondary = Color(0xFF002438),
    secondaryContainer = ElectricCyanContainerDark,
    onSecondaryContainer = Color(0xFFBAE6FD),
    tertiary = VioletAccent,
    onTertiary = Color(0xFF13103D),
    background = ObsidianBg,
    onBackground = TextPrimaryDark,
    surface = ObsidianSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainerHighest = ObsidianSurfaceElevated,
    outline = ObsidianOutline,
    error = CrimsonError,
    onError = Color.White,
    errorContainer = CrimsonErrorContainerDark,
    onErrorContainer = Color(0xFFFECACA)
)

private val RankForgeLightColorScheme = lightColorScheme(
    primary = ForgeGoldDeep,
    onPrimary = Color.White,
    primaryContainer = ForgeGoldContainerLight,
    onPrimaryContainer = Color(0xFF451A03),
    secondary = ElectricCyanDeep,
    onSecondary = Color.White,
    secondaryContainer = ElectricCyanContainerLight,
    onSecondaryContainer = Color(0xFF0C4A6E),
    tertiary = VioletAccentDeep,
    onTertiary = Color.White,
    background = IvoryBg,
    onBackground = TextPrimaryLight,
    surface = IvorySurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = IvorySurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    surfaceContainerHighest = Color(0xFFE2E8F0),
    outline = IvoryOutline,
    error = CrimsonError,
    onError = Color.White,
    errorContainer = CrimsonErrorContainerLight,
    onErrorContainer = Color(0xFF7F1D1D)
)

val RankForgeShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun RankForgeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) RankForgeDarkColorScheme else RankForgeLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = RankForgeTypography,
        shapes = RankForgeShapes,
        content = content
    )
}
