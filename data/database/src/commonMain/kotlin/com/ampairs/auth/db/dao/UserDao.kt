package com.ampairs.auth.db.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.ampairs.auth.db.entity.UserEntity

@Dao
interface UserDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: UserEntity)
    
    @Query("SELECT * FROM userEntity")
    suspend fun selectAll(): List<UserEntity>
    
    @Query("SELECT * FROM userEntity WHERE id = :id")
    suspend fun selectById(id: String): UserEntity?
    
    @Query("DELETE FROM userEntity WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM userEntity WHERE country_code = :countryCode AND phone = :phone")
    suspend fun findByCountryCodeAndPhone(countryCode: Long, phone: String): UserEntity?
}