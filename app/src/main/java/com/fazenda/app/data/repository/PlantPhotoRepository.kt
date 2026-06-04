package com.fazenda.app.data.repository

import com.fazenda.app.data.dao.PlantPhotoDao
import com.fazenda.app.data.entity.PlantPhotoEntity
import kotlinx.coroutines.flow.Flow

class PlantPhotoRepository(private val plantPhotoDao: PlantPhotoDao) {
    fun getPhotosByPlantId(plantId: Long): Flow<List<PlantPhotoEntity>> =
        plantPhotoDao.getPhotosByPlantId(plantId)

    suspend fun getPhotosByPlantIdList(plantId: Long): List<PlantPhotoEntity> =
        plantPhotoDao.getPhotosByPlantIdList(plantId)

    suspend fun getPhotoById(id: Long): PlantPhotoEntity? =
        plantPhotoDao.getPhotoById(id)

    suspend fun insertPhoto(photo: PlantPhotoEntity): Long =
        plantPhotoDao.insertPhoto(photo)

    suspend fun insertPhotos(photos: List<PlantPhotoEntity>) =
        plantPhotoDao.insertPhotos(photos)

    suspend fun updatePhoto(photo: PlantPhotoEntity) =
        plantPhotoDao.updatePhoto(photo)

    suspend fun deletePhoto(photo: PlantPhotoEntity) =
        plantPhotoDao.deletePhoto(photo)

    suspend fun deletePhotosByPlantId(plantId: Long) =
        plantPhotoDao.deletePhotosByPlantId(plantId)
}
