// Copyright 2023, Google LLC, Christopher Banes and the Tivi project contributors
// SPDX-License-Identifier: Apache-2.0

package app.tivi.tasks

import app.tivi.appinitializers.AppInitializer
import app.tivi.inject.ApplicationCoroutineScope
import me.tatarka.inject.annotations.Inject

@Inject
class TasksInitializer(
    tasks: Lazy<Tasks>,
    private val coroutineScope: ApplicationCoroutineScope,
) : AppInitializer {
    private val tasks by tasks

    override fun initialize() {
        tasks.setup()
    }
}
