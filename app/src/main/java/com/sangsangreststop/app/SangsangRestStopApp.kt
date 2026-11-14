package com.sangsangreststop.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sangsangreststop.app.feature.dashboard.DashboardGame
import com.sangsangreststop.app.feature.dashboard.DashboardScreen
import com.sangsangreststop.app.feature.revealwho.RevealWhoPlayScreen
import com.sangsangreststop.app.feature.revealwho.RevealWhoSettings
import com.sangsangreststop.app.feature.revealwho.RevealWhoSetupScreen
import com.sangsangreststop.app.feature.revealwho.RevealWhoViewModel
import com.sangsangreststop.app.feature.voicepoc.VoicePocScreen
import com.sangsangreststop.app.feature.wordchain.WordChainDifficulty
import com.sangsangreststop.app.feature.wordchain.WordChainPlayScreen
import com.sangsangreststop.app.feature.wordchain.WordChainSettings
import com.sangsangreststop.app.feature.wordchain.WordChainSetupScreen
import com.sangsangreststop.app.feature.wordchain.WordChainTextInputDialog
import com.sangsangreststop.app.feature.wordchain.WordChainViewModel
import com.sangsangreststop.app.feature.wordchain.toUiSettings
import com.sangsangreststop.app.ui.components.SangsangRestStopPopupOverlay
import com.sangsangreststop.app.ui.components.popup.CommonPopupAction
import com.sangsangreststop.app.ui.components.popup.CommonPopupOverlay
import com.sangsangreststop.app.ui.components.popup.CommonPopupType
import com.sangsangreststop.app.ui.components.popup.commonPopupModel
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel

/** 게임 화면이 보이는 동안 앱이 백그라운드로 가면 일시정지, 돌아오면 이어서 한다. */
@Composable
private fun GamePlayLifecycle(onBackground: () -> Unit, onForeground: () -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnBackground by rememberUpdatedState(onBackground)
    val currentOnForeground by rememberUpdatedState(onForeground)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> currentOnBackground()
                Lifecycle.Event.ON_START -> currentOnForeground()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}

private enum class AppDestination {
    Dashboard,
    RevealWhoSetup,
    RevealWhoPlay,
    WordChainSetup,
    WordChainPlay,
    VoicePoc,
}

@Composable
fun SangsangRestStopApp(modifier: Modifier = Modifier) {
    var destination by rememberSaveable { mutableStateOf(AppDestination.Dashboard) }
    var unavailableGame by rememberSaveable { mutableStateOf<DashboardGame?>(null) }
    var revealWhoSettings by remember { mutableStateOf<RevealWhoSettings?>(null) }
    var showRevealWhoStopPopup by rememberSaveable {
        mutableStateOf(value = false)
    }
    var wordChainSettings by remember { mutableStateOf<WordChainSettings?>(null) }
    var showWordInput by remember { mutableStateOf(false) }
    val wordChainViewModel: WordChainViewModel = viewModel()
    val revealWhoViewModel: RevealWhoViewModel = viewModel()
    // 그만하기 팝업이 떠 있는 동안에는 게임을 멈추고, 닫으면 이어서 한다
    val closeRevealWhoStopPopup = {
        showRevealWhoStopPopup = false
        revealWhoViewModel.onUserPause(false)
    }
    val leaveRevealWho: (AppDestination) -> Unit = { target ->
        showRevealWhoStopPopup = false
        revealWhoViewModel.endGame()
        destination = target
    }
    val storedWordChainSettings by wordChainViewModel.savedSettings.collectAsState()
    val savedWordChainSettings = storedWordChainSettings?.toUiSettings()
        ?: WordChainSettings(WordChainDifficulty.Easy, hintsEnabled = true)
    val startWordChain: (WordChainSettings) -> Unit = { settings ->
        wordChainSettings = settings
        wordChainViewModel.startGame(settings)
        destination = AppDestination.WordChainPlay
    }

    BackHandler(enabled = unavailableGame != null) {
        unavailableGame = null
    }
    BackHandler(enabled = showRevealWhoStopPopup) {
        closeRevealWhoStopPopup()
    }
    BackHandler(
        enabled = (unavailableGame == null) &&
            !showRevealWhoStopPopup &&
            (destination != AppDestination.Dashboard),
    ) {
        if (destination == AppDestination.WordChainPlay) wordChainViewModel.endGame()
        if (destination == AppDestination.RevealWhoPlay) revealWhoViewModel.endGame()
        destination = when (destination) {
            AppDestination.WordChainPlay -> AppDestination.WordChainSetup
            AppDestination.RevealWhoPlay -> AppDestination.RevealWhoSetup
            else -> AppDestination.Dashboard
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (destination) {
                AppDestination.Dashboard -> {
                    DashboardScreen(
                        onGameSelected = { game ->
                            destination = when (game) {
                                DashboardGame.WordChain -> AppDestination.WordChainSetup
                                DashboardGame.RevealWho -> AppDestination.RevealWhoSetup
                                DashboardGame.SoundQuiz -> {
                                    unavailableGame = game
                                    AppDestination.Dashboard
                                }
                            }
                        },
                        onOpenVoicePoc = { destination = AppDestination.VoicePoc },
                    )
                }

                AppDestination.RevealWhoSetup -> {
                    RevealWhoSetupScreen(
                        onBack = { destination = AppDestination.Dashboard },
                        onStart = { settings ->
                            revealWhoSettings = settings
                            revealWhoViewModel.startGame(settings)
                            destination = AppDestination.RevealWhoPlay
                        },
                        initialSettings = revealWhoSettings ?: RevealWhoSettings(),
                    )
                }

                AppDestination.RevealWhoPlay -> {
                    val playUiState by revealWhoViewModel.playUiState.collectAsState()
                    GamePlayLifecycle(
                        onBackground = revealWhoViewModel::onAppBackground,
                        onForeground = revealWhoViewModel::onAppForeground,
                    )
                    RevealWhoPlayScreen(
                        settings = revealWhoSettings ?: RevealWhoSettings(),
                        onBackClick = { leaveRevealWho(AppDestination.RevealWhoSetup) },
                        onReplayClick = revealWhoViewModel::onReplayClick,
                        onNextHintClick = revealWhoViewModel::onNextHintClick,
                        onStopClick = {
                            showRevealWhoStopPopup = true
                            revealWhoViewModel.onUserPause(true)
                        },
                        uiState = playUiState,
                        onMicClick = revealWhoViewModel::onMicClick,
                    )
                }

                AppDestination.WordChainSetup -> {
                    WordChainSetupScreen(
                        onBack = { destination = AppDestination.Dashboard },
                        onStart = startWordChain,
                        initialSettings = savedWordChainSettings,
                        onSettingsChange = wordChainViewModel::saveSettings,
                    )
                }

                AppDestination.WordChainPlay -> {
                    val settings = wordChainSettings
                    if (settings != null) {
                        val playUiState by wordChainViewModel.playUiState.collectAsState()
                        GamePlayLifecycle(
                            onBackground = wordChainViewModel::onAppBackground,
                            onForeground = wordChainViewModel::onAppForeground,
                        )
                        WordChainPlayScreen(
                            uiState = playUiState,
                            // 음성 인식을 쓸 수 없는 환경이면 마이크도 글자 입력 창을 연다
                            onMicClick = {
                                if (wordChainViewModel.isVoiceInputAvailable) {
                                    wordChainViewModel.onMicClick()
                                } else {
                                    wordChainViewModel.onTextInputOpened()
                                    showWordInput = true
                                }
                            },
                            onStopClick = {
                                wordChainViewModel.endGame()
                                destination = AppDestination.Dashboard
                            },
                            onReplayClick = wordChainViewModel::onReplayClick,
                            onHintClick = wordChainViewModel::onHintClick,
                            onExampleClick = wordChainViewModel::onExampleClick,
                            onTextInputClick = {
                                wordChainViewModel.onTextInputOpened()
                                showWordInput = true
                            },
                            onBackClick = {
                                wordChainViewModel.endGame()
                                destination = AppDestination.WordChainSetup
                            },
                        )
                        if (showWordInput) {
                            WordChainTextInputDialog(
                                onSubmit = { text ->
                                    showWordInput = false
                                    wordChainViewModel.onTextEntered(text)
                                },
                                onDismiss = { showWordInput = false },
                            )
                        }
                    } else {
                        WordChainSetupScreen(
                            onBack = { destination = AppDestination.Dashboard },
                            onStart = startWordChain,
                            initialSettings = savedWordChainSettings,
                            onSettingsChange = wordChainViewModel::saveSettings,
                        )
                    }
                }

                AppDestination.VoicePoc -> {
                    VoicePocScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 52.dp),
                    )
                    TextButton(
                        onClick = { destination = AppDestination.Dashboard },
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .windowInsetsPadding(WindowInsets.safeDrawing)
                            .padding(start = 12.dp, top = 4.dp),
                    ) {
                        Text(stringResource(R.string.dashboard_back))
                    }
                }
            }

            unavailableGame?.let { game ->
                SangsangRestStopPopupOverlay(
                    title = stringResource(R.string.dashboard_unavailable_title),
                    message = stringResource(
                        R.string.dashboard_unavailable_message,
                        stringResource(game.titleRes),
                    ),
                    confirmLabel = stringResource(R.string.dashboard_confirm),
                    onDismiss = { unavailableGame = null },
                )
            }

            if (showRevealWhoStopPopup) {
                CommonPopupOverlay(
                    model = commonPopupModel(CommonPopupType.GAME_PAUSE),
                    onAction = { action ->
                        when (action) {
                            CommonPopupAction.RESUME -> closeRevealWhoStopPopup()
                            CommonPopupAction.STOP -> leaveRevealWho(AppDestination.Dashboard)
                            CommonPopupAction.VOICE_SETTINGS -> {
                                closeRevealWhoStopPopup()
                                unavailableGame = DashboardGame.RevealWho
                            }
                            else -> closeRevealWhoStopPopup()
                        }
                    },
                    onDismissRequest = { closeRevealWhoStopPopup() },
                )
            }

        }
    }
}
