package com.sangsangreststop.app.core.voice

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import ai.pleos.playground.tts.TextToSpeech
import ai.pleos.playground.tts.constant.ErrorType
import ai.pleos.playground.tts.constant.Mode
import ai.pleos.playground.tts.listener.EventListener
import ai.pleos.playground.tts.listener.OnInitializeListener
import java.util.Locale

/**
 * Pleos TextToSpeech SDK 구현 (ON_DEVICE 모드: 앱 등록·네트워크 불필요).
 * Pleos 환경이 아니어서 초기화에 실패하면 Android 기본 TTS([fallbackFactory])로 넘어간다.
 */
class PleosSpeechOutput(
    context: Context,
    private val fallbackFactory: () -> SpeechOutput,
) : SpeechOutput {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val tts = TextToSpeech(context.applicationContext, Mode.ON_DEVICE)
    private var ready = false
    private var fallback: SpeechOutput? = null
    private var pending: Pair<String, () -> Unit>? = null
    private var current: (() -> Unit)? = null

    /** onDone이 오지 않아도 게임이 멈추지 않도록 하는 안전 장치 */
    private val safetyTimeout = Runnable {
        Log.w(TAG, "TTS onDone not received, continuing")
        finishCurrent()
    }

    private val eventListener = object : EventListener {
        override fun onReady() = Unit
        override fun onStart() = Unit
        override fun onUpdatedRms(rms: Double) = Unit
        override fun onDone() = post { finishCurrent() }
        override fun onStop() = Unit
        override fun onError(errorType: ErrorType) = post {
            Log.w(TAG, "TTS error: $errorType")
            finishCurrent()
        }
    }

    init {
        tts.initialize(object : OnInitializeListener {
            override fun onSuccess() = post {
                ready = true
                tts.setLocale(Locale.KOREA)
                tts.addEventListener(eventListener)
                pending?.let { (text, onDone) -> pending = null; speak(text, onDone) }
            }

            override fun onFailure(errorType: ErrorType) = post {
                Log.w(TAG, "Pleos TTS initialize failed: $errorType, using Android TTS")
                val output = fallbackFactory().also { fallback = it }
                pending?.let { (text, onDone) -> pending = null; output.speak(text, onDone) }
            }
        })
    }

    override fun speak(text: String, onDone: () -> Unit) {
        fallback?.let { return it.speak(text, onDone) }
        if (!ready) {
            pending = text to onDone
            return
        }
        if (current != null) runCatching { tts.stop() }
        current = onDone
        mainHandler.removeCallbacks(safetyTimeout)
        mainHandler.postDelayed(safetyTimeout, (text.length * 250L).coerceIn(4_000L, 20_000L))
        runCatching { tts.speak(text, true) }.onFailure {
            Log.w(TAG, "TTS speak failed", it)
            finishCurrent()
        }
    }

    override fun stop() {
        pending = null
        current = null
        mainHandler.removeCallbacks(safetyTimeout)
        fallback?.stop()
        if (ready) runCatching { tts.stop() }
    }

    override fun release() {
        stop()
        fallback?.release()
        runCatching {
            if (ready) tts.removeEventListener(eventListener)
            tts.release()
        }
    }

    private fun finishCurrent() {
        mainHandler.removeCallbacks(safetyTimeout)
        val callback = current ?: return
        current = null
        callback()
    }

    private fun post(block: () -> Unit) {
        mainHandler.post(block)
    }

    private companion object {
        const val TAG = "PleosSpeechOutput"
    }
}
