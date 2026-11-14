package com.sangsangreststop.app.feature.wordchain

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sangsangreststop.app.core.audio.ToneListeningCue
import com.sangsangreststop.app.core.settings.WordChainSettingsRepository
import com.sangsangreststop.app.core.voice.AndroidTtsSpeechOutput
import com.sangsangreststop.app.core.voice.CuedSpeechInput
import com.sangsangreststop.app.core.voice.PleosSpeechInput
import com.sangsangreststop.app.core.voice.PleosSpeechOutput
import com.sangsangreststop.app.core.voice.SpeechInput
import com.sangsangreststop.app.core.voice.SpeechInputError
import com.sangsangreststop.app.core.voice.SpeechOutput
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import com.sangsangreststop.app.game.wordchain.Difficulty
import com.sangsangreststop.app.game.wordchain.GameResult
import com.sangsangreststop.app.game.wordchain.PauseReason
import com.sangsangreststop.app.game.wordchain.WordChainAction
import com.sangsangreststop.app.game.wordchain.WordChainEffect
import com.sangsangreststop.app.game.wordchain.WordChainGame
import com.sangsangreststop.app.game.wordchain.WordChainGameSettings
import com.sangsangreststop.app.game.wordchain.WordChainPhase
import com.sangsangreststop.app.game.wordchain.WordChainState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 3-0 설정 화면·3-1 진행 화면과 끝말잇기 엔진을 잇는 ViewModel.
 * 엔진이 내보낸 effect(말하기·설정 저장 등)를 실제로 실행하고, 엔진 상태를 화면 상태로 바꾼다.
 *
 * 음성 인식은 Pleos SpeechToText SDK를 쓰고, 쓸 수 없는 환경이면 '직접 입력'(글자)으로 안내한다.
 */
class WordChainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = WordChainSettingsRepository(application)
    private val game = WordChainGame()
    // Pleos 음성 합성을 우선 쓰고, 쓸 수 없으면 Android 기본 TTS로 넘어간다
    private val speech: SpeechOutput = runCatching {
        PleosSpeechOutput(application) { AndroidTtsSpeechOutput(application) }
    }.getOrElse {
        Log.w(TAG, "Pleos TTS unavailable", it)
        AndroidTtsSpeechOutput(application)
    }
    // 듣기 직전에 '띵동!' 안내음을 내고, 안내음이 끝난 뒤 실제 듣기를 시작한다
    private val speechInput: CuedSpeechInput? = runCatching { PleosSpeechInput(application) }
        .onFailure { Log.w(TAG, "Pleos STT unavailable", it) }
        .getOrNull()
        ?.let { CuedSpeechInput(it, ToneListeningCue()) }

    /** 저장된 설정. 불러오기 전에는 null. */
    val savedSettings: StateFlow<WordChainGameSettings?> =
        repository.settings.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val gameState = MutableStateFlow<WordChainState?>(null)
    private val speaking = MutableStateFlow(false)
    private val listening = MutableStateFlow(false)
    private var listenJob: Job? = null

    val playUiState: StateFlow<WordChainPlayUiState> =
        combine(gameState, speaking, listening) { state, isSpeaking, isListening ->
            state?.toPlayUiState(isSpeaking, isListening) ?: EmptyPlayUiState
        }.stateIn(viewModelScope, SharingStarted.Eagerly, EmptyPlayUiState)

    /** 음성 인식을 쓸 수 있으면 true. false면 마이크 버튼이 글자 입력 창을 연다. */
    val isVoiceInputAvailable: Boolean get() = speechInput?.isAvailable == true

    fun saveSettings(settings: WordChainSettings) = save(settings.toGameSettings())

    /** 게임 시작 시 설정값을 저장하고 새 게임을 시작한다. */
    fun startGame(settings: WordChainSettings) {
        val gameSettings = settings.toGameSettings()
        endGame()
        save(gameSettings)
        gameState.value = game.newGame(gameSettings)
        dispatch(WordChainAction.Start)
    }

    /** 화면을 떠날 때 진행 중인 게임 세션을 끝낸다. 저장된 설정은 그대로 둔다. */
    fun endGame() {
        if (gameState.value != null) dispatch(WordChainAction.Stop)
        speech.stop()
        stopListening()
        speaking.value = false
        gameState.value = null
    }

    fun onMicClick() = dispatch(WordChainAction.MicButton)

    /** 글자 입력 창을 여는 동안에는 듣기를 멈춘다. */
    fun onTextInputOpened() = stopListening()

    fun onTextEntered(text: String) = dispatch(WordChainAction.TextEntered(text))
    fun onHintClick() = dispatch(WordChainAction.HintButton)
    fun onReplayClick() = dispatch(WordChainAction.ReplayButton)
    fun onExampleClick() = dispatch(WordChainAction.ExampleButton)

    /** 앱이 화면에서 사라지면 일시정지, 돌아오면 이어서 한다. */
    fun onAppBackground() = dispatch(WordChainAction.Pause(PauseReason.APP_BACKGROUND))
    fun onAppForeground() = dispatch(WordChainAction.Resume(PauseReason.APP_BACKGROUND))

    /** 향후 Pleos 전화·내비 이벤트 어댑터가 호출할 진입점 */
    fun onInterruption(reason: PauseReason, active: Boolean) =
        dispatch(if (active) WordChainAction.Pause(reason) else WordChainAction.Resume(reason))

    private fun dispatch(action: WordChainAction) {
        val current = gameState.value ?: return
        val transition = game.reduce(current, action)
        gameState.value = transition.state
        transition.effects.forEach(::handle)
    }

    private fun handle(effect: WordChainEffect) {
        when (effect) {
            is WordChainEffect.Speak -> {
                speaking.value = true
                speech.speak(effect.text) {
                    speaking.value = false
                    dispatch(WordChainAction.SpeechFinished(effect.turnId))
                }
            }
            WordChainEffect.StopSpeaking -> {
                speech.stop()
                speaking.value = false
            }
            is WordChainEffect.StartListening -> startListening(effect.turnId)
            WordChainEffect.StopListening -> stopListening()
            is WordChainEffect.SaveSettings -> save(effect.settings)
            is WordChainEffect.GameOver -> Unit
        }
    }

    private fun startListening(turnId: Long) {
        stopListening()
        val input = speechInput
        if (input == null || !input.isAvailable) {
            dispatch(WordChainAction.VoiceUnavailable)
            return
        }
        listenJob = viewModelScope.launch {
            // TTS 소리가 마이크로 다시 들어가지 않도록 잠깐 기다린 뒤 듣는다
            delay(LISTEN_DELAY_MS)
            if (gameState.value?.turnId != turnId) return@launch
            input.start(object : SpeechInput.Listener {
                override fun onResult(text: String) {
                    listening.value = false
                    dispatch(WordChainAction.ChildSpoke(text, turnId))
                }

                override fun onNoInput() {
                    listening.value = false
                    dispatch(WordChainAction.NoInput(turnId))
                }

                override fun onError(error: SpeechInputError) {
                    listening.value = false
                    when (error) {
                        SpeechInputError.UNAVAILABLE -> dispatch(WordChainAction.VoiceUnavailable)
                        // ON_DEVICE 모드라 네트워크 오류는 드물다. 게임이 멈추지 않도록 인식 실패로 처리한다.
                        SpeechInputError.RECOGNITION, SpeechInputError.NETWORK ->
                            dispatch(WordChainAction.RecognitionFailed(turnId))
                    }
                }
            }) {
                // 안내음이 끝나 실제로 듣기 시작하는 순간에 화면의 듣기 표시를 켠다
                listening.value = true
            }
        }
    }

    private fun stopListening() {
        listenJob?.cancel()
        listenJob = null
        speechInput?.stop() // 재생 중인 안내음도 함께 끊는다
        listening.value = false
    }

    private fun save(settings: WordChainGameSettings) {
        viewModelScope.launch { repository.save(settings) }
    }

    override fun onCleared() {
        speech.release()
        speechInput?.release()
    }

    private companion object {
        const val TAG = "WordChainViewModel"
        const val LISTEN_DELAY_MS = 400L
    }
}

private val EmptyPlayUiState = WordChainPlayUiState(previousWord = "", nextSyllable = "", guidance = "")

internal fun WordChainSettings.toGameSettings() = WordChainGameSettings(
    difficulty = when (difficulty) {
        WordChainDifficulty.Easy -> Difficulty.EASY
        WordChainDifficulty.Normal -> Difficulty.NORMAL
        WordChainDifficulty.Hard -> Difficulty.HARD
    },
    hintsEnabled = hintsEnabled,
)

internal fun WordChainGameSettings.toUiSettings() = WordChainSettings(
    difficulty = difficulty.toUiDifficulty(),
    hintsEnabled = hintsEnabled,
)

private fun Difficulty.toUiDifficulty() = when (this) {
    Difficulty.EASY -> WordChainDifficulty.Easy
    Difficulty.NORMAL -> WordChainDifficulty.Normal
    Difficulty.HARD -> WordChainDifficulty.Hard
}

internal fun WordChainState.toPlayUiState(isSpeaking: Boolean, isListening: Boolean = false) = WordChainPlayUiState(
    currentTurn = if (phase == WordChainPhase.AI_TURN) WordChainTurn.AI else WordChainTurn.CHILD,
    previousWord = currentWord.orEmpty(),
    nextSyllable = currentWord?.lastOrNull()?.toString().orEmpty(),
    guidance = if (phase == WordChainPhase.FINISHED) finishedMessage(result) else lastDisplay.orEmpty(),
    voiceState = when {
        isSpeaking -> WordChainVoiceInputState.SPEAKING
        isListening -> WordChainVoiceInputState.LISTENING
        else -> WordChainVoiceInputState.IDLE
    },
    difficulty = settings.difficulty.toUiDifficulty(),
    hintsEnabled = settings.hintsEnabled,
    isPaused = isPaused && phase != WordChainPhase.FINISHED,
    pauseCause = when {
        !isPaused -> null
        PauseReason.PHONE_CALL in pauseReasons -> WordChainPauseCause.PHONE
        PauseReason.NAVIGATION in pauseReasons -> WordChainPauseCause.NAVIGATION
        else -> WordChainPauseCause.MANUAL
    },
    errorMessage = if (phase == WordChainPhase.NETWORK_ERROR) "연결이 잠시 끊겼어요. 다시 해 볼까요?" else null,
)

private fun finishedMessage(result: GameResult?) = when (result) {
    GameResult.CHILD_WON -> "네가 이겼어! 정말 대단해!"
    GameResult.DRAW -> "이번 판은 비겼어!"
    else -> "재밌었어! 다음에 또 하자!"
}
