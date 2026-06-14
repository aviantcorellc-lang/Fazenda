package com.fazenda.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fazenda.app.FazendaApplication
import com.fazenda.app.data.entity.PlantEntity
import com.fazenda.app.data.entity.PlantPhotoEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AddPlantViewModel(application: Application) : AndroidViewModel(application) {

    private val plantRepository = (application as FazendaApplication).plantRepository
    private val plantPhotoRepository = (application as FazendaApplication).plantPhotoRepository

    private val _created = MutableStateFlow(false)
    val created: StateFlow<Boolean> = _created.asStateFlow()

    fun addPlant(plant: PlantEntity) {
        viewModelScope.launch {
            val plantId = plantRepository.insertPlant(plant)
            plant.photoPath?.let { path ->
                if (path.isNotBlank()) {
                    plantPhotoRepository.insertPhoto(
                        PlantPhotoEntity(
                            plantId = plantId,
                            photoPath = path,
                            order = 0
                        )
                    )
                }
            }
            _created.value = true
        }
    }
}
