package com.sangsangreststop.app.core.voice

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** 게임이 말할 문장을 소리로 내보내는 출력 장치. Pleos TextToSpeech SDK 구현으로 바꿔 끼울 수 있다. */
interface SpeechOutput {
    /** 말하기가 끝나면 메인 스레드에서 onDone을 부른다. stop()으로 끊긴 말은 onDone을 부르지 않는다. */
    fun speak(text: String, onDone: () -> Unit)
    fun stop()
    fun release()
}

/**
 * Android 기본 TextToSpeech 구현 (Pleos가 아닌 일반 Android용 대체 수단).
 * Pleos 에뮬레이터에서는 소리가 나지 않아 [PleosSpeechOutput]을 우선 사용한다.
 * TTS를 쓸 수 없으면 글자 수에 비례한 시간만큼 기다렸다가 onDone을 불러 게임이 멈추지 않게 한다.
 */
class AndroidTtsSpeechOutput(context: Context) : SpeechOutput, TextToSpeech.OnInitListener {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val callbacks = ConcurrentHashMap<String, () -> Unit>()
    private var ready = false
    private var failed = false
    private var queued: Pair<String, () -> Unit>? = null
    private val tts = TextToSpeech(context.applicationContext, this)

    override fun onInit(status: Int) {
        mainHandler.post {
            if (status == TextToSpeech.SUCCESS) {
                val result = tts.setLanguage(Locale.KOREAN)
                ready = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
                tts.setOnUtteranceProgressListener(listener)
            }
            failed = !ready
            queued?.let { (text, onDone) ->
                queued = null
                speak(text, onDone)
            }
        }
    }

    override fun speak(text: String, onDone: () -> Unit) {
        when {
            failed -> mainHandler.postDelayed(onDone, fallbackDurationMs(text))
            !ready -> queued = text to onDone
            else -> {
                val id = UUID.randomUUID().toString()
                callbacks[id] = onDone
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
            }
        }
    }

    override fun stop() {
        queued = null
        callbacks.clear()
        mainHandler.removeCallbacksAndMessages(null)
        if (ready) tts.stop()
    }

    override fun release() {
        stop()
        tts.shutdown()
    }

    private val listener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) = Unit

        override fun onDone(utteranceId: String?) = finish(utteranceId)

        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) = finish(utteranceId)

        override fun onError(utteranceId: String?, errorCode: Int) = finish(utteranceId)

        override fun onStop(utteranceId: String?, interrupted: Boolean) {
            utteranceId?.let(callbacks::remove)
        }
    }

    private fun finish(utteranceId: String?) {
        val callback = utteranceId?.let(callbacks::remove) ?: return
        mainHandler.post(callback)
    }

    private fun fallbackDurationMs(text: String): Long = (text.length * 90L).coerceIn(1_200L, 6_000L)
}
