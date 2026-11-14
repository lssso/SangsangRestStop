package com.sangsangreststop.app.game.wordchain

/**
 * STT가 숫자로 적어 준 부분을 한글 읽기로 바꾼다.
 * Pleos STT는 "일기"를 "1기"처럼 숫자로 돌려줄 때가 있어서, 판정 전에 한자어 수사(일·이·삼…)로 되돌린다.
 */
object SpokenNumbers {

    private val DIGITS = arrayOf("", "일", "이", "삼", "사", "오", "육", "칠", "팔", "구")
    private val SMALL_UNITS = arrayOf("", "십", "백", "천")
    private val LARGE_UNITS = arrayOf("", "만", "억")

    fun toHangul(text: String): String =
        Regex("[0-9]+").replace(text) { readNumber(it.value) }

    /** 12자리 이하의 숫자를 읽는다. "10" → "십", "25" → "이십오", "0" → "영". */
    fun readNumber(digits: String): String {
        val trimmed = digits.trimStart('0')
        if (trimmed.isEmpty()) return "영"
        // 아주 긴 숫자는 한 자리씩 읽는다 (전화번호 등)
        if (trimmed.length > 12) return digits.map { if (it == '0') "영" else DIGITS[it - '0'] }.joinToString("")
        val groups = trimmed.reversed().chunked(4).map { it.reversed() }
        val builder = StringBuilder()
        for (index in groups.indices.reversed()) {
            val group = readGroup(groups[index])
            if (group.isEmpty()) continue
            // "만"·"억" 앞의 "일"은 읽지 않는다 ("10000" → "만")
            builder.append(if (group == "일" && index > 0) "" else group)
            builder.append(LARGE_UNITS[index])
        }
        return builder.toString()
    }

    private fun readGroup(group: String): String {
        val builder = StringBuilder()
        group.forEachIndexed { i, ch ->
            val digit = ch - '0'
            val unit = group.length - 1 - i
            if (digit == 0) return@forEachIndexed
            // "십"·"백"·"천" 앞의 "일"은 읽지 않는다 ("10" → "십")
            if (!(digit == 1 && unit > 0)) builder.append(DIGITS[digit])
            builder.append(SMALL_UNITS[unit])
        }
        return builder.toString()
    }
}
