package com.UTP.linklisten.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.UTP.linklisten.ui.haptic.hapticClick
import com.UTP.linklisten.ui.theme.CoralPrimary
import com.UTP.linklisten.ui.theme.highContrastBorderColor
import com.UTP.linklisten.ui.theme.isHighContrastEnabled

@Composable
fun ElevatedIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    iconTint: Color = MaterialTheme.colorScheme.onSurface
) {
    val highContrast = isHighContrastEnabled()
    val shape = RoundedCornerShape(14.dp)
    val borderColor = highContrastBorderColor()
    val onClickWithHaptic = hapticClick(onClick)

    Surface(
        onClick = onClickWithHaptic,
        modifier = modifier
            .size(size)
            .then(
                if (highContrast) {
                    Modifier.border(2.dp, borderColor, shape)
                } else {
                    Modifier.shadow(4.dp, shape)
                }
            ),
        shape = shape,
        color = MaterialTheme.colorScheme.surface
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = iconTint
            )
        }
    }
}

@Composable
fun BrandLogo(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    val highContrast = isHighContrastEnabled()
    val shape = RoundedCornerShape(12.dp)

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(CoralPrimary)
            .then(
                if (highContrast) {
                    Modifier.border(2.dp, highContrastBorderColor(), shape)
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.VolumeUp,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(size * 0.55f)
        )
    }
}
