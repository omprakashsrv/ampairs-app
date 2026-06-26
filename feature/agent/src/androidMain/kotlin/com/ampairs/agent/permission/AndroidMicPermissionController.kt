package com.ampairs.agent.permission

import android.content.Context
import com.ampairs.common.di.AppScope
import dev.brewkits.grant.AppGrant
import dev.brewkits.grant.GrantFactory
import dev.brewkits.grant.GrantStatus
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

/**
 * Android mic-permission gate (T016) via the Grant library. `GrantManager.request` opens the system
 * `RECORD_AUDIO` dialog from anywhere (Grant ships a self-contained transparent Activity — no
 * lifecycle binding), returning a [GrantStatus]. Requires `RECORD_AUDIO` declared in the manifest.
 */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class AndroidMicPermissionController(context: Context) : MicPermissionController {

    private val grantManager = GrantFactory.create(context)

    override suspend fun ensureMicGranted(): Boolean =
        grantManager.request(AppGrant.MICROPHONE) == GrantStatus.GRANTED
}
