package com.sangsangreststop.app.core.settings

import com.sangsangreststop.app.game.wordchain.Difficulty
import com.sangsangreststop.app.game.wordchain.WordChainGameSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class WordChainSettingsRepositoryTest {
    @Test
    fun missingValuesFallBackToEasyWithHints() {
        assertEquals(WordChainGameSettings(Difficulty.EASY, true), WordChainSettingsRepository.fromStored(null, null))
    }

    @Test
    fun unsupportedDifficultyFallsBackToEasy() {
        assertEquals(Difficulty.EASY, WordChainSettingsRepository.fromStored("SUPER_HARD", false).difficulty)
    }

    @Test
    fun storedValuesAreRestored() {
        assertEquals(WordChainGameSettings(Difficulty.HARD, false), WordChainSettingsRepository.fromStored("HARD", false))
    }
}
