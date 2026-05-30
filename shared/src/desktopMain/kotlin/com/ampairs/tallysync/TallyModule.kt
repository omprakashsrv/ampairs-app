package com.ampairs.tallysync

import com.ampairs.common.di.AppScope
import com.ampairs.tally.TallyApiImpl
import com.ampairs.tally.TallyRepository
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import io.ktor.client.engine.HttpClientEngine

@ContributesTo(AppScope::class)
interface TallyModule {
    companion object {
        @Provides @SingleIn(AppScope::class)
        fun provideTallyRepository(engine: HttpClientEngine): TallyRepository =
            TallyRepository(TallyApiImpl(engine))
    }
}
