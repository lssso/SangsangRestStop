package com.sangsangreststop.app.core.voice

import android.content.Context
import android.util.Log

/**
 * 앱 전체에서 하나만 쓰는 음성 입출력 장치.
 *
 * Pleos STT/TTS 서비스는 앱(패키지)마다 결과 콜백을 하나만 유지한다. 게임마다 SDK 인스턴스를
 * 따로 만들면 나중에 만든 인스턴스가 콜백을 가져가서, 다른 게임의 음성 인식 결과가 사라진다.
 * 그래서 게임들은 이 객체가 만든 인스턴스를 함께 쓰고, 화면을 떠날 때는 release가 아니라 stop만 한다.
 * 한 번에 한 게임만 진행되므로 공유해도 서로 겹치지 않는다.
 */
object SharedVoice {

    private const val TAG = "SharedVoice"

    private var output: SpeechOutput? = null
    private var input: SpeechInput? = null
    private var inputCreated = false

    /** Pleos 음성 합성을 우선 쓰고, 쓸 수 없으면 Android 기본 TTS로 넘어간다. */
    fun speechOutput(context: Context): SpeechOutput = output ?: run {
        val app = context.applicationContext
        runCatching<SpeechOutput> { PleosSpeechOutput(app) { AndroidTtsSpeechOutput(app) } }
            .getOrElse {
                Log.w(TAG, "Pleos TTS unavailable", it)
                AndroidTtsSpeechOutput(app)
            }
            .also { output = it }
    }

    /** Pleos 음성 인식. 이 환경에서 쓸 수 없으면 null. */
    fun speechInput(context: Context): SpeechInput? {
        if (!inputCreated) {
            inputCreated = true
            input = runCatching { PleosSpeechInput(context.applicationContext) }
                .onFailure { Log.w(TAG, "Pleos STT unavailable", it) }
                .getOrNull()
        }
        return input
    }
}
