package com.ampairs.tally.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ampairs.tally.db.entity.StockItemEntity

@Dao
interface StockItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(stockItem: StockItemEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(stockItems: List<StockItemEntity>)
    
    @Update
    suspend fun update(stockItem: StockItemEntity)
    
    @Query("SELECT * FROM stockItemEntity")
    suspend fun selectAll(): List<StockItemEntity>
    
    @Query("SELECT * FROM stockItemEntity WHERE guid = :guid")
    suspend fun getByGuid(guid: String): StockItemEntity?
    
    @Query("SELECT * FROM stockItemEntity WHERE unitName = :unitName")
    suspend fun getByUnitName(unitName: String): StockItemEntity?
    
    @Query("SELECT * FROM stockItemEntity WHERE alias = :alias")
    suspend fun getByAlias(alias: String): StockItemEntity?
    
    @Query("SELECT * FROM stockItemEntity WHERE reservedName = :reservedName")
    suspend fun getByReservedName(reservedName: String): StockItemEntity?
    
    @Query("SELECT * FROM stockItemEntity WHERE parent = :parent")
    suspend fun getByParent(parent: String): List<StockItemEntity>
    
    @Query("SELECT * FROM stockItemEntity WHERE parentGuid = :parentGuid")
    suspend fun getByParentGuid(parentGuid: String): List<StockItemEntity>
    
    @Query("SELECT * FROM stockItemEntity WHERE category = :category")
    suspend fun getByCategory(category: String): List<StockItemEntity>
    
    @Query("SELECT * FROM stockItemEntity WHERE alterId = :alterId")
    suspend fun getByAlterId(alterId: String): StockItemEntity?
    
    @Query("SELECT * FROM stockItemEntity WHERE gstApplicable = :gstApplicable")
    suspend fun getByGstApplicable(gstApplicable: String): List<StockItemEntity>
    
    @Query("SELECT * FROM stockItemEntity WHERE tcsApplicable = :tcsApplicable")
    suspend fun getByTcsApplicable(tcsApplicable: String): List<StockItemEntity>
    
    @Query("SELECT * FROM stockItemEntity WHERE tcsCategory = :tcsCategory")
    suspend fun getByTcsCategory(tcsCategory: String): List<StockItemEntity>
    
    @Query("SELECT * FROM stockItemEntity WHERE gstTypeOfSupply = :gstTypeOfSupply")
    suspend fun getByGstTypeOfSupply(gstTypeOfSupply: String): List<StockItemEntity>
    
    @Query("SELECT * FROM stockItemEntity WHERE baseUnits = :baseUnits")
    suspend fun getByBaseUnits(baseUnits: String): List<StockItemEntity>
    
    @Query("SELECT * FROM stockItemEntity WHERE gstRepUOM = :gstRepUOM")
    suspend fun getByGstRepUOM(gstRepUOM: String): List<StockItemEntity>
    
    @Query("SELECT * FROM stockItemEntity WHERE taxability = :taxability")
    suspend fun getByTaxability(taxability: String): List<StockItemEntity>
    
    @Query("SELECT * FROM stockItemEntity WHERE unitName LIKE '%' || :search || '%' OR alias LIKE '%' || :search || '%' OR reservedName LIKE '%' || :search || '%'")
    suspend fun searchStockItems(search: String): List<StockItemEntity>
    
    @Query("SELECT * FROM stockItemEntity WHERE standardCost != '' AND standardCost != '0'")
    suspend fun getItemsWithStandardCost(): List<StockItemEntity>
    
    @Query("SELECT * FROM stockItemEntity WHERE standardPrice != '' AND standardPrice != '0'")
    suspend fun getItemsWithStandardPrice(): List<StockItemEntity>
    
    @Query("SELECT DISTINCT category FROM stockItemEntity WHERE category != ''")
    suspend fun getAllCategories(): List<String>
    
    @Query("SELECT DISTINCT gstTypeOfSupply FROM stockItemEntity WHERE gstTypeOfSupply != ''")
    suspend fun getAllGstTypesOfSupply(): List<String>
    
    @Query("SELECT DISTINCT baseUnits FROM stockItemEntity WHERE baseUnits != ''")
    suspend fun getAllBaseUnits(): List<String>
    
    @Query("SELECT DISTINCT gstRepUOM FROM stockItemEntity WHERE gstRepUOM != ''")
    suspend fun getAllGstRepUOMs(): List<String>
    
    @Query("SELECT COUNT(*) FROM stockItemEntity WHERE gstApplicable = :gstApplicable")
    suspend fun countByGstApplicable(gstApplicable: String): Int
    
    @Query("SELECT COUNT(*) FROM stockItemEntity WHERE tcsApplicable = :tcsApplicable")
    suspend fun countByTcsApplicable(tcsApplicable: String): Int
    
    @Query("SELECT COUNT(*) FROM stockItemEntity WHERE category = :category")
    suspend fun countByCategory(category: String): Int
    
    @Query("DELETE FROM stockItemEntity WHERE guid = :guid")
    suspend fun deleteByGuid(guid: String)
    
    @Query("DELETE FROM stockItemEntity")
    suspend fun deleteAll()
    
    @Query("SELECT COUNT(*) FROM stockItemEntity")
    suspend fun count(): Int
}