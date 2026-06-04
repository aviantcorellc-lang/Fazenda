package com.fazenda.app.ui.viewmodel

import android.app.Application
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fazenda.app.FazendaApplication
import com.fazenda.app.data.entity.LogActionTypes
import com.fazenda.app.data.entity.LogEntity
import com.fazenda.app.data.entity.PlantEntity
import com.fazenda.app.service.FileService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class CreateLogViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application
    private val plantRepository = (application as FazendaApplication).plantRepository
    private val logRepository = (application as FazendaApplication).logRepository
    private val fileService = FileService(application)

    private val _plants = MutableStateFlow<List<PlantEntity>>(emptyList())
    val plants: StateFlow<List<PlantEntity>> = _plants.asStateFlow()

    private val _selectedPlant = MutableStateFlow<PlantEntity?>(null)
    val selectedPlant: StateFlow<PlantEntity?> = _selectedPlant.asStateFlow()

    private val _selectedDate = MutableStateFlow(System.currentTimeMillis())
    val selectedDate: StateFlow<Long> = _selectedDate.asStateFlow()

    private val _selectedActionType = MutableStateFlow<String?>(null)
    val selectedActionType: StateFlow<String?> = _selectedActionType.asStateFlow()

    private val _photoPath = MutableStateFlow<String?>(null)
    val photoPath: StateFlow<String?> = _photoPath.asStateFlow()

    private val _photoUri = MutableStateFlow<Uri?>(null)
    val photoUri: StateFlow<Uri?> = _photoUri.asStateFlow()

    private var pendingCameraPhotoFile: File? = null

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val actionTypes = LogActionTypes.USER_FACING

    init {
        viewModelScope.launch {
            plantRepository.allPlants.collect { list ->
                _plants.value = list
                _isLoading.value = false
            }
        }
    }

    fun setSelectedPlant(plant: PlantEntity?) { _selectedPlant.value = plant }
    fun setSelectedDate(date: Long) { _selectedDate.value = date }
    fun setSelectedActionType(type: String?) { _selectedActionType.value = type }
    fun clearPhoto() {
        _photoPath.value?.let { fileService.deletePhoto(it) }
        _photoPath.value = null
        _photoUri.value = null
        pendingCameraPhotoFile?.delete()
        pendingCameraPhotoFile = null
    }

    fun createPhotoUri(): Uri {
        pendingCameraPhotoFile?.delete()
        val photoFile = File(context.cacheDir, "temp/camera_${System.currentTimeMillis()}.jpg")
        photoFile.parentFile?.mkdirs()
        pendingCameraPhotoFile = photoFile
        val uri = FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", photoFile
        )
        _photoUri.value = uri
        return uri
    }

    fun savePhotoFromUri(uri: Uri) {
        val oldPhotoPath = _photoPath.value
        val savedPath = fileService.savePhotoFromUri(uri)
        if (fileService.isManagedInternalPhotoPath(oldPhotoPath) && oldPhotoPath != savedPath) {
            oldPhotoPath?.let { fileService.deletePhoto(it) }
        }
        _photoPath.value = savedPath
        _photoUri.value = null
        pendingCameraPhotoFile?.delete()
        pendingCameraPhotoFile = null
    }

    fun savePhotoFromFile(sourceFile: File, deleteSourceAfterSave: Boolean = false) {
        val oldPhotoPath = _photoPath.value
        val savedPath = fileService.savePhotoFromFile(sourceFile)
        if (fileService.isManagedInternalPhotoPath(oldPhotoPath) && oldPhotoPath != savedPath) {
            oldPhotoPath?.let { fileService.deletePhoto(it) }
        }
        _photoPath.value = savedPath
        _photoUri.value = null
        if (deleteSourceAfterSave) {
            sourceFile.delete()
        }
        pendingCameraPhotoFile?.delete()
        pendingCameraPhotoFile = null
    }

    fun saveLog(comment: String, aiDiagnosis: String) {
        viewModelScope.launch {
            val plant = _selectedPlant.value ?: return@launch
            val type = _selectedActionType.value ?: return@launch

            val log = LogEntity(
                date = _selectedDate.value,
                plantId = plant.id,
                actionType = type,
                photoPath = _photoPath.value,
                comment = comment.ifBlank { null },
                aiDiagnosis = aiDiagnosis.ifBlank { null }
            )
            logRepository.insertLog(log)
            _saved.value = true
        }
    }
}
