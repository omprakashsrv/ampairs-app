package com.ampairs.agent.permission

import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

/** Desktop has no on-device recognizer (STT is the unsupported fallback), so no permission is needed. */
@Inject
@ContributesBinding(AppScope::class)
class DesktopMicPermissionController : MicPermissionController {
    override suspend fun ensureMicGranted(): Boolean = true
}
