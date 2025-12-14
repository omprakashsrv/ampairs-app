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
     * Get distinct attribute values for a product and attribute type
     */
    @Query("""
        SELECT DISTINCT attribute_value
        FROM variant_attributes
        WHERE product_id = :productId
        AND attribute_type = :attributeType
        ORDER BY attribute_value ASC
    """)
    suspend fun getAttributeValues(productId: String, attributeType: String): List<String>

    /**
     * Get all sizes for a product
     */
    @Query("""
        SELECT DISTINCT attribute_value
        FROM variant_attributes
        WHERE product_id = :productId
        AND attribute_type = 'SIZE'
        ORDER BY attribute_value ASC
    """)
    suspend fun getSizes(productId: String): List<String>

    /**
     * Get all colors for a product
     */
    @Query("""
        SELECT DISTINCT attribute_value
        FROM variant_attributes
        WHERE product_id = :productId
        AND attribute_type = 'COLOR'
        ORDER BY attribute_value ASC
    """)
    suspend fun getColors(productId: String): List<String>

    /**
     * Get all materials for a product
     */
    @Query("""
        SELECT DISTINCT attribute_value
        FROM variant_attributes
        WHERE product_id = :productId
        AND attribute_type = 'MATERIAL'
        ORDER BY attribute_value ASC
    """)
    suspend fun getMaterials(productId: String): List<String>

    /**
     * Get all distinct attribute types for a product
     */
    @Query("""
        SELECT DISTINCT attribute_type
        FROM variant_attributes
        WHERE product_id = :productId
        ORDER BY attribute_type ASC
    """)
    suspend fun getAttributeTypes(productId: String): List<String>

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
        AND attribute_type = :attributeType
        AND attribute_value = :attributeValue
    """)
    suspend fun deleteAttributeValue(
        productId: String,
        attributeType: String,
        attributeValue: String
    )
}
