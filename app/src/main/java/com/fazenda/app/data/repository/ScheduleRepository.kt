package com.fazenda.app.data.repository

import com.fazenda.app.data.dao.ScheduleDao
import com.fazenda.app.data.entity.ScheduleEntity
import kotlinx.coroutines.flow.Flow

class ScheduleRepository(private val scheduleDao: ScheduleDao) {
    val allSchedules: Flow<List<ScheduleEntity>> = scheduleDao.getAllSchedules()

    fun getUpcomingSchedules(now: Long): Flow<List<ScheduleEntity>> =
        scheduleDao.getUpcomingSchedules(now)

    fun getSchedulesByCategory(category: String): Flow<List<ScheduleEntity>> =
        scheduleDao.getSchedulesByCategory(category)

    suspend fun getScheduleById(id: Long): ScheduleEntity? = scheduleDao.getScheduleById(id)

    suspend fun insertSchedule(schedule: ScheduleEntity): Long = scheduleDao.insertSchedule(schedule)

    suspend fun updateSchedule(schedule: ScheduleEntity) = scheduleDao.updateSchedule(schedule)

    suspend fun deleteSchedule(schedule: ScheduleEntity) = scheduleDao.deleteSchedule(schedule)

    suspend fun markAsCompleted(id: Long) = scheduleDao.markAsCompleted(id)

    suspend fun markAsPending(id: Long) = scheduleDao.markAsPending(id)
}
