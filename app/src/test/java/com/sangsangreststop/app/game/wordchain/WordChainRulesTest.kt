package com.sangsangreststop.app.game.wordchain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class HangulTest {
    @Test
    fun initialSoundLaw() {
        assertEquals('역', Hangul.initialSoundLawVariant('력'))
        assertEquals('이', Hangul.initialSoundLawVariant('리'))
        assertEquals('노', Hangul.initialSoundLawVariant('로'))
        assertEquals('나', Hangul.initialSoundLawVariant('라'))
        assertEquals('여', Hangul.initialSoundLawVariant('녀'))
        assertEquals('요', Hangul.initialSoundLawVariant('료'))
        assertNull(Hangul.initialSoundLawVariant('과'))
        assertNull(Hangul.initialSoundLawVariant('나'))
    }

    @Test
    fun josa() {
        assertEquals("로", Hangul.josaRo("과"))
        assertEquals("로", Hangul.josaRo("물")) // ㄹ 받침
        assertEquals("으로", Hangul.josaRo("통"))
        assertEquals("가", Hangul.josa("사과", "이", "가"))
        assertEquals("이", Hangul.josa("동물", "이", "가"))
    }
}

class WordChainRulesTest {
    @Test
    fun validChain() {
        assertEquals(WordCheck.Valid, WordChainRules.check("과자", "사과", emptySet()))
    }

    @Test
    fun initialSoundLawIsAccepted() {
        assertEquals(WordCheck.Valid, WordChainRules.check("역사", "노력", emptySet()))
        assertEquals(WordCheck.Valid, WordChainRules.check("력사", "노력", emptySet()))
    }

    @Test
    fun initialSoundLawCanBeDisabled() {
        val check = WordChainRules.check("역사", "노력", emptySet(), WordChainRuleConfig(applyInitialSoundLaw = false))
        assertTrue(check is WordCheck.WrongStart)
    }

    @Test
    fun rejectsWrongStartDuplicatesAndShortWords() {
        assertEquals(WordCheck.WrongStart(setOf('과')), WordChainRules.check("자동차", "사과", emptySet()))
        assertEquals(WordCheck.AlreadyUsed, WordChainRules.check("과자", "사과", setOf("과자")))
        assertEquals(WordCheck.TooShort, WordChainRules.check("과", "사과", emptySet()))
        assertEquals(WordCheck.NotKorean, WordChainRules.check("apple", null, emptySet()))
    }

    @Test
    fun normalizeStripsSpacesAndPunctuation() {
        assertEquals("과자", WordChainRules.normalize(" 과자! "))
        assertEquals("자동차", WordChainRules.normalize("자 동 차."))
    }
}

class WordLexiconTest {
    @Test
    fun defaultListIsWellFormed() {
        val lexicon = WordLexicon.Default
        assertTrue("단어 수: ${lexicon.entries.size}", lexicon.entries.size > 400)
        lexicon.entries.forEach {
            assertTrue("한글 2글자 이상이어야 함: ${it.word}", it.word.length >= 2 && it.word.all(Hangul::isSyllable))
            assertTrue("레벨 범위: ${it.word}", it.level in 1..3)
        }
        assertTrue((1..3).all { level -> lexicon.entries.any { it.level == level } })
    }
}

class WordProviderTest {
    private val lexicon = WordLexicon.parse(
        """
        1|과일|사과
        1|음식|과자 자두부침
        2|음식|과메기
        1|탈것|자동차 차고
        1|장소|고모집
        """.trimIndent()
    )

    @Test
    fun picksConnectableWordStartingWithLastSyllable() {
        val provider = LexiconWordProvider(lexicon, Random(1))
        repeat(20) {
            val entry = provider.nextWord("사과", WordChainGameSettings(Difficulty.EASY), setOf("사과"))!!
            assertEquals("과자", entry.word) // 과메기는 레벨 2라서 쉬움에서는 뒤로 밀린다
        }
    }

    @Test
    fun returnsNullWhenNoWordLeft() {
        val provider = LexiconWordProvider(lexicon, Random(1))
        assertNull(provider.nextWord("고모집", WordChainGameSettings(), setOf("고모집")))
    }

    @Test
    fun neverRepeatsUsedWords() {
        val provider = LexiconWordProvider(lexicon, Random(1))
        val entry = provider.nextWord("사과", WordChainGameSettings(), setOf("사과", "과자"))
        assertEquals("과메기", entry?.word)
    }
}

class HintProviderTest {
    private val lexicon = WordLexicon.parse(
        """
        1|간식|과자
        1|과일|과일
        """.trimIndent()
    )

    @Test
    fun hintsAreStepwiseAndUnlimited() {
        val provider = HintProvider(lexicon, Random(3))
        val settings = WordChainGameSettings()
        var progress: HintProgress? = null
        val texts = mutableListOf<String>()
        repeat(7) {
            val hint = provider.nextHint("사과", settings, setOf("사과"), progress)!!
            texts += hint.text
            progress = hint.progress
        }
        assertTrue(texts[0], texts[0].contains("'과'로 시작하는"))
        assertTrue(texts[1], texts[1].contains("2글자"))
        assertTrue(texts[2], texts[2].contains("따라 말해 봐"))
        // 3단계 이후에는 다른 단어로 다시 1단계부터
        assertTrue(texts[3], texts[3].contains("'과'로 시작하는"))
        assertTrue(texts.size == 7)
    }
}
