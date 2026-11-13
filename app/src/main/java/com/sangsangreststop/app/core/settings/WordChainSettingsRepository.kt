package com.sangsangreststop.app.core.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sangsangreststop.app.game.wordchain.Difficulty
import com.sangsangreststop.app.game.wordchain.WordChainGameSettings
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/** 앱 전체에서 하나만 만들어지도록 최상위에 둔다. */
private val Context.sangsangRestStopSettingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "sangsangreststop_settings")

/**
 * 끝말잇기 난이도·힌트 설정 저장소 (Preferences DataStore).
 * 3-0 설정 화면과 3-1 게임 화면이 이 저장소 하나를 같이 쓴다.
 * 아이 이름·음성·대화 내용·위치 정보는 저장하지 않는다.
 */
class WordChainSettingsRepository(context: Context) {

    private val dataStore = context.applicationContext.sangsangRestStopSettingsDataStore

    val settings: Flow<WordChainGameSettings> = dataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { prefs -> fromStored(prefs[KEY_DIFFICULTY], prefs[KEY_HINT_ENABLED]) }

    suspend fun save(settings: WordChainGameSettings) {
        dataStore.edit { prefs ->
            prefs[KEY_DIFFICULTY] = settings.difficulty.name
            prefs[KEY_HINT_ENABLED] = settings.hintsEnabled
        }
    }

    companion object {
        private val KEY_DIFFICULTY = stringPreferencesKey("wordchain_difficulty")
        private val KEY_HINT_ENABLED = booleanPreferencesKey("wordchain_hint_enabled")

        /** 저장값이 없거나 지원하지 않는 값이면 안전한 기본값(쉬움, 힌트 켬)으로 복구한다. */
        fun fromStored(difficulty: String?, hintsEnabled: Boolean?): WordChainGameSettings {
            val defaults = WordChainGameSettings()
            return WordChainGameSettings(
                difficulty = Difficulty.entries.firstOrNull { it.name == difficulty } ?: defaults.difficulty,
                hintsEnabled = hintsEnabled ?: defaults.hintsEnabled,
            )
        }
    }
}
