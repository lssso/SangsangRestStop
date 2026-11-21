package com.sangsangreststop.app.game.revealwho

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RevealWhoGameTest {

    private val giraffe = RevealWhoQuestion(
        "기린", RevealWhoTopic.ANIMAL, RevealWhoLevel.EASY,
        listOf("초원에 살아요", "무늬가 있어요", "잎을 먹어요", "목이 길어요"),
    )
    private val dog = RevealWhoQuestion(
        "강아지", RevealWhoTopic.ANIMAL, RevealWhoLevel.EASY,
        listOf("꼬리를 흔들어요", "산책을 좋아해요", "냄새를 잘 맡아요", "멍멍 짖어요"),
        synonyms = listOf("개", "멍멍이"),
    )
    private val game = RevealWhoGame { listOf(giraffe, dog) }

    private fun reduce(state: RevealWhoState, action: RevealWhoAction) = game.reduce(state, action)

    /** 시작해서 첫 힌트를 다 듣고 아이 차례가 된 상태 */
    private fun answering(): RevealWhoState {
        val started = reduce(game.newGame(RevealWhoGameSettings()), RevealWhoAction.Start).state
        return listen(started)
    }

    /** 앱의 말이 끝나 듣기 시작한 상태 */
    private fun listen(state: RevealWhoState): RevealWhoState =
        reduce(state, RevealWhoAction.SpeechFinished(state.turnId)).state

    private fun say(state: RevealWhoState, text: String) =
        reduce(state, RevealWhoAction.ChildSpoke(text, state.turnId))

    private fun List<RevealWhoEffect>.spoken(): String =
        filterIsInstance<RevealWhoEffect.Speak>().single().text

    @Test
    fun `start speaks the first hint and listens only after speech finishes`() {
        val t = reduce(game.newGame(RevealWhoGameSettings()), RevealWhoAction.Start)

        assertEquals(RevealWhoPhase.HINT, t.state.phase)
        assertTrue(t.effects.spoken().contains("첫 번째 힌트! 초원에 살아요"))
        assertTrue(t.effects.none { it is RevealWhoEffect.StartListening })

        val after = reduce(t.state, RevealWhoAction.SpeechFinished(t.state.turnId))
        assertEquals(RevealWhoPhase.ANSWER, after.state.phase)
        assertEquals(listOf(RevealWhoEffect.StartListening(t.state.turnId)), after.effects)
    }

    @Test
    fun `correct answer scores and moves to the next question`() {
        val t = say(answering(), "기린이요")

        assertEquals(1, t.state.score)
        assertEquals(1, t.state.questionIndex)
        assertEquals(0, t.state.hintIndex)
        assertTrue(t.effects.spoken().contains("정답은 '기린'"))
        assertTrue(t.effects.spoken().contains("두 번째 문제! 첫 번째 힌트! 꼬리를 흔들어요"))
    }

    @Test
    fun `wrong answer gives the next hint without scoring`() {
        val t = say(answering(), "코끼리")

        assertEquals(0, t.state.score)
        assertEquals(0, t.state.questionIndex)
        assertEquals(1, t.state.hintIndex)
        assertTrue(t.effects.spoken().contains("두 번째 힌트! 무늬가 있어요"))
    }

    @Test
    fun `wrong at the last hint allows one more try then reveals the answer`() {
        var state = answering()
        // 같은 오답을 반복하면 힌트를 쓰지 않으므로 매번 다른 오답을 말한다
        for (wrong in listOf("코끼리", "사자", "하마")) state = listen(say(state, wrong).state)
        assertTrue(state.isLastHint)

        val retry = say(state, "호랑이")
        assertEquals(0, retry.state.questionIndex)
        assertTrue(retry.effects.spoken().contains("한 번만 더"))

        val revealed = say(listen(retry.state), "판다")
        assertEquals(1, revealed.state.questionIndex)
        assertEquals(0, revealed.state.score)
        assertTrue(revealed.effects.spoken().contains("정답은 '기린'이었어"))
    }

    @Test
    fun `hint request moves to next hint and give up reveals the answer`() {
        val hint = say(answering(), "힌트 줘")
        assertEquals(1, hint.state.hintIndex)

        val giveUp = say(listen(hint.state), "포기할래")
        assertEquals(1, giveUp.state.questionIndex)
        assertEquals(0, giveUp.state.score)
    }

    @Test
    fun `replay repeats the current hint`() {
        val t = say(answering(), "다시 말해 줘")

        assertEquals(0, t.state.hintIndex)
        assertTrue(t.effects.spoken().contains("첫 번째 힌트! 초원에 살아요"))
    }

    @Test
    fun `next hint button at the last hint reveals and advances so the game works without voice`() {
        var state = answering()
        repeat(3) { state = reduce(state, RevealWhoAction.NextHintButton).state }
        assertEquals(3, state.hintIndex)

        val t = reduce(state, RevealWhoAction.NextHintButton)
        assertEquals(1, t.state.questionIndex)
        assertEquals(RevealWhoEffect.StopListening, t.effects.first())
    }

    @Test
    fun `finishing the last question ends the game with a summary`() {
        val second = listen(say(answering(), "기린").state)
        val t = say(second, "멍멍이")

        assertEquals(RevealWhoPhase.FINISHED, t.state.phase)
        assertEquals(2, t.state.score)
        assertTrue(t.effects.spoken().contains("모두 맞혔어"))
        assertTrue(t.effects.contains(RevealWhoEffect.GameOver(score = 2, total = 2)))
        // 끝난 뒤에는 듣지 않는다
        assertTrue(reduce(t.state, RevealWhoAction.SpeechFinished(t.state.turnId)).effects.isEmpty())
    }

    @Test
    fun `silence repeats then adds a hint then waits for the mic button`() {
        var state = answering()

        val first = reduce(state, RevealWhoAction.NoInput(state.turnId))
        assertEquals(0, first.state.hintIndex)
        assertTrue(first.effects.spoken().contains("천천히"))
        state = listen(first.state)

        val second = reduce(state, RevealWhoAction.NoInput(state.turnId))
        assertEquals(1, second.state.hintIndex)
        state = listen(second.state)

        val third = reduce(state, RevealWhoAction.NoInput(state.turnId))
        assertTrue(third.state.waitingForMic)
        // 마이크 버튼을 기다리는 동안에는 자동으로 듣지 않는다
        val idle = reduce(third.state, RevealWhoAction.SpeechFinished(third.state.turnId))
        assertTrue(idle.effects.isEmpty())

        val mic = reduce(idle.state, RevealWhoAction.MicButton)
        assertFalse(mic.state.waitingForMic)
        assertEquals(
            listOf(RevealWhoEffect.StopSpeaking, RevealWhoEffect.StartListening(mic.state.turnId)),
            mic.effects,
        )
    }

    @Test
    fun `recognition failures are not wrong answers and end in the speech error popup`() {
        var state = answering()
        repeat(2) {
            val t = reduce(state, RevealWhoAction.RecognitionFailed(state.turnId))
            assertEquals(0, t.state.hintIndex)
            assertEquals(null, t.state.error)
            state = listen(t.state)
        }
        val third = reduce(state, RevealWhoAction.RecognitionFailed(state.turnId))

        assertEquals(RevealWhoError.SPEECH_RECOGNITION, third.state.error)
        assertEquals(0, third.state.hintIndex)
        assertEquals(listOf(RevealWhoEffect.StopListening, RevealWhoEffect.StopSpeaking), third.effects)
    }

    @Test
    fun `voice unavailable is announced once and keeps waiting for buttons`() {
        val state = answering()
        val t = reduce(state, RevealWhoAction.VoiceUnavailable)
        assertTrue(t.effects.spoken().contains("글자로 정답"))

        val next = reduce(listen(t.state), RevealWhoAction.NextHintButton)
        assertTrue(next.state.waitingForMic)
        assertTrue(reduce(next.state, RevealWhoAction.SpeechFinished(next.state.turnId)).effects.isEmpty())
    }

    @Test
    fun `pause stops audio and drops stale results, resume repeats the hint`() {
        val state = answering()
        val paused = reduce(state, RevealWhoAction.Pause(RevealWhoPauseReason.PHONE_CALL))
        assertEquals(listOf(RevealWhoEffect.StopListening, RevealWhoEffect.StopSpeaking), paused.effects)

        // 일시정지 전에 시작된 듣기의 결과와 화면 버튼은 무시한다
        assertEquals(paused.state, reduce(paused.state, RevealWhoAction.ChildSpoke("기린", state.turnId)).state)
        assertEquals(paused.state, reduce(paused.state, RevealWhoAction.NextHintButton).state)
        assertEquals(paused.state, reduce(paused.state, RevealWhoAction.MicButton).state)

        // 다른 사유가 남아 있으면 재개하지 않는다
        val both = reduce(paused.state, RevealWhoAction.Pause(RevealWhoPauseReason.NAVIGATION)).state
        val stillPaused = reduce(both, RevealWhoAction.Resume(RevealWhoPauseReason.PHONE_CALL))
        assertTrue(stillPaused.state.isPaused)
        assertTrue(stillPaused.effects.isEmpty())

        val resumed = reduce(stillPaused.state, RevealWhoAction.Resume(RevealWhoPauseReason.NAVIGATION))
        assertFalse(resumed.state.isPaused)
        assertTrue(resumed.effects.spoken().contains("첫 번째 힌트! 초원에 살아요"))
    }

    @Test
    fun `stale turn results are ignored`() {
        val state = answering()
        val t = reduce(state, RevealWhoAction.ChildSpoke("기린", state.turnId - 1))

        assertEquals(state, t.state)
        assertTrue(t.effects.isEmpty())
    }

    @Test
    fun `stop ends the game silently`() {
        val t = reduce(answering(), RevealWhoAction.Stop)

        assertEquals(RevealWhoPhase.FINISHED, t.state.phase)
        assertTrue(t.effects.contains(RevealWhoEffect.StopSpeaking))
        assertTrue(t.effects.none { it is RevealWhoEffect.Speak })
    }

    @Test
    fun `judge accepts synonyms and endings but not accidental one-letter matches`() {
        val ship = RevealWhoQuestion("배", RevealWhoTopic.VEHICLE, RevealWhoLevel.EASY, listOf("a", "b", "c", "d"))

        assertTrue(RevealWhoAnswerJudge.isCorrect("음 강아지 같아요", dog))
        assertTrue(RevealWhoAnswerJudge.isCorrect("개", dog))
        assertTrue(RevealWhoAnswerJudge.isCorrect("개요", dog))
        assertFalse(RevealWhoAnswerJudge.isCorrect("개구리", dog))
        assertTrue(RevealWhoAnswerJudge.isCorrect("배야!", ship))
        assertFalse(RevealWhoAnswerJudge.isCorrect("배고파", ship))
        assertFalse(RevealWhoAnswerJudge.isCorrect("", dog))
    }

    @Test
    fun `judge classifies commands and treats long sentences as unclear`() {
        assertEquals(RevealWhoUtterance.STOP, RevealWhoAnswerJudge.classify("그만할래", giraffe))
        assertEquals(RevealWhoUtterance.PAUSE, RevealWhoAnswerJudge.classify("잠깐만", giraffe))
        assertEquals(RevealWhoUtterance.NEXT_HINT, RevealWhoAnswerJudge.classify("모르겠어", giraffe))
        assertEquals(RevealWhoUtterance.REVEAL, RevealWhoAnswerJudge.classify("정답 알려 줘", giraffe))
        assertEquals(RevealWhoUtterance.WRONG, RevealWhoAnswerJudge.classify("얼룩말", giraffe))
        assertEquals(RevealWhoUtterance.UNCLEAR, RevealWhoAnswerJudge.classify("엄마 우리 언제 도착해 배고파", giraffe))
    }

    // ---- 완성 작업에서 추가한 테스트 ----

    @Test
    fun `questions are picked by the chosen topic and level`() {
        for (topic in RevealWhoTopic.entries) for (level in RevealWhoLevel.entries) {
            val state = RevealWhoGame().newGame(RevealWhoGameSettings(level, topic))
            assertEquals(5, state.questions.size)
            assertTrue("$topic/$level", state.questions.all { it.topic == topic && it.level == level })
        }
    }

    @Test
    fun `hints are given in order and the answer is revealed when they run out`() {
        var state = answering()
        val heard = mutableListOf(state.currentHint)
        repeat(3) {
            state = listen(reduce(state, RevealWhoAction.NextHintButton).state)
            heard += state.currentHint
        }
        assertEquals(giraffe.hints, heard)

        val revealed = reduce(state, RevealWhoAction.NextHintButton)
        assertEquals(1, revealed.state.questionIndex)
        assertTrue(revealed.effects.spoken().contains("정답은 '기린'이었어"))
    }

    @Test
    fun `restart from the end starts a fresh game`() {
        val finished = reduce(answering(), RevealWhoAction.Stop).state
        val restarted = reduce(game.newGame(finished.settings), RevealWhoAction.Start)

        assertEquals(RevealWhoPhase.HINT, restarted.state.phase)
        assertEquals(0, restarted.state.questionIndex)
        assertEquals(0, restarted.state.score)
        assertTrue(restarted.effects.spoken().contains("첫 번째 문제"))
    }

    @Test
    fun `the same STT result delivered twice is judged only once`() {
        val state = answering()
        val first = reduce(state, RevealWhoAction.ChildSpoke("기린", state.turnId))
        val duplicate = reduce(first.state, RevealWhoAction.ChildSpoke("기린", state.turnId))

        assertEquals(1, first.state.score)
        assertEquals(first.state, duplicate.state)
        assertTrue(duplicate.effects.isEmpty())
    }

    @Test
    fun `a late answer from the previous question does not score the next one`() {
        val state = answering()
        val moved = reduce(state, RevealWhoAction.NextHintButton).let { t ->
            // 마지막 힌트까지 넘겨서 정답을 공개하고 다음 문제로 간다
            var s = t.state
            repeat(3) { s = reduce(s, RevealWhoAction.NextHintButton).state }
            s
        }
        assertEquals(1, moved.questionIndex)

        val late = reduce(listen(moved), RevealWhoAction.ChildSpoke("멍멍이", state.turnId))
        assertEquals(0, late.state.score)
        assertTrue(late.effects.isEmpty())
    }

    @Test
    fun `callbacks after the game ended are ignored`() {
        val ended = reduce(answering(), RevealWhoAction.Stop).state
        val actions = listOf(
            RevealWhoAction.ChildSpoke("기린", ended.turnId),
            RevealWhoAction.NoInput(ended.turnId),
            RevealWhoAction.RecognitionFailed(ended.turnId),
            RevealWhoAction.NetworkFailed(ended.turnId),
            RevealWhoAction.SpeechFinished(ended.turnId),
            RevealWhoAction.TextEntered("기린"),
            RevealWhoAction.MicButton,
            RevealWhoAction.NextHintButton,
        )
        actions.forEach { action ->
            val t = reduce(ended, action)
            assertEquals(action.toString(), ended, t.state)
            assertTrue(action.toString(), t.effects.isEmpty())
        }
    }

    @Test
    fun `pause blocks progress until every reason is released`() {
        val paused = reduce(answering(), RevealWhoAction.Pause(RevealWhoPauseReason.USER)).state
        // 일시정지 중에는 TTS 완료가 와도 듣기 시작하지 않고, 다음 문제로도 넘어가지 않는다
        assertTrue(reduce(paused, RevealWhoAction.SpeechFinished(paused.turnId)).effects.isEmpty())
        assertEquals(paused, reduce(paused, RevealWhoAction.TextEntered("기린")).state)
        assertEquals(0, paused.questionIndex)

        val resumed = reduce(paused, RevealWhoAction.Resume(RevealWhoPauseReason.USER))
        assertFalse(resumed.state.isPaused)
        assertEquals(RevealWhoPhase.HINT, resumed.state.phase)
    }

    @Test
    fun `a near-miss is confirmed instead of being marked wrong`() {
        val asked = say(answering(), "기림")
        assertEquals(0, asked.state.hintIndex)
        assertTrue(asked.state.confirmingAnswer)
        assertTrue(asked.effects.spoken().contains("'기린'이라고 말했어?"))

        val yes = say(listen(asked.state), "응")
        assertEquals(1, yes.state.score)
        assertEquals(1, yes.state.questionIndex)
    }

    @Test
    fun `saying no to the confirmation asks again without using a hint`() {
        val asked = say(answering(), "기림")
        val no = say(listen(asked.state), "아니")

        assertEquals(0, no.state.score)
        assertEquals(0, no.state.hintIndex)
        assertFalse(no.state.confirmingAnswer)
        // "응"은 되묻는 중이 아닐 때 정답으로 치지 않는다
        val plainYes = say(listen(no.state), "응")
        assertEquals(0, plainYes.state.score)
    }

    @Test
    fun `repeating the same wrong answer keeps the current hint`() {
        val first = say(answering(), "코끼리")
        assertEquals(1, first.state.hintIndex)

        val again = say(listen(first.state), "코끼리요")
        assertEquals(1, again.state.hintIndex)
        assertTrue(again.effects.spoken().contains("'코끼리'는 아까 말했어"))
    }

    @Test
    fun `network error shows the popup and retry repeats the hint`() {
        val state = answering()
        val failed = reduce(state, RevealWhoAction.NetworkFailed(state.turnId))
        assertEquals(RevealWhoError.NETWORK, failed.state.error)
        // 오류 중에는 듣기와 버튼 진행이 멈춘다
        assertTrue(reduce(failed.state, RevealWhoAction.SpeechFinished(failed.state.turnId)).effects.isEmpty())
        assertEquals(failed.state, reduce(failed.state, RevealWhoAction.NextHintButton).state)

        val retried = reduce(failed.state, RevealWhoAction.Retry)
        assertEquals(null, retried.state.error)
        assertTrue(retried.effects.spoken().contains("첫 번째 힌트! 초원에 살아요"))
    }

    @Test
    fun `typed answers work when voice input is unavailable`() {
        val unavailable = listen(reduce(answering(), RevealWhoAction.VoiceUnavailable).state)
        val t = reduce(unavailable, RevealWhoAction.TextEntered("기린"))

        assertEquals(1, t.state.score)
        assertEquals(RevealWhoEffect.StopListening, t.effects.first())
        // 다음 문제에서도 자동 듣기 대신 글자 입력을 기다린다
        assertTrue(reduce(t.state, RevealWhoAction.SpeechFinished(t.state.turnId)).effects.isEmpty())
    }

    @Test
    fun `digits from STT are read as Korean before judging`() {
        val five = RevealWhoQuestion("오이", RevealWhoTopic.OBJECT, RevealWhoLevel.EASY, listOf("a", "b", "c", "d"))
        assertTrue(RevealWhoAnswerJudge.isCorrect("52", five).not())
        assertTrue(RevealWhoAnswerJudge.isCorrect("오이", five))
        val diary = RevealWhoQuestion("일기", RevealWhoTopic.OBJECT, RevealWhoLevel.EASY, listOf("a", "b", "c", "d"))
        assertTrue(RevealWhoAnswerJudge.isCorrect("1기", diary))
    }

    @Test
    fun `question bank has enough valid questions for every topic and level`() {
        for (topic in RevealWhoTopic.entries) for (level in RevealWhoLevel.entries) {
            val settings = RevealWhoGameSettings(level, topic)
            val picked = RevealWhoQuestionBank.pick(settings)
            assertEquals("$topic/$level", settings.questionCount, picked.size)
            assertEquals(picked.size, picked.distinctBy { it.answer }.size)
        }
        val all = RevealWhoQuestionBank.all
        assertEquals(all.size, all.distinctBy { it.answer }.size)
        all.forEach { q ->
            assertEquals(q.answer, RevealWhoQuestionBank.HINTS_PER_QUESTION, q.hints.size)
            // 힌트가 정답을 그대로 말해 버리면 안 된다
            assertTrue(q.answer, q.hints.none { q.answer.length >= 2 && q.answer in it })
            // 화면 말풍선 두 줄 안에 들어가는 길이
            assertTrue(q.answer, q.hints.all { it.length <= 22 })
        }
    }
}
