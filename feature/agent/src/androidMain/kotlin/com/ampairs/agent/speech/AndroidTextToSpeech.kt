package com.ampairs.agent.speech

import android.content.Context
import java.util.Locale
import kotlinx.coroutines.CompletableDeferred
import android.speech.tts.TextToSpeech as AndroidTts

/**
 * Android TTS actual (T013, FR-009) over [android.speech.tts.TextToSpeech]. Engine init is async, so
 * [speak] awaits the init result before queueing; [isAvailable] reflects init success once it lands
 * (best-effort — a reply spoken in the first moments after open may be skipped until init completes).
 */
class AndroidTextToSpeech(context: Context) : TextToSpeech {

    @Volatile
    private var initialized = false

    private val ready = CompletableDeferred<Boolean>()

    private val tts = AndroidTts(context.applicationContext) { status ->
        initialized = status == AndroidTts.SUCCESS
        ready.complete(initialized)
    }

    override val isAvailable: Boolean
        get() = initialized

    override suspend fun speak(text: String, languageTag: String?) {
        if (!ready.await()) return
        languageTag?.let { tts.language = Locale.forLanguageTag(it) }
        tts.speak(text, AndroidTts.QUEUE_FLUSH, null, "agent-${text.hashCode()}")
    }

    override fun stop() {
        runCatching { tts.stop() }
    }
}
