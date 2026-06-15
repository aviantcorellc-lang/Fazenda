package com.fazenda.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fazenda.app.FazendaApplication
import com.fazenda.app.data.entity.ChemicalEntity
import com.fazenda.app.data.entity.LogWithChemicals
import com.fazenda.app.data.entity.PlantWithPhotos
import kotlinx.coroutines.flow.*

data class SearchResults(
    val plants: List<PlantWithPhotos> = emptyList(),
    val logs: List<LogWithChemicals> = emptyList(),
    val chemicals: List<ChemicalEntity> = emptyList()
)

class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val plantRepository = (application as FazendaApplication).plantRepository
    private val logRepository = (application as FazendaApplication).logRepository
    private val chemicalRepository = (application as FazendaApplication).chemicalRepository
    private val categoryRepository = (application as FazendaApplication).categoryRepository
    private val zoneRepository = (application as FazendaApplication).zoneRepository

    val searchQuery = MutableStateFlow("")

    val categories = categoryRepository.allCategories.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val zones = zoneRepository.allZones.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val searchResults: StateFlow<SearchResults> = combine(
        searchQuery,
        plantRepository.allPlantsWithPhotos,
        logRepository.allLogsWithChemicals,
        chemicalRepository.allChemicals
    ) { query, plants, logs, chemicals ->
        if (query.isBlank()) {
            SearchResults()
        } else {
            val q = query.trim()
            
            val cats = categories.value
            val zns = zones.value
            
            val zoneMap = zns.associateBy { it.id }
            val categoryMap = cats.associateBy { it.id }

            val filteredPlants = plants.filter { pwp ->
                val plant = pwp.plant
                val zoneName = plant.zoneId?.let { zoneMap[it]?.name } ?: ""
                val catName = plant.categoryId?.let { categoryMap[it]?.name } ?: ""
                plant.name.contains(q, ignoreCase = true) ||
                        catName.contains(q, ignoreCase = true) ||
                        zoneName.contains(q, ignoreCase = true) ||
                        plant.row?.toString()?.contains(q) == true ||
                        plant.position?.toString()?.contains(q) == true ||
                        (plant.comment != null && plant.comment.contains(q, ignoreCase = true))
            }

            val filteredLogs = logs.filter { lwc ->
                val log = lwc.log
                val plantName = log.plantId?.let { pid -> plants.find { it.plant.id == pid }?.plant?.name } ?: ""
                val zoneName = log.zoneId?.let { zoneMap[it]?.name } ?: ""
                val catName = log.categoryId?.let { categoryMap[it]?.name } ?: ""
                val chemNames = lwc.chemicals.joinToString(" ") { it.name }
                log.actionType.contains(q, ignoreCase = true) ||
                        plantName.contains(q, ignoreCase = true) ||
                        zoneName.contains(q, ignoreCase = true) ||
                        catName.contains(q, ignoreCase = true) ||
                        chemNames.contains(q, ignoreCase = true) ||
                        (log.comment != null && log.comment.contains(q, ignoreCase = true)) ||
                        (log.aiDiagnosis != null && log.aiDiagnosis.contains(q, ignoreCase = true))
            }

            val filteredChemicals = chemicals.filter { chem ->
                chem.name.contains(q, ignoreCase = true) ||
                        chem.purpose.contains(q, ignoreCase = true) ||
                        chem.chemicalGroup.contains(q, ignoreCase = true)
            }

            SearchResults(
                plants = filteredPlants,
                logs = filteredLogs,
                chemicals = filteredChemicals
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SearchResults())
}
