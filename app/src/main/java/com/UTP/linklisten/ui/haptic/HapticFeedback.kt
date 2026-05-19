package com.UTP.linklisten.ui.haptic

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.UTP.linklisten.ui.theme.LocalAccessibilityState

private val ButtonHaptic = HapticFeedbackType.LongPress
private val ToggleHaptic = HapticFeedbackType.TextHandleMove

@Composable
fun isHapticEnabled(): Boolean = LocalAccessibilityState.current.hapticEnabled

@Composable
fun performButtonHaptic() {
    if (isHapticEnabled()) {
        LocalHapticFeedback.current.performHapticFeedback(ButtonHaptic)
    }
}

@Composable
fun performToggleHaptic() {
    if (isHapticEnabled()) {
        LocalHapticFeedback.current.performHapticFeedback(ToggleHaptic)
    }
}

@Composable
fun performConfirmHaptic() {
    LocalHapticFeedback.current.performHapticFeedback(HapticFeedbackType.LongPress)
}

@Composable
fun hapticClick(onClick: () -> Unit): () -> Unit {
    val haptic = LocalHapticFeedback.current
    val enabled = isHapticEnabled()
    return remember(onClick, enabled, haptic) {
        {
            if (enabled) {
                haptic.performHapticFeedback(ButtonHaptic)
            }
            onClick()
        }
    }
}

@Composable
fun hapticToggleChange(onChange: (Boolean) -> Unit): (Boolean) -> Unit {
    val haptic = LocalHapticFeedback.current
    val hapticEnabled = isHapticEnabled()
    return remember(onChange, hapticEnabled, haptic) {
        { value ->
            if (hapticEnabled) {
                haptic.performHapticFeedback(ToggleHaptic)
            }
            onChange(value)
        }
    }
}
