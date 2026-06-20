package com.ampairs.payment.data.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import com.ampairs.payment.data.db.dao.AdjustmentVoucherDao
import com.ampairs.payment.data.db.dao.LedgerEntryDao
import com.ampairs.payment.data.db.dao.PartyBalanceDao
import com.ampairs.payment.data.db.dao.PaymentAllocationDao
import com.ampairs.payment.data.db.dao.PaymentVoucherDao
import com.ampairs.payment.data.db.entity.AdjustmentVoucherEntity
import com.ampairs.payment.data.db.entity.LedgerEntryEntity
import com.ampairs.payment.data.db.entity.PartyBalanceEntity
import com.ampairs.payment.data.db.entity.PaymentAllocationEntity
import com.ampairs.payment.data.db.entity.PaymentVoucherEntity

@Database(
    entities = [
        PartyBalanceEntity::class,
        LedgerEntryEntity::class,
        PaymentVoucherEntity::class,
        PaymentAllocationEntity::class,
        AdjustmentVoucherEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@ConstructedBy(PaymentRoomDatabaseConstructor::class)
abstract class PaymentRoomDatabase : RoomDatabase() {
    abstract fun partyBalanceDao(): PartyBalanceDao
    abstract fun ledgerEntryDao(): LedgerEntryDao
    abstract fun paymentVoucherDao(): PaymentVoucherDao
    abstract fun paymentAllocationDao(): PaymentAllocationDao
    abstract fun adjustmentVoucherDao(): AdjustmentVoucherDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object PaymentRoomDatabaseConstructor : RoomDatabaseConstructor<PaymentRoomDatabase> {
    override fun initialize(): PaymentRoomDatabase
}
