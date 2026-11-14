package com.sangsangreststop.app.game.wordchain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpokenNumbersTest {

    @Test
    fun `reads numbers the way they are spoken`() {
        assertEquals("영", SpokenNumbers.readNumber("0"))
        assertEquals("일", SpokenNumbers.readNumber("1"))
        assertEquals("십", SpokenNumbers.readNumber("10"))
        assertEquals("이십오", SpokenNumbers.readNumber("25"))
        assertEquals("백일", SpokenNumbers.readNumber("101"))
        assertEquals("만", SpokenNumbers.readNumber("10000"))
        assertEquals("삼만오천", SpokenNumbers.readNumber("35000"))
    }

    @Test
    fun `replaces digits inside STT text`() {
        assertEquals("일기", SpokenNumbers.toHangul("1기"))
        assertEquals("이불", SpokenNumbers.toHangul("2불"))
        assertEquals("오십이 주세요", SpokenNumbers.toHangul("52 주세요"))
        assertEquals("과자", SpokenNumbers.toHangul("과자"))
    }

    @Test
    fun `word chain accepts a word that STT wrote with a digit`() {
        assertEquals(VoiceCommand.Word("일기"), VoiceCommandClassifier.classify("1기"))
    }

    @Test
    fun `AI can continue after the endings that used to end the game at once`() {
        val provider = LexiconWordProvider()
        val settings = WordChainGameSettings()
        for (word in listOf("일본", "자동차", "김밥", "바람", "거울", "멜론", "블록", "계란", "볼링")) {
            assertTrue(word, provider.nextWord(word, settings, setOf(word)) != null)
        }
    }

    @Test
    fun `AI prefers words whose next turn it can still answer`() {
        // '가'로 시작하는 후보 둘 모두 아이가 이을 단어가 있다.
        // '가방' → 아이 '방석' → AI '석유'로 이어지지만, '가지' → 아이 '지붕' 뒤에는 AI가 막힌다.
        val lexicon = WordLexicon.parse("1|물건|가방 가지 방석 석유 지붕")
        val provider = LexiconWordProvider(lexicon)
        repeat(20) {
            assertEquals("가방", provider.nextWord("자가", WordChainGameSettings(), setOf("자가"))?.word)
        }
    }
}
