package com.ampairs.payment

import com.ampairs.common.di.WorkspaceScope
import com.ampairs.payment.agent.PaymentAgentDao
import com.ampairs.payment.data.db.PaymentRoomDatabase
import com.ampairs.payment.data.db.dao.AdjustmentVoucherDao
import com.ampairs.payment.data.db.dao.LedgerEntryDao
import com.ampairs.payment.data.db.dao.PartyBalanceDao
import com.ampairs.payment.data.db.dao.PaymentAllocationDao
import com.ampairs.payment.data.db.dao.PaymentVoucherDao
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides

@ContributesTo(WorkspaceScope::class)
interface PaymentDaoModule {
    companion object {
        @Provides
        fun providePartyBalanceDao(db: PaymentRoomDatabase): PartyBalanceDao = db.partyBalanceDao()

        @Provides
        fun provideLedgerEntryDao(db: PaymentRoomDatabase): LedgerEntryDao = db.ledgerEntryDao()

        @Provides
        fun providePaymentVoucherDao(db: PaymentRoomDatabase): PaymentVoucherDao = db.paymentVoucherDao()

        @Provides
        fun providePaymentAllocationDao(db: PaymentRoomDatabase): PaymentAllocationDao = db.paymentAllocationDao()

        @Provides
        fun provideAdjustmentVoucherDao(db: PaymentRoomDatabase): AdjustmentVoucherDao = db.adjustmentVoucherDao()

        @Provides
        fun providePaymentAgentDao(db: PaymentRoomDatabase): PaymentAgentDao = db.paymentAgentDao()
    }
}
