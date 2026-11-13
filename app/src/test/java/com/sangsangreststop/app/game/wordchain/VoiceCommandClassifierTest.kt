package com.sangsangreststop.app.game.wordchain

import com.sangsangreststop.app.game.wordchain.VoiceCommand.Correction
import com.sangsangreststop.app.game.wordchain.VoiceCommand.SetDifficulty
import com.sangsangreststop.app.game.wordchain.VoiceCommand.SetHints
import com.sangsangreststop.app.game.wordchain.VoiceCommand.StepDifficulty
import com.sangsangreststop.app.game.wordchain.VoiceCommand.SuggestDifficulty
import com.sangsangreststop.app.game.wordchain.VoiceCommand.Word
import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceCommandClassifierTest {

    private fun classify(text: String, awaiting: Boolean = false) = VoiceCommandClassifier.classify(text, awaiting)

    @Test
    fun guideExamples() {
        assertEquals(SuggestDifficulty(harder = true), classify("너무 쉬워"))
        assertEquals(SetDifficulty(Difficulty.NORMAL), classify("보통으로 바꿔줘"))
        assertEquals(SetDifficulty(Difficulty.HARD), classify("어렵게 해줘"))
        assertEquals(SetDifficulty(Difficulty.EASY), classify("쉽게 해줘"))
        assertEquals(SetHints(false), classify("힌트 주지 마"))
        assertEquals(SetHints(true), classify("힌트 켜줘"))
        assertEquals(VoiceCommand.RequestHint, classify("힌트 줘"))
    }

    @Test
    fun relativeAndAmbiguousDifficulty() {
        assertEquals(StepDifficulty(harder = true), classify("좀 더 어렵게 해줘"))
        assertEquals(StepDifficulty(harder = false), classify("더 쉽게"))
        assertEquals(SuggestDifficulty(harder = false), classify("너무 어려워"))
    }

    @Test
    fun corrections() {
        assertEquals(Correction(null), classify("아니"))
        assertEquals(Correction(null), classify("다시 말할게"))
        assertEquals(Correction(null), classify("그거 말고"))
        assertEquals(Correction("과자"), classify("아니 과자"))
        assertEquals(Correction("과자"), classify("아니야, 과자"))
    }

    @Test
    fun yesNoOnlyWhenAwaitingAnswer() {
        assertEquals(VoiceCommand.Yes, classify("응", awaiting = true))
        assertEquals(VoiceCommand.No, classify("아니", awaiting = true))
        assertEquals(Word("응"), classify("응"))
        // 단어 '어묵'은 '어'(예)로 오인하지 않는다
        assertEquals(Word("어묵"), classify("어묵", awaiting = true))
    }

    @Test
    fun otherCommands() {
        assertEquals(VoiceCommand.Repeat, classify("뭐라고?"))
        assertEquals(VoiceCommand.GiveUp, classify("포기할래"))
        assertEquals(VoiceCommand.RequestHint, classify("모르겠어"))
        assertEquals(VoiceCommand.Pause, classify("잠깐 멈춰"))
        assertEquals(VoiceCommand.Stop, classify("그만할래"))
    }

    @Test
    fun plainWords() {
        assertEquals(Word("과자"), classify("과자"))
        assertEquals("과자", (classify("음 과자!") as Word).word)
        assertEquals(Word("자동차"), classify("자동차"))
    }
}
