package com.UTP.linklisten.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight

@Composable
fun isHighContrastEnabled(): Boolean = LocalAccessibilityState.current.highContrast

@Composable
fun accessibleWeight(base: FontWeight = FontWeight.Normal): FontWeight {
    if (!isHighContrastEnabled()) return base
    return when (base) {
        FontWeight.Light,
        FontWeight.Normal,
        FontWeight.Medium -> FontWeight.Bold
        FontWeight.SemiBold -> FontWeight.Bold
        FontWeight.Bold -> FontWeight.ExtraBold
        else -> FontWeight.Black
    }
}

@Composable
fun accessibleTitleWeight(): FontWeight =
    if (isHighContrastEnabled()) FontWeight.Black else FontWeight.Bold

@Composable
fun accessibleSubtitleWeight(): FontWeight =
    if (isHighContrastEnabled()) FontWeight.Bold else FontWeight.Normal
