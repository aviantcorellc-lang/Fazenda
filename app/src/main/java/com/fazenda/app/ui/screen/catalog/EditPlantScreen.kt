package com.fazenda.app.ui.screen.catalog

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.fazenda.app.data.entity.PlantEntity
import com.fazenda.app.data.entity.ZoneEntity
import com.fazenda.app.service.FileService
import com.fazenda.app.service.LocationService
import com.fazenda.app.service.PlantInfoService
import com.fazenda.app.ui.component.ImageViewerDialog
import com.fazenda.app.ui.component.SearchableDropdown
import com.fazenda.app.ui.screen.journal.CameraCaptureDialog
import com.fazenda.app.ui.util.PhotoPathResolver
import com.fazenda.app.ui.viewmodel.CategoryViewModel
import com.fazenda.app.ui.viewmodel.PlantDetailsViewModel
import com.fazenda.app.ui.viewmodel.PlantDetailsViewModelFactory
import com.fazenda.app.ui.viewmodel.ZoneViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPlantScreen(
    plantId: Long,
    onNavigateBack: () -> Unit,
    viewModel: PlantDetailsViewModel = viewModel(
        factory = PlantDetailsViewModelFactory(plantId)
    ),
    zoneViewModel: ZoneViewModel = viewModel(),
    categoryViewModel: CategoryViewModel = viewModel()
) {
    val plant by viewModel.plant.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val zones by zoneViewModel.zones.collectAsState()
    val categories by categoryViewModel.categories.collectAsState()
    val context = LocalContext.current
    val fileService = remember(context) { FileService(context) }
    val locationService = remember(context) { LocationService(context) }
    val plantInfoService = remember { PlantInfoService() }
    val scope = rememberCoroutineScope()

    var categorySearchText by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var nameText by remember { mutableStateOf("") }
    var selectedZone by remember { mutableStateOf<ZoneEntity?>(null) }
    var rowText by remember { mutableStateOf("") }
    var positionText by remember { mutableStateOf("") }
    var commentText by remember { mutableStateOf("") }
    var latitudeText by remember { mutableStateOf("") }
    var longitudeText by remember { mutableStateOf("") }
    var selectedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var cameraPhotoFile by remember { mutableStateOf<File?>(null) }
    var showCamera by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isGettingLocation by remember { mutableStateOf(false) }
    var isFetchingInfo by remember { mutableStateOf(false) }
    var geminiResult by remember { mutableStateOf("") }
    var initialized by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }

    val categoryOptions = remember(categories) { categories.map { it.name } }

    val fetchCurrentLocation = {
        scope.launch {
            isGettingLocation = true
            try {
                val location = locationService.getCurrentLocation()
                if (location != null) {
                    latitudeText = location.latitude.toString()
                    longitudeText = location.longitude.toString()
                } else {
                    Toast.makeText(context, "Не вдалося визначити координати", Toast.LENGTH_SHORT).show()
                }
            } finally {
                isGettingLocation = false
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            fetchCurrentLocation()
        } else {
            Toast.makeText(context, "Потрібен дозвіл на геолокацію", Toast.LENGTH_SHORT).show()
        }
    }

    val uriLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedPhotoUri = uri
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showCamera = true
        } else {
            Toast.makeText(context, "Потрібен дозвіл на камеру", Toast.LENGTH_SHORT).show()
        }
    }

    fun openCamera() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            showCamera = true
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (showCamera) {
        CameraCaptureDialog(
            onDismiss = { showCamera = false },
            onPhotoCaptured = { file ->
                val savedPath = fileService.savePhotoFromFile(file)
                file.delete()
                cameraPhotoFile = File(savedPath)
                showCamera = false
            }
        )
    }

    LaunchedEffect(plant, categories) {
        if (!initialized) {
            plant?.let { p ->
                val catEntity = p.categoryId?.let { id -> categories.find { it.id == id } }
                categorySearchText = catEntity?.name ?: ""
                selectedCategoryId = catEntity?.id
                nameText = p.name
                selectedZone = p.zoneId?.let { id -> zones.find { it.id == id } }
                rowText = p.row?.toString() ?: ""
                positionText = p.position?.toString() ?: ""
                commentText = p.comment ?: ""
                latitudeText = p.latitude?.toString() ?: ""
                longitudeText = p.longitude?.toString() ?: ""
                initialized = true
            }
        }
    }

    val hasUnsavedChanges = plant?.let { p ->
        nameText != p.name ||
        selectedCategoryId != p.categoryId ||
        selectedZone?.id != p.zoneId ||
        rowText != p.row?.toString() ?: "" ||
        positionText != p.position?.toString() ?: "" ||
        commentText != p.comment ?: "" ||
        latitudeText != p.latitude?.toString() ?: "" ||
        longitudeText != p.longitude?.toString() ?: "" ||
        selectedPhotoUri != null || cameraPhotoFile != null
    } ?: false

    BackHandler(enabled = hasUnsavedChanges) {
        showDiscardDialog = true
    }

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else if (plant == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Рослину не знайдено")
        }
    } else {
        val p = plant!!
        val mainPhotoModel = when {
            cameraPhotoFile != null -> Uri.fromFile(cameraPhotoFile)
            selectedPhotoUri != null -> selectedPhotoUri
            else -> PhotoPathResolver.toAsyncImageModel(context, p.photoPath)
        }
        var showEditImageViewer by remember { mutableStateOf(false) }

        if (showEditImageViewer) {
            ImageViewerDialog(images = listOf(mainPhotoModel), onDismiss = { showEditImageViewer = false })
        }

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Редагувати рослину") },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (hasUnsavedChanges) showDiscardDialog = true else onNavigateBack()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            val currentPhotoPath = p.photoPath
                            val finalPhotoPath = cameraPhotoFile?.absolutePath
                                ?: selectedPhotoUri?.let { fileService.savePhotoFromUri(it) }
                                ?: currentPhotoPath

                            scope.launch {
                                val finalCategoryId = selectedCategoryId
                                    ?: if (categorySearchText.isNotBlank()) categoryViewModel.findOrCreateCategory(categorySearchText) else null

                                viewModel.updatePlant(
                                    PlantEntity(
                                        id = p.id,
                                        categoryId = finalCategoryId,
                                        name = nameText,
                                        zoneId = selectedZone?.id,
                                        row = rowText.toFloatOrNull(),
                                        position = positionText.toFloatOrNull(),
                                        comment = (commentText + if (geminiResult.isNotBlank()) "\n\n---\n$geminiResult" else "").ifBlank { null },
                                        photoPath = finalPhotoPath,
                                        latitude = latitudeText.toDoubleOrNull(),
                                        longitude = longitudeText.toDoubleOrNull()
                                    )
                                )

                                if (fileService.isManagedInternalPhotoPath(currentPhotoPath) && currentPhotoPath != finalPhotoPath) {
                                    currentPhotoPath?.let { fileService.deletePhoto(it) }
                                }

                                onNavigateBack()
                            }
                        }) {
                            Icon(Icons.Default.Save, contentDescription = "Зберегти")
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Видалити")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.medium)
                        .align(Alignment.CenterHorizontally)
                        .then(
                            if (mainPhotoModel != null) Modifier.clickable { showEditImageViewer = true }
                            else Modifier
                        )
                ) {
                    if (mainPhotoModel != null) {
                        AsyncImage(
                            model = mainPhotoModel,
                            contentDescription = "${p.name} photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Grass,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .size(64.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { uriLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Галерея")
                    }
                    Button(
                        onClick = { openCamera() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Камера")
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))

                SearchableDropdown(
                    value = categorySearchText,
                    onValueChange = {
                        categorySearchText = it
                        selectedCategoryId = categories.find { cat -> cat.name == it }?.id
                    },
                    options = categoryOptions,
                    label = "Категорія",
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    label = { Text("Назва") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Wikipedia + Gemini section
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "Інформація про рослину",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                val query = if (categorySearchText.isNotBlank() && nameText.isNotBlank()) "$categorySearchText - $nameText" else nameText.ifBlank { categorySearchText }
                                if (query.isNotBlank()) {
                                    isFetchingInfo = true
                                    scope.launch {
                                        try {
                                            val result = plantInfoService.fetchFromWikipedia(query)
                                            if (result != null) {
                                                commentText = if (commentText.isBlank()) result.extract
                                                else "$commentText\n\n${result.extract}"
                                            } else {
                                                Toast.makeText(context, "Не знайдено на Wikipedia", Toast.LENGTH_SHORT).show()
                                            }
                                        } finally {
                                            isFetchingInfo = false
                                        }
                                    }
                                } else {
                                    Toast.makeText(context, "Введіть назву рослини", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isFetchingInfo
                        ) {
                            Icon(Icons.Default.TravelExplore, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isFetchingInfo) "Завантаження..." else "Довантажити з Wikipedia")
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = commentText,
                            onValueChange = { commentText = it },
                            label = { Text("Коментар") },
                            maxLines = 5,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = {
                                val query = URLEncoder.encode("${categorySearchText} ${nameText} догляд вирощування", "UTF-8")
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://gemini.google.com?q=$query"))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.SmartToy, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Запитати Gemini")
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Відповідь від Gemini:", style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = geminiResult,
                            onValueChange = { geminiResult = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Вставте відповідь від Gemini...") },
                            shape = MaterialTheme.shapes.small,
                            minLines = 3
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))

                Text("Розташування", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                SearchableDropdown(
                    value = selectedZone?.name ?: "",
                    onValueChange = { zoneName -> selectedZone = zones.find { it.name == zoneName } },
                    options = zones.map { it.name },
                    label = "Зона",
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = rowText,
                    onValueChange = { rowText = it },
                    label = { Text("Ряд") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = positionText,
                    onValueChange = { positionText = it },
                    label = { Text("Номер у ряду") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))

                Text("GPS Координати", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = latitudeText,
                        onValueChange = { latitudeText = it },
                        label = { Text("Широта") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = KeyboardType.Decimal
                        ),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = longitudeText,
                        onValueChange = { longitudeText = it },
                        label = { Text("Довгота") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = KeyboardType.Decimal
                        ),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        val hasPermission = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED

                        if (hasPermission) {
                            fetchCurrentLocation()
                        } else {
                            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isGettingLocation
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isGettingLocation) "Визначення..." else "Визначити моє місцезнаходження")
                }
            }

            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = { Text("Видалити рослину?") },
                    text = { Text("Ви впевнені, що хочете видалити \"${p.name}\"? Цю дію неможливо скасувати.") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.deletePlant(plantId)
                                showDeleteDialog = false
                                onNavigateBack()
                            }
                        ) {
                            Text("Видалити", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteDialog = false }) {
                            Text("Скасувати")
                        }
                    }
                )
            }

            if (showDiscardDialog) {
                AlertDialog(
                    onDismissRequest = { showDiscardDialog = false },
                    title = { Text("Скасувати зміни?") },
                    text = { Text("Незбережені зміни будуть втрачені.") },
                    confirmButton = {
                        TextButton(onClick = {
                            showDiscardDialog = false
                            onNavigateBack()
                        }) {
                            Text("Скасувати", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDiscardDialog = false }) {
                            Text("Продовжити редагування")
                        }
                    }
                )
            }
        }
    }
}
