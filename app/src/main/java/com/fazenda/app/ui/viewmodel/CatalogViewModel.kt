package com.fazenda.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fazenda.app.FazendaApplication
import com.fazenda.app.data.entity.PlantEntity
import com.fazenda.app.data.entity.ZoneEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CatalogViewModel(application: Application) : AndroidViewModel(application) {

    private val plantRepository = (application as FazendaApplication).plantRepository
    private val zoneRepository = (application as FazendaApplication).zoneRepository

    private val _plantsByCategory = MutableStateFlow<Map<String, List<PlantEntity>>>(emptyMap())
    val plantsByCategory: StateFlow<Map<String, List<PlantEntity>>> = _plantsByCategory.asStateFlow()

    private val _allPlants = MutableStateFlow<List<PlantEntity>>(emptyList())
    val allPlants: StateFlow<List<PlantEntity>> = _allPlants.asStateFlow()

    private val _categories = MutableStateFlow<List<String>>(emptyList())
    val categories: StateFlow<List<String>> = _categories.asStateFlow()

    private val _zones = MutableStateFlow<List<ZoneEntity>>(emptyList())
    val zones: StateFlow<List<ZoneEntity>> = _zones.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        viewModelScope.launch {
            zoneRepository.allZones.collect { list ->
                _zones.value = list
            }
        }
        viewModelScope.launch {
            plantRepository.allPlants.collect { plants ->
                val grouped = plants.groupBy { it.category }
                _categories.value = grouped.keys.sorted()
                _plantsByCategory.value = grouped
                _allPlants.value = plants.sortedBy { it.name }
                _isLoading.value = false
            }
        }
    }
}
