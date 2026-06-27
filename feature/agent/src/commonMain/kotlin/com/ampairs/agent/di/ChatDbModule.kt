package com.ampairs.agent.di

import com.ampairs.agent.data.db.ChatDatabase
import com.ampairs.agent.data.db.dao.ChatMessageDao
import com.ampairs.common.di.WorkspaceScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides

/** Provides the chat DAO from the workspace-scoped [ChatDatabase] (unscoped — new per injection). */
@ContributesTo(WorkspaceScope::class)
interface ChatDbDaoModule {
    companion object {
        @Provides
        fun provideChatMessageDao(db: ChatDatabase): ChatMessageDao = db.chatMessageDao()
    }
}
