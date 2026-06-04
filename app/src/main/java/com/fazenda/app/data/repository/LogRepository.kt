package com.fazenda.app.data.repository

import com.fazenda.app.data.dao.LogDao
import com.fazenda.app.data.entity.LogEntity
import kotlinx.coroutines.flow.Flow

class LogRepository(private val logDao: LogDao) {
    val allLogs: Flow<List<LogEntity>> = logDao.getAllLogs()

    fun getLogsByPlantId(plantId: Long): Flow<List<LogEntity>> =
        logDao.getLogsByPlantId(plantId)

    fun getLogsByDateRange(startDate: Long, endDate: Long): Flow<List<LogEntity>> =
        logDao.getLogsByDateRange(startDate, endDate)

    suspend fun getLogById(id: Long): LogEntity? = logDao.getLogById(id)

    suspend fun insertLog(log: LogEntity): Long = logDao.insertLog(log)

    suspend fun updateLog(log: LogEntity) = logDao.updateLog(log)

    suspend fun deleteLog(log: LogEntity) = logDao.deleteLog(log)

    suspend fun deleteLogsByPlantId(plantId: Long) = logDao.deleteLogsByPlantId(plantId)
}
