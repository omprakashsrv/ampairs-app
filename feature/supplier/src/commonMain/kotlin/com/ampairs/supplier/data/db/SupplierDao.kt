package com.ampairs.supplier.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SupplierDao {

    @Query("SELECT * FROM suppliers WHERE active = 1 ORDER BY name ASC")
    fun getAllSuppliers(): Flow<List<SupplierEntity>>

    @Query("SELECT * FROM suppliers WHERE id = :supplierId")
    suspend fun getSupplierById(supplierId: String): SupplierEntity?

    @Query("SELECT * FROM suppliers WHERE id = :supplierId")
    fun observeSupplierById(supplierId: String): Flow<SupplierEntity?>

    @Query(
        """
        SELECT * FROM suppliers
        WHERE active = 1
        AND (
            :searchQuery = ''
            OR name LIKE '%' || :searchQuery || '%'
            OR REPLACE(phone, ' ', '') LIKE '%' || REPLACE(:searchQuery, ' ', '') || '%'
            OR email LIKE '%' || :searchQuery || '%'
            OR gstNumber LIKE '%' || :searchQuery || '%'
            OR state LIKE '%' || :searchQuery || '%'
        )
        ORDER BY name ASC
    """
    )
    fun filterSuppliers(searchQuery: String): Flow<List<SupplierEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplier(supplier: SupplierEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSuppliers(suppliers: List<SupplierEntity>)

    @Update
    suspend fun updateSupplier(supplier: SupplierEntity)

    @Query("DELETE FROM suppliers WHERE id = :supplierId")
    suspend fun deleteSupplier(supplierId: String)

    @Query("SELECT COUNT(*) FROM suppliers WHERE active = 1")
    suspend fun getSupplierCount(): Int

    @Query("SELECT * FROM suppliers WHERE synced = 0")
    suspend fun getUnsyncedSuppliers(): List<SupplierEntity>

    @Query("SELECT COUNT(*) FROM suppliers WHERE synced = 0")
    fun observeUnsyncedCount(): Flow<Int>

    @Query("DELETE FROM suppliers")
    suspend fun clearWorkspaceSuppliers()
}
