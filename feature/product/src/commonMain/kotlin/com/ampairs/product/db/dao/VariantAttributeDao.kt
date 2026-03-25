package com.ampairs.product.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ampairs.product.db.entity.VariantAttributeEntity

/**
 * Variant Attribute DAO - Database operations for variant attributes
 */
@Dao
interface VariantAttributeDao {

    /**
     * Get distinct attribute values for a product and attribute name
     */
    @Query("""
        SELECT DISTINCT attribute_value
        FROM variant_attributes
        WHERE product_id = :productId
        AND attribute_name = :attributeName
        ORDER BY attribute_value ASC
    """)
    suspend fun getAttributeValues(productId: String, attributeName: String): List<String>

    /**
     * Get all distinct attribute names for a product
     */
    @Query("""
        SELECT DISTINCT attribute_name
        FROM variant_attributes
        WHERE product_id = :productId
        ORDER BY attribute_name ASC
    """)
    suspend fun getAttributeNames(productId: String): List<String>

    /**
     * Insert attribute
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttribute(attribute: VariantAttributeEntity)

    /**
     * Insert multiple attributes
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttributes(attributes: List<VariantAttributeEntity>)

    /**
     * Delete all attributes for a product
     */
    @Query("""
        DELETE FROM variant_attributes
        WHERE product_id = :productId
    """)
    suspend fun deleteProductAttributes(productId: String)

    /**
     * Delete specific attribute value
     */
    @Query("""
        DELETE FROM variant_attributes
        WHERE product_id = :productId
        AND attribute_name = :attributeName
        AND attribute_value = :attributeValue
    """)
    suspend fun deleteAttributeValue(
        productId: String,
        attributeName: String,
        attributeValue: String
    )
}
