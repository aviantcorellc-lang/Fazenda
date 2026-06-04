package com.fazenda.app.data.dao

import androidx.room.*
import com.fazenda.app.data.entity.PlantPhotoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlantPhotoDao {
    @Query("SELECT * FROM plant_photos WHERE plantId = :plantId ORDER BY `order`")
    fun getPhotosByPlantId(plantId: Long): Flow<List<PlantPhotoEntity>>

    @Query("SELECT * FROM plant_photos WHERE plantId = :plantId ORDER BY `order`")
    suspend fun getPhotosByPlantIdList(plantId: Long): List<PlantPhotoEntity>

    @Query("SELECT * FROM plant_photos WHERE id = :id")
    suspend fun getPhotoById(id: Long): PlantPhotoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: PlantPhotoEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhotos(photos: List<PlantPhotoEntity>)

    @Update
    suspend fun updatePhoto(photo: PlantPhotoEntity)

    @Delete
    suspend fun deletePhoto(photo: PlantPhotoEntity)

    @Query("DELETE FROM plant_photos WHERE plantId = :plantId")
    suspend fun deletePhotosByPlantId(plantId: Long)
}
