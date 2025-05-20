package com.tuempresa.fugas.data

import androidx.room.*
import com.tuempresa.fugas.model.AlertStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface AlertStatusDao {
    @Query("SELECT * FROM alert_status")
    fun getAll(): Flow<List<AlertStatus>>

    @Query("SELECT * FROM alert_status WHERE timestamp = :timestamp LIMIT 1")
    suspend fun getByTimestamp(timestamp: String): AlertStatus?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(status: AlertStatus)

    @Query("DELETE FROM alert_status WHERE timestamp = :timestamp")
    suspend fun deleteByTimestamp(timestamp: String)
}
