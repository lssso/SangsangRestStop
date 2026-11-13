package com.sangsangreststop.app.game.wordchain

/** 게임 엔진에서 쓰는 난이도. UI 전용 enum(리소스 포함)과 분리한다. */
enum class Difficulty {
    EASY,
    NORMAL,
    HARD;

    fun harder(): Difficulty? = entries.getOrNull(ordinal + 1)
    fun easier(): Difficulty? = entries.getOrNull(ordinal - 1)
}

data class WordChainGameSettings(
    val difficulty: Difficulty = Difficulty.EASY,
    val hintsEnabled: Boolean = true,
)

/**
 * 끝말잇기 규칙 설정 (v1 제안값 — 팀 확정 전까지 여기서만 바꾼다).
 *
 * - 두음법칙 허용: '력'으로 끝나면 '역'으로 시작해도 인정한다.
 * - 한 글자 단어 불가: 최소 2글자.
 * - 아이가 말한 단어는 내장 단어장에 없어도 한글 규칙만 맞으면 인정한다
 *   (난이도·단어장 크기 때문에 정답을 오답 처리하지 않기 위함).
 * - AI는 내장 단어장(검증된 목록)에 있는 단어만 말한다.
 */
data class WordChainRuleConfig(
    val applyInitialSoundLaw: Boolean = true,
    val minLength: Int = 2,
    val maxLength: Int = 10,
)

sealed interface WordCheck {
    data object Valid : WordCheck
    data object NotKorean : WordCheck
    data object TooShort : WordCheck
    data object TooLong : WordCheck
    data class WrongStart(val expected: Set<Char>) : WordCheck
    data object AlreadyUsed : WordCheck
}

object WordChainRules {

    /** STT/텍스트 입력의 공백·문장부호를 제거한다. 한글 음절 외 문자가 남으면 그대로 둬서 판정에서 걸러진다. */
    fun normalize(raw: String): String =
        raw.filterNot { it.isWhitespace() || it in ".,!?~'\"…·-" }

    /** 이전 단어 뒤에 올 수 있는 첫 글자 집합 (두음법칙 포함). */
    fun allowedStarts(previousWord: String, config: WordChainRuleConfig = WordChainRuleConfig()): Set<Char> {
        val last = previousWord.lastOrNull() ?: return emptySet()
        val variant = if (config.applyInitialSoundLaw) Hangul.initialSoundLawVariant(last) else null
        return setOfNotNull(last, variant)
    }

    fun check(
        word: String,
        previousWord: String?,
        usedWords: Set<String>,
        config: WordChainRuleConfig = WordChainRuleConfig(),
    ): WordCheck {
        if (word.isEmpty() || !word.all(Hangul::isSyllable)) return WordCheck.NotKorean
        if (word.length < config.minLength) return WordCheck.TooShort
        if (word.length > config.maxLength) return WordCheck.TooLong
        if (previousWord != null) {
            val starts = allowedStarts(previousWord, config)
            if (word.first() !in starts) return WordCheck.WrongStart(starts)
        }
        if (word in usedWords) return WordCheck.AlreadyUsed
        return WordCheck.Valid
    }
}
