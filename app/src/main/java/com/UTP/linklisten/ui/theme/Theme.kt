package com.UTP.linklisten.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

data class AccessibilityUiState(
    val darkMode: Boolean = false,
    val highContrast: Boolean = false,
    val fontSizeSp: Int = 16,
    val hapticEnabled: Boolean = true,
    val autoPlayEnabled: Boolean = false
)

val LocalAccessibilityState = staticCompositionLocalOf { AccessibilityUiState() }
val LocalFontScale = compositionLocalOf { 1f }

private val LightColorScheme = lightColorScheme(
    primary = CoralPrimary,
    onPrimary = Color.White,
    background = CreamBackground,
    onBackground = TextPrimary,
    surface = CardWhite,
    onSurface = TextPrimary,
    surfaceVariant = PreviewBoxBg,
    onSurfaceVariant = TextSecondary,
    outline = DividerLight
)

private val DarkColorScheme = darkColorScheme(
    primary = CoralPrimary,
    onPrimary = Color.White,
    background = CreamBackgroundDark,
    onBackground = TextPrimaryDark,
    surface = CardWhiteDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = PreviewBoxBgDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = Color(0xFF666666)
)

private val HighContrastLightScheme = lightColorScheme(
    primary = CoralPrimary,
    onPrimary = Color.White,
    background = HcLightBackground,
    onBackground = Color.Black,
    surface = CardWhite,
    onSurface = Color.Black,
    surfaceVariant = PreviewBoxBg,
    onSurfaceVariant = Color.Black,
    outline = HcLightBorder
)

private val HighContrastDarkScheme = darkColorScheme(
    primary = CoralPrimary,
    onPrimary = Color.White,
    background = CreamBackgroundDark,
    onBackground = TextPrimaryDark,
    surface = CardWhiteDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = PreviewBoxBgDark,
    onSurfaceVariant = TextPrimaryDark,
    outline = HcDarkBorder
)

private val HighContrastTypography = Typography(
    displayLarge = TextStyle(fontWeight = FontWeight.Black),
    displayMedium = TextStyle(fontWeight = FontWeight.Black),
    displaySmall = TextStyle(fontWeight = FontWeight.ExtraBold),
    headlineLarge = TextStyle(fontWeight = FontWeight.Black),
    headlineMedium = TextStyle(fontWeight = FontWeight.ExtraBold),
    headlineSmall = TextStyle(fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontWeight = FontWeight.Bold),
    titleSmall = TextStyle(fontWeight = FontWeight.Bold),
    bodyLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp),
    bodySmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 12.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Bold),
    labelMedium = TextStyle(fontWeight = FontWeight.Bold),
    labelSmall = TextStyle(fontWeight = FontWeight.Bold)
)

@Composable
fun LinkListenTheme(
    accessibility: AccessibilityUiState = AccessibilityUiState(),
    content: @Composable () -> Unit
) {
    val darkTheme = accessibility.darkMode
    val colorScheme = when {
        accessibility.highContrast && darkTheme -> HighContrastDarkScheme
        accessibility.highContrast -> HighContrastLightScheme
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val fontScale = accessibility.fontSizeSp / 16f
    val typography = if (accessibility.highContrast) HighContrastTypography else Typography

    CompositionLocalProvider(
        LocalAccessibilityState provides accessibility,
        LocalFontScale provides fontScale
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content
        )
    }
}

@Composable
fun scaledSp(base: Int): TextUnit = (base * LocalFontScale.current).sp

@Composable
fun accentColor(): Color = CoralPrimary

@Composable
fun highContrastBorderColor(): Color = MaterialTheme.colorScheme.outline

@Composable
fun isDarkModeEnabled(): Boolean = LocalAccessibilityState.current.darkMode

@Composable
fun peachIconBackground(): Color =
    if (isDarkModeEnabled()) PeachIconBgDark else PeachIconBg

@Composable
fun tealIconBackground(): Color =
    if (isDarkModeEnabled()) TealIconBgDark else TealIconBg

@Composable
fun iconBadgeTint(): Color = CoralPrimary

@Composable
fun noticeBackground(): Color =
    if (isDarkModeEnabled()) NoticeBackgroundDark else NoticeBackground

@Composable
fun noticeBorder(): Color =
    if (isDarkModeEnabled()) NoticeBorderDark else NoticeBorder
