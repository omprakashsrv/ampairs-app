// Copyright 2018, Google LLC, Christopher Banes and the Tivi project contributors
// SPDX-License-Identifier: Apache-2.0

package app.tivi.tasks

import androidx.work.WorkManager
import co.touchlab.kermit.Logger
import me.tatarka.inject.annotations.Inject
import kotlin.time.Duration.Companion.hours

@Inject
class AndroidTasks(
    workManager: Lazy<WorkManager>,
) : Tasks {
    private val workManager by workManager
    private val logger by lazy { Logger.withTag("AndroidTasks") }

    internal companion object {
        val LIBRARY_SYNC_INTERVAL = 12.hours
        val SCHEDULE_EPISODE_NOTIFICATIONS_INTERVAL = 6.hours
    }
}
