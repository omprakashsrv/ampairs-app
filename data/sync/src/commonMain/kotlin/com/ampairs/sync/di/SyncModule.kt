package com.ampairs.sync.di

import com.ampairs.common.di.AppScope
import com.ampairs.sync.SyncDelegate
import com.ampairs.sync.SyncEntity
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Multibinds

@ContributesTo(AppScope::class)
interface SyncModule {
    /** Declares the multibinding map so Metro can inject Map<SyncEntity, SyncDelegate>. */
    @Multibinds
    fun syncDelegates(): Map<SyncEntity, SyncDelegate>
}
