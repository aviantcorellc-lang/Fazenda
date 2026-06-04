package com.fazenda.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fazenda.app.FazendaApplication
import com.fazenda.app.data.entity.LogEntity
import com.fazenda.app.data.entity.PlantEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class JournalViewModel(application: Application) : AndroidViewModel(application) {

    private val logRepository = (application as FazendaApplication).logRepository
    private val plantRepository = (application as FazendaApplication).plantRepository

    private val _logs = MutableStateFlow<List<LogEntity>>(emptyList())
    val logs: StateFlow<List<LogEntity>> = _logs.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        viewModelScope.launch {
            logRepository.allLogs.collect { list ->
                _logs.value = list
                _isLoading.value = false
            }
        }
    }

    suspend fun getPlantById(plantId: Long?): PlantEntity? {
        return plantId?.let { plantRepository.getPlantById(it) }
    }

    suspend fun deleteLog(log: LogEntity) {
        logRepository.deleteLog(log)
    }
}
