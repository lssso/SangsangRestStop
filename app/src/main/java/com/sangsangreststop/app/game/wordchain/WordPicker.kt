package com.sangsangreststop.app.game.wordchain

import kotlin.random.Random

/**
 * 앱 차례에 말할 단어를 고른다. 단어장에 있는 단어만 사용하고 규칙 엔진 검증을 통과한다.
 */
interface WordProvider {
    /** previousWord가 null이면 게임 첫 단어. 이어갈 단어가 없으면 null. */
    fun nextWord(previousWord: String?, settings: WordChainGameSettings, usedWords: Set<String>): LexiconEntry?
}

class LexiconWordProvider(
    private val lexicon: WordLexicon = WordLexicon.Default,
    private val random: Random = Random.Default,
    private val config: WordChainRuleConfig = WordChainRuleConfig(),
) : WordProvider {

    override fun nextWord(
        previousWord: String?,
        settings: WordChainGameSettings,
        usedWords: Set<String>,
    ): LexiconEntry? {
        val candidates = (if (previousWord == null) lexicon.entries
        else lexicon.startingWith(WordChainRules.allowedStarts(previousWord, config)))
            .filter { WordChainRules.check(it.word, previousWord, usedWords, config) == WordCheck.Valid }
        if (candidates.isEmpty()) return null

        val pool = levelPool(candidates, settings.difficulty)
        // 아이가 이어갈 수 있는 끝 글자를 우선한다 (쉬움은 레벨 1 단어로 이어갈 수 있어야 함).
        val childLevelLimit = if (settings.difficulty == Difficulty.EASY) 1 else 3
        val connectable = pool.filter { canContinue(it.word, usedWords + it.word, childLevelLimit) }
        // 한 수 더 내다본다: 아이가 이어 말할 만한 단어 뒤에도 AI가 이어갈 수 있는 단어를 우선한다.
        // 그래야 아이의 다음 단어 끝 글자에서 AI가 바로 막혀 판이 일찍 끝나는 일이 줄어든다.
        val safe = connectable.filter { isSafeForNextTurn(it.word, usedWords + it.word, childLevelLimit) }
        val finalPool = safe.ifEmpty { connectable }.ifEmpty { pool }
        return finalPool[random.nextInt(finalPool.size)]
    }

    /**
     * AI가 [word]를 말했을 때, 아이가 쓸 만한 단어(maxLevel 이하) 가운데 AI가 다시 이어갈 수 있는 단어가
     * 절반 이상인지. 아이는 단어장 밖의 단어도 말할 수 있으므로 단어장 단어를 대표 표본으로 쓴다.
     */
    private fun isSafeForNextTurn(word: String, usedWords: Set<String>, maxLevel: Int): Boolean {
        val replies = lexicon.startingWith(WordChainRules.allowedStarts(word, config))
            .filter { it.level <= maxLevel && it.word !in usedWords && it.word.length >= config.minLength }
        if (replies.isEmpty()) return false
        val continuable = replies.count { canContinue(it.word, usedWords + it.word) }
        return continuable * 2 >= replies.size
    }

    /** 난이도별로 단어 수준을 섞는다. 원하는 레벨이 없으면 가까운 레벨로 물러난다. */
    private fun levelPool(candidates: List<LexiconEntry>, difficulty: Difficulty): List<LexiconEntry> {
        val preferredOrder: List<Int> = when (difficulty) {
            Difficulty.EASY -> listOf(1, 2, 3)
            // 보통: 절반은 레벨 2, 나머지는 레벨 1로 다양하게
            Difficulty.NORMAL -> if (random.nextInt(100) < 60) listOf(2, 1, 3) else listOf(1, 2, 3)
            // 어려움: 어려운 단어만 반복하지 않도록 레벨 3은 60%만
            Difficulty.HARD -> if (random.nextInt(100) < 60) listOf(3, 2, 1) else listOf(2, 1, 3)
        }
        for (level in preferredOrder) {
            val atLevel = candidates.filter { it.level == level }
            if (atLevel.isNotEmpty()) return atLevel
        }
        return candidates
    }

    /** 이 단어 뒤에 maxLevel 이하의 단어가 하나라도 이어질 수 있는지. */
    fun canContinue(word: String, usedWords: Set<String>, maxLevel: Int = 3): Boolean =
        lexicon.startingWith(WordChainRules.allowedStarts(word, config))
            .any { it.level <= maxLevel && it.word !in usedWords && it.word.length >= config.minLength }
}
