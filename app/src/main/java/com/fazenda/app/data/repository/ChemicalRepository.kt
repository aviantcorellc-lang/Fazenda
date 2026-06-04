package com.fazenda.app.data.repository

import com.fazenda.app.data.dao.ChemicalDao
import com.fazenda.app.data.entity.ChemicalEntity
import kotlinx.coroutines.flow.Flow

class ChemicalRepository(private val chemicalDao: ChemicalDao) {
    val allChemicals: Flow<List<ChemicalEntity>> = chemicalDao.getAllChemicals()

    suspend fun getChemicalById(id: Long): ChemicalEntity? = chemicalDao.getChemicalById(id)

    suspend fun insertChemical(chemical: ChemicalEntity): Long = chemicalDao.insertChemical(chemical)

    suspend fun updateChemical(chemical: ChemicalEntity) = chemicalDao.updateChemical(chemical)

    suspend fun deleteChemical(chemical: ChemicalEntity) = chemicalDao.deleteChemical(chemical)
}
