package com.UTP.linklisten.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.UTP.linklisten.R
import com.UTP.linklisten.ui.components.BrandLogo
import com.UTP.linklisten.ui.components.ElevatedIconButton
import com.UTP.linklisten.ui.haptic.hapticClick
import com.UTP.linklisten.ui.theme.InputPlaceholder
import com.UTP.linklisten.ui.theme.LinkListenTheme
import com.UTP.linklisten.ui.theme.accessibleSubtitleWeight
import com.UTP.linklisten.ui.theme.accessibleTitleWeight
import com.UTP.linklisten.ui.theme.accessibleWeight
import com.UTP.linklisten.ui.theme.accentColor
import com.UTP.linklisten.ui.theme.highContrastBorderColor
import com.UTP.linklisten.ui.theme.isHighContrastEnabled
import com.UTP.linklisten.ui.theme.scaledSp

@Composable
fun HomeScreen(
    onOpenAccessibility: () -> Unit,
    modifier: Modifier = Modifier
) {
    var urlText by rememberSaveable { mutableStateOf("") }
    val highContrast = isHighContrastEnabled()
    val accent = accentColor()
    val borderColor = highContrastBorderColor()
    val onPlayClick = hapticClick { }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BrandLogo()
                    Text(
                        text = stringResource(R.string.app_name),
                        modifier = Modifier.padding(start = 12.dp),
                        fontSize = scaledSp(20),
                        fontWeight = accessibleTitleWeight(),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                ElevatedIconButton(
                    onClick = onOpenAccessibility,
                    icon = Icons.Default.Settings,
                    contentDescription = stringResource(R.string.settings)
                )
            }

            Spacer(modifier = Modifier.weight(0.35f))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.home_title),
                    fontSize = scaledSp(26),
                    fontWeight = accessibleTitleWeight(),
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    lineHeight = scaledSp(34)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.home_subtitle),
                    fontSize = scaledSp(15),
                    fontWeight = accessibleSubtitleWeight(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(32.dp))

                val fieldShape = RoundedCornerShape(20.dp)
                OutlinedTextField(
                    value = urlText,
                    onValueChange = { urlText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (highContrast) {
                                Modifier.border(2.dp, borderColor, fieldShape)
                            } else {
                                Modifier.shadow(6.dp, fieldShape)
                            }
                        ),
                    placeholder = {
                        Text(
                            text = stringResource(R.string.url_hint),
                            color = if (highContrast) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                InputPlaceholder
                            },
                            fontSize = scaledSp(14),
                            fontWeight = accessibleWeight()
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    singleLine = true,
                    shape = fieldShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = if (highContrast) borderColor else Color.Transparent,
                        unfocusedBorderColor = if (highContrast) borderColor else Color.Transparent
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                )

                Spacer(modifier = Modifier.height(20.dp))

                val buttonShape = RoundedCornerShape(18.dp)
                Button(
                    onClick = onPlayClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .then(
                            if (highContrast) {
                                Modifier.border(2.dp, borderColor, buttonShape)
                            } else {
                                Modifier.shadow(6.dp, buttonShape)
                            }
                        ),
                    shape = buttonShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accent,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = stringResource(R.string.play_audio),
                        modifier = Modifier.padding(start = 10.dp),
                        fontSize = scaledSp(16),
                        fontWeight = accessibleTitleWeight(),
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.weight(0.65f))

            Text(
                text = stringResource(R.string.home_footer),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                fontSize = scaledSp(13),
                fontWeight = accessibleSubtitleWeight(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun HomeScreenPreview() {
    LinkListenTheme {
        HomeScreen(onOpenAccessibility = {})
    }
}
