package com.sangsangreststop.app.game.revealwho

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

    /** 한글만 남긴다 (공백·문장부호·영문·숫자 제거). */
    fun normalize(text: String): String = text.filter { it in '가'..'힣' }

    fun isCorrect(utterance: String, question: RevealWhoQuestion): Boolean {
        val compact = normalize(utterance)
        if (compact.isEmpty()) return false
        // "배요", "차야"처럼 어미가 붙은 말에서 어미를 뗀 후보
        val bare = ENDINGS.mapNotNull { e -> compact.takeIf { it.endsWith(e) }?.removeSuffix(e) } + compact
        return (question.synonyms + question.answer).map(::normalize).any { key ->
            // 한 글자 정답("배", "공", "차")은 다른 낱말 속에 우연히 들어가기 쉬워서 그 말만 했을 때 인정한다
            if (key.length >= 2) key in compact else key in bare
        }
    }

    fun classify(utterance: String, question: RevealWhoQuestion): RevealWhoUtterance {
        if (isCorrect(utterance, question)) return RevealWhoUtterance.CORRECT
        val compact = normalize(utterance)
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
