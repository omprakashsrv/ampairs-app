package com.ampairs.purchase

import com.ampairs.common.di.WorkspaceScope
import com.ampairs.purchase.db.PurchaseDatabase
import com.ampairs.purchase.db.dao.PurchaseDao
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides

@ContributesTo(WorkspaceScope::class)
interface PurchaseDaoModule {
    companion object {
        @Provides
        fun providePurchaseDao(db: PurchaseDatabase): PurchaseDao = db.purchaseDao()
    }
}
