package com.sangsangreststop.app.core.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * 마이크가 켜지기 직전에 들려주는 안내음('띵동!').
 * 아이가 언제부터 말하면 되는지 알 수 있게 한다.
 */
interface ListeningCue {
    /**
     * 안내음을 재생하고, 소리가 완전히 끝난 뒤 메인 스레드에서 [onDone]을 부른다.
     * 재생할 수 없으면 소리 없이 곧바로 [onDone]을 불러 게임이 멈추지 않게 한다.
     * [stop]으로 끊기면 [onDone]을 부르지 않는다.
     */
    fun play(onDone: () -> Unit)
    fun stop()
    fun release()
}

/**
 * '띵동' 두 음을 코드로 합성한 PCM 데이터 (16bit mono).
 * 외부 음원 파일을 쓰지 않으므로 별도 라이선스가 필요 없다.
 */
object MicChimeTone {
    const val SAMPLE_RATE = 22_050
    private const val DING_HZ = 659.25 // E5
    private const val DONG_HZ = 523.25 // C5
    private const val DING_MS = 170
    private const val DONG_MS = 300
    const val DURATION_MS = DING_MS + DONG_MS
    private const val AMPLITUDE = 0.6 * Short.MAX_VALUE

    fun generate(): ShortArray = note(DING_HZ, DING_MS) + note(DONG_HZ, DONG_MS)

    private fun note(hz: Double, durationMs: Int): ShortArray {
        val count = SAMPLE_RATE * durationMs / 1000
        val attack = SAMPLE_RATE * 4 / 1000
        val release = SAMPLE_RATE * 12 / 1000
        return ShortArray(count) { i ->
            val t = i.toDouble() / SAMPLE_RATE
            // 기본음에 배음을 조금 섞어 종소리처럼 들리게 한다
            val wave = sin(2 * PI * hz * t) + 0.35 * sin(2 * PI * hz * 2 * t)
            val fadeIn = (i.toDouble() / attack).coerceAtMost(1.0)
            val fadeOut = ((count - i).toDouble() / release).coerceAtMost(1.0)
            val envelope = fadeIn * fadeOut * exp(-5.0 * t)
            (wave / 1.35 * envelope * AMPLITUDE).toInt().toShort()
        }
    }
}

/** [MicChimeTone]을 AudioTrack으로 재생하는 구현. */
class ToneListeningCue : ListeningCue {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val samples = MicChimeTone.generate()
    private var track: AudioTrack? = null
    private var pending: Runnable? = null

    override fun play(onDone: () -> Unit) {
        stop()
        val started = runCatching {
            val t = track ?: createTrack().also { track = it }
            if (t.playState != AudioTrack.PLAYSTATE_STOPPED) t.stop()
            t.reloadStaticData()
            t.play()
        }.onFailure { Log.w(TAG, "Mic chime unavailable", it) }.isSuccess
        val done = Runnable {
            pending = null
            onDone()
        }
        pending = done
        // 스피커 잔향이 마이크로 들어가지 않도록 소리가 끝난 뒤 조금 더 기다린다
        mainHandler.postDelayed(done, if (started) MicChimeTone.DURATION_MS + TAIL_MS else 0L)
    }

    override fun stop() {
        pending?.let(mainHandler::removeCallbacks)
        pending = null
        runCatching {
            track?.takeIf { it.playState == AudioTrack.PLAYSTATE_PLAYING }?.pause()
        }
    }

    override fun release() {
        stop()
        runCatching { track?.release() }
        track = null
    }

    private fun createTrack(): AudioTrack {
        val created = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(MicChimeTone.SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(samples.size * 2)
            .build()
        created.write(samples, 0, samples.size)
        return created
    }

    private companion object {
        const val TAG = "ToneListeningCue"
        const val TAIL_MS = 150L
    }
}
