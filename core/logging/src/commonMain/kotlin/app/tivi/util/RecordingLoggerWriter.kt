// Copyright 2023, Christopher Banes
// SPDX-License-Identifier: Apache-2.0

package app.tivi.util

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

private const val BUFFER_SIZE: Int = 300

object RecordingLoggerWriter : LogWriter() {
  private val _buffer = MutableStateFlow<List<LogMessage>>(emptyList())

  val buffer: Flow<List<LogMessage>> = _buffer.asStateFlow()

  @OptIn(ExperimentalTime::class)
  override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
    addLog(LogMessage(severity, message, throwable))
  }

  private fun addLog(logMessage: LogMessage) {
    _buffer.update { logs ->
      val newLogs = ArrayDeque(logs)
      while (newLogs.size >= BUFFER_SIZE) {
        newLogs.removeFirst()
      }
      newLogs.addLast(logMessage)
      newLogs.toList()
    }
  }
}

data class LogMessage @OptIn(ExperimentalTime::class) constructor(
  val severity: Severity,
  val message: String,
  val throwable: Throwable?,
  val timestamp: Instant = Clock.System.now(),
)
