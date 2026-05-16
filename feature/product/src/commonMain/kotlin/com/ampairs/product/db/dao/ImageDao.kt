package com.ampairs.product.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.ampairs.product.db.entity.ImageEntity

@Dao
interface ImageDao {

    @Query("SELECT * FROM imageEntity WHERE id = :id")
    suspend fun imageById(id: String): ImageEntity?

    @Query("SELECT * FROM imageEntity")
    suspend fun getAllImages(): List<ImageEntity>

    @Query("SELECT * FROM imageEntity WHERE name LIKE '%' || :searchText || '%' ORDER BY name ASC")
    suspend fun getImagesByName(searchText: String): List<ImageEntity>

    @Query("SELECT * FROM imageEntity WHERE bucket = :bucket")
    suspend fun getImagesByBucket(bucket: String): List<ImageEntity>

    @Query("SELECT * FROM imageEntity WHERE object_key = :objectKey")
    suspend fun getImageByObjectKey(objectKey: String): ImageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(image: ImageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(images: List<ImageEntity>)

    @Update
    suspend fun update(image: ImageEntity)

    @Query("DELETE FROM imageEntity WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM imageEntity")
    suspend fun deleteAll()

    @Transaction
    suspend fun insertImages(images: List<ImageEntity>) {
        insertAll(images)
    }
}