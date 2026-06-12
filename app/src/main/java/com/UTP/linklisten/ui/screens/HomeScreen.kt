package com.UTP.linklisten.ui.screens

import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.UTP.linklisten.R
import com.UTP.linklisten.model.ArticleContent
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
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val PlaybackSpeeds = listOf(0.85f, 1.0f, 1.15f, 1.3f)

@Composable
fun HomeScreen(
    state: HomeUiState,
    autoPlayEnabled: Boolean,
    onUrlChange: (String) -> Unit,
    onProcessUrl: () -> Unit,
    onReturnToInput: () -> Unit,
    onOpenAccessibility: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val highContrast = isHighContrastEnabled()
    val borderColor = highContrastBorderColor()
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val scope = rememberCoroutineScope()
    var textToSpeech by remember { mutableStateOf<TextToSpeech?>(null) }
    var ttsReady by remember { mutableStateOf(false) }
    var isSpeaking by remember { mutableStateOf(false) }
    var speechRate by remember { mutableFloatStateOf(1.0f) }
    var currentSegmentIndex by remember(state.article?.sourceUrl) { mutableIntStateOf(0) }
    var pendingAutoplay by remember(state.article?.sourceUrl) { mutableStateOf(false) }
    var showFullText by remember(state.article?.sourceUrl) { mutableStateOf(false) }
    var playbackJob by remember { mutableStateOf<Job?>(null) }
    var activeUtteranceId by remember { mutableStateOf<String?>(null) }
    var activeUtteranceCompletion by remember { mutableStateOf<CompletableDeferred<Boolean>?>(null) }

    val article = state.article
    val segments = remember(article?.sourceUrl, article?.body, article?.summary) {
        article?.readingSegments.orEmpty()
    }
    val segmentsState = rememberUpdatedState(segments)

    fun segmentIndexFromUtteranceId(utteranceId: String?): Int? {
        if (utteranceId.isNullOrBlank()) return null
        val prefix = "segment-"
        if (!utteranceId.startsWith(prefix)) return null
        return utteranceId.removePrefix(prefix).substringBefore('-').toIntOrNull()
    }

    fun stopPlayback() {
        playbackJob?.cancel()
        playbackJob = null
        activeUtteranceCompletion?.cancel()
        activeUtteranceCompletion = null
        activeUtteranceId = null
        textToSpeech?.stop()
        isSpeaking = false
    }

    suspend fun speakSegmentAndAwait(index: Int): Boolean {
        val currentSegments = segmentsState.value
        if (!ttsReady || article == null || currentSegments.isEmpty()) {
            pendingAutoplay = true
            return false
        }

        val safeIndex = index.coerceIn(0, currentSegments.lastIndex)
        currentSegmentIndex = safeIndex
        val utteranceId = "segment-$safeIndex-${System.nanoTime()}"
        val completion = CompletableDeferred<Boolean>()
        activeUtteranceId = utteranceId
        activeUtteranceCompletion = completion
        val result = textToSpeech?.speak(
            currentSegments[safeIndex],
            TextToSpeech.QUEUE_FLUSH,
            null,
            utteranceId
        ) ?: TextToSpeech.ERROR

        if (result == TextToSpeech.ERROR) {
            isSpeaking = false
            pendingAutoplay = false
            activeUtteranceCompletion = null
            activeUtteranceId = null
            return false
        }

        return try {
            completion.await()
        } finally {
            if (activeUtteranceId == utteranceId) {
                activeUtteranceCompletion = null
                activeUtteranceId = null
            }
        }
    }

    fun startPlayback(index: Int) {
        val currentSegments = segmentsState.value
        if (!ttsReady || article == null || currentSegments.isEmpty()) {
            pendingAutoplay = true
            return
        }

        stopPlayback()
        val safeIndex = index.coerceIn(0, currentSegments.lastIndex)
        playbackJob = scope.launch {
            try {
                for (segmentIndex in safeIndex..currentSegments.lastIndex) {
                    val shouldContinue = speakSegmentAndAwait(segmentIndex)
                    if (!shouldContinue) break
                }
            } catch (_: CancellationException) {
                // Playback was interrupted intentionally.
            } finally {
                activeUtteranceCompletion = null
                activeUtteranceId = null
                playbackJob = null
                isSpeaking = false
            }
        }
    }

    fun togglePlayback() {
        if (isSpeaking) {
            stopPlayback()
        } else {
            pendingAutoplay = false
            startPlayback(currentSegmentIndex)
        }
    }

    fun skipToSegment(offset: Int) {
        val currentSegments = segmentsState.value
        if (currentSegments.isEmpty()) return

        val targetIndex = (currentSegmentIndex + offset).coerceIn(0, currentSegments.lastIndex)
        if (targetIndex == currentSegmentIndex) return

        pendingAutoplay = false
        if (isSpeaking) {
            startPlayback(targetIndex)
        } else {
            stopPlayback()
            currentSegmentIndex = targetIndex
        }
    }

    DisposableEffect(context) {
        var engine: TextToSpeech? = null
        engine = TextToSpeech(context) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
            if (ttsReady) {
                engine?.language = Locale.forLanguageTag("es-ES")
                engine?.setSpeechRate(speechRate)
            }
        }.also { tts ->
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    mainHandler.post {
                        segmentIndexFromUtteranceId(utteranceId)?.let { index ->
                            currentSegmentIndex = index
                        }
                        isSpeaking = true
                    }
                }

                override fun onDone(utteranceId: String?) {
                    if (utteranceId == activeUtteranceId) {
                        activeUtteranceCompletion?.complete(true)
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    if (utteranceId == activeUtteranceId) {
                        activeUtteranceCompletion?.complete(false)
                    }
                }
            })
        }
        textToSpeech = engine

        onDispose {
            engine?.stop()
            engine?.shutdown()
            textToSpeech = null
            isSpeaking = false
            pendingAutoplay = false
        }
    }

    LaunchedEffect(speechRate, ttsReady) {
        if (ttsReady) {
            textToSpeech?.setSpeechRate(speechRate)
        }
    }

    LaunchedEffect(ttsReady, pendingAutoplay, state.mode, article?.sourceUrl) {
        if (ttsReady && pendingAutoplay && state.mode == HomeScreenMode.PLAYER && article != null) {
            pendingAutoplay = false
            startPlayback(currentSegmentIndex)
        }
    }

    LaunchedEffect(state.mode, article?.sourceUrl, autoPlayEnabled) {
        if (state.mode == HomeScreenMode.PLAYER && article != null && autoPlayEnabled) {
            currentSegmentIndex = 0
            pendingAutoplay = true
        }
    }

    when (state.mode) {
        HomeScreenMode.INPUT -> InputScreen(
            state = state,
            onUrlChange = onUrlChange,
            onProcessUrl = hapticClick(onProcessUrl),
            onOpenAccessibility = onOpenAccessibility,
            modifier = modifier
        )

        HomeScreenMode.PROCESSING -> ProcessingScreen(
            stage = state.processingStage,
            onBack = onReturnToInput,
            modifier = modifier
        )

        HomeScreenMode.PLAYER -> PlayerScreen(
            article = article,
            guidedSegments = segments,
            currentSegmentIndex = currentSegmentIndex,
            totalSegments = segments.size,
            isSpeaking = isSpeaking,
            isTtsReady = ttsReady,
            speechRate = speechRate,
            showFullText = showFullText,
            onBack = {
                stopPlayback()
                onReturnToInput()
            },
            onTogglePlayback = hapticClick(::togglePlayback),
            onPreviousSegment = hapticClick { skipToSegment(-1) },
            onNextSegment = hapticClick { skipToSegment(1) },
            onChangeSpeed = { newRate ->
                speechRate = newRate
                if (ttsReady) {
                    textToSpeech?.setSpeechRate(newRate)
                    if (isSpeaking) {
                        startPlayback(currentSegmentIndex)
                    }
                }
            },
            onToggleTextMode = hapticClick { showFullText = !showFullText },
            modifier = modifier
        )
    }
}

@Composable
private fun InputScreen(
    state: HomeUiState,
    onUrlChange: (String) -> Unit,
    onProcessUrl: () -> Unit,
    onOpenAccessibility: () -> Unit,
    modifier: Modifier = Modifier
) {
    val highContrast = isHighContrastEnabled()
    val accent = accentColor()
    val borderColor = highContrastBorderColor()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
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

            Spacer(modifier = Modifier.height(36.dp))

            Text(
                text = stringResource(R.string.home_title),
                fontSize = scaledSp(27),
                fontWeight = accessibleTitleWeight(),
                color = MaterialTheme.colorScheme.onBackground,
                lineHeight = scaledSp(34)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.home_subtitle),
                fontSize = scaledSp(15),
                fontWeight = accessibleSubtitleWeight(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = scaledSp(24)
            )

            Spacer(modifier = Modifier.height(28.dp))

            val fieldShape = RoundedCornerShape(20.dp)
            OutlinedTextField(
                value = state.urlText,
                onValueChange = onUrlChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (highContrast) {
                            Modifier.border(2.dp, borderColor, fieldShape)
                        } else {
                            Modifier.shadow(6.dp, fieldShape)
                        }
                    ),
                label = {
                    Text(
                        text = stringResource(R.string.link_field_label),
                        fontSize = scaledSp(14),
                        fontWeight = accessibleWeight()
                    )
                },
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
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Go
                ),
                keyboardActions = KeyboardActions(onGo = { onProcessUrl() })
            )

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = onProcessUrl,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accent)
            ) {
                Icon(
                        imageVector = Icons.AutoMirrored.Filled.Article,
                    contentDescription = null,
                    tint = Color.White
                )
                Text(
                    text = stringResource(R.string.play_audio),
                    modifier = Modifier.padding(start = 10.dp),
                    color = Color.White,
                    fontSize = scaledSp(16),
                    fontWeight = accessibleTitleWeight()
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            state.errorMessage?.takeIf { it.isNotBlank() }?.let { message ->
                InfoCard(
                    icon = Icons.AutoMirrored.Filled.Article,
                    title = stringResource(R.string.link_error_title),
                    message = message,
                    highlightColor = accent
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = stringResource(R.string.home_footer),
                modifier = Modifier.fillMaxWidth(),
                fontSize = scaledSp(13),
                fontWeight = accessibleSubtitleWeight(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ProcessingScreen(
    stage: ProcessingStage,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = accentColor()
    val steps = listOf(
        Triple(
            ProcessingStage.EXTRACTING,
            stringResource(R.string.processing_step_extract),
            Icons.AutoMirrored.Filled.Article
        ),
        Triple(
            ProcessingStage.CLEANING,
            stringResource(R.string.processing_step_clean),
            Icons.Default.Tune
        ),
        Triple(
            ProcessingStage.GENERATING_AUDIO,
            stringResource(R.string.processing_step_audio),
            Icons.Default.PlayArrow
        )
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                ElevatedIconButton(
                    onClick = onBack,
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Box(contentAlignment = Alignment.Center) {
                CircularProgressDecoration(accent = accent)
                Box(
                    modifier = Modifier
                        .size(98.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(accent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(52.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = stringResource(R.string.processing_link),
                fontSize = scaledSp(24),
                fontWeight = accessibleTitleWeight(),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.processing_desc),
                fontSize = scaledSp(15),
                fontWeight = accessibleSubtitleWeight(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = scaledSp(22)
            )

            Spacer(modifier = Modifier.height(28.dp))

            steps.forEach { (step, label, icon) ->
                val currentOrder = stage.ordinal
                val stepOrder = step.ordinal
                ProcessingStepCard(
                    icon = icon,
                    title = label,
                    isDone = stepOrder < currentOrder,
                    isCurrent = stepOrder == currentOrder
                )
                Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }
}

@Composable
private fun PlayerScreen(
    article: ArticleContent?,
    guidedSegments: List<String>,
    currentSegmentIndex: Int,
    totalSegments: Int,
    isSpeaking: Boolean,
    isTtsReady: Boolean,
    speechRate: Float,
    showFullText: Boolean,
    onBack: () -> Unit,
    onTogglePlayback: () -> Unit,
    onPreviousSegment: () -> Unit,
    onNextSegment: () -> Unit,
    onChangeSpeed: (Float) -> Unit,
    onToggleTextMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (article == null) return

    val progress = if (totalSegments <= 1) 0f else {
        currentSegmentIndex.toFloat() / (totalSegments - 1).toFloat()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ElevatedIconButton(
                    onClick = onBack,
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back)
                )
                Text(
                    text = stringResource(R.string.player_screen_title),
                    fontSize = scaledSp(17),
                    fontWeight = accessibleTitleWeight(),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.width(48.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = article.title,
                fontSize = scaledSp(25),
                fontWeight = accessibleTitleWeight(),
                color = MaterialTheme.colorScheme.onBackground,
                lineHeight = scaledSp(33)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = article.sourceName,
                fontSize = scaledSp(14),
                fontWeight = accessibleSubtitleWeight(),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(50)),
                color = accentColor(),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = stringResource(
                    R.string.player_progress,
                    currentSegmentIndex + 1,
                    totalSegments.coerceAtLeast(1)
                ),
                fontSize = scaledSp(13),
                fontWeight = accessibleSubtitleWeight(),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ControlButton(
                    icon = Icons.Default.SkipPrevious,
                    label = stringResource(R.string.previous_fragment),
                    onClick = onPreviousSegment,
                    highlighted = false
                )
                PrimaryControlButton(
                    icon = if (isSpeaking) Icons.Default.Pause else Icons.Default.PlayArrow,
                    label = if (isSpeaking) {
                        stringResource(R.string.pause_audio)
                    } else {
                        stringResource(R.string.listen_article)
                    },
                    onClick = onTogglePlayback
                )
                ControlButton(
                    icon = Icons.Default.SkipNext,
                    label = stringResource(R.string.next_fragment),
                    onClick = onNextSegment,
                    highlighted = false
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = if (isSpeaking) {
                    stringResource(R.string.listening_now)
                } else if (isTtsReady) {
                    stringResource(R.string.ready_to_listen)
                } else {
                    stringResource(R.string.tts_not_ready)
                },
                modifier = Modifier.fillMaxWidth(),
                fontSize = scaledSp(14),
                fontWeight = accessibleSubtitleWeight(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(18.dp))

            SpeedSelector(
                selectedSpeed = speechRate,
                onSelectSpeed = onChangeSpeed
            )

            Spacer(modifier = Modifier.height(18.dp))

            Surface(
                onClick = onToggleTextMode,
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Article,
                        contentDescription = null,
                        tint = accentColor()
                    )
                    Text(
                        text = if (showFullText) {
                            stringResource(R.string.hide_full_text)
                        } else {
                            stringResource(R.string.show_full_text)
                        },
                        modifier = Modifier.padding(start = 8.dp),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = scaledSp(14),
                        fontWeight = accessibleTitleWeight()
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            GuidedTextCard(
                title = stringResource(R.string.current_fragment),
                segments = guidedSegments,
                currentSegmentIndex = currentSegmentIndex
            )

            if (showFullText) {
                Spacer(modifier = Modifier.height(16.dp))
                ReadingTextCard(
                    title = stringResource(R.string.result_body),
                    body = article.body
                )
            }
        }
    }
}

@Composable
private fun GuidedTextCard(
    title: String,
    segments: List<String>,
    currentSegmentIndex: Int
) {
    val accent = accentColor()
    val scrollState = rememberScrollState()
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val textModel = remember(segments) { buildGuidedTextModel(segments) }
    val highlightedText = remember(textModel, currentSegmentIndex, accent) {
        buildAnnotatedString {
            append(textModel.fullText)
            textModel.segmentRanges.getOrNull(currentSegmentIndex)?.let { range ->
                addStyle(
                    style = SpanStyle(background = accent.copy(alpha = 0.18f)),
                    start = range.first,
                    end = range.last + 1
                )
            }
        }
    }

    LaunchedEffect(currentSegmentIndex, textModel, layoutResult) {
        val layout = layoutResult ?: return@LaunchedEffect
        val range = textModel.segmentRanges.getOrNull(currentSegmentIndex) ?: return@LaunchedEffect
        val lineIndex = layout.getLineForOffset(range.first)
        val top = layout.getLineTop(lineIndex)
        scrollState.animateScrollTo((top - 24f).coerceAtLeast(0f).roundToInt())
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 6.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = title,
                fontSize = scaledSp(16),
                fontWeight = accessibleTitleWeight(),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(14.dp))
            SelectionContainer {
                Text(
                    text = highlightedText,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .verticalScroll(scrollState),
                    onTextLayout = { layoutResult = it },
                    fontSize = scaledSp(16),
                    fontWeight = accessibleWeight(),
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = scaledSp(25)
                )
            }
        }
    }
}

private data class GuidedTextModel(
    val fullText: String,
    val segmentRanges: List<IntRange>
)

private fun buildGuidedTextModel(segments: List<String>): GuidedTextModel {
    if (segments.isEmpty()) {
        return GuidedTextModel(fullText = "", segmentRanges = emptyList())
    }

    val builder = StringBuilder()
    val ranges = mutableListOf<IntRange>()

    segments.forEachIndexed { index, segment ->
        if (index > 0) {
            builder.append("\n\n")
        }
        val start = builder.length
        builder.append(segment.trim())
        val endExclusive = builder.length
        ranges.add(start until endExclusive)
    }

    return GuidedTextModel(
        fullText = builder.toString(),
        segmentRanges = ranges
    )
}

@Composable
private fun ProcessingStepCard(
    icon: ImageVector,
    title: String,
    isDone: Boolean,
    isCurrent: Boolean
) {
    val accent = accentColor()
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = if (isCurrent) 8.dp else 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isCurrent || isDone) accent.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isCurrent || isDone) accent else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = title,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp),
                fontSize = scaledSp(16),
                fontWeight = accessibleTitleWeight(),
                color = MaterialTheme.colorScheme.onSurface
            )
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = if (isDone) Color(0xFF31B057) else {
                    if (isCurrent) accent else MaterialTheme.colorScheme.outline
                }
            )
        }
    }
}

@Composable
private fun CircularProgressDecoration(accent: Color) {
    Box(
        modifier = Modifier
            .size(170.dp)
            .clip(CircleShape)
            .background(accent.copy(alpha = 0.15f))
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(126.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.background)
        )
    }
}

@Composable
private fun ControlButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    highlighted: Boolean
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = if (highlighted) accentColor() else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (highlighted) Color.White else MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                fontSize = scaledSp(11),
                fontWeight = accessibleSubtitleWeight(),
                color = if (highlighted) Color.White else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun PrimaryControlButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = accentColor()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 26.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(34.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                fontSize = scaledSp(13),
                fontWeight = accessibleTitleWeight(),
                color = Color.White
            )
        }
    }
}

@Composable
private fun SpeedSelector(
    selectedSpeed: Float,
    onSelectSpeed: (Float) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        PlaybackSpeeds.forEach { speed ->
            val selected = speed == selectedSpeed
            Surface(
                modifier = Modifier.clickable { onSelectSpeed(speed) },
                shape = RoundedCornerShape(14.dp),
                color = if (selected) accentColor() else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = stringResource(R.string.speed_label, speed),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    fontSize = scaledSp(13),
                    fontWeight = accessibleTitleWeight(),
                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun ReadingTextCard(
    title: String,
    body: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 6.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = title,
                fontSize = scaledSp(16),
                fontWeight = accessibleTitleWeight(),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(10.dp))
            SelectionContainer {
                Text(
                    text = body,
                    fontSize = scaledSp(16),
                    fontWeight = accessibleWeight(),
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = scaledSp(25)
                )
            }
        }
    }
}

@Composable
private fun InfoCard(
    icon: ImageVector,
    title: String,
    message: String,
    highlightColor: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = highlightColor
                )
                Text(
                    text = title,
                    modifier = Modifier.padding(start = 10.dp),
                    fontSize = scaledSp(16),
                    fontWeight = accessibleTitleWeight(),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = message,
                fontSize = scaledSp(14),
                fontWeight = accessibleWeight(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = scaledSp(22)
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun HomeInputPreview() {
    LinkListenTheme {
        InputScreen(
            state = HomeUiState(),
            onUrlChange = {},
            onProcessUrl = {},
            onOpenAccessibility = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ProcessingPreview() {
    LinkListenTheme {
        ProcessingScreen(
            stage = ProcessingStage.CLEANING,
            onBack = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun PlayerPreview() {
    LinkListenTheme {
        PlayerScreen(
            article = ArticleContent(
                sourceUrl = "https://example.com/noticia",
                sourceName = "Science Direct",
                title = "Como el cambio climatico esta transformando la agricultura global",
                summary = "Una mirada accesible a los efectos de la temperatura y las lluvias sobre los cultivos.",
                body = "El articulo explica como cambian los ciclos de siembra.\n\nTambien describe el impacto economico en comunidades rurales.\n\nFinalmente resume las medidas de adaptacion mas urgentes."
            ),
            guidedSegments = listOf(
                "Como el cambio climatico esta transformando la agricultura global",
                "Una mirada accesible a los efectos de la temperatura y las lluvias sobre los cultivos.",
                "El articulo explica como cambian los ciclos de siembra.",
                "Tambien describe el impacto economico en comunidades rurales.",
                "Finalmente resume las medidas de adaptacion mas urgentes."
            ),
            currentSegmentIndex = 1,
            totalSegments = 5,
            isSpeaking = true,
            isTtsReady = true,
            speechRate = 1.0f,
            showFullText = false,
            onBack = {},
            onTogglePlayback = {},
            onPreviousSegment = {},
            onNextSegment = {},
            onChangeSpeed = {},
            onToggleTextMode = {}
        )
    }
}
