package com.sangsangreststop.app.game.wordchain

/**
 * 한글 음절 처리 유틸리티. Android API에 의존하지 않는다.
 */
internal object Hangul {
    private const val SYLLABLE_BASE = 0xAC00
    private const val SYLLABLE_LAST = 0xD7A3
    private const val JUNGSEONG_COUNT = 21
    private const val JONGSEONG_COUNT = 28

    // 초성 인덱스
    private const val CHO_NIEUN = 2 // ㄴ
    private const val CHO_RIEUL = 5 // ㄹ
    private const val CHO_IEUNG = 11 // ㅇ

    // 중성 인덱스: ㅏ0 ㅐ1 ㅑ2 ㅒ3 ㅓ4 ㅔ5 ㅕ6 ㅖ7 ㅗ8 ㅘ9 ㅙ10 ㅚ11 ㅛ12 ㅜ13 ㅝ14 ㅞ15 ㅟ16 ㅠ17 ㅡ18 ㅢ19 ㅣ20
    /** 'ㄹ'이 'ㅇ'으로 바뀌는 모음: 랴·려·례·료·류·리 → 야·여·예·요·유·이 */
    private val RIEUL_TO_IEUNG_VOWELS = setOf(2, 6, 7, 12, 17, 20)

    /** 'ㄹ'이 'ㄴ'으로 바뀌는 모음: 라·래·로·뢰·루·르 → 나·내·노·뇌·누·느 */
    private val RIEUL_TO_NIEUN_VOWELS = setOf(0, 1, 8, 11, 13, 18)

    /** 'ㄴ'이 'ㅇ'으로 바뀌는 모음: 녀·뇨·뉴·니 → 여·요·유·이 */
    private val NIEUN_TO_IEUNG_VOWELS = setOf(2, 6, 12, 17, 20)

    fun isSyllable(ch: Char): Boolean = ch.code in SYLLABLE_BASE..SYLLABLE_LAST

    fun hasFinalConsonant(ch: Char): Boolean =
        isSyllable(ch) && (ch.code - SYLLABLE_BASE) % JONGSEONG_COUNT != 0

    /**
     * 두음법칙을 적용했을 때 바뀌는 음절. 바뀌지 않으면 null.
     * 예: 력 → 역, 리 → 이, 로 → 노, 녀 → 여
     */
    fun initialSoundLawVariant(ch: Char): Char? {
        if (!isSyllable(ch)) return null
        val offset = ch.code - SYLLABLE_BASE
        val cho = offset / (JUNGSEONG_COUNT * JONGSEONG_COUNT)
        val jung = (offset / JONGSEONG_COUNT) % JUNGSEONG_COUNT
        val jong = offset % JONGSEONG_COUNT
        val newCho = when (cho) {
            CHO_RIEUL -> when (jung) {
                in RIEUL_TO_IEUNG_VOWELS -> CHO_IEUNG
                in RIEUL_TO_NIEUN_VOWELS -> CHO_NIEUN
                else -> return null
            }
            CHO_NIEUN -> if (jung in NIEUN_TO_IEUNG_VOWELS) CHO_IEUNG else return null
            else -> return null
        }
        return (SYLLABLE_BASE + (newCho * JUNGSEONG_COUNT + jung) * JONGSEONG_COUNT + jong).toChar()
    }

    /** 받침 유무에 따라 조사를 고른다. 예: josa("사과", "이", "가") → "가" */
    fun josa(word: String, withFinal: String, withoutFinal: String): String {
        val last = word.lastOrNull() ?: return withoutFinal
        return if (hasFinalConsonant(last)) withFinal else withoutFinal
    }

    /** "으로/로": 받침이 없거나 받침이 'ㄹ'이면 "로" */
    fun josaRo(word: String): String {
        val last = word.lastOrNull() ?: return "로"
        if (!isSyllable(last)) return "로"
        val jong = (last.code - SYLLABLE_BASE) % JONGSEONG_COUNT
        return if (jong == 0 || jong == 8) "로" else "으로"
    }
}
