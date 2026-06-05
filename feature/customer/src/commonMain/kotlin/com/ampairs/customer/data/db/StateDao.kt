package com.ampairs.customer.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StateDao {

    @Query("SELECT * FROM states ORDER BY name ASC")
    fun getAllStates(): Flow<List<StateEntity>>

    @Query("SELECT * FROM states WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchStates(query: String): Flow<List<StateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStates(states: List<StateEntity>)

    @Query("DELETE FROM states WHERE id = :id")
    suspend fun deleteStateById(id: String)
}
