package com.fazenda.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fazenda.app.FazendaApplication
import com.fazenda.app.data.entity.CategoryEntity
import com.fazenda.app.data.entity.PlantWithPhotos
import com.fazenda.app.data.entity.ZoneEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CatalogViewModel(application: Application) : AndroidViewModel(application) {

    private val plantRepository = (application as FazendaApplication).plantRepository
    private val zoneRepository = (application as FazendaApplication).zoneRepository
    private val categoryRepository = (application as FazendaApplication).categoryRepository

    private val _allPlantsWithPhotos = MutableStateFlow<List<PlantWithPhotos>>(emptyList())
    val allPlantsWithPhotos: StateFlow<List<PlantWithPhotos>> = _allPlantsWithPhotos.asStateFlow()

    private val _categories = MutableStateFlow<List<CategoryEntity>>(emptyList())
    val categories: StateFlow<List<CategoryEntity>> = _categories.asStateFlow()

    private val _zones = MutableStateFlow<List<ZoneEntity>>(emptyList())
    val zones: StateFlow<List<ZoneEntity>> = _zones.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val selectedCategoryId = MutableStateFlow<Long?>(null)
    val searchQuery = MutableStateFlow("")
    val showSearch = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            zoneRepository.allZones.collect { list ->
                _zones.value = list
            }
        }
        viewModelScope.launch {
            categoryRepository.allCategories.collect { list ->
                _categories.value = list
            }
        }
        viewModelScope.launch {
            plantRepository.allPlantsWithPhotos.collect { plants ->
                _allPlantsWithPhotos.value = plants
                _isLoading.value = false
            }
        }
    }
}
