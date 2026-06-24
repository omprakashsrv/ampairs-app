package com.ampairs.agent.speech

import android.content.Context
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CompletableDeferred
import kotlin.coroutines.cancellation.CancellationException
import android.speech.tts.TextToSpeech as AndroidTts

/**
 * Android TTS actual (T013, FR-009) over [android.speech.tts.TextToSpeech]. Engine init is async, so
 * [speak] awaits the init result before queueing; [isAvailable] reflects init success once it lands.
 *
 * [speak] **suspends until the utterance finishes** (via [UtteranceProgressListener]) so the
 * hands-free voice loop re-opens the mic only after the reply is fully spoken. [stop] (mute / barge-in
 * / exit) completes every pending wait so those coroutines resume immediately; cancelling the calling
 * coroutine also stops playback.
 */
class AndroidTextToSpeech(context: Context) : TextToSpeech {

    @Volatile
    private var initialized = false

    private val ready = CompletableDeferred<Boolean>()

    /** utteranceId → completion signal, completed on done/error/stop. */
    private val pending = ConcurrentHashMap<String, CompletableDeferred<Unit>>()
    private val utteranceCounter = AtomicLong(0)

    private val tts = AndroidTts(context.applicationContext) { status ->
        initialized = status == AndroidTts.SUCCESS
        ready.complete(initialized)
    }

    init {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) { utteranceId?.let { pending.remove(it)?.complete(Unit) } }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) { utteranceId?.let { pending.remove(it)?.complete(Unit) } }
            override fun onError(utteranceId: String?, errorCode: Int) {
                utteranceId?.let { pending.remove(it)?.complete(Unit) }
            }
        })
    }

    override val isAvailable: Boolean
        get() = initialized

    override suspend fun speak(text: String, languageTag: String?) {
        if (!ready.await()) return
        languageTag?.let { tts.language = Locale.forLanguageTag(it) }
        val id = "agent-${utteranceCounter.incrementAndGet()}"
        val done = CompletableDeferred<Unit>()
        pending[id] = done
        tts.speak(text, AndroidTts.QUEUE_FLUSH, null, id)
        try {
            done.await()
        } catch (c: CancellationException) {
            // Calling coroutine cancelled (barge-in / exit) — stop playback and propagate.
            runCatching { tts.stop() }
            pending.remove(id)
            throw c
        }
    }

    override fun stop() {
        runCatching { tts.stop() }
        // Resume any coroutine awaiting a utterance that QUEUE_FLUSH just dropped (no onDone fires).
        pending.values.forEach { it.complete(Unit) }
        pending.clear()
    }
}
