package com.UTP.linklisten

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.UTP.linklisten.navigation.Routes
import com.UTP.linklisten.ui.screens.AccessibilityScreen
import com.UTP.linklisten.ui.screens.HomeScreen
import com.UTP.linklisten.ui.theme.AccessibilityUiState
import com.UTP.linklisten.ui.theme.LinkListenTheme

@Composable
fun LinkListenApp() {
    val navController = rememberNavController()
    var accessibilityState by remember {
        mutableStateOf(AccessibilityUiState())
    }

    LinkListenTheme(accessibility = accessibilityState) {
        NavHost(
            navController = navController,
            startDestination = Routes.HOME
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onOpenAccessibility = {
                        navController.navigate(Routes.ACCESSIBILITY)
                    }
                )
            }
            composable(Routes.ACCESSIBILITY) {
                AccessibilityScreen(
                    state = accessibilityState,
                    onStateChange = { accessibilityState = it },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
