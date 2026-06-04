package com.fazenda.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.fazenda.app.data.entity.ZoneEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ZoneDao {
    @Query("SELECT * FROM zones ORDER BY name")
    fun getAllZones(): Flow<List<ZoneEntity>>

    @Query("SELECT * FROM zones WHERE id = :id")
    suspend fun getZoneById(id: Long): ZoneEntity?

    @Query("SELECT * FROM zones ORDER BY name")
    suspend fun getAllZonesList(): List<ZoneEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertZone(zone: ZoneEntity): Long

    @Update
    suspend fun updateZone(zone: ZoneEntity)

    @Delete
    suspend fun deleteZone(zone: ZoneEntity)
}
