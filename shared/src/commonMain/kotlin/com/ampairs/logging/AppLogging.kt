package com.ampairs.logging

import co.touchlab.kermit.DefaultFormatter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Message
import co.touchlab.kermit.MessageStringFormatter
import co.touchlab.kermit.Severity
import co.touchlab.kermit.Tag
import co.touchlab.kermit.platformLogWriter
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Installs a Kermit log writer that prefixes every line with a date + wall-clock timestamp
 * (`yyyy-MM-dd HH:mm:ss.SSS`, device timezone) —
 * e.g. `2026-06-28 14:03:21.512 i CentralSyncService: CUSTOMER sync …`.
 *
 * Call once from each platform entry point (desktop `main`, Android `Application.onCreate`,
 * iOS `MainViewController`) before any logging happens.
 *
 * Note: on Android the timestamp rides through the formatted message; Logcat additionally prints
 * its own timestamp column, so lines there carry both.
 */
fun initAppLogging() {
    Logger.setLogWriters(platformLogWriter(TimestampMessageFormatter))
}

private object TimestampMessageFormatter : MessageStringFormatter {
    override fun formatSeverity(severity: Severity): String =
        DefaultFormatter.formatSeverity(severity)

    override fun formatTag(tag: Tag): String =
        DefaultFormatter.formatTag(tag)

    override fun formatMessage(severity: Severity?, tag: Tag?, message: Message): String =
        "${now()} ${DefaultFormatter.formatMessage(severity, tag, message)}"

    private fun now(): String {
        val t = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val millis = (t.nanosecond / 1_000_000).toString().padStart(3, '0')
        val date = "${t.year}-${pad2(t.month.ordinal + 1)}-${pad2(t.day)}"
        val time = "${pad2(t.hour)}:${pad2(t.minute)}:${pad2(t.second)}.$millis"
        return "$date $time"
    }

    private fun pad2(value: Int) = value.toString().padStart(2, '0')
}
