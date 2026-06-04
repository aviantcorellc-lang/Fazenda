package com.fazenda.app.data.repository

import com.fazenda.app.data.dao.ZoneDao
import com.fazenda.app.data.entity.ZoneEntity
import kotlinx.coroutines.flow.Flow

class ZoneRepository(
    private val zoneDao: ZoneDao
) {
    val allZones: Flow<List<ZoneEntity>> = zoneDao.getAllZones()

    suspend fun getZoneById(id: Long): ZoneEntity? = zoneDao.getZoneById(id)

    suspend fun getAllZonesList(): List<ZoneEntity> = zoneDao.getAllZonesList()

    suspend fun insertZone(zone: ZoneEntity): Long = zoneDao.insertZone(zone)

    suspend fun updateZone(zone: ZoneEntity) = zoneDao.updateZone(zone)

    suspend fun deleteZone(zone: ZoneEntity) = zoneDao.deleteZone(zone)
}
