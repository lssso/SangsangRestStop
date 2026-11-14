package com.sangsangreststop.app.core.voice

import com.sangsangreststop.app.core.audio.ListeningCue

/**
 * '띵동!' 안내음 → 실제 듣기 시작 순서를 보장하는 음성 입력 래퍼.
 *
 * - 안내음이 끝난 뒤에만 [SpeechInput.start]를 부르므로 안내음이 발화로 인식되지 않는다.
 * - onListeningStarted는 실제 듣기를 시작하는 순간에 불려서 화면의 듣기 표시와 일치한다.
 * - [stop]은 재생 중인 안내음과 듣기를 모두 취소한다 (일시정지·전화·내비 개입 시).
 *
 * 모든 호출과 콜백은 메인 스레드에서 이뤄진다.
 */
class CuedSpeechInput(
    private val input: SpeechInput,
    private val cue: ListeningCue?,
) {
    private var token = 0
    private var cuePlaying = false
    private var inputActive = false

    val isAvailable: Boolean get() = input.isAvailable

    fun start(listener: SpeechInput.Listener, onListeningStarted: () -> Unit) {
        stop()
        val mine = ++token
        val begin = {
            if (mine == token) {
                cuePlaying = false
                inputActive = true
                onListeningStarted()
                input.start(Relay(mine, listener))
            }
        }
        if (cue == null) {
            begin()
        } else {
            cuePlaying = true
            cue.play(begin)
        }
    }

    fun stop() {
        token++
        if (cuePlaying) {
            cuePlaying = false
            cue?.stop()
        }
        if (inputActive) {
            inputActive = false
            input.stop()
        }
    }

    fun release() {
        stop()
        cue?.release()
        input.release()
    }

    /** 끝났거나 취소된 듣기의 늦은 콜백은 버린다. */
    private inner class Relay(
        private val mine: Int,
        private val listener: SpeechInput.Listener,
    ) : SpeechInput.Listener {
        override fun onPartial(text: String) {
            if (mine == token) listener.onPartial(text)
        }

        override fun onResult(text: String) = finish { listener.onResult(text) }
        override fun onNoInput() = finish { listener.onNoInput() }
        override fun onError(error: SpeechInputError) = finish { listener.onError(error) }

        private fun finish(deliver: () -> Unit) {
            if (mine != token) return
            inputActive = false
            deliver()
        }
    }
}
