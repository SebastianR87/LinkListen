package com.UTP.linklisten.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.UTP.linklisten.R
import com.UTP.linklisten.ui.components.ElevatedIconButton
import com.UTP.linklisten.ui.haptic.hapticClick
import com.UTP.linklisten.ui.haptic.hapticToggleChange
import com.UTP.linklisten.ui.theme.AccessibilityUiState
import com.UTP.linklisten.ui.theme.LinkListenTheme
import com.UTP.linklisten.ui.theme.CoralPrimary
import com.UTP.linklisten.ui.theme.accessibleSubtitleWeight
import com.UTP.linklisten.ui.theme.accessibleTitleWeight
import com.UTP.linklisten.ui.theme.accessibleWeight
import com.UTP.linklisten.ui.theme.accentColor
import com.UTP.linklisten.ui.theme.highContrastBorderColor
import com.UTP.linklisten.ui.theme.iconBadgeTint
import com.UTP.linklisten.ui.theme.isHighContrastEnabled
import com.UTP.linklisten.ui.theme.noticeBackground
import com.UTP.linklisten.ui.theme.noticeBorder
import com.UTP.linklisten.ui.theme.peachIconBackground
import com.UTP.linklisten.ui.theme.scaledSp
import com.UTP.linklisten.ui.theme.tealIconBackground

private val FontSizeOptions = listOf(14, 16, 18, 20, 24)

@Composable
fun AccessibilityScreen(
    state: AccessibilityUiState,
    onStateChange: (AccessibilityUiState) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val highContrast = isHighContrastEnabled()
    val accent = accentColor()
    val borderColor = highContrastBorderColor()
    val onDarkModeChange = hapticToggleChange { onStateChange(state.copy(darkMode = it)) }
    val onHighContrastChange = hapticToggleChange { onStateChange(state.copy(highContrast = it)) }
    val onAutoplayChange = hapticToggleChange { onStateChange(state.copy(autoPlayEnabled = it)) }
    val haptic = LocalHapticFeedback.current
    val onHapticChange = remember(state, onStateChange, haptic) {
        { enabled: Boolean ->
            onStateChange(state.copy(hapticEnabled = enabled))
            if (enabled) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ElevatedIconButton(
                    onClick = onBack,
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back)
                )
                Text(
                    text = stringResource(R.string.accessibility_title),
                    modifier = Modifier.padding(start = 16.dp),
                    fontSize = scaledSp(22),
                    fontWeight = accessibleTitleWeight(),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            SettingToggleCard(
                icon = Icons.Default.DarkMode,
                iconBackground = peachIconBackground(),
                title = stringResource(R.string.dark_mode),
                subtitle = stringResource(R.string.dark_mode_desc),
                checked = state.darkMode,
                onCheckedChange = onDarkModeChange,
                borderColor = borderColor,
                highContrast = highContrast
            )

            Spacer(modifier = Modifier.height(16.dp))

            FontSizeCard(
                selectedSize = state.fontSizeSp,
                onSizeSelected = { onStateChange(state.copy(fontSizeSp = it)) },
                accent = accent,
                borderColor = borderColor,
                highContrast = highContrast
            )

            Spacer(modifier = Modifier.height(16.dp))

            SettingToggleCard(
                icon = Icons.Default.Contrast,
                iconBackground = tealIconBackground(),
                title = stringResource(R.string.high_contrast),
                subtitle = stringResource(R.string.high_contrast_desc),
                checked = state.highContrast,
                onCheckedChange = onHighContrastChange,
                borderColor = borderColor,
                highContrast = highContrast
            )

            Spacer(modifier = Modifier.height(16.dp))

            SettingToggleCard(
                icon = Icons.Default.PhoneAndroid,
                iconBackground = peachIconBackground(),
                title = stringResource(R.string.haptic),
                subtitle = stringResource(R.string.haptic_desc),
                checked = state.hapticEnabled,
                onCheckedChange = onHapticChange,
                borderColor = borderColor,
                highContrast = highContrast
            )

            Spacer(modifier = Modifier.height(16.dp))

            AutoplayCard(
                checked = state.autoPlayEnabled,
                onCheckedChange = onAutoplayChange,
                borderColor = borderColor,
                highContrast = highContrast
            )

            Spacer(modifier = Modifier.height(16.dp))

            SettingsSavedNotice(highContrast = highContrast, borderColor = borderColor)

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingToggleCard(
    icon: ImageVector,
    iconBackground: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    borderColor: Color,
    highContrast: Boolean
) {
    SettingCard(highContrast = highContrast, borderColor = borderColor) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBadge(
                icon = icon,
                background = iconBackground,
                highContrast = highContrast,
                borderColor = borderColor
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp)
            ) {
                Text(
                    text = title,
                    fontSize = scaledSp(16),
                    fontWeight = accessibleTitleWeight(),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = scaledSp(13),
                    fontWeight = accessibleSubtitleWeight(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            AccessibilitySwitch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                highContrast = highContrast,
                borderColor = borderColor
            )
        }
    }
}

@Composable
private fun FontSizeCard(
    selectedSize: Int,
    onSizeSelected: (Int) -> Unit,
    accent: Color,
    borderColor: Color,
    highContrast: Boolean
) {
    SettingCard(highContrast = highContrast, borderColor = borderColor) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(
                icon = Icons.Default.TextFields,
                background = peachIconBackground(),
                highContrast = highContrast,
                borderColor = borderColor
            )
            Column(modifier = Modifier.padding(start = 14.dp)) {
                Text(
                    text = stringResource(R.string.font_size),
                    fontSize = scaledSp(16),
                    fontWeight = accessibleTitleWeight(),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.font_size_desc),
                    fontSize = scaledSp(13),
                    fontWeight = accessibleSubtitleWeight(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            FontSizeOptions.forEach { size ->
                FontSizeChip(
                    size = size,
                    selected = size == selectedSize,
                    onClick = hapticClick { onSizeSelected(size) },
                    accent = accent,
                    borderColor = borderColor,
                    highContrast = highContrast
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val previewModifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(
                if (highContrast) {
                    Modifier.border(2.dp, borderColor, RoundedCornerShape(14.dp))
                } else Modifier
            )
            .padding(16.dp)

        Box(modifier = previewModifier) {
            Text(
                text = stringResource(R.string.font_preview),
                fontSize = selectedSize.sp,
                fontWeight = accessibleWeight(FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = (selectedSize + 8).sp
            )
        }
    }
}

@Composable
private fun FontSizeChip(
    size: Int,
    selected: Boolean,
    onClick: () -> Unit,
    accent: Color,
    borderColor: Color,
    highContrast: Boolean
) {
    val shape = RoundedCornerShape(12.dp)
    val chipBorder = if (highContrast) 2.dp else 1.dp

    Box(
        modifier = Modifier
            .size(52.dp)
            .then(if (!highContrast) Modifier.shadow(if (selected) 0.dp else 2.dp, shape) else Modifier)
            .clip(shape)
            .background(
                when {
                    selected && highContrast -> accent
                    selected -> accent
                    highContrast -> MaterialTheme.colorScheme.surface
                    else -> MaterialTheme.colorScheme.surface
                }
            )
            .border(
                width = chipBorder,
                color = when {
                    highContrast -> borderColor
                    selected -> Color.Transparent
                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                },
                shape = shape
            )
            .clickable(onClick = onClick)
            .semantics { role = Role.Button },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = size.toString(),
            fontSize = scaledSp(15),
            fontWeight = accessibleWeight(if (selected) FontWeight.Bold else FontWeight.SemiBold),
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun AutoplayCard(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    borderColor: Color,
    highContrast: Boolean
) {
    SettingCard(highContrast = highContrast, borderColor = borderColor) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AutoplayIconBadge(
                highContrast = highContrast,
                borderColor = borderColor
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp)
            ) {
                Text(
                    text = stringResource(R.string.autoplay),
                    fontSize = scaledSp(16),
                    fontWeight = accessibleTitleWeight(),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.autoplay_desc),
                    fontSize = scaledSp(13),
                    fontWeight = accessibleSubtitleWeight(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            AccessibilitySwitch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                highContrast = highContrast,
                borderColor = borderColor
            )
        }
    }
}

@Composable
private fun AutoplayIconBadge(
    highContrast: Boolean,
    borderColor: Color
) {
    val outerShape = RoundedCornerShape(16.dp)
    val iconBg = peachIconBackground()
    Box(
        modifier = Modifier
            .width(52.dp)
            .height(56.dp)
            .clip(outerShape)
            .background(iconBg)
            .then(if (highContrast) Modifier.border(2.dp, borderColor, outerShape) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(50))
                .background(CoralPrimary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun SettingsSavedNotice(
    highContrast: Boolean,
    borderColor: Color
) {
    val shape = RoundedCornerShape(18.dp)
    val bg = noticeBackground()
    val border = if (highContrast) borderColor else noticeBorder()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(bg)
            .border(1.dp, border, shape)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.settings_saved_notice),
            fontSize = scaledSp(13),
            fontWeight = accessibleSubtitleWeight(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = scaledSp(20)
        )
    }
}

@Composable
private fun AccessibilitySwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    highContrast: Boolean,
    borderColor: Color
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = SwitchDefaults.colors(
            checkedTrackColor = CoralPrimary,
            checkedThumbColor = Color.White,
            uncheckedTrackColor = if (highContrast) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                Color(0xFFD0D0D0)
            },
            uncheckedThumbColor = Color.White,
            uncheckedBorderColor = if (highContrast) borderColor else Color(0xFFBDBDBD)
        )
    )
}

@Composable
private fun SettingCard(
    highContrast: Boolean,
    borderColor: Color,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (!highContrast) Modifier.shadow(4.dp, shape) else Modifier)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .then(
                if (highContrast) Modifier.border(2.dp, borderColor, shape) else Modifier
            )
            .padding(20.dp)
    ) {
        content()
    }
}

@Composable
private fun IconBadge(
    icon: ImageVector,
    background: Color,
    highContrast: Boolean,
    borderColor: Color
) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(shape)
            .background(background)
            .then(if (highContrast) Modifier.border(2.dp, borderColor, shape) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconBadgeTint(),
            modifier = Modifier.size(24.dp)
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun AccessibilityScreenPreview() {
    LinkListenTheme {
        AccessibilityScreen(
            state = AccessibilityUiState(hapticEnabled = true),
            onStateChange = {},
            onBack = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Alto contraste claro")
@Composable
private fun AccessibilityHighContrastPreview() {
    LinkListenTheme(accessibility = AccessibilityUiState(highContrast = true)) {
        AccessibilityScreen(
            state = AccessibilityUiState(highContrast = true),
            onStateChange = {},
            onBack = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Alto contraste oscuro")
@Composable
private fun AccessibilityHcDarkPreview() {
    LinkListenTheme(
        accessibility = AccessibilityUiState(darkMode = true, highContrast = true)
    ) {
        AccessibilityScreen(
            state = AccessibilityUiState(darkMode = true, highContrast = true),
            onStateChange = {},
            onBack = {}
        )
    }
}
