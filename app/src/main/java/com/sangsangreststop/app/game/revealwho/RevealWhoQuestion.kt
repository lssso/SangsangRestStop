package com.sangsangreststop.app.game.revealwho

enum class RevealWhoLevel { EASY, NORMAL, HARD }

enum class RevealWhoTopic { ANIMAL, VEHICLE, OBJECT }

/**
 * 문제 하나. [hints]는 알쏭달쏭한 것부터 결정적인 것 순서로 둔다.
 * [synonyms]는 정답으로 인정하는 다른 말이다 (예: 강아지 → 개, 멍멍이).
 */
data class RevealWhoQuestion(
    val answer: String,
    val topic: RevealWhoTopic,
    val level: RevealWhoLevel,
    val hints: List<String>,
    val synonyms: List<String> = emptyList(),
)

data class RevealWhoGameSettings(
    val level: RevealWhoLevel = RevealWhoLevel.EASY,
    val topic: RevealWhoTopic = RevealWhoTopic.ANIMAL,
    /** 한 판에 내는 문제 수 */
    val questionCount: Int = DEFAULT_QUESTION_COUNT,
) {
    companion object {
        const val DEFAULT_QUESTION_COUNT = 5
    }
}
