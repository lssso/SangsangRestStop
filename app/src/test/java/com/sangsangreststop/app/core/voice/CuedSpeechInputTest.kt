package com.sangsangreststop.app.core.voice

import com.sangsangreststop.app.core.audio.ListeningCue
import com.sangsangreststop.app.core.audio.MicChimeTone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CuedSpeechInputTest {

    private val events = mutableListOf<String>()

    private inner class FakeCue : ListeningCue {
        var onDone: (() -> Unit)? = null
        override fun play(onDone: () -> Unit) {
            events += "cue.play"
            this.onDone = onDone
        }

        override fun stop() {
            events += "cue.stop"
            onDone = null
        }

        override fun release() = Unit
        fun finish() = onDone?.also { onDone = null }?.invoke()
    }

    private inner class FakeInput : SpeechInput {
        var listener: SpeechInput.Listener? = null
        override val isAvailable = true
        override fun start(listener: SpeechInput.Listener) {
            events += "stt.start"
            this.listener = listener
        }

        override fun stop() {
            events += "stt.stop"
        }

        override fun release() = Unit
    }

    private inner class RecordingListener : SpeechInput.Listener {
        override fun onResult(text: String) {
            events += "result:$text"
        }

        override fun onNoInput() {
            events += "noInput"
        }

        override fun onError(error: SpeechInputError) {
            events += "error:$error"
        }
    }

    private val cue = FakeCue()
    private val input = FakeInput()
    private val cued = CuedSpeechInput(input, cue)

    private fun start() = cued.start(RecordingListener()) { events += "listening" }

    @Test
    fun `chime plays first and listening starts only after it finishes`() {
        start()
        assertEquals(listOf("cue.play"), events)

        cue.finish()
        assertEquals(listOf("cue.play", "listening", "stt.start"), events)
    }

    @Test
    fun `stop during chime cancels it and never starts listening`() {
        start()
        val onDone = cue.onDone!!
        cued.stop()
        onDone() // 늦게 도착한 완료 콜백

        assertEquals(listOf("cue.play", "cue.stop"), events)
    }

    @Test
    fun `stop while listening stops the recognizer and drops late results`() {
        start()
        cue.finish()
        val relay = input.listener!!
        cued.stop()
        relay.onResult("기린")

        assertEquals(listOf("cue.play", "listening", "stt.start", "stt.stop"), events)
    }

    @Test
    fun `result is delivered once and a later stop does not touch the recognizer`() {
        start()
        cue.finish()
        input.listener!!.onResult("기린")
        cued.stop()

        assertEquals(listOf("cue.play", "listening", "stt.start", "result:기린"), events)
    }

    @Test
    fun `every reactivation plays the chime again`() {
        start()
        cue.finish()
        input.listener!!.onNoInput()
        start()
        cue.finish()

        assertEquals(2, events.count { it == "cue.play" })
        assertEquals(2, events.count { it == "stt.start" })
    }

    @Test
    fun `without a cue listening starts immediately`() {
        CuedSpeechInput(input, cue = null).start(RecordingListener()) { events += "listening" }

        assertEquals(listOf("listening", "stt.start"), events)
    }

    @Test
    fun `chime tone is short and fades out to silence`() {
        val samples = MicChimeTone.generate()

        assertEquals(MicChimeTone.SAMPLE_RATE * MicChimeTone.DURATION_MS / 1000, samples.size)
        assertTrue(MicChimeTone.DURATION_MS <= 600)
        assertTrue(samples.any { it > 5_000 })
        assertEquals(0, samples.first().toInt())
        assertTrue(kotlin.math.abs(samples.last().toInt()) < 200)
    }
}
