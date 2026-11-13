package com.sangsangreststop.app.game.wordchain

import kotlin.random.Random

/** 현재 턴의 힌트 진행 상황. 메모리에만 두며 횟수 제한은 없다. */
data class HintProgress(
    val targetWord: String,
    val category: String,
    /** 다음에 줄 힌트 단계 (1부터 시작) */
    val nextStep: Int = 1,
)

data class Hint(val text: String, val progress: HintProgress)

/**
 * 정답을 바로 알려주지 않고 단계적으로 힌트를 준다.
 * 1단계: 분류(범주) → 2단계: 글자 수 → 3단계: 단어 제안 → 이후 다른 단어로 다시 1단계.
 */
class HintProvider(
    private val lexicon: WordLexicon = WordLexicon.Default,
    private val random: Random = Random.Default,
    private val config: WordChainRuleConfig = WordChainRuleConfig(),
) {

    fun nextHint(
        previousWord: String,
        settings: WordChainGameSettings,
        usedWords: Set<String>,
        current: HintProgress?,
    ): Hint? {
        val starts = WordChainRules.allowedStarts(previousWord, config)
        val progress = if (current == null || current.nextStep > MAX_STEP || current.targetWord in usedWords) {
            val target = pickTarget(starts, previousWord, settings, usedWords, exclude = current?.targetWord)
                ?: return null
            HintProgress(target.word, target.category, nextStep = 1)
        } else {
            current
        }
        val first = progress.targetWord.first().toString()
        val startsWith = "'$first'${Hangul.josaRo(first)} 시작"
        val text = when (progress.nextStep) {
            1 -> "${startsWith}하는 ${progress.category}${Hangul.josa(progress.category, "이", "가")} 있어! 한번 생각해 볼래?"
            2 -> "${progress.targetWord.length}글자짜리 ${progress.category}${Hangul.josa(progress.category, "이야", "야")}. " +
                "${startsWith}해!"
            else -> "'${progress.targetWord}' 어때? 따라 말해 봐!"
        }
        return Hint(text, progress.copy(nextStep = progress.nextStep + 1))
    }

    private fun pickTarget(
        starts: Set<Char>,
        previousWord: String,
        settings: WordChainGameSettings,
        usedWords: Set<String>,
        exclude: String?,
    ): LexiconEntry? {
        val candidates = lexicon.startingWith(starts)
            .filter { it.word != exclude }
            .filter { WordChainRules.check(it.word, previousWord, usedWords, config) == WordCheck.Valid }
        if (candidates.isEmpty()) return null
        // 힌트는 아이가 알 법한 쉬운 단어를 우선한다.
        val maxLevel = when (settings.difficulty) {
            Difficulty.EASY -> 1
            Difficulty.NORMAL -> 2
            Difficulty.HARD -> 3
        }
        val preferred = candidates.filter { it.level <= maxLevel }.ifEmpty { candidates }
        val minLevel = preferred.minOf { it.level }
        val easiest = preferred.filter { it.level == minLevel }
        return easiest[random.nextInt(easiest.size)]
    }

    companion object {
        const val MAX_STEP = 3
    }
}
