package com.sangsangreststop.app.feature.revealwho

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sangsangreststop.app.core.audio.ToneListeningCue
import com.sangsangreststop.app.core.voice.CuedSpeechInput
import com.sangsangreststop.app.core.voice.SharedVoice
import com.sangsangreststop.app.core.voice.SpeechInput
import com.sangsangreststop.app.core.voice.SpeechInputError
import com.sangsangreststop.app.core.voice.SpeechOutput
import com.sangsangreststop.app.game.revealwho.RevealWhoAction
import com.sangsangreststop.app.game.revealwho.RevealWhoEffect
import com.sangsangreststop.app.game.revealwho.RevealWhoGame
import com.sangsangreststop.app.game.revealwho.RevealWhoGameSettings
import com.sangsangreststop.app.game.revealwho.RevealWhoLevel
import com.sangsangreststop.app.game.revealwho.RevealWhoPauseReason
import com.sangsangreststop.app.game.revealwho.RevealWhoPhase
import com.sangsangreststop.app.game.revealwho.RevealWhoState
import com.sangsangreststop.app.game.revealwho.RevealWhoTopic
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class RevealWhoVoiceState { IDLE, SPEAKING, LISTENING }

/** 「짜잔! 누구?」 진행 화면이 그리는 상태. 기본값은 디자인 미리보기용이다. */
data class RevealWhoPlayUiState(
    val questionNumber: Int = 1,
    val hintNumber: Int = 2,
    val hintCount: Int = 4,
    val hintText: String = "나는 목이 아주 길어요!",
    val voiceState: RevealWhoVoiceState = RevealWhoVoiceState.LISTENING,
    val isFinished: Boolean = false,
    val isPaused: Boolean = false,
)

/**
 * 「짜잔! 누구?」 진행 화면과 게임 엔진을 잇는 ViewModel.
 * 엔진이 내보낸 effect(말하기·듣기)를 실제로 실행하고, 엔진 상태를 화면 상태로 바꾼다.
 *
 * 음성 장치는 게임을 시작할 때 만들고 끝낼 때 해제해서, 다른 게임의 음성 세션과 겹치지 않게 한다.
 */
class RevealWhoViewModel(application: Application) : AndroidViewModel(application) {

    private val game = RevealWhoGame()
    private var speech: SpeechOutput? = null
    private var speechInput: CuedSpeechInput? = null

    private val gameState = MutableStateFlow<RevealWhoState?>(null)
    private val speaking = MutableStateFlow(false)
    private val listening = MutableStateFlow(false)
    private var listenJob: Job? = null

    val playUiState: StateFlow<RevealWhoPlayUiState> =
        combine(gameState, speaking, listening) { state, isSpeaking, isListening ->
            state?.toPlayUiState(isSpeaking, isListening) ?: EmptyPlayUiState
        }.stateIn(viewModelScope, SharingStarted.Eagerly, EmptyPlayUiState)

    fun startGame(settings: RevealWhoSettings) {
        endGame()
        openVoice()
        gameState.value = game.newGame(settings.toGameSettings())
        dispatch(RevealWhoAction.Start)
    }

    /** 화면을 떠날 때 진행 중인 게임 세션을 끝내고 음성 장치를 해제한다. */
    fun endGame() {
        if (gameState.value != null) dispatch(RevealWhoAction.Stop)
        stopListening()
        closeVoice()
        speaking.value = false
        gameState.value = null
    }

    fun onMicClick() = dispatch(RevealWhoAction.MicButton)
    fun onReplayClick() = dispatch(RevealWhoAction.ReplayButton)
    fun onNextHintClick() = dispatch(RevealWhoAction.NextHintButton)

    /** 그만하기 팝업이 떠 있는 동안 게임을 멈춘다. */
    fun onUserPause(paused: Boolean) = onInterruption(RevealWhoPauseReason.USER, paused)

    /** 앱이 화면에서 사라지면 일시정지, 돌아오면 이어서 한다. */
    fun onAppBackground() = onInterruption(RevealWhoPauseReason.APP_BACKGROUND, active = true)
    fun onAppForeground() = onInterruption(RevealWhoPauseReason.APP_BACKGROUND, active = false)

    /** 향후 Pleos 전화·내비 이벤트 어댑터가 호출할 진입점 */
    fun onInterruption(reason: RevealWhoPauseReason, active: Boolean) =
        dispatch(if (active) RevealWhoAction.Pause(reason) else RevealWhoAction.Resume(reason))

    private fun dispatch(action: RevealWhoAction) {
        val current = gameState.value ?: return
        val transition = game.reduce(current, action)
        gameState.value = transition.state
        transition.effects.forEach(::handle)
    }

    private fun handle(effect: RevealWhoEffect) {
        when (effect) {
            is RevealWhoEffect.Speak -> {
                speaking.value = true
                speech?.speak(effect.text) {
                    speaking.value = false
                    dispatch(RevealWhoAction.SpeechFinished(effect.turnId))
                }
            }
            RevealWhoEffect.StopSpeaking -> {
                speech?.stop()
                speaking.value = false
            }
            is RevealWhoEffect.StartListening -> startListening(effect.turnId)
            RevealWhoEffect.StopListening -> stopListening()
            is RevealWhoEffect.GameOver -> Unit
        }
    }

    private fun startListening(turnId: Long) {
        stopListening()
        val input = speechInput
        if (input == null || !input.isAvailable) {
            dispatch(RevealWhoAction.VoiceUnavailable)
            return
        }
        listenJob = viewModelScope.launch {
            // TTS 소리가 마이크로 다시 들어가지 않도록 잠깐 기다린 뒤 '띵동!' → 듣기 순서로 진행한다
            delay(LISTEN_DELAY_MS)
            if (gameState.value?.turnId != turnId) return@launch
            input.start(object : SpeechInput.Listener {
                override fun onResult(text: String) {
                    listening.value = false
                    dispatch(RevealWhoAction.ChildSpoke(text, turnId))
                }

                override fun onNoInput() {
                    listening.value = false
                    dispatch(RevealWhoAction.NoInput(turnId))
                }

                override fun onError(error: SpeechInputError) {
                    listening.value = false
                    when (error) {
                        SpeechInputError.UNAVAILABLE -> dispatch(RevealWhoAction.VoiceUnavailable)
                        SpeechInputError.RECOGNITION, SpeechInputError.NETWORK ->
                            dispatch(RevealWhoAction.RecognitionFailed(turnId))
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
        speechInput?.stop()
        listening.value = false
    }

    /** 음성 장치는 단어 짝짝꿍과 공유한다 (Pleos 서비스가 앱마다 콜백을 하나만 유지하기 때문). */
    private fun openVoice() {
        val app = getApplication<Application>()
        speech = SharedVoice.speechOutput(app)
        speechInput = SharedVoice.speechInput(app)?.let { CuedSpeechInput(it, ToneListeningCue()) }
    }

    /** 공유 음성 장치는 해제하지 않고 멈추기만 한다. 안내음 장치만 해제한다. */
    private fun closeVoice() {
        speech?.stop()
        speech = null
        speechInput?.release()
        speechInput = null
    }

    override fun onCleared() {
        stopListening()
        closeVoice()
    }

    private companion object {
        const val LISTEN_DELAY_MS = 400L
    }
}

private val EmptyPlayUiState = RevealWhoPlayUiState(
    questionNumber = 1,
    hintNumber = 1,
    hintCount = 4,
    hintText = "",
    voiceState = RevealWhoVoiceState.IDLE,
)

internal fun RevealWhoSettings.toGameSettings() = RevealWhoGameSettings(
    level = when (difficulty) {
        RevealWhoDifficulty.Easy -> RevealWhoLevel.EASY
        RevealWhoDifficulty.Normal -> RevealWhoLevel.NORMAL
        RevealWhoDifficulty.Hard -> RevealWhoLevel.HARD
    },
    topic = when (category) {
        RevealWhoCategory.Animal -> RevealWhoTopic.ANIMAL
        RevealWhoCategory.Vehicle -> RevealWhoTopic.VEHICLE
        RevealWhoCategory.Object -> RevealWhoTopic.OBJECT
    },
)

internal fun RevealWhoState.toPlayUiState(isSpeaking: Boolean, isListening: Boolean): RevealWhoPlayUiState {
    val finished = phase == RevealWhoPhase.FINISHED
    return RevealWhoPlayUiState(
        questionNumber = questionIndex + 1,
        hintNumber = hintIndex + 1,
        hintCount = hintCount.coerceAtLeast(1),
        // 끝나면 마무리 인사를 보여 준다
        hintText = if (finished) FINISHED_MESSAGE.format(score, questions.size) else currentHint.orEmpty(),
        voiceState = when {
            isSpeaking -> RevealWhoVoiceState.SPEAKING
            isListening -> RevealWhoVoiceState.LISTENING
            else -> RevealWhoVoiceState.IDLE
        },
        isFinished = finished,
        isPaused = isPaused && !finished,
    )
}

private const val FINISHED_MESSAGE = "%2\$d문제 중 %1\$d문제 정답! 참 잘했어요!"
