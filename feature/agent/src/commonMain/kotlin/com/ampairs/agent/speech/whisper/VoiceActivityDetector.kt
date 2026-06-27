package com.ampairs.agent.speech.whisper

import kotlin.math.max
import kotlin.math.sqrt

/** What the detector wants the capture loop to do after the latest audio. */
enum class VadDecision {
    /** Keep capturing. */
    CONTINUE,

    /** Speech started and has now been followed by enough trailing silence — end + transcribe. */
    ENDPOINT,

    /** No speech was ever detected within the no-speech window — give up (re-listen). */
    NO_SPEECH_TIMEOUT,
}

/**
 * Dependency-free, frame-synchronous voice-activity detector for the offline Whisper capture loop.
 *
 * Energy-based with an **adaptive noise floor**: rather than a single hand-tuned RMS threshold (which
 * never fits every device/mic/room), it seeds the floor low and tracks the ambient level on silence
 * frames, flagging speech only when a frame rises [speechMargin]× above that floor (never below
 * [absoluteFloor]). This self-calibrates — quiet rooms get a low gate, noisy ones a higher gate.
 *
 * Robustness features of a real endpointer:
 * - **Onset debounce** — needs [onsetFrames] consecutive speech frames to start, so a click/tap or the
 *   start-tone tail doesn't trigger it.
 * - **Hangover** — after speech, ends only after [hangoverMs] of continuous silence, so short
 *   intra-utterance pauses don't cut you off.
 * - **No-speech timeout** — if nothing is ever said, stops after [noSpeechTimeoutMs] instead of
 *   recording to the hard cap.
 *
 * Feed it the raw capture chunks (any size) via [accept]; it splits them into fixed [frameMs] frames
 * internally (carrying the remainder between calls) and returns the current [VadDecision]. Stateful
 * and single-utterance — create a fresh instance per listen.
 */
class VoiceActivityDetector(
    sampleRate: Int = 16_000,
    private val frameMs: Int = 20,
    private val onsetFrames: Int = 3,          // ~60 ms of speech to trigger
    private val hangoverMs: Int = 1_200,        // trailing silence that ends the utterance
    private val noSpeechTimeoutMs: Int = 10_000,
    private val speechMargin: Float = 3.0f,     // threshold = noiseFloor * margin
    private val absoluteFloor: Float = 0.006f,  // never trigger below this RMS
    private val noiseEmaAlpha: Float = 0.05f,   // how fast the floor tracks ambient (silence frames)
) {
    private val frameSamples: Int = (sampleRate * frameMs) / 1000

    /** True once [onsetFrames] consecutive speech frames have been seen. */
    var speechStarted: Boolean = false
        private set

    /** Loudest frame RMS seen so far — logged for threshold tuning. */
    var maxRms: Float = 0f
        private set

    /** Current adaptive ambient floor — logged for tuning. */
    var noiseFloor: Float = absoluteFloor
        private set

    private var onsetCount = 0
    private var silenceMs = 0
    private var totalMs = 0
    private var carry = FloatArray(0)

    /** Process one capture chunk; returns the first non-[VadDecision.CONTINUE] decision it reaches. */
    fun accept(chunk: FloatArray): VadDecision {
        val data = if (carry.isEmpty()) chunk else carry + chunk
        var i = 0
        var decision = VadDecision.CONTINUE
        while (i + frameSamples <= data.size) {
            decision = step(rms(data, i, frameSamples))
            i += frameSamples
            if (decision != VadDecision.CONTINUE) break
        }
        carry = if (i < data.size) data.copyOfRange(i, data.size) else EMPTY
        return decision
    }

    private fun step(r: Float): VadDecision {
        if (r > maxRms) maxRms = r
        totalMs += frameMs
        val threshold = max(absoluteFloor, noiseFloor * speechMargin)
        if (r >= threshold) {
            if (!speechStarted) {
                onsetCount++
                if (onsetCount >= onsetFrames) speechStarted = true
            }
            silenceMs = 0
        } else {
            onsetCount = 0
            noiseFloor += (r - noiseFloor) * noiseEmaAlpha // track ambient on silence only
            if (speechStarted) {
                silenceMs += frameMs
                if (silenceMs >= hangoverMs) return VadDecision.ENDPOINT
            } else if (totalMs >= noSpeechTimeoutMs) {
                return VadDecision.NO_SPEECH_TIMEOUT
            }
        }
        return VadDecision.CONTINUE
    }

    private fun rms(a: FloatArray, off: Int, n: Int): Float {
        var sum = 0.0
        var k = off
        val end = off + n
        while (k < end) {
            val s = a[k].toDouble()
            sum += s * s
            k++
        }
        return sqrt(sum / n).toFloat()
    }

    private companion object {
        val EMPTY = FloatArray(0)
    }
}
