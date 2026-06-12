package com.UTP.linklisten.ui.screens

import com.UTP.linklisten.model.ArticleContent

enum class HomeScreenMode {
    INPUT,
    PROCESSING,
    PLAYER
}

enum class ProcessingStage {
    EXTRACTING,
    CLEANING,
    GENERATING_AUDIO
}

data class HomeUiState(
    val urlText: String = "",
    val mode: HomeScreenMode = HomeScreenMode.INPUT,
    val processingStage: ProcessingStage = ProcessingStage.EXTRACTING,
    val article: ArticleContent? = null,
    val errorMessage: String? = null
)
