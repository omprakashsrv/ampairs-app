package com.ampairs.customer.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StateDao {

    @Query("SELECT * FROM states ORDER BY name ASC")
    fun getAllStates(): Flow<List<StateEntity>>

    @Query("SELECT * FROM states WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchStates(query: String): Flow<List<StateEntity>>

    @Query("SELECT * FROM states WHERE id = :id")
    suspend fun getStateById(id: String): StateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStates(states: List<StateEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertState(state: StateEntity)

    @Query("DELETE FROM states")
    suspend fun deleteAllStates()

    @Query("SELECT COUNT(*) FROM states")
    suspend fun getStatesCount(): Int

    @Update
    suspend fun updateState(state: StateEntity)

    @Delete
    suspend fun deleteState(state: StateEntity)

    @Query("DELETE FROM states WHERE id = :id")
    suspend fun deleteStateById(id: String)
}