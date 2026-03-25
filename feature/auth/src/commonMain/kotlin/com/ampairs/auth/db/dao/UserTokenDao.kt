package com.ampairs.auth.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ampairs.auth.db.entity.UserTokenEntity
import com.ampairs.common.time.currentTimeMillis

@Dao
interface UserTokenDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserToken(userToken: UserTokenEntity)
    
    @Query("SELECT * FROM userTokenEntity")
    suspend fun selectAll(): List<UserTokenEntity>
    
    @Query("SELECT * FROM userTokenEntity WHERE id = :id")
    suspend fun selectById(id: String = "1"): UserTokenEntity?
    
    // New multi-user methods
    @Query("SELECT * FROM userTokenEntity WHERE user_id = :userId")
    suspend fun selectByUserId(userId: String): UserTokenEntity?
    
    @Query("SELECT * FROM userTokenEntity WHERE is_active = 1 ORDER BY last_used DESC")
    suspend fun selectActiveUsers(): List<UserTokenEntity>
    
    @Query("UPDATE userTokenEntity SET is_active = 0 WHERE user_id = :userId")
    suspend fun deactivateUser(userId: String)
    
    @Query("UPDATE userTokenEntity SET is_active = 1, last_used = :lastUsed WHERE user_id = :userId")
    suspend fun activateUser(userId: String, lastUsed: Long = currentTimeMillis())
    
    @Query("UPDATE userTokenEntity SET last_used = :lastUsed WHERE user_id = :userId")
    suspend fun updateLastUsed(userId: String, lastUsed: Long = currentTimeMillis())
    
    @Query("DELETE FROM userTokenEntity WHERE user_id = :userId")
    suspend fun deleteByUserId(userId: String)
    
    @Query("UPDATE userTokenEntity SET access_token = '', refresh_token = '', is_active = 0 WHERE user_id = :userId")
    suspend fun clearTokensForUser(userId: String)
}