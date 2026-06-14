package com.fazenda.app.ui.screen.journal

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.fazenda.app.data.entity.LogActionTypes
import com.fazenda.app.ui.component.ImageViewerDialog
import com.fazenda.app.ui.util.PhotoPathResolver
import com.fazenda.app.ui.viewmodel.CategoryViewModel
import com.fazenda.app.ui.viewmodel.CreateLogViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateLogScreen(
    onNavigateBack: () -> Unit,
    createLogViewModel: CreateLogViewModel = viewModel(),
    categoryViewModel: CategoryViewModel = viewModel()
) {
    val plants by createLogViewModel.plants.collectAsState()
    val selectedPlant by createLogViewModel.selectedPlant.collectAsState()
    val selectedActionType by createLogViewModel.selectedActionType.collectAsState()
    val saved by createLogViewModel.saved.collectAsState()
    val categories by categoryViewModel.categories.collectAsState()
    val categoryMap = remember(categories) { categories.associateBy { it.id } }
    val isLoading by createLogViewModel.isLoading.collectAsState()

    val targetType by createLogViewModel.selectedTargetType.collectAsState()
    val selectedZone by createLogViewModel.selectedZone.collectAsState()
    val selectedCategory by createLogViewModel.selectedCategory.collectAsState()
    val selectedChemicals by createLogViewModel.selectedChemicals.collectAsState()

    val context = LocalContext.current
    var comment by remember { mutableStateOf("") }
    var aiDiagnosis by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var showPlantDropdown by remember { mutableStateOf(false) }
    var showActionTypeDropdown by remember { mutableStateOf(false) }
    var showCamera by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }

    val hasUnsavedChanges = comment.isNotBlank() || aiDiagnosis.isNotBlank() ||
        selectedPlant != null || selectedZone != null || selectedCategory != null ||
        selectedActionType != null || selectedChemicals.isNotEmpty() ||
        createLogViewModel.photoPath.value != null

    BackHandler(enabled = hasUnsavedChanges && !saved) {
        showDiscardDialog = true
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            createLogViewModel.savePhotoFromUri(uri)
        }
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
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            showCamera = true
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(saved) {
        if (saved) {
            Toast.makeText(context, "Запис успішно створено", Toast.LENGTH_SHORT).show()
            onNavigateBack()
        }
    }

    if (showCamera) {
        CameraCaptureDialog(
            onDismiss = { showCamera = false },
            onPhotoCaptured = { file ->
                createLogViewModel.savePhotoFromFile(file, deleteSourceAfterSave = true)
                showCamera = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Новий запис") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (hasUnsavedChanges) showDiscardDialog = true else onNavigateBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.padding(paddingValues).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Date
                Text("Дата", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedCard(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(12.dp))
                        val dateFormat = remember { java.text.SimpleDateFormat("yyyy-MM-dd", Locale("uk", "UA")) }
                        Text(dateFormat.format(Date(createLogViewModel.selectedDate.collectAsState().value)))
                    }
                }

                if (showDatePicker) {
                    val datePickerState = rememberDatePickerState(
                        initialSelectedDateMillis = createLogViewModel.selectedDate.value
                    )
                    DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                datePickerState.selectedDateMillis?.let {
                                    createLogViewModel.setSelectedDate(it)
                                }
                                showDatePicker = false
                            }) { Text("OK") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDatePicker = false }) { Text("Скасувати") }
                        }
                    ) {
                        DatePicker(state = datePickerState)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Target Selection
                val zones by createLogViewModel.zones.collectAsState()
                val targetCategories by createLogViewModel.categories.collectAsState()
                var showZoneDropdown by remember { mutableStateOf(false) }
                var showCategoryDropdown by remember { mutableStateOf(false) }

                Text("Об'єкт обробки", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    com.fazenda.app.ui.viewmodel.TargetType.values().forEach { type ->
                        FilterChip(
                            selected = targetType == type,
                            onClick = { createLogViewModel.setSelectedTargetType(type) },
                            label = { Text(type.displayName) }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))

                when (targetType) {
                    com.fazenda.app.ui.viewmodel.TargetType.PLANT -> {
                        ExposedDropdownMenuBox(
                            expanded = showPlantDropdown,
                            onExpandedChange = { showPlantDropdown = it }
                        ) {
                            OutlinedTextField(
                                value = selectedPlant?.let { p -> "${p.name} (${p.categoryId?.let { categoryMap[it]?.name } ?: "?"})" } ?: "",
                                onValueChange = {},
                                readOnly = true,
                                placeholder = { Text("Виберіть рослину") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showPlantDropdown) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = showPlantDropdown,
                                onDismissRequest = { showPlantDropdown = false }
                            ) {
                                plants.forEach { plant ->
                                    DropdownMenuItem(
                                        text = { Text("${plant.name} (${plant.categoryId?.let { categoryMap[it]?.name } ?: "?"})") },
                                        onClick = {
                                            createLogViewModel.setSelectedPlant(plant)
                                            showPlantDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    com.fazenda.app.ui.viewmodel.TargetType.ZONE -> {
                        ExposedDropdownMenuBox(
                            expanded = showZoneDropdown,
                            onExpandedChange = { showZoneDropdown = it }
                        ) {
                            OutlinedTextField(
                                value = selectedZone?.name ?: "",
                                onValueChange = {},
                                readOnly = true,
                                placeholder = { Text("Виберіть зону") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showZoneDropdown) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = showZoneDropdown,
                                onDismissRequest = { showZoneDropdown = false }
                            ) {
                                zones.forEach { zone ->
                                    DropdownMenuItem(
                                        text = { Text(zone.name) },
                                        onClick = {
                                            createLogViewModel.setSelectedZone(zone)
                                            showZoneDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    com.fazenda.app.ui.viewmodel.TargetType.CATEGORY -> {
                        ExposedDropdownMenuBox(
                            expanded = showCategoryDropdown,
                            onExpandedChange = { showCategoryDropdown = it }
                        ) {
                            OutlinedTextField(
                                value = selectedCategory?.name ?: "",
                                onValueChange = {},
                                readOnly = true,
                                placeholder = { Text("Виберіть категорію рослин") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoryDropdown) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = showCategoryDropdown,
                                onDismissRequest = { showCategoryDropdown = false }
                            ) {
                                targetCategories.forEach { category ->
                                    DropdownMenuItem(
                                        text = { Text(category.name) },
                                        onClick = {
                                            createLogViewModel.setSelectedCategory(category)
                                            showCategoryDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action type
                Text("Тип дії", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(8.dp))
                ExposedDropdownMenuBox(
                    expanded = showActionTypeDropdown,
                    onExpandedChange = { showActionTypeDropdown = it }
                ) {
                    OutlinedTextField(
                        value = selectedActionType ?: "",
                        onValueChange = {},
                        readOnly = true,
                        placeholder = { Text("Виберіть тип дії") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showActionTypeDropdown) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = showActionTypeDropdown,
                        onDismissRequest = { showActionTypeDropdown = false }
                    ) {
                        createLogViewModel.actionTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                    createLogViewModel.setSelectedActionType(type)
                                    showActionTypeDropdown = false
                                }
                            )
                        }
                    }
                }

                val chemicals by createLogViewModel.chemicals.collectAsState()
                var showChemicalDropdown by remember { mutableStateOf(false) }

                if (selectedActionType == LogActionTypes.SPRAYING || selectedActionType == LogActionTypes.FEEDING) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Препарати (бакова суміш)", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    if (selectedChemicals.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            selectedChemicals.forEach { chemical ->
                                InputChip(
                                    selected = true,
                                    onClick = { createLogViewModel.toggleChemicalSelection(chemical) },
                                    label = { Text(chemical.name) },
                                    trailingIcon = {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Видалити",
                                            modifier = Modifier.clickable { createLogViewModel.toggleChemicalSelection(chemical) }
                                        )
                                    }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    } else {
                        Text(
                            "Не обрано препаратів (запис без препаратів)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    ExposedDropdownMenuBox(
                        expanded = showChemicalDropdown,
                        onExpandedChange = { showChemicalDropdown = it }
                    ) {
                        OutlinedTextField(
                            value = if (selectedChemicals.isEmpty()) "Додати препарат..." else "${selectedChemicals.size} обрано. Додати ще...",
                            onValueChange = {},
                            readOnly = true,
                            placeholder = { Text("Виберіть препарат") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showChemicalDropdown) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = showChemicalDropdown,
                            onDismissRequest = { showChemicalDropdown = false }
                        ) {
                            chemicals.forEach { chemical ->
                                val isSelected = selectedChemicals.any { it.id == chemical.id }
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("${chemical.name} (${chemical.purpose})")
                                            if (isSelected) {
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = "Обрано",
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        createLogViewModel.toggleChemicalSelection(chemical)
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Photo
                Text("Фото", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(8.dp))

                val currentPhotoPath by createLogViewModel.photoPath.collectAsState()

                if (currentPhotoPath != null) {
                    val imagePath = PhotoPathResolver.toAsyncImageModel(context, currentPhotoPath)
                    var showLogImageViewer by remember { mutableStateOf(false) }
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                            Box(
                                modifier = Modifier.fillMaxSize().clickable { showLogImageViewer = true }
                            ) {
                                AsyncImage(
                                    model = imagePath,
                                    contentDescription = "Фото",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            IconButton(
                                onClick = { createLogViewModel.clearPhoto() },
                                modifier = Modifier.align(Alignment.TopEnd)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Видалити фото",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        if (showLogImageViewer) {
                            ImageViewerDialog(images = listOf(imagePath), onDismiss = { showLogImageViewer = false })
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { openCamera() },
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Камера")
                            }
                            OutlinedButton(
                                onClick = { imagePickerLauncher.launch("image/*") },
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) {
                                Icon(Icons.Default.Image, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Галерея")
                            }
                        }
                    }
                } else {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { openCamera() },
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Камера")
                        }
                        OutlinedButton(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Галерея")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Comment
                Text("Коментар", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Введіть коментар...") },
                    shape = RoundedCornerShape(8.dp),
                    minLines = 3
                )

                Spacer(modifier = Modifier.height(24.dp))

                // AI
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.SmartToy, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Запитати ШІ", style = MaterialTheme.typography.titleSmall)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://gemini.google.com"))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.OpenInNew, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Відкрити Gemini")
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Відповідь від ШІ:", style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = aiDiagnosis,
                            onValueChange = { aiDiagnosis = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Вставте відповідь від ШІ...") },
                            shape = RoundedCornerShape(8.dp),
                            minLines = 3
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Save
                Button(
                    onClick = { createLogViewModel.saveLog(comment, aiDiagnosis) },
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Зберегти запис")
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Скасувати запис?") },
            text = { Text("Незбережені дані будуть втрачені.") },
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
                    Text("Продовжити")
                }
            }
        )
    }
}
