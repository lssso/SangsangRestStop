package com.sangsangreststop.app.feature.voicepoc

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognitionService
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import java.util.Locale

internal enum class VoicePocMode(val label: String) {
    Idle("대기"),
    Listening("듣는 중"),
    Processing("인식 처리 중"),
    Speaking("말하는 중"),
    Error("오류"),
}

internal enum class TtsAvailability {
    Initializing,
    Available,
    Unavailable,
}

internal data class VoicePocState(
    val recognitionAvailable: Boolean = false,
    val ttsAvailability: TtsAvailability = TtsAvailability.Initializing,
    val mode: VoicePocMode = VoicePocMode.Idle,
    val recognizedText: String = "",
    val errorMessage: String? = null,
    val events: List<String> = emptyList(),
)

private data class SpeechRecognitionErrorInfo(
    val name: String,
    val message: String,
)

internal class VoicePocController(
    context: Context,
    private val onStateChanged: (VoicePocState) -> Unit,
) : RecognitionListener {

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val recognitionAvailable = SpeechRecognizer.isRecognitionAvailable(appContext)
    private val speechRecognizer = createSpeechRecognizer()
    private val ttsEnginePackages = queryTtsEnginePackages()
    private val defaultTtsEnginePackage =
        Settings.Secure.getString(
            appContext.contentResolver,
            TTS_DEFAULT_SYNTH_SETTING,
        ).orEmpty().ifBlank { null }
    private var textToSpeech: TextToSpeech? = null
    private var state = VoicePocState(recognitionAvailable = speechRecognizer != null)
    private var released = false
    private var listenAfterTts = false
    private var ignoreNextRecognitionError = false
    private var pendingStartListening: Runnable? = null
    private var recognitionTimeout: Runnable? = null
    private var eventNumber = 0

    init {
        speechRecognizer?.setRecognitionListener(this)
        mainHandler.post {
            if (!released) {
                publishEvent(
                    "CONFIG permission=${permissionStatus()}, " +
                        "serviceAvailable=$recognitionAvailable, " +
                        "recognizerCreated=${speechRecognizer != null}, " +
                        "language=$KOREAN_LANGUAGE_TAG",
                )
                publishEvent("CONFIG ${recognitionServiceDiagnostics()}")
                publishEvent(
                    "CONFIG defaultTtsEngine=${defaultTtsEnginePackage ?: "<none>"}, " +
                        "ttsEngines=$ttsEnginePackages",
                )
            }
        }
        textToSpeech = TextToSpeech(appContext, ::handleTtsInitialization)
    }

    fun startListening() {
        cancelPendingStartListening()
        startListeningInternal(trigger = "manual")
    }

    private fun startListeningInternal(trigger: String) {
        if (released) return
        if (!hasRecordAudioPermission()) {
            publishError("마이크 권한이 필요합니다.")
            return
        }
        if (speechRecognizer == null) {
            publishError("이 기기에서 음성 인식 서비스를 사용할 수 없습니다.")
            return
        }
        if (state.mode == VoicePocMode.Speaking) {
            publishError("TTS 재생 중에는 STT를 시작할 수 없습니다.")
            return
        }
        if (
            (state.mode == VoicePocMode.Listening) ||
            (state.mode == VoicePocMode.Processing)
        ) {
            publishError("이미 음성을 듣고 있습니다.")
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, KOREAN_LANGUAGE_TAG)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, KOREAN_LANGUAGE_TAG)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }

        cancelRecognitionTimeout()
        ignoreNextRecognitionError = false
        updateState(
            state.copy(
                mode = VoicePocMode.Listening,
                errorMessage = null,
            ),
        )
        publishEvent(
            "REQUEST startListening(trigger=$trigger, permission=${permissionStatus()}, " +
                "serviceAvailable=$recognitionAvailable, language=$KOREAN_LANGUAGE_TAG)",
        )

        try {
            speechRecognizer.startListening(intent)
            scheduleRecognitionTimeout(
                expectedCallback = "onReadyForSpeech/onError",
                delayMillis = READY_CALLBACK_TIMEOUT_MS,
            )
        } catch (exception: SecurityException) {
            cancelRecognitionTimeout()
            Log.e(LOG_TAG, "startListening SecurityException", exception)
            publishError("마이크 권한을 확인할 수 없습니다: ${exception.javaClass.simpleName}")
        } catch (exception: RuntimeException) {
            cancelRecognitionTimeout()
            Log.e(LOG_TAG, "startListening RuntimeException", exception)
            publishError(
                "STT를 시작하지 못했습니다: " +
                    "${exception.javaClass.simpleName} ${exception.message.orEmpty()}",
            )
        }
    }

    fun speak(text: String, thenListen: Boolean = false) {
        if (released) return
        if (state.ttsAvailability != TtsAvailability.Available) {
            publishError("한국어 TTS가 준비되지 않았습니다.")
            return
        }
        if (thenListen && !hasRecordAudioPermission()) {
            publishError("연속 테스트를 위해 마이크 권한이 필요합니다.")
            return
        }

        cancelPendingStartListening()
        listenAfterTts = thenListen
        cancelRecognitionSilently()
        updateState(
            state.copy(
                mode = VoicePocMode.Speaking,
                errorMessage = null,
            ),
        )
        publishEvent(
            if (thenListen) {
                "TTS 완료 후 STT를 시작합니다."
            } else {
                "한국어 TTS 재생을 시작합니다."
            },
        )

        val result = textToSpeech?.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            TEST_UTTERANCE_ID,
        )
        if (result == TextToSpeech.ERROR) {
            listenAfterTts = false
            publishError("TTS 재생 요청에 실패했습니다.")
        }
    }

    fun stop() {
        if (released) return
        listenAfterTts = false
        cancelPendingStartListening()
        cancelRecognitionSilently()
        textToSpeech?.stop()
        updateState(
            state.copy(
                mode = VoicePocMode.Idle,
                errorMessage = null,
            ),
        )
        publishEvent("REQUEST stop: 음성 입출력과 예약된 STT를 중지했습니다.")
    }

    fun release() {
        if (released) return
        released = true
        listenAfterTts = false
        cancelPendingStartListening()
        cancelRecognitionTimeout()
        mainHandler.removeCallbacksAndMessages(null)
        speechRecognizer?.cancel()
        speechRecognizer?.destroy()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
    }

    private fun createSpeechRecognizer(): SpeechRecognizer? {
        if (!recognitionAvailable) return null
        return try {
            SpeechRecognizer.createSpeechRecognizer(appContext)
        } catch (exception: RuntimeException) {
            Log.e(LOG_TAG, "createSpeechRecognizer failed", exception)
            null
        }
    }

    private fun recognitionServiceDiagnostics(): String {
        val packageManager = appContext.packageManager
        val defaultService =
            Settings.Secure.getString(
                appContext.contentResolver,
                VOICE_RECOGNITION_SERVICE_SETTING,
            ).orEmpty().ifBlank { "<none>" }
        val services =
            packageManager.queryIntentServices(
                Intent(RecognitionService.SERVICE_INTERFACE),
                PackageManager.MATCH_ALL or PackageManager.MATCH_DISABLED_COMPONENTS,
            )
        val candidates = services.joinToString(separator = ", ") { resolveInfo ->
            val serviceInfo = resolveInfo.serviceInfo
            val component = ComponentName(serviceInfo.packageName, serviceInfo.name)
            val componentState =
                when (packageManager.getComponentEnabledSetting(component)) {
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> "ENABLED"
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED -> "DISABLED"
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER -> "DISABLED_USER"
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED ->
                        "DISABLED_UNTIL_USED"
                    else -> "DEFAULT"
                }
            component.flattenToShortString() +
                "(serviceEnabled=${serviceInfo.enabled}, " +
                "appEnabled=${serviceInfo.applicationInfo?.enabled}, " +
                "componentState=$componentState)"
        }.ifBlank { "<none>" }
        return "defaultRecognitionService=$defaultService, recognitionServices=[$candidates]"
    }

    private fun queryTtsEnginePackages(): List<String> =
        appContext.packageManager
            .queryIntentServices(
                Intent(TTS_SERVICE_ACTION),
                PackageManager.MATCH_ALL or PackageManager.MATCH_DISABLED_COMPONENTS,
            )
            .mapNotNull { resolveInfo -> resolveInfo.serviceInfo?.packageName }
            .distinct()

    private fun handleTtsInitialization(status: Int) {
        if (released) return
        if (status != TextToSpeech.SUCCESS) {
            updateState(
                state.copy(
                    ttsAvailability = TtsAvailability.Unavailable,
                    mode = VoicePocMode.Error,
                    errorMessage = "TTS 엔진 초기화에 실패했습니다.",
                ),
            )
            publishEvent(
                "CALLBACK TTS onInit(status=$status, " +
                    "engine=${defaultTtsEnginePackage ?: "<default>"}) 실패",
            )
            return
        }

        val koreanLocale = Locale.forLanguageTag(KOREAN_LANGUAGE_TAG)
        val languageResult = textToSpeech?.setLanguage(koreanLocale)
        publishEvent(
            "CONFIG TTS setLanguage(language=$KOREAN_LANGUAGE_TAG, result=$languageResult)",
        )
        val isKoreanAvailable =
            (languageResult != null) &&
                (languageResult != TextToSpeech.LANG_MISSING_DATA) &&
                (languageResult != TextToSpeech.LANG_NOT_SUPPORTED)

        if (!isKoreanAvailable) {
            updateState(
                state.copy(
                    ttsAvailability = TtsAvailability.Unavailable,
                    mode = VoicePocMode.Error,
                    errorMessage = "설치된 TTS 엔진이 한국어를 지원하지 않습니다.",
                ),
            )
            publishEvent("한국어 TTS 지원 없음")
            return
        }

        textToSpeech?.setOnUtteranceProgressListener(
            object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    if (utteranceId != TEST_UTTERANCE_ID) return
                    runOnMain {
                        if (!released) {
                            updateState(
                                state.copy(
                                    mode = VoicePocMode.Speaking,
                                    errorMessage = null,
                                ),
                            )
                            publishEvent("CALLBACK TTS onStart")
                        }
                    }
                }

                override fun onDone(utteranceId: String?) {
                    if (utteranceId != TEST_UTTERANCE_ID) return
                    runOnMain {
                        if (released) return@runOnMain
                        val shouldListen = listenAfterTts
                        listenAfterTts = false
                        updateState(state.copy(mode = VoicePocMode.Idle))
                        publishEvent("CALLBACK TTS onDone")
                        if (shouldListen) {
                            scheduleStartListeningAfterTts()
                        }
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    handleTtsError(utteranceId)
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    handleTtsError(utteranceId, errorCode)
                }
            },
        )

        updateState(
            state.copy(
                ttsAvailability = TtsAvailability.Available,
                mode = VoicePocMode.Idle,
                errorMessage = null,
            ),
        )
        publishEvent(
            "CALLBACK TTS onInit(status=$status, " +
                "engine=${textToSpeech?.defaultEngine ?: defaultTtsEnginePackage}) 성공: " +
                "한국어 TTS가 준비되었습니다.",
        )
    }

    private fun handleTtsError(utteranceId: String?, errorCode: Int? = null) {
        if (utteranceId != TEST_UTTERANCE_ID) return
        runOnMain {
            listenAfterTts = false
            cancelPendingStartListening()
            val suffix = errorCode?.let { " (code=$it)" }.orEmpty()
            publishError("CALLBACK TTS onError$suffix: TTS 재생 중 오류가 발생했습니다.")
        }
    }

    private fun scheduleStartListeningAfterTts() {
        cancelPendingStartListening()
        val runnable = Runnable {
            pendingStartListening = null
            if (!released) {
                startListeningInternal(trigger = "tts_onDone")
            }
        }
        pendingStartListening = runnable
        mainHandler.postDelayed(runnable, STT_AFTER_TTS_DELAY_MS)
        publishEvent("SCHEDULE STT after TTS (${STT_AFTER_TTS_DELAY_MS}ms)")
    }

    private fun cancelPendingStartListening() {
        pendingStartListening?.let(mainHandler::removeCallbacks)
        pendingStartListening = null
    }

    private fun scheduleRecognitionTimeout(
        expectedCallback: String,
        delayMillis: Long,
    ) {
        cancelRecognitionTimeout()
        val runnable = Runnable {
            recognitionTimeout = null
            if (
                !released &&
                ((state.mode == VoicePocMode.Listening) ||
                    (state.mode == VoicePocMode.Processing))
            ) {
                ignoreNextRecognitionError = true
                speechRecognizer?.cancel()
                publishError(
                    "APP_TIMEOUT waitingFor=$expectedCallback after=${delayMillis}ms: " +
                        "SpeechRecognizer가 오류 코드나 콜백을 반환하지 않았습니다.",
                )
            }
        }
        recognitionTimeout = runnable
        mainHandler.postDelayed(runnable, delayMillis)
    }

    private fun cancelRecognitionTimeout() {
        recognitionTimeout?.let(mainHandler::removeCallbacks)
        recognitionTimeout = null
    }

    private fun cancelRecognitionSilently() {
        cancelRecognitionTimeout()
        if (
            (state.mode == VoicePocMode.Listening) ||
            (state.mode == VoicePocMode.Processing)
        ) {
            ignoreNextRecognitionError = true
            publishEvent("REQUEST cancelRecognition")
            speechRecognizer?.cancel()
        }
    }

    private fun hasRecordAudioPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED

    private fun permissionStatus(): String =
        if (hasRecordAudioPermission()) "GRANTED" else "DENIED"

    override fun onReadyForSpeech(params: Bundle?) {
        scheduleRecognitionTimeout(
            expectedCallback = "onBeginningOfSpeech/onError",
            delayMillis = SPEECH_START_TIMEOUT_MS,
        )
        publishEvent("CALLBACK onReadyForSpeech")
    }

    override fun onBeginningOfSpeech() {
        scheduleRecognitionTimeout(
            expectedCallback = "onEndOfSpeech/onError",
            delayMillis = SPEECH_END_TIMEOUT_MS,
        )
        publishEvent("CALLBACK onBeginningOfSpeech")
    }

    override fun onRmsChanged(rmsdB: Float) = Unit

    override fun onBufferReceived(buffer: ByteArray?) = Unit

    override fun onEndOfSpeech() {
        updateState(state.copy(mode = VoicePocMode.Processing))
        scheduleRecognitionTimeout(
            expectedCallback = "onResults/onError",
            delayMillis = RESULT_CALLBACK_TIMEOUT_MS,
        )
        publishEvent("CALLBACK onEndOfSpeech")
    }

    override fun onError(error: Int) {
        cancelRecognitionTimeout()
        val errorInfo = speechRecognitionErrorInfo(error)
        if (ignoreNextRecognitionError) {
            ignoreNextRecognitionError = false
            publishEvent(
                "CALLBACK onError(code=$error, name=${errorInfo.name}) ignoredAfterCancel",
            )
            return
        }
        publishError(
            "CALLBACK onError(code=$error, name=${errorInfo.name}): ${errorInfo.message}",
        )
    }

    override fun onResults(results: Bundle?) {
        cancelRecognitionTimeout()
        val recognizedTexts =
            results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                .orEmpty()
        val recognizedText = recognizedTexts.firstOrNull().orEmpty()

        updateState(
            state.copy(
                mode = VoicePocMode.Idle,
                recognizedText = recognizedText,
                errorMessage = if (recognizedText.isBlank()) {
                    "인식 결과가 비어 있습니다."
                } else {
                    null
                },
            ),
        )
        publishEvent(
            "CALLBACK onResults(count=${recognizedTexts.size}, " +
                "best=${recognizedText.ifBlank { "<empty>" }})",
        )
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val partialText =
            partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                .orEmpty()
        if (partialText.isNotBlank()) {
            updateState(state.copy(recognizedText = partialText))
            publishEvent("CALLBACK onPartialResults(best=$partialText)")
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) = Unit

    private fun speechRecognitionErrorInfo(error: Int): SpeechRecognitionErrorInfo =
        when (error) {
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                SpeechRecognitionErrorInfo("ERROR_NETWORK_TIMEOUT", "STT 네트워크 연결 시간이 초과되었습니다.")
            SpeechRecognizer.ERROR_NETWORK ->
                SpeechRecognitionErrorInfo("ERROR_NETWORK", "STT 네트워크 오류가 발생했습니다.")
            SpeechRecognizer.ERROR_AUDIO ->
                SpeechRecognitionErrorInfo("ERROR_AUDIO", "오디오 입력 오류가 발생했습니다.")
            SpeechRecognizer.ERROR_SERVER ->
                SpeechRecognitionErrorInfo("ERROR_SERVER", "음성 인식 서비스 오류가 발생했습니다.")
            SpeechRecognizer.ERROR_CLIENT ->
                SpeechRecognitionErrorInfo("ERROR_CLIENT", "STT 클라이언트 오류가 발생했습니다.")
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                SpeechRecognitionErrorInfo("ERROR_SPEECH_TIMEOUT", "음성이 감지되지 않았습니다.")
            SpeechRecognizer.ERROR_NO_MATCH ->
                SpeechRecognitionErrorInfo("ERROR_NO_MATCH", "음성을 인식하지 못했습니다. 다시 말해 주세요.")
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
                SpeechRecognitionErrorInfo("ERROR_RECOGNIZER_BUSY", "음성 인식기가 이미 사용 중입니다.")
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                SpeechRecognitionErrorInfo("ERROR_INSUFFICIENT_PERMISSIONS", "마이크 권한이 없습니다.")
            SpeechRecognizer.ERROR_TOO_MANY_REQUESTS ->
                SpeechRecognitionErrorInfo("ERROR_TOO_MANY_REQUESTS", "음성 인식 요청이 너무 많습니다.")
            SpeechRecognizer.ERROR_SERVER_DISCONNECTED ->
                SpeechRecognitionErrorInfo("ERROR_SERVER_DISCONNECTED", "음성 인식 서버 연결이 끊겼습니다.")
            SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED ->
                SpeechRecognitionErrorInfo(
                    "ERROR_LANGUAGE_NOT_SUPPORTED",
                    "$KOREAN_LANGUAGE_TAG 언어가 지원되지 않습니다.",
                )
            SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE ->
                SpeechRecognitionErrorInfo(
                    "ERROR_LANGUAGE_UNAVAILABLE",
                    "$KOREAN_LANGUAGE_TAG 언어를 현재 사용할 수 없습니다.",
                )
            SpeechRecognizer.ERROR_CANNOT_CHECK_SUPPORT ->
                SpeechRecognitionErrorInfo(
                    "ERROR_CANNOT_CHECK_SUPPORT",
                    "음성 인식 언어 지원 여부를 확인할 수 없습니다.",
                )
            SpeechRecognizer.ERROR_CANNOT_LISTEN_TO_DOWNLOAD_EVENTS ->
                SpeechRecognitionErrorInfo(
                    "ERROR_CANNOT_LISTEN_TO_DOWNLOAD_EVENTS",
                    "언어 모델 다운로드 이벤트를 수신할 수 없습니다.",
                )
            else ->
                SpeechRecognitionErrorInfo("UNKNOWN", "알 수 없는 STT 오류가 발생했습니다.")
        }

    private fun publishError(message: String) {
        Log.e(LOG_TAG, message)
        updateState(
            state.copy(
                mode = VoicePocMode.Error,
                errorMessage = message,
            ),
        )
        publishEvent(message)
    }

    private fun publishEvent(message: String) {
        Log.d(LOG_TAG, message)
        eventNumber += 1
        val updatedEvents = (listOf("#$eventNumber $message") + state.events).take(MAX_EVENTS)
        updateState(state.copy(events = updatedEvents))
    }

    private fun updateState(newState: VoicePocState) {
        state = newState
        onStateChanged(newState)
    }

    private fun runOnMain(action: () -> Unit) {
        mainHandler.post(action)
    }

    private companion object {
        const val LOG_TAG = "SangsangRestStopVoicePoc"
        const val VOICE_RECOGNITION_SERVICE_SETTING = "voice_recognition_service"
        const val TTS_DEFAULT_SYNTH_SETTING = "tts_default_synth"
        const val TTS_SERVICE_ACTION = "android.intent.action.TTS_SERVICE"
        const val KOREAN_LANGUAGE_TAG = "ko-KR"
        const val TEST_UTTERANCE_ID = "sangsangreststop-voice-poc"
        const val STT_AFTER_TTS_DELAY_MS = 500L
        const val READY_CALLBACK_TIMEOUT_MS = 10_000L
        const val SPEECH_START_TIMEOUT_MS = 15_000L
        const val SPEECH_END_TIMEOUT_MS = 15_000L
        const val RESULT_CALLBACK_TIMEOUT_MS = 10_000L
        const val MAX_EVENTS = 20
    }
}
