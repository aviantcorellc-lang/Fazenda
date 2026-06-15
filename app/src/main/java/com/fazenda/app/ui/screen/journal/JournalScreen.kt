package com.fazenda.app.ui.screen.journal

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.fazenda.app.ui.component.MultiFloatingActionButton
import com.fazenda.app.ui.component.FabItem
import com.fazenda.app.data.entity.CategoryEntity
import com.fazenda.app.data.entity.LogActionTypes
import com.fazenda.app.data.entity.LogWithChemicals
import com.fazenda.app.data.entity.PlantEntity
import com.fazenda.app.data.entity.ZoneEntity
import com.fazenda.app.ui.component.ImageViewerDialog
import com.fazenda.app.ui.util.PhotoPathResolver
import com.fazenda.app.ui.viewmodel.JournalViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalScreen(
    onNavigateToCreateLog: () -> Unit = {},
    journalViewModel: JournalViewModel = viewModel()
) {
    val logsWithChemicals by journalViewModel.logsWithChemicals.collectAsState()
    val isLoading by journalViewModel.isLoading.collectAsState()
    val plantMap by journalViewModel.plants.collectAsState()
    val zoneMap by journalViewModel.zones.collectAsState()
    val categoryMap by journalViewModel.categories.collectAsState()

    var logToDelete by remember { mutableStateOf<LogWithChemicals?>(null) }

    // Діалог підтвердження видалення
    if (logToDelete != null) {
        AlertDialog(
            onDismissRequest = { logToDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Видалити запис?") },
            text = { Text("Запис буде видалено безповоротно.") },
            confirmButton = {
                Button(
                    onClick = {
                        logToDelete?.log?.let { journalViewModel.deleteLog(it) }
                        logToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Видалити") }
            },
            dismissButton = {
                TextButton(onClick = { logToDelete = null }) { Text("Скасувати") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Журнал") })
        },
        floatingActionButton = {
            val fabItems = listOf(
                FabItem(1, Icons.Default.EditNote, "Новий запис"),
                FabItem(2, Icons.Default.SmartToy, "Діагноз ШІ")
            )
            MultiFloatingActionButton(
                items = fabItems,
                mainIcon = Icons.Default.Add,
                onItemClick = { item ->
                    when (item.id) {
                        1 -> onNavigateToCreateLog()
                        2 -> onNavigateToCreateLog() // Обидва ведуть на сторінку логів
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            when {
                isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                logsWithChemicals.isEmpty() -> Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Журнал порожній", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Натисніть + щоб додати перший запис",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                else -> LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = logsWithChemicals,
                        key = { it.log.id }
                    ) { logWithChems ->
                        SwipeToDismissLogCard(
                            logWithChems = logWithChems,
                            plantName = logWithChems.log.plantId?.let { plantMap[it]?.name },
                            zoneName = logWithChems.log.zoneId?.let { zoneMap[it]?.name },
                            categoryName = logWithChems.log.categoryId?.let { categoryMap[it]?.name },
                            onDelete = { logToDelete = logWithChems }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDismissLogCard(
    logWithChems: LogWithChemicals,
    plantName: String?,
    zoneName: String?,
    categoryName: String?,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
            }
            false // Не приховуємо одразу — чекаємо підтвердження
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromEndToStart = true,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val color by animateColorAsState(
                when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
                label = "swipe_bg_color"
            )
            val scale by animateFloatAsState(
                if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) 1f else 0.75f,
                label = "swipe_icon_scale"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color, RoundedCornerShape(12.dp))
                    .padding(end = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Видалити",
                    modifier = Modifier.scale(scale),
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        },
        content = {
            LogCard(
                logWithChems = logWithChems,
                plantName = plantName,
                zoneName = zoneName,
                categoryName = categoryName,
                onDelete = onDelete
            )
        }
    )
}

@Composable
fun LogCard(
    logWithChems: LogWithChemicals,
    plantName: String?,
    zoneName: String?,
    categoryName: String?,
    onDelete: () -> Unit
) {
    val log = logWithChems.log
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("uk", "UA")) }
    val context = LocalContext.current
    var showImageViewer by remember { mutableStateOf(false) }
    val logPhotoModel = log.photoPath?.let { PhotoPathResolver.toAsyncImageModel(context, it) }
    var showMenu by remember { mutableStateOf(false) }

    if (showImageViewer) {
        ImageViewerDialog(images = listOf(logPhotoModel), onDismiss = { showImageViewer = false })
    }

    // Визначаємо цільовий об'єкт обробки
    val targetLabel = when {
        plantName != null -> plantName
        zoneName != null -> "Зона: $zoneName"
        categoryName != null -> "Категорія: $categoryName"
        else -> "Невідомий об'єкт"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Заголовок
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    getActionIcon(log.actionType),
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        targetLabel,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        dateFormat.format(Date(log.date)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Меню")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Видалити", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            },
                            onClick = {
                                showMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Тип дії
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.tertiaryContainer) {
                    Text(
                        log.actionType,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        fontSize = MaterialTheme.typography.labelSmall.fontSize,
                        fontWeight = FontWeight.W500
                    )
                }
                // Групова обробка (зона / категорія)
                if (log.zoneId != null || log.categoryId != null) {
                    val groupIcon = if (log.zoneId != null) "🗺" else "🌿"
                    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                        Text(
                            "$groupIcon Групова обробка",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontSize = MaterialTheme.typography.labelSmall.fontSize,
                            fontWeight = FontWeight.W500
                        )
                    }
                }
            }

            // Препарати
            if (logWithChems.chemicals.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Science,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = logWithChems.chemicals.joinToString(", ") { it.name },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Фото
            if (log.photoPath != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 150.dp)
                        .clickable { showImageViewer = true }
                ) {
                    AsyncImage(
                        model = logPhotoModel,
                        contentDescription = "Фото",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            // Коментар
            if (!log.comment.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text("Коментар:", style = MaterialTheme.typography.labelSmall)
                Spacer(modifier = Modifier.height(4.dp))
                Text(log.comment, style = MaterialTheme.typography.bodySmall)
            }

            // Діагноз ШІ
            if (!log.aiDiagnosis.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.SmartToy,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Діагноз ШІ",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(log.aiDiagnosis, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

fun getActionIcon(actionType: String): String {
    return when (actionType) {
        LogActionTypes.NOTE -> "📝"
        LogActionTypes.FEEDING -> "🌱"
        LogActionTypes.SPRAYING -> "💨"
        LogActionTypes.REPLACEMENT -> "🔄"
        LogActionTypes.PLANT_ADDED -> "➕"
        LogActionTypes.PLANT_DELETED -> "🗑"
        LogActionTypes.OTHER -> "📌"
        else -> "📄"
    }
}
