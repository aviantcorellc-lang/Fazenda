package com.fazenda.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fazenda.app.FazendaApplication
import com.fazenda.app.data.entity.ZoneEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ZoneViewModel(application: Application) : AndroidViewModel(application) {

    private val zoneRepository = (application as FazendaApplication).zoneRepository

    private val _zones = MutableStateFlow<List<ZoneEntity>>(emptyList())
    val zones: StateFlow<List<ZoneEntity>> = _zones.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        viewModelScope.launch {
            zoneRepository.allZones.collect { list ->
                _zones.value = list
                _isLoading.value = false
            }
        }
    }

    fun addZone(name: String) {
        viewModelScope.launch {
            zoneRepository.insertZone(ZoneEntity(name = name))
        }
    }

    fun updateZone(zone: ZoneEntity) {
        viewModelScope.launch {
            zoneRepository.updateZone(zone)
        }
    }

    fun deleteZone(zone: ZoneEntity) {
        viewModelScope.launch {
            zoneRepository.deleteZone(zone)
        }
    }
}
