package com.ampairs.cbmaintenance.data.db.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.ampairs.cbmaintenance.data.db.entity.AssetCategoryAliasEntity
import com.ampairs.cbmaintenance.data.db.entity.PmEntryEntity
import com.ampairs.cbmaintenance.data.db.entity.PmScheduleEntity
import com.ampairs.cbmaintenance.data.db.entity.TicketBucketEntity
import com.ampairs.cbmaintenance.data.db.entity.TicketEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PmScheduleDao {

    @Query("SELECT * FROM cb_pm_schedules WHERE active = 1 ORDER BY asset_category ASC")
    fun getAllSchedules(): Flow<List<PmScheduleEntity>>

    @Query("SELECT * FROM cb_pm_schedules WHERE id = :id")
    suspend fun getScheduleById(id: String): PmScheduleEntity?

    @Query("SELECT * FROM cb_pm_schedules WHERE synced = 0")
    suspend fun getUnsyncedSchedules(): List<PmScheduleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: PmScheduleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedules(schedules: List<PmScheduleEntity>)

    @Query("UPDATE cb_pm_schedules SET active = 0, synced = 0 WHERE id = :id")
    suspend fun softDeleteSchedule(id: String)

    @Query("DELETE FROM cb_pm_schedules WHERE id = :id")
    suspend fun hardDeleteSchedule(id: String)
}

@Dao
interface PmEntryDao {

    @Query("SELECT * FROM cb_pm_entries WHERE active = 1 ORDER BY due_date ASC")
    fun getAllEntries(): Flow<List<PmEntryEntity>>

    /** Due / overdue queue for the UI — everything not yet closed out, soonest first. */
    @Query(
        "SELECT * FROM cb_pm_entries WHERE active = 1 AND status IN " +
            "('DUE','OVERDUE','ASSIGNED','IN_PROGRESS') ORDER BY due_date ASC",
    )
    fun getOpenEntries(): Flow<List<PmEntryEntity>>

    @Query("SELECT * FROM cb_pm_entries WHERE id = :id")
    suspend fun getEntryById(id: String): PmEntryEntity?

    @Query("SELECT * FROM cb_pm_entries WHERE synced = 0")
    suspend fun getUnsyncedEntries(): List<PmEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: PmEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<PmEntryEntity>)

    @Query("DELETE FROM cb_pm_entries WHERE id = :id")
    suspend fun hardDeleteEntry(id: String)
}

@Dao
interface TicketDao {

    @Query("SELECT * FROM cb_tickets WHERE active = 1 ORDER BY raised_at DESC")
    fun getAllTickets(): Flow<List<TicketEntity>>

    @Query("SELECT * FROM cb_tickets WHERE id = :id")
    suspend fun getTicketById(id: String): TicketEntity?

    @Query("SELECT * FROM cb_tickets WHERE synced = 0")
    suspend fun getUnsyncedTickets(): List<TicketEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTicket(ticket: TicketEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTickets(tickets: List<TicketEntity>)

    @Query("DELETE FROM cb_tickets WHERE id = :id")
    suspend fun hardDeleteTicket(id: String)
}

@Dao
interface TicketBucketDao {

    /** Full active taxonomy — the cascade (dept → category → sub-cat) is derived in the ViewModel. */
    @Query("SELECT * FROM cb_ticket_buckets WHERE active = 1 ORDER BY department ASC, category ASC, sub_category_1 ASC")
    fun getAllBuckets(): Flow<List<TicketBucketEntity>>

    @Query("SELECT * FROM cb_ticket_buckets WHERE synced = 0")
    suspend fun getUnsyncedBuckets(): List<TicketBucketEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBuckets(buckets: List<TicketBucketEntity>)

    @Query("DELETE FROM cb_ticket_buckets WHERE id = :id")
    suspend fun hardDeleteBucket(id: String)
}

@Dao
interface AssetCategoryAliasDao {

    @Query("SELECT * FROM cb_asset_category_aliases WHERE active = 1 ORDER BY canonical ASC")
    fun getAllAliases(): Flow<List<AssetCategoryAliasEntity>>

    @Query("SELECT * FROM cb_asset_category_aliases WHERE id = :id")
    suspend fun getAliasById(id: String): AssetCategoryAliasEntity?

    @Query("SELECT * FROM cb_asset_category_aliases WHERE synced = 0")
    suspend fun getUnsyncedAliases(): List<AssetCategoryAliasEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlias(alias: AssetCategoryAliasEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAliases(aliases: List<AssetCategoryAliasEntity>)

    @Query("DELETE FROM cb_asset_category_aliases WHERE id = :id")
    suspend fun hardDeleteAlias(id: String)
}
