package com.sangsangreststop.app.game.wordchain

/**
 * AI가 말할 수 있는 검증된 단어 한 개.
 * level: 1=쉬움(약 4~6세), 2=보통(약 6~8세), 3=어려움(약 8세 이상). 임시 개발 기준이다.
 */
data class LexiconEntry(
    val word: String,
    val level: Int,
    val category: String,
)

/** 내장 단어장. 첫 글자로 빠르게 찾을 수 있게 색인한다. */
class WordLexicon(entries: List<LexiconEntry>) {

    val entries: List<LexiconEntry> = entries.distinctBy { it.word }
    private val byWord: Map<String, LexiconEntry> = this.entries.associateBy { it.word }
    private val byFirst: Map<Char, List<LexiconEntry>> = this.entries.groupBy { it.word.first() }

    operator fun contains(word: String): Boolean = word in byWord
    fun find(word: String): LexiconEntry? = byWord[word]

    fun startingWith(starts: Set<Char>): List<LexiconEntry> =
        starts.flatMap { byFirst[it].orEmpty() }

    companion object {
        /**
         * 형식: `레벨|분류|단어 단어 단어 ...` (한 줄에 한 분류)
         * `#`으로 시작하는 줄은 주석이다.
         */
        fun parse(text: String): WordLexicon {
            val entries = text.lineSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") }
                .flatMap { line ->
                    val parts = line.split("|")
                    require(parts.size == 3) { "잘못된 단어장 줄: $line" }
                    val level = parts[0].trim().toInt()
                    val category = parts[1].trim()
                    parts[2].trim().split(Regex("\\s+"))
                        .filter { it.isNotEmpty() }
                        .map { LexiconEntry(it, level, category) }
                }
                .toList()
            return WordLexicon(entries)
        }

        val Default: WordLexicon by lazy { parse(DefaultWordList.V1) }
    }
}
