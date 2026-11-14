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
        repeat(3) { state = listen(say(state, "코끼리").state) }
        assertTrue(state.isLastHint)

        val retry = say(state, "코끼리")
        assertEquals(0, retry.state.questionIndex)
        assertTrue(retry.effects.spoken().contains("한 번만 더"))

        val revealed = say(listen(retry.state), "코끼리")
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
    fun `recognition failures are not wrong answers and end in waiting for the mic`() {
        var state = answering()
        repeat(2) {
            val t = reduce(state, RevealWhoAction.RecognitionFailed(state.turnId))
            assertEquals(0, t.state.hintIndex)
            state = listen(t.state)
        }
        val third = reduce(state, RevealWhoAction.RecognitionFailed(state.turnId))

        assertTrue(third.state.waitingForMic)
        assertEquals(0, third.state.hintIndex)
    }

    @Test
    fun `voice unavailable is announced once and keeps waiting for buttons`() {
        val state = answering()
        val t = reduce(state, RevealWhoAction.VoiceUnavailable)
        assertTrue(t.effects.spoken().contains("다음 힌트 버튼"))

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
