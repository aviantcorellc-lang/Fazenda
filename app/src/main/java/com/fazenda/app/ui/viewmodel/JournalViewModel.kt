package com.fazenda.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fazenda.app.FazendaApplication
import com.fazenda.app.data.entity.CategoryEntity
import com.fazenda.app.data.entity.LogEntity
import com.fazenda.app.data.entity.LogWithChemicals
import com.fazenda.app.data.entity.PlantEntity
import com.fazenda.app.data.entity.ZoneEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class JournalViewModel(application: Application) : AndroidViewModel(application) {

    private val logRepository = (application as FazendaApplication).logRepository
    private val plantRepository = (application as FazendaApplication).plantRepository
    private val zoneRepository = (application as FazendaApplication).zoneRepository
    private val categoryRepository = (application as FazendaApplication).categoryRepository

    // Журнал разом з хімікатами — один запит замість N
    val logsWithChemicals: StateFlow<List<LogWithChemicals>> =
        logRepository.allLogsWithChemicals
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Мапи для відображення назв рослин, зон, категорій
    val plants: StateFlow<Map<Long, PlantEntity>> =
        MutableStateFlow<Map<Long, PlantEntity>>(emptyMap()).also { flow ->
            viewModelScope.launch {
                plantRepository.allPlants.collect { list ->
                    flow.value = list.associateBy { it.id }
                }
            }
        }

    val zones: StateFlow<Map<Long, ZoneEntity>> =
        MutableStateFlow<Map<Long, ZoneEntity>>(emptyMap()).also { flow ->
            viewModelScope.launch {
                zoneRepository.allZones.collect { list ->
                    flow.value = list.associateBy { it.id }
                }
            }
        }

    val categories: StateFlow<Map<Long, CategoryEntity>> =
        MutableStateFlow<Map<Long, CategoryEntity>>(emptyMap()).also { flow ->
            viewModelScope.launch {
                categoryRepository.allCategories.collect { list ->
                    flow.value = list.associateBy { it.id }
                }
            }
        }

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        viewModelScope.launch {
            logRepository.allLogsWithChemicals.collect {
                _isLoading.value = false
            }
        }
    }

    fun deleteLog(log: LogEntity) {
        viewModelScope.launch {
            logRepository.deleteLog(log)
        }
    }
}
