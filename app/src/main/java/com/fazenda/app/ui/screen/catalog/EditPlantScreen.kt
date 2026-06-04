package com.fazenda.app.ui.screen.catalog

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.fazenda.app.data.entity.PlantEntity
import com.fazenda.app.data.entity.ZoneEntity
import com.fazenda.app.service.FileService
import com.fazenda.app.service.LocationService
import com.fazenda.app.ui.util.PhotoPathResolver
import com.fazenda.app.ui.viewmodel.PlantDetailsViewModel
import com.fazenda.app.ui.viewmodel.PlantDetailsViewModelFactory
import com.fazenda.app.ui.viewmodel.ZoneViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPlantScreen(
    plantId: Long,
    onNavigateBack: () -> Unit,
    viewModel: PlantDetailsViewModel = viewModel(
        factory = PlantDetailsViewModelFactory(plantId)
    ),
    zoneViewModel: ZoneViewModel = viewModel()
) {
    val plant by viewModel.plant.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val zones by zoneViewModel.zones.collectAsState()
    val context = LocalContext.current
    val fileService = remember(context) { FileService(context) }
    val locationService = remember(context) { LocationService(context) }
    val scope = rememberCoroutineScope()

    var categoryText by remember { mutableStateOf("") }
    var nameText by remember { mutableStateOf("") }
    var selectedZone by remember { mutableStateOf<ZoneEntity?>(null) }
    var rowText by remember { mutableStateOf("") }
    var positionText by remember { mutableStateOf("") }
    var commentText by remember { mutableStateOf("") }
    var latitudeText by remember { mutableStateOf("") }
    var longitudeText by remember { mutableStateOf("") }
    var selectedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isGettingLocation by remember { mutableStateOf(false) }
    var zoneDropdownExpanded by remember { mutableStateOf(false) }

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

    LaunchedEffect(plant) {
        plant?.let { p ->
            categoryText = p.category
            nameText = p.name
            selectedZone = p.zoneId?.let { id -> zones.find { it.id == id } }
            rowText = p.row?.toString() ?: ""
            positionText = p.position?.toString() ?: ""
            commentText = p.comment ?: ""
            latitudeText = p.latitude?.toString() ?: ""
            longitudeText = p.longitude?.toString() ?: ""
        }
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
            selectedPhotoUri != null -> selectedPhotoUri
            else -> PhotoPathResolver.toAsyncImageModel(context, p.photoPath)
        }

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Редагувати рослину") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            val currentPhotoPath = p.photoPath
                            val finalPhotoPath = selectedPhotoUri?.let { fileService.savePhotoFromUri(it) }
                                ?: currentPhotoPath

                            viewModel.updatePlant(
                                PlantEntity(
                                    id = p.id,
                                    category = categoryText,
                                    name = nameText,
                                    zoneId = selectedZone?.id,
                                    row = rowText.toFloatOrNull(),
                                    position = positionText.toFloatOrNull(),
                                    comment = commentText.ifBlank { null },
                                    photoPath = finalPhotoPath,
                                    latitude = latitudeText.toDoubleOrNull(),
                                    longitude = longitudeText.toDoubleOrNull()
                                )
                            )

                            if (selectedPhotoUri != null && fileService.isManagedInternalPhotoPath(currentPhotoPath) && currentPhotoPath != finalPhotoPath) {
                                currentPhotoPath?.let { fileService.deletePhoto(it) }
                            }

                            onNavigateBack()
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

                Button(
                    onClick = { uriLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Змінити фото")
                }
                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = categoryText,
                    onValueChange = { categoryText = it },
                    label = { Text("Категорія") },
                    singleLine = true,
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

                Text("Розташування", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                ExposedDropdownMenuBox(
                    expanded = zoneDropdownExpanded,
                    onExpandedChange = { zoneDropdownExpanded = !zoneDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedZone?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Зона") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = zoneDropdownExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = zoneDropdownExpanded,
                        onDismissRequest = { zoneDropdownExpanded = false }
                    ) {
                        zones.forEach { zone ->
                            DropdownMenuItem(
                                text = { Text(zone.name) },
                                onClick = {
                                    selectedZone = zone
                                    zoneDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
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
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    label = { Text("Коментар") },
                    maxLines = 3,
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
        }
    }
}
