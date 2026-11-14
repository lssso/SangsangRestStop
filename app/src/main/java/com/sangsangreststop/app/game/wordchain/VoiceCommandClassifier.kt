package com.sangsangreststop.app.game.wordchain

/** 아이 발화를 로컬 규칙으로 분류한 결과. 설정을 직접 바꾸지 않는다. */
sealed interface VoiceCommand {
    /** "보통으로 바꿔줘" 같은 명확한 난이도 지정 */
    data class SetDifficulty(val difficulty: Difficulty) : VoiceCommand

    /** "더 어렵게 해줘" 같은 명확한 한 단계 조정 */
    data class StepDifficulty(val harder: Boolean) : VoiceCommand

    /** "너무 쉬워" 같은 모호한 표현 — 바꿀지 되묻는다 */
    data class SuggestDifficulty(val harder: Boolean) : VoiceCommand

    data class SetHints(val enabled: Boolean) : VoiceCommand
    data object RequestHint : VoiceCommand
    data object Repeat : VoiceCommand
    data object GiveUp : VoiceCommand
    data object Pause : VoiceCommand
    data object Resume : VoiceCommand
    data object Stop : VoiceCommand
    data object Yes : VoiceCommand
    data object No : VoiceCommand

    /** "아니", "다시 말할게" — replacement가 있으면 "아니 과자"처럼 새 단어를 함께 말한 경우 */
    data class Correction(val replacement: String?) : VoiceCommand

    /**
     * 명령이 아니면 단어 시도로 본다. 규칙 판정은 게임 엔진이 한다.
     * tokens: 여러 어절로 말한 경우 각 어절 ("음 과자" → [음, 과자]). 엔진이 규칙에 맞는 어절을 고른다.
     */
    data class Word(val word: String, val tokens: List<String> = listOf(word)) : VoiceCommand
}

object VoiceCommandClassifier {

    private val YES = setOf("응", "어", "네", "예", "웅", "좋아", "좋아요", "그래", "그래요", "그러자", "당연하지", "바꿔줘", "켜줘", "해줘")
    private val NO = setOf("아니", "아니야", "아니요", "아뇨", "싫어", "안할래", "괜찮아", "됐어", "그냥할래")

    private val HINT_OFF_CUES = listOf("주지마", "그만", "꺼", "끄", "필요없", "안줘", "없이", "빼")
    private val HINT_ON_CUES = listOf("켜", "다시줘", "다시써", "쓸래", "쓰자")
    private val HELP_CUES = listOf("도와줘", "모르겠어", "모르겠다", "몰라", "생각이안나")

    private val CORRECTION_CUES = listOf("그거말고", "다시말할게", "잘못말했어", "잘못말했어요", "아니야", "아니")
    private val CORRECTION_FILLERS = listOf("요", "고", "야")

    fun classify(raw: String, awaitingAnswer: Boolean = false): VoiceCommand {
        // STT가 "1기"처럼 숫자로 적어 준 부분은 한글 읽기로 바꾼 뒤 판정한다
        val text = SpokenNumbers.toHangul(raw)
        val compact = WordChainRules.normalize(text)
        if (compact.isEmpty()) return VoiceCommand.Word("")

        if (awaitingAnswer) {
            if (compact in YES || compact in setOf("응응", "네네", "좋아좋아")) return VoiceCommand.Yes
            if (compact in NO) return VoiceCommand.No
        }

        if ("힌트" in compact) {
            return when {
                HINT_OFF_CUES.any { it in compact } -> VoiceCommand.SetHints(false)
                HINT_ON_CUES.any { it in compact } -> VoiceCommand.SetHints(true)
                else -> VoiceCommand.RequestHint
            }
        }

        difficultyCommand(compact)?.let { return it }

        if (listOf("다시말해줘", "뭐라고", "다시한번", "한번더말해", "다시들려줘").any { it in compact }) {
            return VoiceCommand.Repeat
        }
        if (HELP_CUES.any { it in compact }) return VoiceCommand.RequestHint
        if (listOf("포기", "항복", "못하겠").any { it in compact }) return VoiceCommand.GiveUp
        if (listOf("잠깐멈춰", "멈춰", "잠깐만", "일시정지").any { it in compact }) return VoiceCommand.Pause
        if (listOf("계속하자", "계속해", "이어서하자", "다시하자").any { it in compact }) return VoiceCommand.Resume
        if (listOf("그만할래", "그만하자", "그만해", "끝낼래", "끝내자", "종료").any { it in compact } || compact == "그만") {
            return VoiceCommand.Stop
        }

        CORRECTION_CUES.firstOrNull { compact.startsWith(it) }?.let { cue ->
            var rest = compact.removePrefix(cue)
            CORRECTION_FILLERS.firstOrNull { rest.startsWith(it) }?.let { rest = rest.removePrefix(it) }
            // "아니 과자" → 새 단어 "과자"를 함께 말한 경우
            return VoiceCommand.Correction(rest.takeIf { it.length >= 2 })
        }

        // 여러 어절이면 기본은 마지막 어절 ("음… 과자" → "과자"), 엔진이 다른 어절도 확인한다
        val tokens = text.trim().split(Regex("\\s+")).map(WordChainRules::normalize).filter { it.isNotEmpty() }
        return VoiceCommand.Word(tokens.lastOrNull().orEmpty(), tokens.ifEmpty { listOf("") })
    }

    private fun difficultyCommand(compact: String): VoiceCommand? = when {
        listOf("너무쉬워", "너무쉽다", "너무쉽네", "시시해").any { it in compact } -> VoiceCommand.SuggestDifficulty(harder = true)
        listOf("너무어려워", "너무어렵다", "너무어렵네").any { it in compact } -> VoiceCommand.SuggestDifficulty(harder = false)
        listOf("더어렵게", "더어려운").any { it in compact } -> VoiceCommand.StepDifficulty(harder = true)
        listOf("더쉽게", "더쉬운").any { it in compact } -> VoiceCommand.StepDifficulty(harder = false)
        listOf("보통으로", "보통난이도", "보통해", "보통단계").any { it in compact } -> VoiceCommand.SetDifficulty(Difficulty.NORMAL)
        listOf("어렵게", "어려움으로", "어려운걸로", "어려운거로", "어려운단계").any { it in compact } -> VoiceCommand.SetDifficulty(Difficulty.HARD)
        listOf("쉽게", "쉬움으로", "쉬운걸로", "쉬운거로", "쉬운단계").any { it in compact } -> VoiceCommand.SetDifficulty(Difficulty.EASY)
        else -> null
    }
}
