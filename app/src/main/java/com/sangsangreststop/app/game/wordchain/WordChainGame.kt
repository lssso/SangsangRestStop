package com.sangsangreststop.app.game.wordchain

/** 엔진 내부 진행 단계. 일시정지는 phase를 바꾸지 않고 pauseReasons로 관리한다. */
enum class WordChainPhase { READY, AI_TURN, CHILD_TURN, NETWORK_ERROR, FINISHED }

/** UI·외부에 노출하는 상태. 일시정지 중이면 PAUSED. */
enum class WordChainStatus { READY, AI_TURN, CHILD_TURN, PAUSED, NETWORK_ERROR, FINISHED }

/** 여러 사유가 겹칠 수 있으며 모든 사유가 풀려야 재개한다. */
enum class PauseReason { USER, PHONE_CALL, NAVIGATION, AUDIO_FOCUS, APP_BACKGROUND }

enum class Speaker { CHILD, AI, HELPER }

data class PlayedWord(val word: String, val speaker: Speaker)

enum class GameResult { CHILD_WON, DRAW, STOPPED, SESSION_ENDED }

/** 아이에게 "응/아니" 답을 기다리는 질문 */
sealed interface PendingQuestion {
    data class ChangeDifficulty(val to: Difficulty) : PendingQuestion
    data object EnableHints : PendingQuestion
    data class ConfirmWord(val word: String) : PendingQuestion
}

data class WordChainState(
    val settings: WordChainGameSettings = WordChainGameSettings(),
    val phase: WordChainPhase = WordChainPhase.READY,
    val history: List<PlayedWord> = emptyList(),
    val pauseReasons: Set<PauseReason> = emptySet(),
    /**
     * 말하기·듣기 요청마다 증가한다. 이전 turnId로 도착한 STT 결과·TTS 완료는 버려서
     * 일시정지/재개 뒤 중복 처리를 막는다.
     */
    val turnId: Long = 0,
    val hint: HintProgress? = null,
    val pending: PendingQuestion? = null,
    val noInputCount: Int = 0,
    val recognitionFailures: Int = 0,
    val suggestTextInput: Boolean = false,
    /** 여러 번 조용하거나 못 알아들으면 자동 듣기를 멈추고 마이크 버튼을 기다린다 (무한 반복 방지) */
    val waitingForMic: Boolean = false,
    val lastSpeech: String? = null,
    /** 화면에 보여 줄 짧은 문구 (lastSpeech는 TTS용 전체 문장) */
    val lastDisplay: String? = null,
    val phaseBeforeError: WordChainPhase? = null,
    /** 아이가 "아니, 그거 말고"라고 하면 되돌릴 직전 상태 */
    val undo: WordChainState? = null,
    val result: GameResult? = null,
) {
    val currentWord: String? get() = history.lastOrNull()?.word
    val usedWords: Set<String> get() = history.mapTo(mutableSetOf()) { it.word }
    val isPaused: Boolean get() = pauseReasons.isNotEmpty()
    val status: WordChainStatus
        get() = when {
            phase == WordChainPhase.FINISHED -> WordChainStatus.FINISHED
            isPaused -> WordChainStatus.PAUSED
            else -> WordChainStatus.valueOf(phase.name)
        }
}

sealed interface WordChainAction {
    data object Start : WordChainAction
    /** TTS가 끝났음 (음성 계층이 보고) */
    data class SpeechFinished(val turnId: Long) : WordChainAction
    /** STT 결과. confidence가 낮으면 확정하지 않고 되묻는다. */
    data class ChildSpoke(val text: String, val turnId: Long, val confidence: Float? = null) : WordChainAction
    /** 화면 글자 입력 */
    data class TextEntered(val text: String) : WordChainAction
    /** 아이가 아무 말도 하지 않음 (STT 타임아웃) */
    data class NoInput(val turnId: Long) : WordChainAction
    /** 음성 인식 오류 — 끝말잇기 오답과 구분한다 */
    data class RecognitionFailed(val turnId: Long) : WordChainAction
    data object HintButton : WordChainAction
    data object ReplayButton : WordChainAction
    /** "예시 단어 들려줘요" — 예시 단어를 알려 주고 게임을 이어 간다 */
    data object ExampleButton : WordChainAction
    /** 마이크 버튼 — 말하는 중이면 끊고 바로 듣기 시작 */
    data object MicButton : WordChainAction
    /** 이 환경에서 음성 인식을 쓸 수 없음 — 글자 입력으로 안내 */
    data object VoiceUnavailable : WordChainAction
    data class ChangeSettings(val settings: WordChainGameSettings) : WordChainAction
    data class Pause(val reason: PauseReason) : WordChainAction
    data class Resume(val reason: PauseReason) : WordChainAction
    data class NetworkFailed(val turnId: Long) : WordChainAction
    data object Retry : WordChainAction
    data object Stop : WordChainAction
    data object VehicleOff : WordChainAction
}

sealed interface WordChainEffect {
    data class Speak(val text: String, val turnId: Long) : WordChainEffect
    data class StartListening(val turnId: Long) : WordChainEffect
    data object StopListening : WordChainEffect
    data object StopSpeaking : WordChainEffect
    /** 설정 저장은 ViewModel이 Repository로 처리한다. 게임 엔진은 저장소를 직접 건드리지 않는다. */
    data class SaveSettings(val settings: WordChainGameSettings) : WordChainEffect
    data class GameOver(val result: GameResult) : WordChainEffect
}

data class Transition(val state: WordChainState, val effects: List<WordChainEffect> = emptyList())

/**
 * 끝말잇기 게임 엔진 (순수 Kotlin, 상태 → 액션 → 새 상태 + 부수효과).
 * 음성 입출력·저장·차량 이벤트는 effect로 내보내고, 실제 실행은 바깥 계층이 맡는다.
 */
class WordChainGame(
    private val wordProvider: WordProvider = LexiconWordProvider(),
    private val hintProvider: HintProvider = HintProvider(),
    private val config: WordChainRuleConfig = WordChainRuleConfig(),
) {

    fun newGame(settings: WordChainGameSettings): WordChainState = WordChainState(settings = settings)

    fun reduce(state: WordChainState, action: WordChainAction): Transition = when (action) {
        WordChainAction.Start -> start(state)
        is WordChainAction.SpeechFinished -> onSpeechFinished(state, action)
        is WordChainAction.ChildSpoke ->
            if (acceptsInput(state) && action.turnId == state.turnId) {
                onUtterance(state.resetCounters(), action.text, action.confidence)
            } else Transition(state)
        is WordChainAction.TextEntered ->
            if (acceptsInput(state)) {
                val t = onUtterance(state.resetCounters(), action.text, confidence = null)
                t.copy(effects = listOf(WordChainEffect.StopListening) + t.effects)
            } else Transition(state)
        is WordChainAction.NoInput -> onNoInput(state, action.turnId)
        is WordChainAction.RecognitionFailed -> onRecognitionFailed(state, action.turnId)
        WordChainAction.HintButton ->
            if (acceptsInput(state)) withStopListening(giveHint(state.copy(waitingForMic = false))) else Transition(state)
        WordChainAction.ReplayButton -> onReplay(state.copy(waitingForMic = false))
        WordChainAction.ExampleButton ->
            if (acceptsInput(state)) withStopListening(giveUp(state.copy(pending = null, waitingForMic = false)))
            else Transition(state)
        WordChainAction.MicButton -> onMicButton(state)
        WordChainAction.VoiceUnavailable ->
            if (acceptsInput(state) && !state.suggestTextInput) {
                speak(
                    state.copy(waitingForMic = true, suggestTextInput = true),
                    "지금은 목소리를 들을 수 없어. '직접 입력'으로 단어를 써 줄래?",
                )
            } else Transition(state.copy(waitingForMic = true))
        is WordChainAction.ChangeSettings -> applySettings(state, action.settings)
        is WordChainAction.Pause -> pause(state, action.reason)
        is WordChainAction.Resume -> resume(state, action.reason)
        is WordChainAction.NetworkFailed -> onNetworkFailed(state, action.turnId)
        WordChainAction.Retry -> onRetry(state)
        WordChainAction.Stop -> finish(state, GameResult.STOPPED, speech = null)
        WordChainAction.VehicleOff -> Transition(
            newGame(state.settings),
            listOf(
                WordChainEffect.StopListening,
                WordChainEffect.StopSpeaking,
                WordChainEffect.GameOver(GameResult.SESSION_ENDED),
            ),
        )
    }

    // ---- 시작·턴 진행 ----

    private fun start(state: WordChainState): Transition {
        if (state.phase != WordChainPhase.READY) return Transition(state)
        val first = wordProvider.nextWord(null, state.settings, emptySet())
            ?: return finish(state, GameResult.DRAW, "단어장을 불러오지 못했어. 다음에 다시 하자!")
        val next = state.copy(
            phase = WordChainPhase.AI_TURN,
            history = listOf(PlayedWord(first.word, Speaker.AI)),
        )
        return speak(next, "내가 먼저 할게! '${first.word}'! ${prompt(first.word)}")
    }

    private fun onSpeechFinished(state: WordChainState, action: WordChainAction.SpeechFinished): Transition {
        if (state.isPaused || action.turnId != state.turnId) return Transition(state)
        return when (state.phase) {
            WordChainPhase.AI_TURN, WordChainPhase.CHILD_TURN -> Transition(
                state.copy(phase = WordChainPhase.CHILD_TURN),
                if (state.waitingForMic) emptyList() else listOf(WordChainEffect.StartListening(state.turnId)),
            )
            else -> Transition(state)
        }
    }

    private fun onMicButton(state: WordChainState): Transition {
        if (state.isPaused || state.phase !in setOf(WordChainPhase.AI_TURN, WordChainPhase.CHILD_TURN)) {
            return Transition(state)
        }
        val next = state.copy(
            phase = WordChainPhase.CHILD_TURN,
            waitingForMic = false,
            noInputCount = 0,
            recognitionFailures = 0,
            turnId = state.turnId + 1,
        )
        return Transition(next, listOf(WordChainEffect.StopSpeaking, WordChainEffect.StartListening(next.turnId)))
    }

    private fun onUtterance(state: WordChainState, text: String, confidence: Float?): Transition {
        val command = VoiceCommandClassifier.classify(text, awaitingAnswer = state.pending != null)
        val pending = state.pending
        if (pending != null) {
            when (command) {
                VoiceCommand.Yes -> return answerPending(state.copy(pending = null), pending, yes = true)
                VoiceCommand.No -> return answerPending(state.copy(pending = null), pending, yes = false)
                else -> Unit // 다른 말을 하면 질문은 취소하고 그 말을 처리한다
            }
        }
        val s = state.copy(pending = null)
        return when (command) {
            is VoiceCommand.Word -> onWord(s, command.word, command.tokens, confidence)
            is VoiceCommand.SetDifficulty -> changeDifficulty(s, command.difficulty)
            is VoiceCommand.StepDifficulty -> {
                val target = if (command.harder) s.settings.difficulty.harder() else s.settings.difficulty.easier()
                if (target == null) speak(s, limitMessage(command.harder) + " " + prompt(s.currentWord))
                else changeDifficulty(s, target)
            }
            is VoiceCommand.SuggestDifficulty -> {
                val target = if (command.harder) s.settings.difficulty.harder() else s.settings.difficulty.easier()
                if (target == null) {
                    speak(s, limitMessage(command.harder) + " " + prompt(s.currentWord))
                } else {
                    val question = if (command.harder) "그럼 조금 더 어렵게 바꿔 볼까?" else "그럼 조금 더 쉽게 바꿔 볼까?"
                    speak(s.copy(pending = PendingQuestion.ChangeDifficulty(target)), question)
                }
            }
            is VoiceCommand.SetHints -> {
                val t = applySettings(s, s.settings.copy(hintsEnabled = command.enabled))
                val msg = if (command.enabled) "힌트를 켰어! 필요하면 언제든 힌트 달라고 해." else "알았어, 힌트 없이 해 볼게!"
                val spoken = speak(t.state, "$msg ${prompt(s.currentWord)}")
                spoken.copy(effects = t.effects + spoken.effects)
            }
            VoiceCommand.RequestHint -> giveHint(s)
            VoiceCommand.Repeat -> speak(s, repeatLine(s))
            VoiceCommand.GiveUp -> giveUp(s)
            VoiceCommand.Pause -> {
                val ack = speak(s, "알았어, 잠깐 쉴게! 다시 하고 싶으면 알려 줘.")
                val paused = pause(ack.state, PauseReason.USER, stopSpeaking = false)
                paused.copy(effects = ack.effects + paused.effects)
            }
            VoiceCommand.Resume -> speak(s, repeatLine(s))
            VoiceCommand.Stop -> finish(s, GameResult.STOPPED, "재밌었어! 다음에 또 하자!")
            is VoiceCommand.Correction -> onCorrection(s, command.replacement)
            VoiceCommand.Yes, VoiceCommand.No -> speak(s, repeatLine(s))
        }
    }

    private fun onWord(state: WordChainState, raw: String, tokens: List<String>, confidence: Float?): Transition {
        if (raw.isEmpty()) return speak(state, "잘 못 들었어. 다시 한번 말해 줄래?")
        // 여러 어절로 말했으면 규칙에 맞는 어절을 찾는다 (뒤쪽 어절 우선)
        val chosen = if (tokens.size > 1) {
            tokens.asReversed().firstOrNull { check(state, it).second == WordCheck.Valid }
        } else raw
        if (chosen == null) {
            // 맞는 어절이 없는 긴 문장은 주변 대화나 잘못 들은 것으로 보고 오답 처리하지 않는다
            if (tokens.size >= SENTENCE_TOKENS) return speak(state, "잘 못 들었어. 단어 하나만 말해 줄래?")
            return judgeWord(state, raw)
        }
        if (confidence != null && confidence < LOW_CONFIDENCE) {
            return speak(
                state.copy(pending = PendingQuestion.ConfirmWord(chosen)),
                "'$chosen'${Hangul.josa(chosen, "이라고", "라고")} 했어?",
            )
        }
        return judgeWord(state, chosen)
    }

    /** "과자요"처럼 존댓말 어미가 붙은 경우 어미를 뗀 단어를 먼저 본다 ("동요"처럼 떼면 한 글자가 되는 단어는 그대로) */
    private fun check(state: WordChainState, raw: String): Pair<String, WordCheck> {
        val candidates = buildList {
            if (raw.length >= 4 && raw.endsWith("이요")) add(raw.dropLast(2))
            if (raw.length >= 3 && raw.endsWith("요")) add(raw.dropLast(1))
            add(raw)
        }
        val checks = candidates.map { it to WordChainRules.check(it, state.currentWord, state.usedWords, config) }
        return checks.firstOrNull { it.second == WordCheck.Valid } ?: checks.first()
    }

    private fun judgeWord(state: WordChainState, raw: String): Transition {
        val (word, check) = check(state, raw)

        return when (check) {
            WordCheck.Valid -> acceptChildWord(state, word)
            is WordCheck.WrongStart -> speak(
                state,
                "'$word'${Hangul.josa(word, "은", "는")} ${startsPhrase(check.expected)} 시작하지 않아. " +
                    "${startsPhrase(check.expected)} 시작하는 단어를 말해 줄래?",
            )
            WordCheck.AlreadyUsed -> speak(
                state,
                "'$word'${Hangul.josa(word, "은", "는")} 벌써 나왔어! 다른 단어를 생각해 볼까?",
            )
            WordCheck.TooShort -> speak(state, "두 글자 이상인 단어로 말해 줄래?")
            // 한글이 아니거나 너무 길면 인식 문제로 보고 오답 처리하지 않는다
            WordCheck.NotKorean, WordCheck.TooLong -> speak(state, "잘 못 들었어. 다시 한번 말해 줄래?")
        }
    }

    private fun acceptChildWord(state: WordChainState, word: String): Transition {
        val snapshot = state.copy(undo = null)
        val afterChild = state.copy(
            history = state.history + PlayedWord(word, Speaker.CHILD),
            hint = null,
            suggestTextInput = false,
        )
        val ai = wordProvider.nextWord(word, afterChild.settings, afterChild.usedWords)
            ?: return finish(
                afterChild,
                GameResult.CHILD_WON,
                "'$word'! 우와, 나는 ${startsPhrase(WordChainRules.allowedStarts(word, config))} " +
                    "시작하는 단어가 생각이 안 나. 네가 이겼어!",
            )
        val next = afterChild.copy(
            phase = WordChainPhase.AI_TURN,
            history = afterChild.history + PlayedWord(ai.word, Speaker.AI),
            undo = snapshot,
        )
        return speak(next, "'$word', 좋아! 그럼 나는 '${ai.word}'! ${prompt(ai.word)}")
    }

    private fun giveUp(state: WordChainState): Transition {
        val previous = state.currentWord ?: return speak(state, repeatLine(state))
        val example = state.hint?.targetWord?.takeIf { it !in state.usedWords }
            ?: hintProvider.nextHint(previous, state.settings, state.usedWords, null)?.progress?.targetWord
            ?: return finish(state, GameResult.DRAW, "이 글자는 정말 어렵다! 나도 생각이 안 나. 이번 판은 비겼어!")
        val afterExample = state.copy(
            history = state.history + PlayedWord(example, Speaker.HELPER),
            hint = null,
        )
        val ai = wordProvider.nextWord(example, afterExample.settings, afterExample.usedWords)
            ?: return finish(afterExample, GameResult.DRAW, "괜찮아! '$example'도 있어. 어? 이제 나도 모르겠다. 이번 판은 비겼어!")
        val next = afterExample.copy(
            phase = WordChainPhase.AI_TURN,
            history = afterExample.history + PlayedWord(ai.word, Speaker.AI),
            undo = null,
        )
        return speak(next, "괜찮아! '$example'도 있어. 그럼 나는 '${ai.word}'! ${prompt(ai.word)}")
    }

    private fun onCorrection(state: WordChainState, replacement: String?): Transition {
        val undo = state.undo
        val base = if (undo != null) {
            // 직전에 인정된 아이 단어와 AI 응답을 되돌린다. turnId는 뒤로 가지 않는다.
            undo.copy(
                turnId = state.turnId,
                settings = state.settings,
                pauseReasons = state.pauseReasons,
                phase = WordChainPhase.CHILD_TURN,
                undo = null,
            )
        } else state
        return if (replacement != null) {
            judgeWord(base, replacement)
        } else {
            speak(base, "알았어! 다시 말해 줘. ${prompt(base.currentWord)}")
        }
    }

    private fun answerPending(state: WordChainState, pending: PendingQuestion, yes: Boolean): Transition = when (pending) {
        is PendingQuestion.ChangeDifficulty ->
            if (yes) changeDifficulty(state, pending.to)
            else speak(state, "그래, 지금처럼 할게! ${prompt(state.currentWord)}")
        PendingQuestion.EnableHints ->
            if (yes) {
                val t = applySettings(state, state.settings.copy(hintsEnabled = true))
                val hint = giveHint(t.state)
                hint.copy(effects = t.effects + hint.effects)
            } else speak(state, "좋아, 힌트 없이 해 보자! ${prompt(state.currentWord)}")
        is PendingQuestion.ConfirmWord ->
            if (yes) judgeWord(state, pending.word)
            else speak(state, "그럼 다시 한번 말해 줄래?")
    }

    // ---- 힌트 (횟수 제한 없음) ----

    private fun giveHint(state: WordChainState): Transition {
        val previous = state.currentWord ?: return speak(state, repeatLine(state))
        if (!state.settings.hintsEnabled) {
            return speak(state.copy(pending = PendingQuestion.EnableHints), "지금은 힌트가 꺼져 있어. 힌트를 다시 켤까?")
        }
        val hint = hintProvider.nextHint(previous, state.settings, state.usedWords, state.hint)
            ?: return speak(state, "음, 이 글자는 나도 어렵다! 같이 천천히 생각해 보자.")
        return speak(state.copy(hint = hint.progress), hint.text)
    }

    // ---- 무응답·인식 오류 ----

    private fun onNoInput(state: WordChainState, turnId: Long): Transition {
        if (!acceptsInput(state) || turnId != state.turnId) return Transition(state)
        val s = state.copy(noInputCount = state.noInputCount + 1)
        return when {
            // 계속 조용하면 자동 듣기를 멈추고 마이크 버튼을 기다린다
            s.noInputCount >= MAX_NO_INPUT ->
                speak(s.copy(waitingForMic = true), "준비되면 마이크 버튼을 누르고 말해 줘!")
            // 두 번 조용하면 힌트가 켜져 있을 때 자동 힌트를 준다
            s.noInputCount >= 2 && s.settings.hintsEnabled -> giveHint(s)
            else -> speak(s, "천천히 생각해도 돼. ${prompt(s.currentWord)}")
        }
    }

    private fun onRecognitionFailed(state: WordChainState, turnId: Long): Transition {
        if (!acceptsInput(state) || turnId != state.turnId) return Transition(state)
        val failures = state.recognitionFailures + 1
        return if (failures >= MAX_RECOGNITION_FAILURES) {
            speak(
                state.copy(recognitionFailures = failures, suggestTextInput = true, waitingForMic = true),
                "잘 안 들려. 다시 말해 주거나 화면에서 글자로 입력해도 돼!",
            )
        } else {
            speak(state.copy(recognitionFailures = failures), "잘 못 들었어. 다시 한번 말해 줄래?")
        }
    }

    private fun onReplay(state: WordChainState): Transition {
        if (state.isPaused || state.phase !in setOf(WordChainPhase.AI_TURN, WordChainPhase.CHILD_TURN)) {
            return Transition(state)
        }
        return withStopListening(speak(state, repeatLine(state)))
    }

    // ---- 설정 변경 (게임 진행 상태는 유지) ----

    private fun changeDifficulty(state: WordChainState, difficulty: Difficulty): Transition {
        val t = applySettings(state, state.settings.copy(difficulty = difficulty))
        val label = difficultyLabel(difficulty)
        val spoken = speak(t.state, "좋아! 이제 $label${Hangul.josaRo(label)} 할게. ${prompt(state.currentWord)}")
        return spoken.copy(effects = t.effects + spoken.effects)
    }

    private fun applySettings(state: WordChainState, settings: WordChainGameSettings): Transition {
        if (settings == state.settings) return Transition(state)
        val next = state.copy(
            settings = settings,
            hint = if (settings.hintsEnabled) state.hint else null,
        )
        return Transition(next, listOf(WordChainEffect.SaveSettings(settings)))
    }

    // ---- 일시정지·재개·네트워크 ----

    private fun pause(state: WordChainState, reason: PauseReason, stopSpeaking: Boolean = true): Transition {
        if (state.phase == WordChainPhase.FINISHED) return Transition(state)
        val wasPaused = state.isPaused
        val next = state.copy(pauseReasons = state.pauseReasons + reason)
        if (wasPaused) return Transition(next)
        // turnId를 올려서 진행 중이던 STT/TTS 결과를 무효화한다
        val effects = buildList {
            add(WordChainEffect.StopListening)
            if (stopSpeaking) add(WordChainEffect.StopSpeaking)
        }
        return Transition(next.copy(turnId = next.turnId + 1), effects)
    }

    private fun resume(state: WordChainState, reason: PauseReason): Transition {
        if (reason !in state.pauseReasons) return Transition(state)
        val next = state.copy(pauseReasons = state.pauseReasons - reason)
        if (next.isPaused) return Transition(next) // 다른 사유가 남아 있음
        return when (next.phase) {
            WordChainPhase.AI_TURN -> speak(next, next.lastSpeech ?: repeatLine(next))
            WordChainPhase.CHILD_TURN -> speak(next, "다시 해 보자! ${repeatLine(next)}")
            else -> Transition(next)
        }
    }

    private fun onNetworkFailed(state: WordChainState, turnId: Long): Transition {
        if (turnId != state.turnId || state.phase !in setOf(WordChainPhase.AI_TURN, WordChainPhase.CHILD_TURN)) {
            return Transition(state)
        }
        return Transition(
            state.copy(phase = WordChainPhase.NETWORK_ERROR, phaseBeforeError = state.phase, turnId = state.turnId + 1),
            listOf(WordChainEffect.StopListening),
        )
    }

    private fun onRetry(state: WordChainState): Transition {
        if (state.phase != WordChainPhase.NETWORK_ERROR) return Transition(state)
        val restored = state.copy(phase = state.phaseBeforeError ?: WordChainPhase.CHILD_TURN, phaseBeforeError = null)
        if (restored.isPaused) return Transition(restored)
        return speak(restored, repeatLine(restored))
    }

    private fun finish(state: WordChainState, result: GameResult, speech: String?): Transition {
        if (state.phase == WordChainPhase.FINISHED) return Transition(state)
        val finished = state.copy(phase = WordChainPhase.FINISHED, result = result, pending = null, undo = null)
        val effects = buildList {
            add(WordChainEffect.StopListening)
            if (speech != null) add(WordChainEffect.Speak(speech, finished.turnId + 1))
            else add(WordChainEffect.StopSpeaking)
            add(WordChainEffect.GameOver(result))
        }
        return Transition(
            finished.copy(turnId = finished.turnId + 1, lastSpeech = speech ?: finished.lastSpeech),
            effects,
        )
    }

    // ---- 문장 도우미 ----

    private fun speak(state: WordChainState, text: String): Transition {
        val next = state.copy(turnId = state.turnId + 1, lastSpeech = text, lastDisplay = displayFor(state, text))
        return Transition(next, listOf(WordChainEffect.Speak(text, next.turnId)))
    }

    /**
     * 화면에는 두 줄 안에 들어가는 짧은 문구만 보여 준다.
     * 다음 글자 안내로 끝나는 말이면 안내만, 아니면 첫 문장(너무 짧으면 다음 문장까지)을 쓴다.
     */
    private fun displayFor(state: WordChainState, text: String): String {
        val word = state.currentWord
        if (word != null && text.endsWith(prompt(word))) {
            return "${startsPhrase(WordChainRules.allowedStarts(word, config))} 시작하는 단어를 말해 줘!"
        }
        val sentences = text.split(Regex("(?<=[.!?])\\s+")).filter { it.isNotBlank() }
        val builder = StringBuilder()
        for (sentence in sentences) {
            if (builder.isNotEmpty()) builder.append(' ')
            builder.append(sentence)
            if (builder.length >= 8) break
        }
        return builder.toString()
    }

    private fun withStopListening(t: Transition) = t.copy(effects = listOf(WordChainEffect.StopListening) + t.effects)

    private fun acceptsInput(state: WordChainState) = !state.isPaused && state.phase == WordChainPhase.CHILD_TURN

    private fun WordChainState.resetCounters() = copy(noInputCount = 0, recognitionFailures = 0, waitingForMic = false)

    private fun repeatLine(state: WordChainState): String {
        val word = state.currentWord ?: return "단어 짝짝꿍을 시작해 볼까?"
        return "'$word'! ${prompt(word)}"
    }

    private fun prompt(word: String?): String {
        if (word == null) return ""
        return "이제 ${startsPhrase(WordChainRules.allowedStarts(word, config))} 시작하는 단어를 말해 줘!"
    }

    /** {'력','역'} → "'력'이나 '역'으로", {'과'} → "'과'로" */
    private fun startsPhrase(starts: Set<Char>): String {
        val list = starts.toList()
        val last = list.last().toString()
        val head = if (list.size > 1) {
            val first = list.first().toString()
            "'$first'${Hangul.josa(first, "이나", "나")} "
        } else ""
        return "$head'$last'${Hangul.josaRo(last)}"
    }

    private fun limitMessage(harder: Boolean) =
        if (harder) "지금이 제일 어려운 단계야!" else "지금이 제일 쉬운 단계야!"

    private fun difficultyLabel(d: Difficulty) = when (d) {
        Difficulty.EASY -> "쉬움"
        Difficulty.NORMAL -> "보통"
        Difficulty.HARD -> "어려움"
    }

    companion object {
        const val LOW_CONFIDENCE = 0.5f
        const val MAX_RECOGNITION_FAILURES = 3
        const val MAX_NO_INPUT = 3
        const val SENTENCE_TOKENS = 3
    }
}
