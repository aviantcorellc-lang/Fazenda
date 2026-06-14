package com.fazenda.app.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fazenda.app.FazendaApplication
import com.fazenda.app.data.entity.ChemicalEntity
import com.fazenda.app.data.entity.LogActionTypes
import com.fazenda.app.data.entity.LogEntity
import com.fazenda.app.data.entity.PlantEntity
import com.fazenda.app.data.entity.ZoneEntity
import com.fazenda.app.data.entity.CategoryEntity
import com.fazenda.app.data.entity.LogChemicalCrossRef
import com.fazenda.app.service.FileService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

enum class TargetType(val displayName: String) {
    PLANT("Рослина"),
    ZONE("Зона"),
    CATEGORY("Категорія")
}

class CreateLogViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application
    private val plantRepository = (application as FazendaApplication).plantRepository
    private val logRepository = (application as FazendaApplication).logRepository
    private val chemicalRepository = (application as FazendaApplication).chemicalRepository
    private val zoneRepository = (application as FazendaApplication).zoneRepository
    private val categoryRepository = (application as FazendaApplication).categoryRepository
    private val fileService = FileService(application)

    private val _plants = MutableStateFlow<List<PlantEntity>>(emptyList())
    val plants: StateFlow<List<PlantEntity>> = _plants.asStateFlow()

    private val _chemicals = MutableStateFlow<List<ChemicalEntity>>(emptyList())
    val chemicals: StateFlow<List<ChemicalEntity>> = _chemicals.asStateFlow()

    private val _zones = MutableStateFlow<List<ZoneEntity>>(emptyList())
    val zones: StateFlow<List<ZoneEntity>> = _zones.asStateFlow()

    private val _categories = MutableStateFlow<List<CategoryEntity>>(emptyList())
    val categories: StateFlow<List<CategoryEntity>> = _categories.asStateFlow()

    private val _selectedTargetType = MutableStateFlow(TargetType.PLANT)
    val selectedTargetType: StateFlow<TargetType> = _selectedTargetType.asStateFlow()

    private val _selectedPlant = MutableStateFlow<PlantEntity?>(null)
    val selectedPlant: StateFlow<PlantEntity?> = _selectedPlant.asStateFlow()

    private val _selectedZone = MutableStateFlow<ZoneEntity?>(null)
    val selectedZone: StateFlow<ZoneEntity?> = _selectedZone.asStateFlow()

    private val _selectedCategory = MutableStateFlow<CategoryEntity?>(null)
    val selectedCategory: StateFlow<CategoryEntity?> = _selectedCategory.asStateFlow()

    private val _selectedChemicals = MutableStateFlow<List<ChemicalEntity>>(emptyList())
    val selectedChemicals: StateFlow<List<ChemicalEntity>> = _selectedChemicals.asStateFlow()

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
        viewModelScope.launch {
            chemicalRepository.allChemicals.collect { list ->
                _chemicals.value = list
            }
        }
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
    }

    fun setSelectedTargetType(type: TargetType) {
        _selectedTargetType.value = type
        when (type) {
            TargetType.PLANT -> {
                _selectedZone.value = null
                _selectedCategory.value = null
            }
            TargetType.ZONE -> {
                _selectedPlant.value = null
                _selectedCategory.value = null
            }
            TargetType.CATEGORY -> {
                _selectedPlant.value = null
                _selectedZone.value = null
            }
        }
    }

    fun setSelectedPlant(plant: PlantEntity?) { _selectedPlant.value = plant }
    fun setSelectedZone(zone: ZoneEntity?) { _selectedZone.value = zone }
    fun setSelectedCategory(category: CategoryEntity?) { _selectedCategory.value = category }

    fun toggleChemicalSelection(chemical: ChemicalEntity) {
        val current = _selectedChemicals.value.toMutableList()
        if (current.any { it.id == chemical.id }) {
            current.removeAll { it.id == chemical.id }
        } else {
            current.add(chemical)
        }
        _selectedChemicals.value = current
    }

    fun setSelectedDate(date: Long) { _selectedDate.value = date }

    fun setSelectedActionType(type: String?) {
        _selectedActionType.value = type
        if (type != LogActionTypes.SPRAYING && type != LogActionTypes.FEEDING) {
            _selectedChemicals.value = emptyList()
        }
    }

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
            val type = _selectedActionType.value ?: return@launch
            
            val targetType = _selectedTargetType.value
            val plantId = if (targetType == TargetType.PLANT) _selectedPlant.value?.id else null
            val zoneId = if (targetType == TargetType.ZONE) _selectedZone.value?.id else null
            val categoryId = if (targetType == TargetType.CATEGORY) _selectedCategory.value?.id else null

            if (plantId == null && zoneId == null && categoryId == null) return@launch

            val log = LogEntity(
                date = _selectedDate.value,
                plantId = plantId,
                zoneId = zoneId,
                categoryId = categoryId,
                actionType = type,
                photoPath = _photoPath.value,
                comment = comment.ifBlank { null },
                aiDiagnosis = aiDiagnosis.ifBlank { null }
            )
            
            val logId = logRepository.insertLog(log)

            if (type == LogActionTypes.SPRAYING || type == LogActionTypes.FEEDING) {
                val crossRefs = _selectedChemicals.value.map {
                    LogChemicalCrossRef(logId = logId, chemicalId = it.id)
                }
                logRepository.insertLogChemicals(crossRefs)
            }

            _saved.value = true
        }
    }
}
