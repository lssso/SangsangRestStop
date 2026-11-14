package com.sangsangreststop.app.game.revealwho

/** 일시정지는 phase를 바꾸지 않고 pauseReasons로 관리한다. */
enum class RevealWhoPhase {
    READY,
    /** 앱이 힌트·안내를 말하는 중 */
    HINT,
    /** 아이가 답할 차례 */
    ANSWER,
    FINISHED,
}

/** 여러 사유가 겹칠 수 있으며 모든 사유가 풀려야 재개한다. */
enum class RevealWhoPauseReason { USER, PHONE_CALL, NAVIGATION, AUDIO_FOCUS, APP_BACKGROUND }

data class RevealWhoState(
    val settings: RevealWhoGameSettings = RevealWhoGameSettings(),
    val questions: List<RevealWhoQuestion> = emptyList(),
    val phase: RevealWhoPhase = RevealWhoPhase.READY,
    val questionIndex: Int = 0,
    /** 지금까지 알려 준 마지막 힌트의 위치 (0부터) */
    val hintIndex: Int = 0,
    val score: Int = 0,
    val pauseReasons: Set<RevealWhoPauseReason> = emptySet(),
    /**
     * 말하기·듣기 요청마다 증가한다. 이전 turnId로 도착한 STT 결과·TTS 완료는 버려서
     * 일시정지/재개 뒤 중복 처리를 막는다.
     */
    val turnId: Long = 0,
    val noInputCount: Int = 0,
    val recognitionFailures: Int = 0,
    /** 자동 듣기를 멈추고 마이크 버튼을 기다린다 (무한 반복 방지) */
    val waitingForMic: Boolean = false,
    /** 이 환경에서 음성 인식을 쓸 수 없음 — 화면 버튼으로 진행한다 */
    val voiceUnavailable: Boolean = false,
    /** 마지막 힌트에서 틀렸을 때 주는 한 번의 추가 기회를 썼는지 */
    val finalRetryUsed: Boolean = false,
    val lastSpeech: String? = null,
) {
    val question: RevealWhoQuestion? get() = questions.getOrNull(questionIndex)
    val hintCount: Int get() = question?.hints?.size ?: 0
    val currentHint: String? get() = question?.hints?.getOrNull(hintIndex)
    val isLastHint: Boolean get() = hintIndex >= hintCount - 1
    val isPaused: Boolean get() = pauseReasons.isNotEmpty()
}

sealed interface RevealWhoAction {
    data object Start : RevealWhoAction
    /** TTS가 끝났음 (음성 계층이 보고) */
    data class SpeechFinished(val turnId: Long) : RevealWhoAction
    data class ChildSpoke(val text: String, val turnId: Long) : RevealWhoAction
    /** 아이가 아무 말도 하지 않음 (STT 타임아웃) */
    data class NoInput(val turnId: Long) : RevealWhoAction
    /** 음성 인식 오류 — 오답과 구분한다 */
    data class RecognitionFailed(val turnId: Long) : RevealWhoAction
    /** 이 환경에서 음성 인식을 쓸 수 없음 */
    data object VoiceUnavailable : RevealWhoAction
    data object ReplayButton : RevealWhoAction
    /** 다음 힌트. 마지막 힌트에서 누르면 정답을 알려 주고 다음 문제로 간다. */
    data object NextHintButton : RevealWhoAction
    /** 마이크 버튼 — 말하는 중이면 끊고 바로 듣기 시작 */
    data object MicButton : RevealWhoAction
    data class Pause(val reason: RevealWhoPauseReason) : RevealWhoAction
    data class Resume(val reason: RevealWhoPauseReason) : RevealWhoAction
    data object Stop : RevealWhoAction
}

sealed interface RevealWhoEffect {
    data class Speak(val text: String, val turnId: Long) : RevealWhoEffect
    data class StartListening(val turnId: Long) : RevealWhoEffect
    data object StopListening : RevealWhoEffect
    data object StopSpeaking : RevealWhoEffect
    data class GameOver(val score: Int, val total: Int) : RevealWhoEffect
}

data class RevealWhoTransition(val state: RevealWhoState, val effects: List<RevealWhoEffect> = emptyList())

/**
 * 「짜잔! 누구?」 게임 엔진 (순수 Kotlin, 상태 → 액션 → 새 상태 + 부수효과).
 * 앱이 힌트를 한 단계씩 말하고 아이가 정답을 말한다. 예·아니요 질문 방식이 아니다.
 * 음성 입출력은 effect로 내보내고 실제 실행은 바깥 계층이 맡는다.
 */
class RevealWhoGame(
    private val questionProvider: (RevealWhoGameSettings) -> List<RevealWhoQuestion> = { RevealWhoQuestionBank.pick(it) },
) {

    fun newGame(settings: RevealWhoGameSettings): RevealWhoState =
        RevealWhoState(settings = settings, questions = questionProvider(settings))

    fun reduce(state: RevealWhoState, action: RevealWhoAction): RevealWhoTransition = when (action) {
        RevealWhoAction.Start -> start(state)
        is RevealWhoAction.SpeechFinished -> onSpeechFinished(state, action.turnId)
        is RevealWhoAction.ChildSpoke ->
            if (acceptsInput(state) && action.turnId == state.turnId) onUtterance(state.resetCounters(), action.text)
            else RevealWhoTransition(state)
        is RevealWhoAction.NoInput -> onNoInput(state, action.turnId)
        is RevealWhoAction.RecognitionFailed -> onRecognitionFailed(state, action.turnId)
        RevealWhoAction.VoiceUnavailable -> onVoiceUnavailable(state)
        RevealWhoAction.ReplayButton -> onButton(state) { speak(it, hintLine(it)) }
        RevealWhoAction.NextHintButton -> onButton(state) { nextHint(it, prefix = "") }
        RevealWhoAction.MicButton -> onMicButton(state)
        is RevealWhoAction.Pause -> pause(state, action.reason)
        is RevealWhoAction.Resume -> resume(state, action.reason)
        RevealWhoAction.Stop -> finish(state, speech = null)
    }

    // ---- 시작·진행 ----

    private fun start(state: RevealWhoState): RevealWhoTransition {
        if (state.phase != RevealWhoPhase.READY) return RevealWhoTransition(state)
        if (state.question == null) return finish(state, "문제를 불러오지 못했어. 다음에 다시 하자!")
        return speak(state, "짜잔! 누구일까? 힌트를 잘 듣고 정답을 말해 줘! 첫 번째 문제! ${hintLine(state)}")
    }

    private fun onSpeechFinished(state: RevealWhoState, turnId: Long): RevealWhoTransition {
        if (state.isPaused || turnId != state.turnId || state.phase != RevealWhoPhase.HINT) {
            return RevealWhoTransition(state)
        }
        return RevealWhoTransition(
            state.copy(phase = RevealWhoPhase.ANSWER),
            if (state.waitingForMic) emptyList() else listOf(RevealWhoEffect.StartListening(state.turnId)),
        )
    }

    private fun onUtterance(state: RevealWhoState, text: String): RevealWhoTransition {
        val question = state.question ?: return RevealWhoTransition(state)
        return when (RevealWhoAnswerJudge.classify(text, question)) {
            RevealWhoUtterance.CORRECT ->
                advance(state.copy(score = state.score + 1), "딩동댕! 정답은 '${question.answer}'! 정말 잘했어!")
            RevealWhoUtterance.WRONG -> onWrong(state)
            RevealWhoUtterance.UNCLEAR -> speak(state, "잘 못 들었어. 정답만 다시 말해 줄래?")
            RevealWhoUtterance.NEXT_HINT -> nextHint(state, prefix = "")
            RevealWhoUtterance.REPLAY -> speak(state, hintLine(state))
            RevealWhoUtterance.REVEAL -> reveal(state)
            RevealWhoUtterance.PAUSE -> {
                val ack = speak(state, "알았어, 잠깐 쉴게!")
                val paused = pause(ack.state, RevealWhoPauseReason.USER, stopSpeaking = false)
                paused.copy(effects = ack.effects + paused.effects)
            }
            RevealWhoUtterance.STOP -> finish(state, "재밌었어! 다음에 또 놀자!")
        }
    }

    /** 오답이면 다음 힌트를 준다. 마지막 힌트에서는 한 번 더 기회를 준 뒤 정답을 알려 준다. */
    private fun onWrong(state: RevealWhoState): RevealWhoTransition = when {
        !state.isLastHint -> nextHint(state, prefix = "아쉬워, 조금만 더 생각해 보자! ")
        !state.finalRetryUsed ->
            speak(state.copy(finalRetryUsed = true), "거의 다 왔어! 한 번만 더 생각해 볼까? ${state.currentHint.orEmpty()}")
        else -> reveal(state)
    }

    private fun nextHint(state: RevealWhoState, prefix: String): RevealWhoTransition {
        if (state.isLastHint) return reveal(state)
        val next = state.copy(hintIndex = state.hintIndex + 1, finalRetryUsed = false)
        return speak(next, prefix + hintLine(next))
    }

    private fun reveal(state: RevealWhoState): RevealWhoTransition {
        val answer = state.question?.answer ?: return RevealWhoTransition(state)
        val ending = if (endsWithConsonant(answer)) "이었어" else "였어"
        return advance(state, "괜찮아! 정답은 '$answer'$ending!")
    }

    /** 다음 문제로 넘어가거나, 마지막 문제였으면 게임을 끝낸다. */
    private fun advance(state: RevealWhoState, feedback: String): RevealWhoTransition {
        val nextIndex = state.questionIndex + 1
        if (nextIndex >= state.questions.size) return finish(state, "$feedback ${summary(state)}")
        val next = state.copy(questionIndex = nextIndex, hintIndex = 0, finalRetryUsed = false).resetCounters()
        return speak(next, "$feedback 이번엔 ${ordinal(nextIndex)} 문제! ${hintLine(next)}")
    }

    // ---- 무응답·인식 오류 ----

    private fun onNoInput(state: RevealWhoState, turnId: Long): RevealWhoTransition {
        if (!acceptsInput(state) || turnId != state.turnId) return RevealWhoTransition(state)
        val s = state.copy(noInputCount = state.noInputCount + 1)
        return when {
            // 계속 조용하면 자동 듣기를 멈추고 마이크 버튼을 기다린다
            s.noInputCount >= MAX_NO_INPUT ->
                speak(s.copy(waitingForMic = true), "준비되면 마이크 버튼을 누르고 말해 줘!")
            s.noInputCount == 2 && !s.isLastHint -> nextHint(s, prefix = "힌트를 하나 더 줄게! ")
            else -> speak(s, "천천히 생각해도 돼. ${hintLine(s)}")
        }
    }

    private fun onRecognitionFailed(state: RevealWhoState, turnId: Long): RevealWhoTransition {
        if (!acceptsInput(state) || turnId != state.turnId) return RevealWhoTransition(state)
        val failures = state.recognitionFailures + 1
        return if (failures >= MAX_RECOGNITION_FAILURES) {
            speak(
                state.copy(recognitionFailures = failures, waitingForMic = true),
                "잘 안 들려. 마이크 버튼을 누르고 다시 말해 주거나, 다음 힌트 버튼을 눌러 줘!",
            )
        } else {
            speak(state.copy(recognitionFailures = failures), "잘 못 들었어. 다시 한번 말해 줄래?")
        }
    }

    private fun onVoiceUnavailable(state: RevealWhoState): RevealWhoTransition {
        val next = state.copy(waitingForMic = true, voiceUnavailable = true)
        return if (acceptsInput(state) && !state.voiceUnavailable) {
            speak(next, "지금은 목소리를 들을 수 없어. 화면의 다음 힌트 버튼을 눌러 줘!")
        } else RevealWhoTransition(next)
    }

    // ---- 화면 버튼 ----

    private fun onButton(
        state: RevealWhoState,
        block: (RevealWhoState) -> RevealWhoTransition,
    ): RevealWhoTransition {
        if (!isPlaying(state)) return RevealWhoTransition(state)
        // 버튼을 눌렀다는 건 아이가 다시 참여한다는 뜻이므로 자동 듣기를 되살린다
        val t = block(state.resetCounters())
        return t.copy(effects = listOf(RevealWhoEffect.StopListening) + t.effects)
    }

    private fun onMicButton(state: RevealWhoState): RevealWhoTransition {
        if (!isPlaying(state)) return RevealWhoTransition(state)
        val next = state.copy(
            phase = RevealWhoPhase.ANSWER,
            waitingForMic = false,
            noInputCount = 0,
            recognitionFailures = 0,
            turnId = state.turnId + 1,
        )
        return RevealWhoTransition(
            next,
            listOf(RevealWhoEffect.StopSpeaking, RevealWhoEffect.StartListening(next.turnId)),
        )
    }

    // ---- 일시정지·재개·종료 ----

    private fun pause(
        state: RevealWhoState,
        reason: RevealWhoPauseReason,
        stopSpeaking: Boolean = true,
    ): RevealWhoTransition {
        if (state.phase == RevealWhoPhase.FINISHED) return RevealWhoTransition(state)
        val wasPaused = state.isPaused
        val next = state.copy(pauseReasons = state.pauseReasons + reason)
        if (wasPaused) return RevealWhoTransition(next)
        // turnId를 올려서 진행 중이던 STT/TTS 결과를 무효화한다
        val effects = buildList {
            add(RevealWhoEffect.StopListening)
            if (stopSpeaking) add(RevealWhoEffect.StopSpeaking)
        }
        return RevealWhoTransition(next.copy(turnId = next.turnId + 1), effects)
    }

    private fun resume(state: RevealWhoState, reason: RevealWhoPauseReason): RevealWhoTransition {
        if (reason !in state.pauseReasons) return RevealWhoTransition(state)
        val next = state.copy(pauseReasons = state.pauseReasons - reason)
        if (next.isPaused) return RevealWhoTransition(next) // 다른 사유가 남아 있음
        return if (isPlaying(next)) speak(next, "다시 해 보자! ${hintLine(next)}") else RevealWhoTransition(next)
    }

    private fun finish(state: RevealWhoState, speech: String?): RevealWhoTransition {
        if (state.phase == RevealWhoPhase.FINISHED) return RevealWhoTransition(state)
        val finished = state.copy(
            phase = RevealWhoPhase.FINISHED,
            turnId = state.turnId + 1,
            lastSpeech = speech ?: state.lastSpeech,
        )
        val effects = buildList {
            add(RevealWhoEffect.StopListening)
            if (speech != null) add(RevealWhoEffect.Speak(speech, finished.turnId))
            else add(RevealWhoEffect.StopSpeaking)
            add(RevealWhoEffect.GameOver(finished.score, finished.questions.size))
        }
        return RevealWhoTransition(finished, effects)
    }

    // ---- 문장 도우미 ----

    private fun speak(state: RevealWhoState, text: String): RevealWhoTransition {
        val next = state.copy(phase = RevealWhoPhase.HINT, turnId = state.turnId + 1, lastSpeech = text)
        return RevealWhoTransition(next, listOf(RevealWhoEffect.Speak(text, next.turnId)))
    }

    private fun hintLine(state: RevealWhoState): String {
        val label = if (state.isLastHint) "마지막 힌트!" else "${ordinal(state.hintIndex)} 힌트!"
        return "$label ${state.currentHint.orEmpty()}"
    }

    private fun summary(state: RevealWhoState): String = when (state.score) {
        0 -> "끝까지 함께해 줘서 고마워! 다음에 또 놀자!"
        state.questions.size -> "우와, ${state.questions.size}문제를 모두 맞혔어! 정말 대단해! 다음에 또 놀자!"
        else -> "오늘은 ${state.questions.size}문제 중에 ${state.score}문제를 맞혔어! 정말 잘했어! 다음에 또 놀자!"
    }

    private fun ordinal(index: Int): String = ORDINALS.getOrElse(index) { "${index + 1}번째" }

    private fun endsWithConsonant(word: String): Boolean {
        val last = word.lastOrNull { it in '가'..'힣' } ?: return false
        return (last - '가') % 28 != 0
    }

    private fun isPlaying(state: RevealWhoState) =
        !state.isPaused && state.phase in setOf(RevealWhoPhase.HINT, RevealWhoPhase.ANSWER)

    private fun acceptsInput(state: RevealWhoState) = !state.isPaused && state.phase == RevealWhoPhase.ANSWER

    /** 음성 인식을 쓸 수 없는 환경에서는 계속 마이크 버튼을 기다린다. */
    private fun RevealWhoState.resetCounters() =
        copy(noInputCount = 0, recognitionFailures = 0, waitingForMic = voiceUnavailable)

    companion object {
        const val MAX_NO_INPUT = 3
        const val MAX_RECOGNITION_FAILURES = 3
        private val ORDINALS = listOf(
            "첫 번째", "두 번째", "세 번째", "네 번째", "다섯 번째",
            "여섯 번째", "일곱 번째", "여덟 번째", "아홉 번째", "열 번째",
        )
    }
}
