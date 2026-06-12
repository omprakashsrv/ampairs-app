package com.ampairs.sequence.di

import com.ampairs.common.di.WorkspaceScope
import com.ampairs.sequence.data.db.SequenceDatabase
import com.ampairs.sequence.data.db.dao.SequenceAllocationDao
import com.ampairs.sequence.data.db.dao.SequenceDefinitionDao
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides

@ContributesTo(WorkspaceScope::class)
interface SequenceDaoModule {
    companion object {
        @Provides
        fun provideSequenceDefinitionDao(db: SequenceDatabase): SequenceDefinitionDao =
            db.sequenceDefinitionDao()

        @Provides
        fun provideSequenceAllocationDao(db: SequenceDatabase): SequenceAllocationDao =
            db.sequenceAllocationDao()
    }
}

fun sequenceModule() = Unit
