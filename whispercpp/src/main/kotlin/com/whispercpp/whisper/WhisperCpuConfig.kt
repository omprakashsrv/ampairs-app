package com.whispercpp.whisper

import android.util.Log

/**
 * Picks how many threads whisper.cpp inference uses.
 *
 * The whisper.cpp reference counts only "high-performance" cores by parsing `/proc/cpuinfo` +
 * `/sys/.../cpufreq`, but on many modern Android devices those reads are blocked (SELinux) or resolve
 * to just 1–2 big cores — far too few, which makes inference crawl. We use a simple, robust heuristic
 * instead: a few of the available cores, capped so we don't thrash the little cores or starve the UI.
 */
object WhisperCpuConfig {
    private const val LOG_TAG = "WhisperCpuConfig"
    private const val MAX_THREADS = 4

    val preferredThreadCount: Int
        get() {
            val cores = Runtime.getRuntime().availableProcessors()
            val threads = cores.coerceIn(2, MAX_THREADS)
            Log.i(LOG_TAG, "whisper threads = $threads (availableProcessors = $cores)")
            return threads
        }
}
