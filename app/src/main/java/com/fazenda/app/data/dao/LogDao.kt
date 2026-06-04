package com.fazenda.app.data.dao

import androidx.room.*
import com.fazenda.app.data.entity.LogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LogDao {
    @Query("SELECT * FROM logs ORDER BY date DESC")
    fun getAllLogs(): Flow<List<LogEntity>>

    @Query("SELECT * FROM logs WHERE id = :id")
    suspend fun getLogById(id: Long): LogEntity?

    @Query("SELECT * FROM logs WHERE plantId = :plantId ORDER BY date DESC")
    fun getLogsByPlantId(plantId: Long): Flow<List<LogEntity>>

    @Query("SELECT * FROM logs WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getLogsByDateRange(startDate: Long, endDate: Long): Flow<List<LogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: LogEntity): Long

    @Update
    suspend fun updateLog(log: LogEntity)

    @Query("DELETE FROM logs WHERE plantId = :plantId")
    suspend fun deleteLogsByPlantId(plantId: Long)

    @Delete
    suspend fun deleteLog(log: LogEntity)
}
