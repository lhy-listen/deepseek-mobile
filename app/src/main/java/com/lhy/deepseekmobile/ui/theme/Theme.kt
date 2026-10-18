package com.lhy.deepseekmobile.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

@Immutable
data class DsPalette(
    val dark: Boolean,
    val accent: Accent,
    val bubbleTop: Color,
    val bubbleBottom: Color,
    val assistantSurface: Color,
    val glowA: Color,
    val glowB: Color,
    val hairline: Color,
)

private val DefaultPalette = DsPalette(
    dark = true,
    accent = Accent.Blue,
    bubbleTop = Color(0xFF6B8AFF),
    bubbleBottom = Color(0xFF9B7BFF),
    assistantSurface = InkSurface,
    glowA = Color(0x336B8AFF),
    glowB = Color(0x22A98BFF),
    hairline = Color(0x1FFFFFFF),
)

val LocalDsPalette = staticCompositionLocalOf { DefaultPalette }

private val DsShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp),
)

private fun blend(a: Color, b: Color, t: Float): Color = Color(
    red = a.red + (b.red - a.red) * t,
    green = a.green + (b.green - a.green) * t,
    blue = a.blue + (b.blue - a.blue) * t,
    alpha = a.alpha + (b.alpha - a.alpha) * t,
)

private fun darkScheme(accent: Accent, amoled: Boolean) = darkColorScheme(
    primary = accent.primary,
    onPrimary = Color(0xFF08101F),
    primaryContainer = accent.primary.copy(alpha = 0.18f).compositeOver(if (amoled) Color.Black else InkBg),
    onPrimaryContainer = blend(accent.primary, Color.White, 0.55f),
    inversePrimary = accent.primaryLight,
    secondary = accent.secondary,
    onSecondary = Color(0xFF100A1E),
    secondaryContainer = accent.secondary.copy(alpha = 0.16f).compositeOver(if (amoled) Color.Black else InkBg),
    onSecondaryContainer = blend(accent.secondary, Color.White, 0.55f),
    tertiary = accent.tertiary,
    onTertiary = Color(0xFF04140F),
    tertiaryContainer = accent.tertiary.copy(alpha = 0.16f).compositeOver(if (amoled) Color.Black else InkBg),
    onTertiaryContainer = blend(accent.tertiary, Color.White, 0.5f),
    background = if (amoled) Color(0xFF000000) else InkBg,
    onBackground = InkText,
    surface = if (amoled) Color(0xFF08090D) else InkSurface,
    onSurface = InkText,
    surfaceVariant = if (amoled) Color(0xFF111319) else InkSurfaceVariant,
    onSurfaceVariant = InkTextDim,
    surfaceTint = accent.primary,
    surfaceBright = if (amoled) Color(0xFF16181F) else Color(0xFF1B2130),
    surfaceDim = if (amoled) Color(0xFF05060A) else Color(0xFF0C0F16),
    surfaceContainer = if (amoled) Color(0xFF0B0D12) else InkSurfaceHigh,
    surfaceContainerHigh = if (amoled) Color(0xFF101218) else Color(0xFF1A2030),
    surfaceContainerHighest = if (amoled) Color(0xFF151821) else Color(0xFF232B3D),
    surfaceContainerLow = if (amoled) Color(0xFF07080C) else Color(0xFF0E1119),
    surfaceContainerLowest = if (amoled) Color(0xFF000000) else Color(0xFF080A0F),
    inverseSurface = InkText,
    inverseOnSurface = InkBg,
    error = DangerRed,
    onError = Color(0xFF2A0508),
    errorContainer = DangerRed.copy(alpha = 0.16f).compositeOver(if (amoled) Color.Black else InkBg),
    onErrorContainer = Color(0xFFFFC9CD),
    outline = if (amoled) Color(0xFF272D3B) else InkOutline,
    outlineVariant = if (amoled) Color(0xFF1A1F2A) else Color(0xFF212836),
    scrim = Color.Black,
)

private fun lightScheme(accent: Accent) = lightColorScheme(
    primary = accent.primaryLight,
    onPrimary = Color.White,
    primaryContainer = accent.primaryLight.copy(alpha = 0.12f).compositeOver(LightBg),
    onPrimaryContainer = blend(accent.primaryLight, Color.Black, 0.15f),
    inversePrimary = accent.primary,
    secondary = accent.secondaryLight,
    onSecondary = Color.White,
    secondaryContainer = accent.secondaryLight.copy(alpha = 0.12f).compositeOver(LightBg),
    onSecondaryContainer = blend(accent.secondaryLight, Color.Black, 0.2f),
    tertiary = accent.tertiaryLight,
    onTertiary = Color.White,
    tertiaryContainer = accent.tertiaryLight.copy(alpha = 0.12f).compositeOver(LightBg),
    onTertiaryContainer = blend(accent.tertiaryLight, Color.Black, 0.2f),
    background = LightBg,
    onBackground = LightText,
    surface = LightSurface,
    onSurface = LightText,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextDim,
    surfaceTint = accent.primaryLight,
    surfaceBright = Color.White,
    surfaceDim = Color(0xFFE3E8F1),
    surfaceContainer = Color(0xFFECF0F7),
    surfaceContainerHigh = Color(0xFFE5E9F2),
    surfaceContainerHighest = Color(0xFFDCE2EE),
    surfaceContainerLow = Color(0xFFF5F7FB),
    surfaceContainerLowest = Color.White,
    inverseSurface = Color(0xFF1B2030),
    inverseOnSurface = Color(0xFFF2F4FA),
    error = Color(0xFFC3303C),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD9),
    onErrorContainer = Color(0xFF410006),
    outline = LightOutline,
    outlineVariant = Color(0xFFE6EAF3),
    scrim = Color.Black,
)

@Composable
fun DsTheme(
    darkTheme: Boolean = androidx.compose.foundation.isSystemInDarkTheme(),
    accent: Accent = Accent.Blue,
    amoled: Boolean = false,
    fontScale: Float = 1f,
    content: @Composable () -> Unit,
) {
    val scheme = if (darkTheme) darkScheme(accent, amoled) else lightScheme(accent)
    val baseDensity = LocalDensity.current
    val palette = DsPalette(
        dark = darkTheme,
        accent = accent,
        bubbleTop = if (darkTheme) accent.primary else accent.primaryLight,
        bubbleBottom = if (darkTheme) accent.secondary else accent.secondaryLight,
        assistantSurface = scheme.surface,
        glowA = (if (darkTheme) accent.primary else accent.primaryLight).copy(alpha = if (darkTheme) 0.20f else 0.10f),
        glowB = (if (darkTheme) accent.secondary else accent.secondaryLight).copy(alpha = if (darkTheme) 0.14f else 0.08f),
        hairline = if (darkTheme) Color(0x14FFFFFF) else Color(0x0F000000),
    )
    CompositionLocalProvider(
        LocalDsPalette provides palette,
        LocalDensity provides Density(baseDensity.density, baseDensity.fontScale * fontScale.coerceIn(0.8f, 1.4f)),
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = DsTypography,
            shapes = DsShapes,
            content = content,
        )
    }
}
