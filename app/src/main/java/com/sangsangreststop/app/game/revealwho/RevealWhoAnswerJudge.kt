package com.sangsangreststop.app.game.revealwho

import com.sangsangreststop.app.game.wordchain.SpokenNumbers

/** 아이 발화를 로컬 규칙으로 분류한 결과. */
enum class RevealWhoUtterance {
    /** 정답을 말함 */
    CORRECT,
    /** 정답이 아닌 답을 말함 */
    WRONG,
    /** 긴 문장 등 답으로 보기 어려운 말 — 오답으로 치지 않고 다시 묻는다 */
    UNCLEAR,
    NEXT_HINT,
    REPLAY,
    /** "포기", "정답 알려줘" — 정답을 알려 주고 다음 문제로 간다 */
    REVEAL,
    PAUSE,
    STOP,
    /** 되묻는 질문에 "응" */
    YES,
    /** 되묻는 질문에 "아니" */
    NO,
}

/** 정답 판정과 음성 명령 분류 (순수 Kotlin). */
object RevealWhoAnswerJudge {

    private val ENDINGS = listOf("입니다", "이에요", "예요", "이야", "이요", "같아요", "같아", "요", "야", "다")

    private val STOP_CUES = listOf("그만할래", "그만하자", "그만해", "끝낼래", "끝내자", "종료")
    private val PAUSE_CUES = listOf("멈춰", "잠깐만", "일시정지")
    private val REPLAY_CUES = listOf("다시말해", "다시들려", "뭐라고", "다시한번", "한번더")
    private val REVEAL_CUES = listOf("포기", "패스", "정답알려", "정답이뭐", "넘어가", "못하겠")
    private val HINT_CUES = listOf("힌트", "모르겠", "몰라", "도와줘", "어려워")

    /** 답으로 보기에는 너무 긴 문장의 어절 수 */
    private const val SENTENCE_TOKENS = 4

    /** 숫자는 한글 읽기로 바꾸고("1기" → "일기"), 한글만 남긴다 (공백·문장부호·영문 제거). */
    fun normalize(text: String): String = SpokenNumbers.toHangul(text).filter { it in '가'..'힣' }

    fun isCorrect(utterance: String, question: RevealWhoQuestion): Boolean {
        val compact = normalize(utterance)
        if (compact.isEmpty()) return false
        val bare = bareCandidates(compact)
        return answerKeys(question).any { key ->
            // 한 글자 정답("배", "공", "차")은 다른 낱말 속에 우연히 들어가기 쉬워서 그 말만 했을 때 인정한다
            if (key.length >= 2) key in compact else key in bare
        }
    }

    /**
     * 정답과 발음이 아주 비슷한 말인지 ("기린" ↔ "기림", "토끼" ↔ "토기").
     * STT가 한 소리를 잘못 들었을 가능성이 높으므로 오답 처리하지 않고 되물을 때 쓴다.
     * 두 글자 이상인 정답에서, 글자 수가 같고 자모 하나만 다를 때만 해당한다.
     */
    fun isNearMiss(utterance: String, question: RevealWhoQuestion): Boolean {
        val compact = normalize(utterance)
        if (compact.isEmpty() || isCorrect(utterance, question)) return false
        val words = utterance.trim().split(Regex("\\s+")).map(::normalize).filter { it.isNotEmpty() }
        val candidates = (bareCandidates(compact) + words.flatMap(::bareCandidates)).toSet()
        return answerKeys(question).filter { it.length >= 2 }.any { key ->
            candidates.any { it.length == key.length && jamoDistance(it, key) == 1 }
        }
    }

    /** 오답을 기억하고 되말할 때 쓰는 대표 표기 (어미를 뗀 가장 짧은 후보). */
    fun answerForm(utterance: String): String {
        val compact = normalize(utterance)
        return bareCandidates(compact).filter { it.isNotEmpty() }.minByOrNull { it.length } ?: compact
    }

    private fun answerKeys(question: RevealWhoQuestion) = (question.synonyms + question.answer).map(::normalize)

    /** "배요", "차야"처럼 어미가 붙은 말에서 어미를 뗀 후보들 (원래 말 포함). */
    private fun bareCandidates(compact: String): List<String> =
        ENDINGS.mapNotNull { e -> compact.takeIf { it.endsWith(e) && it.length > e.length }?.removeSuffix(e) } + compact

    /** 두 한글 낱말을 초성·중성·종성 단위로 나눠 편집 거리를 잰다. */
    private fun jamoDistance(a: String, b: String): Int {
        val x = jamo(a)
        val y = jamo(b)
        val dp = IntArray(y.size + 1) { it }
        for (i in 1..x.size) {
            var prev = dp[0]
            dp[0] = i
            for (j in 1..y.size) {
                val temp = dp[j]
                dp[j] = minOf(dp[j] + 1, dp[j - 1] + 1, prev + if (x[i - 1] == y[j - 1]) 0 else 1)
                prev = temp
            }
        }
        return dp[y.size]
    }

    private fun jamo(word: String): List<Int> = word.flatMap { ch ->
        val offset = ch - '가'
        // 위치마다 겹치지 않는 번호를 붙인다 (초성 0~18, 중성 100~, 종성 200~, 받침 없음은 생략)
        listOfNotNull(offset / 588, 100 + (offset % 588) / 28, (offset % 28).takeIf { it > 0 }?.plus(200))
    }

    private val YES = setOf("응", "어", "네", "예", "웅", "맞아", "맞아요", "그래", "응응", "네네", "맞아맞아")
    private val NO = setOf("아니", "아니야", "아니요", "아뇨", "아니에요", "틀려", "아닌데")

    /** [awaitingConfirm]이면 "응"·"아니"를 먼저 본다 (되묻는 질문에 답하는 중). */
    fun classify(utterance: String, question: RevealWhoQuestion, awaitingConfirm: Boolean = false): RevealWhoUtterance {
        if (isCorrect(utterance, question)) return RevealWhoUtterance.CORRECT
        val compact = normalize(utterance)
        if (awaitingConfirm) {
            if (compact in YES) return RevealWhoUtterance.YES
            if (compact in NO) return RevealWhoUtterance.NO
        }
        return when {
            compact.isEmpty() -> RevealWhoUtterance.UNCLEAR
            compact == "그만" || STOP_CUES.any { it in compact } -> RevealWhoUtterance.STOP
            PAUSE_CUES.any { it in compact } -> RevealWhoUtterance.PAUSE
            REPLAY_CUES.any { it in compact } -> RevealWhoUtterance.REPLAY
            REVEAL_CUES.any { it in compact } -> RevealWhoUtterance.REVEAL
            HINT_CUES.any { it in compact } -> RevealWhoUtterance.NEXT_HINT
            utterance.trim().split(Regex("\\s+")).size >= SENTENCE_TOKENS -> RevealWhoUtterance.UNCLEAR
            else -> RevealWhoUtterance.WRONG
        }
    }
}
