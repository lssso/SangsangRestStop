package com.sangsangreststop.app.core.voice

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import ai.pleos.playground.stt.SpeechToText
import ai.pleos.playground.stt.constant.ErrorType
import ai.pleos.playground.stt.constant.Mode
import ai.pleos.playground.stt.listener.OnInitializeListener
import ai.pleos.playground.stt.listener.ResultListener
import java.util.Locale

/** 음성 인식 오류 종류. 끝말잇기 오답과 구분해서 처리한다. */
enum class SpeechInputError {
    /** 잘 못 알아들음 (마이크·엔진·오디오 문제 등) — 다시 말하게 하면 된다 */
    RECOGNITION,
    /** 네트워크 문제 */
    NETWORK,
    /** 이 환경에서 음성 인식을 쓸 수 없음 (권한·미지원) — 글자 입력으로 안내 */
    UNAVAILABLE,
}

/** 한 번의 발화를 듣고 결과를 돌려주는 음성 입력 장치. 모든 콜백은 메인 스레드에서 호출된다. */
interface SpeechInput {
    val isAvailable: Boolean

    fun start(listener: Listener)
    fun stop()
    fun release()

    interface Listener {
        /** 말하는 중 중간 결과 */
        fun onPartial(text: String) = Unit
        fun onResult(text: String)
        /** 아무 말도 인식되지 않음 */
        fun onNoInput()
        fun onError(error: SpeechInputError)
    }
}

/**
 * Pleos SpeechToText SDK 구현 (ON_DEVICE 모드: 앱 등록·네트워크 불필요).
 * 음성 데이터는 앱에 저장하지 않는다.
 */
class PleosSpeechInput(context: Context) : SpeechInput {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val stt = SpeechToText(context.applicationContext, Mode.ON_DEVICE)
    private var initialized = false
    private var initFailed = false
    private var pendingStart: SpeechInput.Listener? = null

    /** 현재 듣기 세션. 새로 시작하거나 stop()하면 바뀌어서 늦게 온 콜백을 버린다. */
    private var session = 0
    private var activeListener: SpeechInput.Listener? = null
    private var lastPartial = ""
    private var delivered = false

    override val isAvailable: Boolean get() = !initFailed

    private val resultListener = object : ResultListener {
        override fun onReady() = Unit
        override fun onStartedRecognition() = Unit
        override fun onUpdatedEpdData(on: Long, off: Long) = Unit
        override fun onUpdatedRms(rms: Double) = Unit

        override fun onUpdated(stt: String, completed: Boolean) = post { listener ->
            if (completed) {
                if (stt.isBlank()) finish { listener.onNoInput() } else finish { listener.onResult(stt) }
            } else if (stt.isNotBlank()) {
                if (lastPartial.isEmpty()) scheduleTimeout(MAX_UTTERANCE_MS) // 말하기 시작하면 발화 길이 제한으로 바꾼다
                lastPartial = stt
                listener.onPartial(stt)
            }
        }

        override fun onEndedRecognition() = post { listener ->
            // 최종 결과 없이 끝나면 마지막 중간 결과를 쓰고, 그것도 없으면 무응답으로 본다
            if (lastPartial.isNotBlank()) finish { listener.onResult(lastPartial) } else finish { listener.onNoInput() }
        }

        override fun onError(errorType: ErrorType) = post { listener ->
            Log.w(TAG, "STT error: $errorType")
            finish { listener.onError(errorType.toSpeechInputError()) }
        }
    }

    init {
        stt.initialize(object : OnInitializeListener {
            override fun onSuccess() = mainHandler.post {
                initialized = true
                stt.setLocale(Locale.KOREA)
                stt.addListener(resultListener)
                pendingStart?.let { pendingStart = null; start(it) }
            }.let { }

            override fun onFailure(errorType: ErrorType) = mainHandler.post {
                Log.w(TAG, "STT initialize failed: $errorType")
                initFailed = true
                pendingStart?.let { pendingStart = null; it.onError(SpeechInputError.UNAVAILABLE) }
            }.let { }
        })
    }

    override fun start(listener: SpeechInput.Listener) {
        when {
            initFailed -> listener.onError(SpeechInputError.UNAVAILABLE)
            !initialized -> pendingStart = listener
            else -> {
                session++
                activeListener = listener
                lastPartial = ""
                delivered = false
                // 조용한 환경에서는 SDK가 인식을 끝내지 않아 무한히 들을 수 있으므로 앱에서 시간 제한을 둔다
                scheduleTimeout(NO_SPEECH_TIMEOUT_MS)
                runCatching { stt.request() }.onFailure {
                    Log.w(TAG, "STT request failed", it)
                    finish { listener.onError(SpeechInputError.RECOGNITION) }
                }
            }
        }
    }

    override fun stop() {
        pendingStart = null
        mainHandler.removeCallbacks(timeoutRunnable)
        session++
        activeListener = null
        if (initialized) runCatching { stt.stop() }
    }

    override fun release() {
        stop()
        runCatching {
            if (initialized) stt.removeListener(resultListener)
            stt.release()
        }
    }

    /** SDK 콜백을 메인 스레드로 옮기고, 이미 끝났거나 취소된 세션이면 버린다. */
    private fun post(block: (SpeechInput.Listener) -> Unit) {
        val expected = session
        mainHandler.post {
            val listener = activeListener
            if (expected == session && listener != null && !delivered) block(listener)
        }
    }

    private fun finish(block: () -> Unit) {
        if (delivered) return
        delivered = true
        activeListener = null
        mainHandler.removeCallbacks(timeoutRunnable)
        block()
    }

    /** 시간 제한에 걸리면 인식을 멈추고, 들은 말이 있으면 그것을, 없으면 무응답을 돌려준다. */
    private val timeoutRunnable = Runnable {
        val listener = activeListener ?: return@Runnable
        val heard = lastPartial
        session++
        runCatching { stt.stop() }
        if (heard.isNotBlank()) finish { listener.onResult(heard) } else finish { listener.onNoInput() }
    }

    private fun scheduleTimeout(delayMs: Long) {
        mainHandler.removeCallbacks(timeoutRunnable)
        mainHandler.postDelayed(timeoutRunnable, delayMs)
    }

    private fun ErrorType.toSpeechInputError(): SpeechInputError = when (this) {
        ErrorType.NETWORK_ERROR, ErrorType.TIMEOUT -> SpeechInputError.NETWORK
        ErrorType.PERMISSION_DENIED, ErrorType.NOT_SUPPORTED_API,
        ErrorType.UNAVAILABLE_LANGUAGE, ErrorType.UNAUTHENTICATED -> SpeechInputError.UNAVAILABLE
        else -> SpeechInputError.RECOGNITION
    }

    private companion object {
        const val TAG = "PleosSpeechInput"
        /** 아무 말도 없을 때 기다리는 시간 (아이가 생각할 시간 포함) */
        const val NO_SPEECH_TIMEOUT_MS = 10_000L
        /** 말을 시작한 뒤 최대 발화 시간 */
        const val MAX_UTTERANCE_MS = 6_000L
    }
}
