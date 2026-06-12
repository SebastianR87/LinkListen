package com.UTP.linklisten

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.UTP.linklisten.data.ArticleExtractor
import com.UTP.linklisten.navigation.Routes
import com.UTP.linklisten.ui.screens.AccessibilityScreen
import com.UTP.linklisten.ui.screens.HomeScreenMode
import com.UTP.linklisten.ui.screens.HomeScreen
import com.UTP.linklisten.ui.screens.HomeUiState
import com.UTP.linklisten.ui.screens.ProcessingStage
import com.UTP.linklisten.ui.theme.AccessibilityUiState
import com.UTP.linklisten.ui.theme.LinkListenTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LinkListenApp() {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    var accessibilityState by remember {
        mutableStateOf(AccessibilityUiState())
    }
    var homeState by remember {
        mutableStateOf(HomeUiState())
    }

    LinkListenTheme(accessibility = accessibilityState) {
        NavHost(
            navController = navController,
            startDestination = Routes.HOME
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    state = homeState,
                    autoPlayEnabled = accessibilityState.autoPlayEnabled,
                    onUrlChange = { value ->
                        homeState = homeState.copy(
                            urlText = value,
                            errorMessage = null,
                            mode = if (homeState.mode == HomeScreenMode.PLAYER) {
                                HomeScreenMode.INPUT
                            } else {
                                homeState.mode
                            }
                        )
                    },
                    onProcessUrl = {
                        scope.launch {
                            if (homeState.urlText.isBlank()) {
                                homeState = homeState.copy(
                                    errorMessage = "Ingresa un enlace para comenzar.",
                                    mode = HomeScreenMode.INPUT
                                )
                                return@launch
                            }

                            homeState = homeState.copy(
                                mode = HomeScreenMode.PROCESSING,
                                processingStage = ProcessingStage.EXTRACTING,
                                errorMessage = null,
                                article = null
                            )

                            val result = ArticleExtractor.extract(homeState.urlText)
                            homeState = result.fold(
                                onSuccess = { article ->
                                    homeState = homeState.copy(
                                        processingStage = ProcessingStage.CLEANING
                                    )
                                    delay(350)
                                    homeState = homeState.copy(
                                        processingStage = ProcessingStage.GENERATING_AUDIO
                                    )
                                    delay(300)
                                    homeState.copy(
                                        mode = HomeScreenMode.PLAYER,
                                        article = article,
                                        errorMessage = null
                                    )
                                },
                                onFailure = { error ->
                                    homeState.copy(
                                        mode = HomeScreenMode.INPUT,
                                        article = null,
                                        errorMessage = error.message ?: "No se pudo procesar el enlace."
                                    )
                                }
                            )
                        }
                    },
                    onReturnToInput = {
                        homeState = homeState.copy(
                            mode = HomeScreenMode.INPUT,
                            article = null,
                            errorMessage = null
                        )
                    },
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
