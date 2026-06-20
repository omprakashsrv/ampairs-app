package com.ampairs.printing.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PrinterDao {
    @Query("SELECT * FROM printers WHERE active = 1 ORDER BY name")
    fun observeAll(): Flow<List<PrinterEntity>>

    @Query("SELECT * FROM printers WHERE id = :id")
    suspend fun getById(id: String): PrinterEntity?

    @Query("SELECT * FROM printers WHERE id = :id AND active = 1")
    suspend fun getActiveById(id: String): PrinterEntity?

    @Query("SELECT * FROM printers WHERE active = 1 ORDER BY name LIMIT 1")
    suspend fun firstActive(): PrinterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PrinterEntity)

    @Query("UPDATE printers SET active = 0 WHERE id = :id")
    suspend fun deactivate(id: String)
}

@Dao
interface PrintRoutingDao {
    @Query("SELECT * FROM print_routing")
    fun observeAll(): Flow<List<PrintRoutingEntity>>

    @Query("SELECT * FROM print_routing WHERE document_type = :documentType")
    suspend fun getForType(documentType: String): PrintRoutingEntity?

    @Query("DELETE FROM print_routing")
    suspend fun clearAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PrintRoutingEntity)
}

@Dao
interface PrintJobDao {
    @Query("SELECT * FROM print_jobs ORDER BY created_at_millis DESC")
    fun observeAll(): Flow<List<PrintJobEntity>>

    @Query("SELECT * FROM print_jobs WHERE id = :id")
    suspend fun getById(id: String): PrintJobEntity?

    @Query("SELECT * FROM print_jobs WHERE state IN ('QUEUED', 'FAILED') ORDER BY created_at_millis")
    suspend fun pending(): List<PrintJobEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PrintJobEntity)

    @Query("DELETE FROM print_jobs WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface PrintTemplateDao {
    @Query("SELECT * FROM print_templates WHERE active = 1 AND document_type = :documentType")
    fun observeByType(documentType: String): Flow<List<PrintTemplateEntity>>

    @Query("SELECT * FROM print_templates WHERE active = 1 ORDER BY document_type, printer_class")
    fun observeAllActive(): Flow<List<PrintTemplateEntity>>

    @Query("SELECT COUNT(*) FROM print_templates")
    suspend fun count(): Int

    @Query("SELECT * FROM print_templates WHERE id = :id")
    suspend fun getById(id: String): PrintTemplateEntity?

    @Query("SELECT * FROM print_templates WHERE active = 1 AND document_type = :documentType LIMIT 1")
    suspend fun firstByType(documentType: String): PrintTemplateEntity?

    @Query("SELECT * FROM print_templates WHERE active = 1 AND document_type = :documentType AND printer_class = :printerClass LIMIT 1")
    suspend fun firstByTypeAndClass(documentType: String, printerClass: String): PrintTemplateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PrintTemplateEntity)

    @Query("SELECT * FROM print_templates WHERE synced = 0")
    suspend fun unsynced(): List<PrintTemplateEntity>

    @Query("UPDATE print_templates SET synced = 1 WHERE id = :id")
    suspend fun markSynced(id: String)

    @Query("DELETE FROM print_templates WHERE id = :id")
    suspend fun delete(id: String)
}
