package com.fazenda.app.data.dao

import androidx.room.*
import com.fazenda.app.data.entity.ScheduleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM schedules WHERE endDate >= :now AND isCompleted = 0 ORDER BY startDate LIMIT 20")
    fun getUpcomingSchedules(now: Long): Flow<List<ScheduleEntity>>

    @Query("SELECT * FROM schedules ORDER BY phaseTime")
    fun getAllSchedules(): Flow<List<ScheduleEntity>>

    @Query("UPDATE schedules SET isCompleted = 1 WHERE id = :id")
    suspend fun markAsCompleted(id: Long)

    @Query("UPDATE schedules SET isCompleted = 0 WHERE id = :id")
    suspend fun markAsPending(id: Long)

    @Query("SELECT * FROM schedules WHERE categoryId = :categoryId ORDER BY phaseTime")
    fun getSchedulesByCategoryId(categoryId: Long): Flow<List<ScheduleEntity>>

    @Query("SELECT * FROM schedules WHERE id = :id")
    suspend fun getScheduleById(id: Long): ScheduleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: ScheduleEntity): Long

    @Update
    suspend fun updateSchedule(schedule: ScheduleEntity)

    @Delete
    suspend fun deleteSchedule(schedule: ScheduleEntity)
}
