package com.fazenda.app.ui.screen.catalog

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.fazenda.app.ui.util.PhotoPathResolver
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
    zoneViewModel: ZoneViewModel = viewModel()
) {
    val plant by plantDetailsViewModel.plant.collectAsState()
    val plantPhotos by plantDetailsViewModel.plantPhotos.collectAsState()
    val plantLogs by plantDetailsViewModel.plantLogs.collectAsState()
    val isLoading by plantDetailsViewModel.isLoading.collectAsState()
    val zones by zoneViewModel.zones.collectAsState()
    val context = LocalContext.current

    val zoneMap = remember(zones) { zones.associateBy { it.id } }

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
                    Icon(Icons.Default.PhotoCamera, contentDescription = "Додати фото")
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
            val galleryPhotos = when {
                p.photoPath != null -> plantPhotos.filterNot { it.photoPath == p.photoPath }
                plantPhotos.isNotEmpty() -> plantPhotos.drop(1)
                else -> emptyList()
            }

            LazyColumn(
                modifier = Modifier.padding(paddingValues)
            ) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(250.dp),
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
                                val photoModel = PhotoPathResolver.toAsyncImageModel(context, photo.photoPath)
                                Box(
                                    modifier = Modifier
                                        .width(150.dp)
                                        .height(150.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .combinedClickable(
                                            onClick = {},
                                            onLongClick = { plantDetailsViewModel.deletePlantPhoto(photo) }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
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
                                }
                            }
                        }
                    }
                    item {
                        Text(
                            "Довге натискання для видалення фото",
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
                                p.category,
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
                        LogItem(log = log)
                    }
                }
            }
        }
    }
}

@Composable
fun LogItem(log: LogEntity) {
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
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
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
                Text(dateFormat.format(Date(log.date)), style = MaterialTheme.typography.labelSmall)
            }
            if (!log.comment.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(log.comment, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
