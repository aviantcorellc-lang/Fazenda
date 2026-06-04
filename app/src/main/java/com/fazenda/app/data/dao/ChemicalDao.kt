package com.fazenda.app.data.dao

import androidx.room.*
import com.fazenda.app.data.entity.ChemicalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChemicalDao {
    @Query("SELECT * FROM chemicals ORDER BY name")
    fun getAllChemicals(): Flow<List<ChemicalEntity>>

    @Query("SELECT * FROM chemicals WHERE id = :id")
    suspend fun getChemicalById(id: Long): ChemicalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChemical(chemical: ChemicalEntity): Long

    @Update
    suspend fun updateChemical(chemical: ChemicalEntity)

    @Delete
    suspend fun deleteChemical(chemical: ChemicalEntity)
}
