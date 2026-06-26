package com.whispercpp.whisper

import android.util.Log
import java.io.File

/**
 * Picks how many threads whisper.cpp inference uses.
 *
 * The sweet spot for whisper.cpp on a CPU is **one thread per high-performance core** — never more.
 * whisper_full waits on its slowest thread, so a thread that lands on a little/efficiency core drags
 * the whole inference down; and going past the physical core count just adds context-switch overhead
 * (whisper.cpp's own guidance: keep threads ≤ cores; benchmarks show diminishing/negative returns
 * beyond the big-core count).
 *
 * So we count the "performance" cores — every core whose max clock is above the slowest tier (i.e.
 * prime + big cores, excluding the efficiency cluster) — by reading each core's `cpuinfo_max_freq`.
 * On a typical 1+3+4 or 2+6 phone that yields 4 / 2 big cores. The upstream whisper.android reference
 * parses `/proc/cpuinfo`, which SELinux often blocks; the per-core `cpufreq` sysfs nodes are world-
 * readable far more often, and we fall back to a conservative core-based heuristic if even those fail.
 */
object WhisperCpuConfig {
    private const val LOG_TAG = "WhisperCpuConfig"

    // Safety cap only. Performance-core detection already returns the right number on heterogeneous
    // SoCs; this just bounds homogeneous/many-core chips where extra threads stop paying off.
    private const val MAX_THREADS = 8
    private const val MIN_THREADS = 2

    val preferredThreadCount: Int
        get() {
            val threads = (highPerfCoreCount() ?: fallbackThreadCount())
                .coerceIn(MIN_THREADS, MAX_THREADS)
            Log.i(LOG_TAG, "whisper threads = $threads (cores = ${Runtime.getRuntime().availableProcessors()})")
            return threads
        }

    /**
     * Count cores in the top frequency tier(s): cores whose max clock is strictly above the slowest
     * core's. Returns null if the cpufreq nodes can't be read (SELinux / missing) so the caller can
     * fall back. A homogeneous CPU (all cores equal) reports every core.
     */
    private fun highPerfCoreCount(): Int? {
        val freqs = ArrayList<Long>()
        var i = 0
        while (i < 64) {
            val node = File("/sys/devices/system/cpu/cpu$i/cpufreq/cpuinfo_max_freq")
            if (!node.exists()) break
            val freq = runCatching { node.readText().trim().toLong() }.getOrNull()
            if (freq != null && freq > 0) freqs.add(freq)
            i++
        }
        if (freqs.isEmpty()) return null

        val minFreq = freqs.min()
        // Performance cores = everything above the efficiency tier. If the chip is homogeneous
        // (all freqs equal), this is 0, so fall back to the full core count in that case.
        val perfCores = freqs.count { it > minFreq }
        return if (perfCores > 0) perfCores else freqs.size
    }

    /** No cpufreq access: assume roughly half the cores are "big" on a modern phone. */
    private fun fallbackThreadCount(): Int {
        val cores = Runtime.getRuntime().availableProcessors()
        return (cores + 1) / 2
    }
}