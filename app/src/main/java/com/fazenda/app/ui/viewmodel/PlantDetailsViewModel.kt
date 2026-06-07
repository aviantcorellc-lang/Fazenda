package com.fazenda.app.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.fazenda.app.FazendaApplication
import com.fazenda.app.data.entity.LogEntity
import com.fazenda.app.data.entity.PlantEntity
import com.fazenda.app.data.entity.PlantPhotoEntity
import com.fazenda.app.service.FileService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlantDetailsViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val plantId: Long = savedStateHandle["plantId"] ?: 0L
    private val plantRepository = (application as FazendaApplication).plantRepository
    private val plantPhotoRepository = (application as FazendaApplication).plantPhotoRepository
    private val logRepository = (application as FazendaApplication).logRepository
    private val fileService = FileService(application)

    private val _plant = MutableStateFlow<PlantEntity?>(null)
    val plant: StateFlow<PlantEntity?> = _plant.asStateFlow()

    private val _plantPhotos = MutableStateFlow<List<PlantPhotoEntity>>(emptyList())
    val plantPhotos: StateFlow<List<PlantPhotoEntity>> = _plantPhotos.asStateFlow()

    private val _plantLogs = MutableStateFlow<List<LogEntity>>(emptyList())
    val plantLogs: StateFlow<List<LogEntity>> = _plantLogs.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        viewModelScope.launch {
            _plant.value = plantRepository.getPlantById(plantId)
            _isLoading.value = false
        }
        viewModelScope.launch {
            plantPhotoRepository.getPhotosByPlantId(plantId).collect { photos ->
                _plantPhotos.value = photos
            }
        }
        viewModelScope.launch {
            logRepository.getLogsByPlantId(plantId).collect { logs ->
                _plantLogs.value = logs
            }
        }
    }

    fun addPlantPhoto(plantId: Long, uri: Uri) {
        viewModelScope.launch {
            val photoPath = fileService.savePhotoFromUri(uri)
            val photo = PlantPhotoEntity(
                plantId = plantId,
                photoPath = photoPath,
                order = _plantPhotos.value.size
            )
            plantPhotoRepository.insertPhoto(photo)

            val currentPlant = _plant.value
            if (currentPlant != null && currentPlant.photoPath.isNullOrBlank()) {
                val updatedPlant = currentPlant.copy(photoPath = photoPath)
                plantRepository.updatePlant(updatedPlant)
                _plant.value = updatedPlant
            }
        }
    }

    fun replacePlantMainPhoto(plantId: Long, uri: Uri) {
        viewModelScope.launch {
            val currentPlant = _plant.value ?: plantRepository.getPlantById(plantId) ?: return@launch
            val oldPhotoPath = currentPlant.photoPath
            val newPhotoPath = fileService.savePhotoFromUri(uri)

            if (fileService.isManagedInternalPhotoPath(oldPhotoPath) && oldPhotoPath != newPhotoPath) {
                oldPhotoPath?.let { fileService.deletePhoto(it) }
            }

            val updatedPlant = currentPlant.copy(photoPath = newPhotoPath)
            plantRepository.updatePlant(updatedPlant)
            _plant.value = updatedPlant
        }
    }

    fun deletePlantPhoto(photo: PlantPhotoEntity) {
        viewModelScope.launch {
            if (fileService.isManagedInternalPhotoPath(photo.photoPath)) {
                photo.photoPath?.let { fileService.deletePhoto(it) }
            }
            plantPhotoRepository.deletePhoto(photo)

            val remainingPhotos = _plantPhotos.value.filterNot { it.id == photo.id }
            _plantPhotos.value = remainingPhotos

            val currentPlant = _plant.value ?: return@launch
            if (currentPlant.photoPath == photo.photoPath) {
                val replacementPath = remainingPhotos.firstOrNull()?.photoPath
                val updatedPlant = currentPlant.copy(photoPath = replacementPath)
                plantRepository.updatePlant(updatedPlant)
                _plant.value = updatedPlant
            }
        }
    }

    fun setPlantMainPhoto(photo: PlantPhotoEntity) {
        viewModelScope.launch {
            val currentPlant = _plant.value ?: return@launch
            if (currentPlant.photoPath != photo.photoPath) {
                val updatedPlant = currentPlant.copy(photoPath = photo.photoPath)
                plantRepository.updatePlant(updatedPlant)
                _plant.value = updatedPlant
            }
        }
    }

    fun updatePlant(plant: PlantEntity) {
        viewModelScope.launch {
            plantRepository.updatePlant(plant)
            _plant.value = plant
        }
    }

    fun deletePlant(plantId: Long) {
        viewModelScope.launch {
            plantRepository.getPlantById(plantId)?.let { plant ->
                plantRepository.deletePlant(plant)
            }
        }
    }
}
