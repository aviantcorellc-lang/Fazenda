package com.fazenda.app.data.repository

import com.fazenda.app.data.dao.PlantDao
import com.fazenda.app.data.entity.LogEntity
import com.fazenda.app.data.entity.LogActionTypes
import com.fazenda.app.data.entity.PlantEntity
import com.fazenda.app.data.entity.PlantWithPhotos
import kotlinx.coroutines.flow.Flow

class PlantRepository(
    private val plantDao: PlantDao,
    private val logRepository: LogRepository
) {
    val allPlants: Flow<List<PlantEntity>> = plantDao.getAllPlants()
    val allPlantsWithPhotos: Flow<List<PlantWithPhotos>> = plantDao.getAllPlantsWithPhotos()

    suspend fun getPlantById(id: Long): PlantEntity? = plantDao.getPlantById(id)

    suspend fun insertPlant(plant: PlantEntity): Long {
        val id = plantDao.insertPlant(plant)
        logRepository.insertLog(
            LogEntity(
                date = System.currentTimeMillis(),
                plantId = id,
                actionType = LogActionTypes.PLANT_ADDED,
                comment = "Рослину '${plant.name}' додано"
            )
        )
        return id
    }

    suspend fun updatePlant(plant: PlantEntity) = plantDao.updatePlant(plant)

    suspend fun deletePlant(plant: PlantEntity) {
        logRepository.insertLog(
            LogEntity(
                date = System.currentTimeMillis(),
                plantId = plant.id,
                actionType = LogActionTypes.PLANT_DELETED,
                comment = "Рослину '${plant.name}' видалено"
            )
        )
        plantDao.deletePlant(plant)
    }
}
