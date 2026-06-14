package com.fazenda.app.data.dao

import androidx.room.*
import com.fazenda.app.data.entity.LogChemicalCrossRef
import com.fazenda.app.data.entity.LogEntity
import com.fazenda.app.data.entity.LogWithChemicals
import kotlinx.coroutines.flow.Flow

@Dao
interface LogDao {
    @Query("SELECT * FROM logs ORDER BY date DESC")
    fun getAllLogs(): Flow<List<LogEntity>>

    @Transaction
    @Query("SELECT * FROM logs ORDER BY date DESC")
    fun getAllLogsWithChemicals(): Flow<List<LogWithChemicals>>

    @Query("SELECT * FROM logs WHERE id = :id")
    suspend fun getLogById(id: Long): LogEntity?

    @Transaction
    @Query("SELECT * FROM logs WHERE id = :id")
    suspend fun getLogWithChemicalsById(id: Long): LogWithChemicals?

    @Transaction
    @Query("SELECT * FROM logs WHERE plantId = :plantId ORDER BY date DESC")
    fun getLogsByPlantId(plantId: Long): Flow<List<LogWithChemicals>>

    @Transaction
    @Query("""
        SELECT * FROM logs 
        WHERE plantId = :plantId 
           OR (zoneId IS NOT NULL AND zoneId = :zoneId) 
           OR (categoryId IS NOT NULL AND categoryId = :categoryId)
        ORDER BY date DESC
    """)
    fun getLogsForPlant(plantId: Long, zoneId: Long?, categoryId: Long?): Flow<List<LogWithChemicals>>

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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogChemicals(crossRefs: List<LogChemicalCrossRef>)

    @Query("DELETE FROM log_chemicals WHERE logId = :logId")
    suspend fun deleteLogChemicalsByLogId(logId: Long)
}
