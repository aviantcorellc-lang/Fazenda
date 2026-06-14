package com.fazenda.app.ui.screen.catalog

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.fazenda.app.data.entity.LogEntity
import com.fazenda.app.data.entity.LogWithChemicals
import com.fazenda.app.ui.component.ImageViewerDialog
import com.fazenda.app.ui.util.PhotoPathResolver
import com.fazenda.app.ui.viewmodel.CategoryViewModel
import com.fazenda.app.ui.viewmodel.PlantDetailsViewModel
import com.fazenda.app.ui.viewmodel.PlantDetailsViewModelFactory
import com.fazenda.app.ui.viewmodel.ZoneViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PlantDetailsScreen(
    plantId: Long,
    onNavigateBack: () -> Unit,
    onEditClick: () -> Unit = {},
    plantDetailsViewModel: PlantDetailsViewModel = viewModel(
        factory = PlantDetailsViewModelFactory(plantId)
    ),
    zoneViewModel: ZoneViewModel = viewModel(),
    categoryViewModel: CategoryViewModel = viewModel()
) {
    val plant by plantDetailsViewModel.plant.collectAsState()
    val plantPhotos by plantDetailsViewModel.plantPhotos.collectAsState()
    val plantLogs by plantDetailsViewModel.plantLogs.collectAsState()
    val quarantineInfo by plantDetailsViewModel.quarantineInfo.collectAsState()
    val isLoading by plantDetailsViewModel.isLoading.collectAsState()
    val zones by zoneViewModel.zones.collectAsState()
    val categories by categoryViewModel.categories.collectAsState()
    val context = LocalContext.current
    var showImageViewer by remember { mutableStateOf(false) }
    var imageViewerIndex by remember { mutableStateOf(0) }
    var menuPhotoId by remember { mutableStateOf<Long?>(null) }

    val zoneMap = remember(zones) { zones.associateBy { it.id } }
    val categoryMap = remember(categories) { categories.associateBy { it.id } }

    val addPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { plantDetailsViewModel.addPlantPhoto(plantId, it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Деталі рослини") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FloatingActionButton(
                    onClick = { addPhotoLauncher.launch("image/*") },
                    containerColor = MaterialTheme.colorScheme.secondary
                ) {
                    Icon(Icons.Default.Image, contentDescription = "Додати фото")
                }
                FloatingActionButton(onClick = onEditClick) {
                    Icon(Icons.Default.Edit, contentDescription = "Редагувати")
                }
            }
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier.padding(paddingValues).fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (plant == null) {
            Box(
                modifier = Modifier.padding(paddingValues).fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Рослину не знайдено")
            }
        } else {
            val p = plant!!
            val primaryPhotoPath = p.photoPath ?: plantPhotos.firstOrNull()?.photoPath
            val primaryPhotoModel = PhotoPathResolver.toAsyncImageModel(context, primaryPhotoPath)
            val zoneName = p.zoneId?.let { zoneMap[it]?.name }
            val categoryName = p.categoryId?.let { categoryMap[it]?.name } ?: ""
            val galleryPhotos = when {
                p.photoPath != null -> plantPhotos.filterNot { it.photoPath == p.photoPath }
                plantPhotos.isNotEmpty() -> plantPhotos.drop(1)
                else -> emptyList()
            }

            val allPhotoModels = remember(primaryPhotoModel, galleryPhotos, context) {
                buildList {
                    if (primaryPhotoModel != null) add(primaryPhotoModel)
                    galleryPhotos.forEach { photo ->
                        PhotoPathResolver.toAsyncImageModel(context, photo.photoPath)?.let { add(it) }
                    }
                }
            }

            if (showImageViewer) {
                ImageViewerDialog(
                    images = allPhotoModels,
                    initialIndex = imageViewerIndex,
                    onDismiss = { showImageViewer = false }
                )
            }

            LazyColumn(
                modifier = Modifier.padding(paddingValues)
            ) {
                quarantineInfo?.let { info ->
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Увага! Карантин після обробки",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy", Locale("uk", "UA")) }
                                    Text(
                                        text = "Препарат: ${info.chemicalName}\nДіє до: ${dateFormat.format(Date(info.endDate))} (залишилось ${info.remainingDays} дн.)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(250.dp)
                            .clickable(enabled = primaryPhotoModel != null) { imageViewerIndex = 0; showImageViewer = true },
                        contentAlignment = Alignment.Center
                    ) {
                        if (primaryPhotoModel != null) {
                            AsyncImage(
                                model = primaryPhotoModel,
                                contentDescription = p.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Grass,
                                        contentDescription = null,
                                        modifier = Modifier.size(80.dp),
                                        tint = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }

                if (galleryPhotos.isNotEmpty()) {
                    item {
                        Text(
                            "Фото (${galleryPhotos.size})",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(galleryPhotos) { photo ->
                                Box(
                                    modifier = Modifier
                                        .width(150.dp)
                                        .height(150.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .combinedClickable(
                                            onClick = {
                                                val idx = galleryPhotos.indexOf(photo) + if (primaryPhotoModel != null) 1 else 0
                                                imageViewerIndex = idx
                                                showImageViewer = true
                                            },
                                            onLongClick = { menuPhotoId = photo.id }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val photoModel = PhotoPathResolver.toAsyncImageModel(context, photo.photoPath)
                                    if (photoModel != null) {
                                        AsyncImage(
                                            model = photoModel,
                                            contentDescription = "${p.name} photo",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Surface(
                                            modifier = Modifier.fillMaxSize(),
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.Default.Grass,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(48.dp),
                                                    tint = Color.Gray
                                                )
                                            }
                                        }
                                    }
                                    DropdownMenu(
                                        expanded = menuPhotoId == photo.id,
                                        onDismissRequest = { menuPhotoId = null }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Зробити головним") },
                                            onClick = {
                                                menuPhotoId = null
                                                plantDetailsViewModel.setPlantMainPhoto(photo)
                                            },
                                            leadingIcon = { Icon(Icons.Default.Star, contentDescription = null) }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Видалити") },
                                            onClick = {
                                                menuPhotoId = null
                                                plantDetailsViewModel.deletePlantPhoto(photo)
                                            },
                                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item {
                        Text(
                            "Довге натискання — меню дій з фото",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray)
                        )
                    }
                }

                item {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            p.name,
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                categoryName,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.W500
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        if (zoneName != null || p.row != null || p.position != null) {
                            Text("Розташування:", style = MaterialTheme.typography.labelMedium)
                            Spacer(modifier = Modifier.height(4.dp))
                            val locParts = mutableListOf<String>()
                            zoneName?.let { locParts.add(it) }
                            p.row?.let { locParts.add("Ряд ${it.toInt()}") }
                            p.position?.let { locParts.add("№${it.toInt()}") }
                            Text(
                                locParts.joinToString(" · "),
                                style = MaterialTheme.typography.bodyMedium
                            )

                            if (p.row != null || p.position != null) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    p.row?.let { row ->
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.secondaryContainer
                                        ) {
                                            Text(
                                                "Ряд: ${row.toInt()}",
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }
                                    }
                                    p.position?.let { pos ->
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.secondaryContainer
                                        ) {
                                            Text(
                                                "Номер: ${pos.toInt()}",
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (!p.comment.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Коментар:", style = MaterialTheme.typography.labelMedium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(p.comment, style = MaterialTheme.typography.bodyMedium)
                        }

                        if (p.latitude != null && p.longitude != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "${String.format(Locale.US, "%.6f", p.latitude)}, ${String.format(Locale.US, "%.6f", p.longitude)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Text(
                            "Історія дій (${plantLogs.size})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                if (plantLogs.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.padding(32.dp).fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Немає записів для цієї рослини",
                                style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray)
                            )
                        }
                    }
                } else {
                    items(plantLogs) { log ->
                        LogItem(logWithChems = log, zoneMap = zoneMap, categoryMap = categoryMap)
                    }
                }
            }
        }
    }
}

@Composable
fun LogItem(
    logWithChems: LogWithChemicals,
    zoneMap: Map<Long, com.fazenda.app.data.entity.ZoneEntity>,
    categoryMap: Map<Long, com.fazenda.app.data.entity.CategoryEntity>
) {
    val log = logWithChems.log
    val dateFormat = androidx.compose.runtime.remember {
        SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("uk", "UA"))
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(8.dp),
        border = ButtonDefaults.outlinedButtonBorder
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Text(
                            log.actionType,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            fontSize = MaterialTheme.typography.labelSmall.fontSize
                        )
                    }
                    
                    // Group treatment indicators
                    val groupText = when {
                        log.zoneId != null -> " (Вся зона: ${zoneMap[log.zoneId]?.name ?: "?"})"
                        log.categoryId != null -> " (Категорія: ${categoryMap[log.categoryId]?.name ?: "?"})"
                        else -> null
                    }
                    if (groupText != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = groupText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(dateFormat.format(Date(log.date)), style = MaterialTheme.typography.labelSmall)
            }
            
            if (logWithChems.chemicals.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                val chemsText = logWithChems.chemicals.joinToString(", ") { it.name }
                Text(
                    text = "Препарати: $chemsText",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!log.comment.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(log.comment, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
