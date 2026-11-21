package com.sangsangreststop.app.game.revealwho

/**
 * 엔진 진행 단계. 일시정지·오류는 phase를 바꾸지 않고 pauseReasons·error로 관리한다.
 *
 * 화면에서 말하는 상태와의 대응:
 * - 게임 시작 전: READY
 * - 문제·힌트 안내, 칭찬·정답 공개·다음 문제 안내 중: HINT (앱이 말하는 중이며 마이크는 꺼져 있다)
 * - 아이 답변 대기: ANSWER (앱의 말이 끝난 뒤에만 들어간다)
 * - 게임 종료: FINISHED
 * 정답 판정은 결과가 도착한 그 자리에서 끝나므로 따로 머무는 단계가 없다.
 */
enum class RevealWhoPhase {
    READY,
    HINT,
    ANSWER,
    FINISHED,
}

/** 여러 사유가 겹칠 수 있으며 모든 사유가 풀려야 재개한다. */
enum class RevealWhoPauseReason { USER, PHONE_CALL, NAVIGATION, AUDIO_FOCUS, APP_BACKGROUND }

/** 공통 오류 팝업으로 알리는 문제. 오류가 있는 동안에는 진행을 멈춘다. */
enum class RevealWhoError {
    /** 여러 번 연속으로 잘 못 알아들음 */
    SPEECH_RECOGNITION,
    NETWORK,
}

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
     * 일시정지/재개 뒤나 다음 문제로 넘어간 뒤의 중복 처리를 막는다.
     */
    val turnId: Long = 0,
    val noInputCount: Int = 0,
    val recognitionFailures: Int = 0,
    /** 자동 듣기를 멈추고 마이크 버튼을 기다린다 (무한 반복 방지) */
    val waitingForMic: Boolean = false,
    /** 이 환경에서 음성 인식을 쓸 수 없음 — 마이크 버튼이 글자 입력 창을 연다 */
    val voiceUnavailable: Boolean = false,
    /** 마지막 힌트에서 틀렸을 때 주는 한 번의 추가 기회를 썼는지 */
    val finalRetryUsed: Boolean = false,
    /** 이번 문제에서 이미 말한 오답 (같은 말을 반복하면 힌트를 쓰지 않고 다른 답을 생각하게 한다) */
    val triedAnswers: Set<String> = emptySet(),
    /** 정답과 발음이 아주 비슷해서 "이라고 했어?"라고 되묻는 중 */
    val confirmingAnswer: Boolean = false,
    val error: RevealWhoError? = null,
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
    /** 음성 인식을 쓸 수 없을 때 화면 글자 입력으로 낸 답 */
    data class TextEntered(val text: String) : RevealWhoAction
    /** 아이가 아무 말도 하지 않음 (STT 타임아웃) */
    data class NoInput(val turnId: Long) : RevealWhoAction
    /** 음성 인식 오류 — 오답과 구분한다 */
    data class RecognitionFailed(val turnId: Long) : RevealWhoAction
    /** 음성 인식 중 네트워크 오류 */
    data class NetworkFailed(val turnId: Long) : RevealWhoAction
    /** 오류 팝업의 '다시 시도' */
    data object Retry : RevealWhoAction
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
        is RevealWhoAction.TextEntered ->
            // 글자로 낸 답은 앱이 말하는 중이어도 받는다 (말을 끊고 바로 판정)
            if (isPlaying(state) && action.text.isNotBlank()) {
                val t = onUtterance(state.resetCounters(), action.text)
                t.copy(effects = listOf(RevealWhoEffect.StopListening, RevealWhoEffect.StopSpeaking) + t.effects)
            } else RevealWhoTransition(state)
        is RevealWhoAction.NoInput -> onNoInput(state, action.turnId)
        is RevealWhoAction.RecognitionFailed -> onRecognitionFailed(state, action.turnId)
        is RevealWhoAction.NetworkFailed -> onNetworkFailed(state, action.turnId)
        RevealWhoAction.Retry -> onRetry(state)
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
        if (state.isPaused || state.error != null || turnId != state.turnId || state.phase != RevealWhoPhase.HINT) {
            return RevealWhoTransition(state)
        }
        return RevealWhoTransition(
            state.copy(phase = RevealWhoPhase.ANSWER),
            if (state.waitingForMic) emptyList() else listOf(RevealWhoEffect.StartListening(state.turnId)),
        )
    }

    private fun onUtterance(state: RevealWhoState, text: String): RevealWhoTransition {
        val question = state.question ?: return RevealWhoTransition(state)
        val confirming = state.confirmingAnswer
        val s = state.copy(confirmingAnswer = false)
        return when (RevealWhoAnswerJudge.classify(text, question, awaitingConfirm = confirming)) {
            RevealWhoUtterance.CORRECT -> correct(s)
            RevealWhoUtterance.YES -> correct(s)
            RevealWhoUtterance.NO -> speak(s, "그렇구나! 그럼 다시 한번 말해 줄래?")
            RevealWhoUtterance.WRONG -> when {
                // 정답과 발음이 아주 비슷하면 잘못 들었을 수 있으니 오답 처리하지 않고 되묻는다
                !confirming && RevealWhoAnswerJudge.isNearMiss(text, question) ->
                    speak(s.copy(confirmingAnswer = true), "혹시 '${question.answer}'${josa(question.answer, "이라고", "라고")} 말했어?")
                else -> onWrong(s, RevealWhoAnswerJudge.answerForm(text))
            }
            RevealWhoUtterance.UNCLEAR -> speak(s, "잘 못 들었어. 정답만 다시 말해 줄래?")
            RevealWhoUtterance.NEXT_HINT -> nextHint(s, prefix = "")
            RevealWhoUtterance.REPLAY -> speak(s, hintLine(s))
            RevealWhoUtterance.REVEAL -> reveal(s)
            RevealWhoUtterance.PAUSE -> {
                val ack = speak(s, "알았어, 잠깐 쉴게!")
                val paused = pause(ack.state, RevealWhoPauseReason.USER, stopSpeaking = false)
                paused.copy(effects = ack.effects + paused.effects)
            }
            RevealWhoUtterance.STOP -> finish(s, "재밌었어! 다음에 또 놀자!")
        }
    }

    private fun correct(state: RevealWhoState): RevealWhoTransition {
        val answer = state.question?.answer ?: return RevealWhoTransition(state)
        return advance(state.copy(score = state.score + 1), "딩동댕! 정답은 '$answer'! 정말 잘했어!")
    }

    /**
     * 오답이면 다음 힌트를 준다. 마지막 힌트에서는 한 번 더 기회를 준 뒤 정답을 알려 준다.
     * 이번 문제에서 이미 말한 오답을 또 말하면 힌트를 쓰지 않고 다른 답을 생각해 보게 한다.
     */
    private fun onWrong(state: RevealWhoState, said: String): RevealWhoTransition = when {
        said.isNotEmpty() && said in state.triedAnswers ->
            speak(state, "'$said'${josa(said, "은", "는")} 아까 말했어! 다른 걸 생각해 볼까? ${hintLine(state)}")
        else -> {
            val s = if (said.isEmpty()) state else state.copy(triedAnswers = state.triedAnswers + said)
            when {
                !s.isLastHint -> nextHint(s, prefix = "아쉬워, 조금만 더 생각해 보자! ")
                !s.finalRetryUsed ->
                    speak(s.copy(finalRetryUsed = true), "거의 다 왔어! 한 번만 더 생각해 볼까? ${s.currentHint.orEmpty()}")
                else -> reveal(s)
            }
        }
    }

    private fun nextHint(state: RevealWhoState, prefix: String): RevealWhoTransition {
        if (state.isLastHint) return reveal(state)
        val next = state.copy(hintIndex = state.hintIndex + 1, finalRetryUsed = false)
        return speak(next, prefix + hintLine(next))
    }

    private fun reveal(state: RevealWhoState): RevealWhoTransition {
        val answer = state.question?.answer ?: return RevealWhoTransition(state)
        return advance(state, "괜찮아! 정답은 '$answer'${josa(answer, "이었어", "였어")}!")
    }

    /** 다음 문제로 넘어가거나, 마지막 문제였으면 게임을 끝낸다. */
    private fun advance(state: RevealWhoState, feedback: String): RevealWhoTransition {
        val nextIndex = state.questionIndex + 1
        if (nextIndex >= state.questions.size) return finish(state, "$feedback ${summary(state)}")
        val next = state.copy(
            questionIndex = nextIndex,
            hintIndex = 0,
            finalRetryUsed = false,
            triedAnswers = emptySet(),
            confirmingAnswer = false,
        ).resetCounters()
        return speak(next, "$feedback 이번엔 ${ordinal(nextIndex)} 문제! ${hintLine(next)}")
    }

    // ---- 무응답·인식 오류 ----

    private fun onNoInput(state: RevealWhoState, turnId: Long): RevealWhoTransition {
        if (!acceptsInput(state) || turnId != state.turnId) return RevealWhoTransition(state)
        val s = state.copy(noInputCount = state.noInputCount + 1, confirmingAnswer = false)
        return when {
            // 계속 조용하면 자동 듣기를 멈추고 마이크 버튼을 기다린다
            s.noInputCount >= MAX_NO_INPUT ->
                speak(s.copy(waitingForMic = true), "준비되면 마이크 버튼을 누르고 말해 줘!")
            s.noInputCount == 2 && !s.isLastHint -> nextHint(s, prefix = "힌트를 하나 더 줄게! ")
            else -> speak(s, "천천히 생각해도 돼. ${hintLine(s)}")
        }
    }

    /** 잘 못 알아들은 것은 오답이 아니다. 계속 실패하면 오류 팝업으로 알리고 멈춘다. */
    private fun onRecognitionFailed(state: RevealWhoState, turnId: Long): RevealWhoTransition {
        if (!acceptsInput(state) || turnId != state.turnId) return RevealWhoTransition(state)
        val failures = state.recognitionFailures + 1
        if (failures >= MAX_RECOGNITION_FAILURES) {
            return showError(state.copy(recognitionFailures = failures), RevealWhoError.SPEECH_RECOGNITION)
        }
        return speak(state.copy(recognitionFailures = failures), "잘 못 들었어. 다시 한번 말해 줄래?")
    }

    private fun onNetworkFailed(state: RevealWhoState, turnId: Long): RevealWhoTransition {
        if (!acceptsInput(state) || turnId != state.turnId) return RevealWhoTransition(state)
        return showError(state, RevealWhoError.NETWORK)
    }

    /** 진행을 멈추고 오류를 표시한다. turnId를 올려서 진행 중이던 음성 결과를 무효화한다. */
    private fun showError(state: RevealWhoState, error: RevealWhoError): RevealWhoTransition = RevealWhoTransition(
        state.copy(error = error, turnId = state.turnId + 1, confirmingAnswer = false),
        listOf(RevealWhoEffect.StopListening, RevealWhoEffect.StopSpeaking),
    )

    /** 오류 팝업의 '다시 시도': 오류를 지우고 지금 힌트부터 다시 들려준다. */
    private fun onRetry(state: RevealWhoState): RevealWhoTransition {
        if (state.error == null || state.phase == RevealWhoPhase.FINISHED) return RevealWhoTransition(state)
        val cleared = state.copy(error = null).resetCounters()
        if (cleared.isPaused) return RevealWhoTransition(cleared)
        return speak(cleared, "다시 해 보자! ${hintLine(cleared)}")
    }

    private fun onVoiceUnavailable(state: RevealWhoState): RevealWhoTransition {
        val next = state.copy(waitingForMic = true, voiceUnavailable = true)
        return if (acceptsInput(state) && !state.voiceUnavailable) {
            speak(next, "지금은 목소리를 들을 수 없어. 마이크를 누르면 글자로 정답을 쓸 수 있어!")
        } else RevealWhoTransition(next)
    }

    // ---- 화면 버튼 ----

    private fun onButton(
        state: RevealWhoState,
        block: (RevealWhoState) -> RevealWhoTransition,
    ): RevealWhoTransition {
        if (!isPlaying(state)) return RevealWhoTransition(state)
        // 버튼을 눌렀다는 건 아이가 다시 참여한다는 뜻이므로 자동 듣기를 되살린다
        val t = block(state.resetCounters().copy(confirmingAnswer = false))
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
        return if (isPlaying(next)) speak(next.copy(confirmingAnswer = false), "다시 해 보자! ${hintLine(next)}")
        else RevealWhoTransition(next)
    }

    private fun finish(state: RevealWhoState, speech: String?): RevealWhoTransition {
        if (state.phase == RevealWhoPhase.FINISHED) return RevealWhoTransition(state)
        val finished = state.copy(
            phase = RevealWhoPhase.FINISHED,
            turnId = state.turnId + 1,
            confirmingAnswer = false,
            error = null,
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

    /** 받침이 있으면 [withFinal], 없으면 [withoutFinal]. */
    private fun josa(word: String, withFinal: String, withoutFinal: String): String {
        val last = word.lastOrNull { it in '가'..'힣' } ?: return withoutFinal
        return if ((last - '가') % 28 != 0) withFinal else withoutFinal
    }

    private fun isPlaying(state: RevealWhoState) =
        !state.isPaused && state.error == null && state.phase in setOf(RevealWhoPhase.HINT, RevealWhoPhase.ANSWER)

    private fun acceptsInput(state: RevealWhoState) =
        !state.isPaused && state.error == null && state.phase == RevealWhoPhase.ANSWER

    /** 음성 인식을 쓸 수 없는 환경에서는 계속 마이크 버튼(글자 입력)을 기다린다. */
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
