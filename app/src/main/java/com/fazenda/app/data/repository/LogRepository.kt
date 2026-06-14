package com.fazenda.app.data.repository

import com.fazenda.app.data.dao.LogDao
import com.fazenda.app.data.entity.LogChemicalCrossRef
import com.fazenda.app.data.entity.LogEntity
import com.fazenda.app.data.entity.LogWithChemicals
import kotlinx.coroutines.flow.Flow

class LogRepository(private val logDao: LogDao) {
    val allLogs: Flow<List<LogEntity>> = logDao.getAllLogs()
    
    val allLogsWithChemicals: Flow<List<LogWithChemicals>> = logDao.getAllLogsWithChemicals()

    fun getLogsByPlantId(plantId: Long): Flow<List<LogWithChemicals>> =
        logDao.getLogsByPlantId(plantId)

    fun getLogsForPlant(plantId: Long, zoneId: Long?, categoryId: Long?): Flow<List<LogWithChemicals>> =
        logDao.getLogsForPlant(plantId, zoneId, categoryId)

    fun getLogsByDateRange(startDate: Long, endDate: Long): Flow<List<LogEntity>> =
        logDao.getLogsByDateRange(startDate, endDate)

    suspend fun getLogById(id: Long): LogEntity? = logDao.getLogById(id)

    suspend fun getLogWithChemicalsById(id: Long): LogWithChemicals? = logDao.getLogWithChemicalsById(id)

    suspend fun insertLog(log: LogEntity): Long = logDao.insertLog(log)

    suspend fun updateLog(log: LogEntity) = logDao.updateLog(log)

    suspend fun deleteLog(log: LogEntity) = logDao.deleteLog(log)

    suspend fun deleteLogsByPlantId(plantId: Long) = logDao.deleteLogsByPlantId(plantId)

    suspend fun insertLogChemicals(crossRefs: List<LogChemicalCrossRef>) =
        logDao.insertLogChemicals(crossRefs)

    suspend fun deleteLogChemicalsByLogId(logId: Long) =
        logDao.deleteLogChemicalsByLogId(logId)
}
