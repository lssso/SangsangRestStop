package com.sangsangreststop.app.game.wordchain

import com.sangsangreststop.app.game.wordchain.WordChainAction.ChildSpoke
import com.sangsangreststop.app.game.wordchain.WordChainAction.SpeechFinished
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WordChainGameTest {

    /** 정해진 순서로 단어를 내는 가짜 제공자: 첫 단어는 '사과', 이후 단어장에서 이어갈 수 있는 첫 단어 */
    private class ScriptedProvider(private val lexicon: WordLexicon) : WordProvider {
        override fun nextWord(previousWord: String?, settings: WordChainGameSettings, usedWords: Set<String>): LexiconEntry? {
            if (previousWord == null) return lexicon.find("사과")
            return lexicon.startingWith(WordChainRules.allowedStarts(previousWord))
                .firstOrNull { it.word !in usedWords && it.word.length >= 2 }
        }
    }

    private val lexicon = WordLexicon.parse(
        """
        1|과일|사과
        1|간식|과자
        1|탈것|자동차 차표
        1|채소|표고버섯
        1|동물|역마 력사
        """.trimIndent()
    )
    private val game = WordChainGame(ScriptedProvider(lexicon), HintProvider(lexicon, kotlin.random.Random(0)))

    private fun WordChainState.reduce(action: WordChainAction) = game.reduce(this, action)

    /** 게임을 시작하고 AI 첫 단어 발화가 끝나 아이 차례가 된 상태 */
    private fun childTurn(settings: WordChainGameSettings = WordChainGameSettings()): WordChainState {
        val started = game.newGame(settings).reduce(WordChainAction.Start).state
        return started.reduce(SpeechFinished(started.turnId)).state
    }

    private fun WordChainState.say(text: String, confidence: Float? = null) = reduce(ChildSpoke(text, turnId, confidence))

    private fun Transition.spoken(): String =
        effects.filterIsInstance<WordChainEffect.Speak>().joinToString(" ") { it.text }

    @Test
    fun defaultSettingsAreEasyWithHints() {
        assertEquals(WordChainGameSettings(Difficulty.EASY, hintsEnabled = true), WordChainGameSettings())
    }

    @Test
    fun startThenListenAfterSpeech() {
        val start = game.newGame(WordChainGameSettings()).reduce(WordChainAction.Start)
        assertEquals(WordChainStatus.AI_TURN, start.state.status)
        assertEquals("사과", start.state.currentWord)
        assertTrue(start.spoken().contains("'과'로 시작하는"))

        val listen = start.state.reduce(SpeechFinished(start.state.turnId))
        assertEquals(WordChainStatus.CHILD_TURN, listen.state.status)
        assertEquals(listOf(WordChainEffect.StartListening(start.state.turnId)), listen.effects)
    }

    @Test
    fun validWordGetsAiReply() {
        val t = childTurn().say("과자")
        assertEquals(listOf("사과", "과자", "자동차"), t.state.history.map { it.word })
        assertEquals(WordChainStatus.AI_TURN, t.state.status)
        assertTrue(t.spoken(), t.spoken().contains("'자동차'"))
    }

    @Test
    fun politeEndingIsAccepted() {
        val t = childTurn().say("과자요")
        assertEquals("과자", t.state.history[1].word)
    }

    @Test
    fun wrongStartKeepsTurnWithoutResettingGame() {
        val before = childTurn()
        val t = before.say("자동차")
        assertEquals(WordChainStatus.CHILD_TURN, t.state.status)
        assertEquals(before.history, t.state.history)
        assertTrue(t.spoken(), t.spoken().contains("'과'로 시작하지 않아"))
    }

    @Test
    fun staleResultsAreIgnored() {
        val s = childTurn()
        val t = s.reduce(ChildSpoke("과자", turnId = s.turnId - 1))
        assertEquals(s, t.state)
        assertTrue(t.effects.isEmpty())
    }

    @Test
    fun aiCannotContinueChildWins() {
        // 사과 → 과자 → 자동차 → 차표 → 표고버섯 → '섯'으로 시작하는 단어 없음
        var s = childTurn().say("과자").state
        s = s.reduce(SpeechFinished(s.turnId)).state
        val t = s.say("차표") // AI: 표고버섯
        s = t.state.reduce(SpeechFinished(t.state.turnId)).state
        val end = s.say("섯다리") // 단어장에 없어도 규칙이 맞으면 아이 단어는 인정
        assertEquals(WordChainStatus.FINISHED, end.state.status)
        assertEquals(GameResult.CHILD_WON, end.state.result)
        assertTrue(end.effects.contains(WordChainEffect.GameOver(GameResult.CHILD_WON)))
    }

    @Test
    fun settingsChangeKeepsProgressAndIsSaved() {
        val before = childTurn()
        val t = before.say("보통으로 바꿔줘")
        assertEquals(Difficulty.NORMAL, t.state.settings.difficulty)
        assertEquals(before.history, t.state.history)
        assertEquals(WordChainStatus.CHILD_TURN, t.state.status)
        assertTrue(t.effects.contains(WordChainEffect.SaveSettings(WordChainGameSettings(Difficulty.NORMAL, true))))
    }

    @Test
    fun ambiguousDifficultyAsksFirst() {
        val ask = childTurn().say("너무 쉬워")
        assertEquals(PendingQuestion.ChangeDifficulty(Difficulty.NORMAL), ask.state.pending)
        assertTrue(ask.effects.none { it is WordChainEffect.SaveSettings })
        val yes = ask.state.reduce(SpeechFinished(ask.state.turnId)).state.say("응")
        assertEquals(Difficulty.NORMAL, yes.state.settings.difficulty)
    }

    @Test
    fun hintsAreUnlimitedAndStepwise() {
        var s = childTurn()
        repeat(10) {
            val t = s.reduce(WordChainAction.HintButton)
            assertTrue(t.spoken().isNotEmpty())
            s = t.state
        }
        assertEquals(listOf("사과"), s.history.map { it.word })
    }

    @Test
    fun hintOffAsksToTurnBackOn() {
        val s = childTurn(WordChainGameSettings(hintsEnabled = false))
        val ask = s.say("힌트 줘")
        assertEquals(PendingQuestion.EnableHints, ask.state.pending)
        val yes = ask.state.say("응")
        assertTrue(yes.state.settings.hintsEnabled)
        assertTrue(yes.effects.contains(WordChainEffect.SaveSettings(WordChainGameSettings(hintsEnabled = true))))
        assertNotNull(yes.state.hint)
    }

    @Test
    fun correctionUndoesLastAcceptedWord() {
        // 실제로는 다른 말을 했는데 '과자'로 인식되어 AI가 '자동차'로 이어간 뒤
        val aiReplied = childTurn().say("과자").state
        val afterWrongRecognition = aiReplied.reduce(SpeechFinished(aiReplied.turnId)).state
        val t = afterWrongRecognition.say("아니 그거 말고")
        assertEquals(listOf("사과"), t.state.history.map { it.word })
        assertEquals(WordChainPhase.CHILD_TURN, t.state.phase)
        assertTrue(t.state.turnId > afterWrongRecognition.turnId)
    }

    @Test
    fun lowConfidenceIsConfirmedBeforeJudging() {
        val ask = childTurn().say("과자", confidence = 0.3f)
        assertEquals(PendingQuestion.ConfirmWord("과자"), ask.state.pending)
        assertEquals(1, ask.state.history.size)
        val yes = ask.state.say("응")
        assertEquals("과자", yes.state.history[1].word)
    }

    @Test
    fun overlappingPausesResumeOnlyWhenAllCleared() {
        val s = childTurn()
        val phone = s.reduce(WordChainAction.Pause(PauseReason.PHONE_CALL))
        assertEquals(WordChainStatus.PAUSED, phone.state.status)
        assertTrue(phone.effects.contains(WordChainEffect.StopListening))

        val navi = phone.state.reduce(WordChainAction.Pause(PauseReason.NAVIGATION)).state
        val phoneEnded = navi.reduce(WordChainAction.Resume(PauseReason.PHONE_CALL))
        assertEquals(WordChainStatus.PAUSED, phoneEnded.state.status)
        assertTrue(phoneEnded.effects.isEmpty())

        // 일시정지 중에 도착한 예전 STT 결과는 무시
        val stale = phoneEnded.state.reduce(ChildSpoke("과자", s.turnId))
        assertEquals(1, stale.state.history.size)

        val resumed = phoneEnded.state.reduce(WordChainAction.Resume(PauseReason.NAVIGATION))
        assertEquals(WordChainStatus.CHILD_TURN, resumed.state.status)
        assertEquals(s.history, resumed.state.history)
        assertTrue(resumed.spoken(), resumed.spoken().contains("'사과'"))
    }

    @Test
    fun recognitionFailuresAreNotWrongAnswers() {
        var s = childTurn()
        repeat(3) { s = s.reduce(WordChainAction.RecognitionFailed(s.turnId)).state }
        assertTrue(s.suggestTextInput)
        assertEquals(1, s.history.size)
        assertEquals(WordChainStatus.CHILD_TURN, s.status)
    }

    @Test
    fun networkErrorKeepsStateAndRetries() {
        val s = childTurn()
        val err = s.reduce(WordChainAction.NetworkFailed(s.turnId)).state
        assertEquals(WordChainStatus.NETWORK_ERROR, err.status)
        val retry = err.reduce(WordChainAction.Retry)
        assertEquals(WordChainStatus.CHILD_TURN, retry.state.status)
        assertEquals(s.history, retry.state.history)
    }

    @Test
    fun vehicleOffEndsSessionButKeepsSettings() {
        val settings = WordChainGameSettings(Difficulty.HARD, hintsEnabled = false)
        val s = childTurn(settings).say("과자").state
        val off = s.reduce(WordChainAction.VehicleOff)
        assertEquals(WordChainStatus.READY, off.state.status)
        assertTrue(off.state.history.isEmpty())
        assertEquals(settings, off.state.settings)
        assertTrue(off.effects.contains(WordChainEffect.GameOver(GameResult.SESSION_ENDED)))
    }

    @Test
    fun silenceTwiceGivesAutomaticHintOnlyWhenEnabled() {
        var s = childTurn()
        s = s.reduce(WordChainAction.NoInput(s.turnId)).state
        val second = s.reduce(WordChainAction.NoInput(s.turnId))
        assertNotNull(second.state.hint)

        var off = childTurn(WordChainGameSettings(hintsEnabled = false))
        off = off.reduce(WordChainAction.NoInput(off.turnId)).state
        val secondOff = off.reduce(WordChainAction.NoInput(off.turnId))
        assertFalse(secondOff.state.hint != null)
    }
}

class WordChainGameDisplayTest {
    private val lexicon = WordLexicon.parse("1|과일|사과\n1|간식|과자\n1|탈것|자동차")
    private val game = WordChainGame(
        object : WordProvider {
            override fun nextWord(previousWord: String?, settings: WordChainGameSettings, usedWords: Set<String>) =
                if (previousWord == null) lexicon.find("사과")
                else lexicon.startingWith(WordChainRules.allowedStarts(previousWord)).firstOrNull { it.word !in usedWords }
        },
        HintProvider(lexicon, kotlin.random.Random(0)),
    )

    private fun childTurn(): WordChainState {
        val started = game.reduce(game.newGame(WordChainGameSettings()), WordChainAction.Start).state
        return game.reduce(started, WordChainAction.SpeechFinished(started.turnId)).state
    }

    @Test
    fun screenTextIsShort() {
        assertEquals("'과'로 시작하는 단어를 말해 줘!", childTurn().lastDisplay)
        val wrong = game.reduce(childTurn(), WordChainAction.TextEntered("자동차")).state
        assertEquals("'자동차'는 '과'로 시작하지 않아.", wrong.lastDisplay)
    }

    @Test
    fun exampleButtonGivesWordAndContinues() {
        val t = game.reduce(childTurn(), WordChainAction.ExampleButton)
        assertEquals(listOf("사과", "과자", "자동차"), t.state.history.map { it.word })
        assertEquals(Speaker.HELPER, t.state.history[1].speaker)
    }
}

class WordChainGameVoiceTest {
    private val lexicon = WordLexicon.parse("1|과일|사과\n1|간식|과자\n1|탈것|자동차")
    private val game = WordChainGame(
        object : WordProvider {
            override fun nextWord(previousWord: String?, settings: WordChainGameSettings, usedWords: Set<String>) =
                if (previousWord == null) lexicon.find("사과")
                else lexicon.startingWith(WordChainRules.allowedStarts(previousWord)).firstOrNull { it.word !in usedWords }
        },
        HintProvider(lexicon, kotlin.random.Random(0)),
    )

    private fun childTurn(hints: Boolean = true): WordChainState {
        val started = game.reduce(game.newGame(WordChainGameSettings(hintsEnabled = hints)), WordChainAction.Start).state
        return game.reduce(started, WordChainAction.SpeechFinished(started.turnId)).state
    }

    @Test
    fun repeatedSilenceStopsAutoListeningUntilMicButton() {
        var s = childTurn(hints = false)
        repeat(WordChainGame.MAX_NO_INPUT) {
            s = game.reduce(s, WordChainAction.NoInput(s.turnId)).state
        }
        assertTrue(s.waitingForMic)
        val done = game.reduce(s, WordChainAction.SpeechFinished(s.turnId))
        assertTrue(done.effects.none { it is WordChainEffect.StartListening })

        val mic = game.reduce(done.state, WordChainAction.MicButton)
        assertFalse(mic.state.waitingForMic)
        assertTrue(mic.effects.contains(WordChainEffect.StartListening(mic.state.turnId)))
    }

    @Test
    fun micButtonInterruptsAiSpeech() {
        val started = game.reduce(game.newGame(WordChainGameSettings()), WordChainAction.Start).state
        val mic = game.reduce(started, WordChainAction.MicButton)
        assertEquals(WordChainStatus.CHILD_TURN, mic.state.status)
        assertEquals(WordChainEffect.StopSpeaking, mic.effects.first())
    }

    @Test
    fun voiceUnavailableSuggestsTextInputOnce() {
        val first = game.reduce(childTurn(), WordChainAction.VoiceUnavailable)
        assertTrue(first.state.suggestTextInput)
        assertTrue(first.effects.any { it is WordChainEffect.Speak })
        val second = game.reduce(first.state, WordChainAction.VoiceUnavailable)
        assertTrue(second.effects.isEmpty())
    }
}

class WordChainGameSentenceTest {
    private val lexicon = WordLexicon.parse("1|과일|사과\n1|간식|과자\n1|탈것|자동차")
    private val game = WordChainGame(
        object : WordProvider {
            override fun nextWord(previousWord: String?, settings: WordChainGameSettings, usedWords: Set<String>) =
                if (previousWord == null) lexicon.find("사과")
                else lexicon.startingWith(WordChainRules.allowedStarts(previousWord)).firstOrNull { it.word !in usedWords }
        },
        HintProvider(lexicon, kotlin.random.Random(0)),
    )

    private fun childTurn(): WordChainState {
        val started = game.reduce(game.newGame(WordChainGameSettings()), WordChainAction.Start).state
        return game.reduce(started, WordChainAction.SpeechFinished(started.turnId)).state
    }

    @Test
    fun picksMatchingWordInsideSentence() {
        val s = childTurn()
        val t = game.reduce(s, WordChainAction.ChildSpoke("나는 과자 할래", s.turnId))
        assertEquals("과자", t.state.history[1].word)
    }

    @Test
    fun unrelatedSentenceIsNotTreatedAsWrongAnswer() {
        val s = childTurn()
        val t = game.reduce(s, WordChainAction.ChildSpoke("다른 건 안되는 거야 되는 건데", s.turnId))
        assertEquals(1, t.state.history.size)
        assertTrue(t.state.lastSpeech!!.startsWith("잘 못 들었어"))
    }
}
