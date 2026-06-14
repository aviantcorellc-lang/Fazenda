package com.fazenda.app.ui.screen.journal

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.fazenda.app.data.entity.LogActionTypes
import com.fazenda.app.data.entity.LogEntity
import com.fazenda.app.data.entity.PlantEntity
import com.fazenda.app.ui.component.ImageViewerDialog
import com.fazenda.app.ui.util.PhotoPathResolver
import com.fazenda.app.ui.viewmodel.JournalViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalScreen(
    onNavigateToCreateLog: () -> Unit = {},
    journalViewModel: JournalViewModel = viewModel()
) {
    val logs by journalViewModel.logs.collectAsState()
    val isLoading by journalViewModel.isLoading.collectAsState()
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Журнал") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToCreateLog) {
                Icon(Icons.Default.Add, contentDescription = "Додати запис")
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (logs.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.Gray.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Журнал порожній", style = MaterialTheme.typography.titleMedium)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(logs) { log ->
                        var plantName by remember { mutableStateOf<String?>(null) }
                        LaunchedEffect(log.plantId) {
                            plantName = journalViewModel.getPlantById(log.plantId)?.name
                        }
                        LogCard(
                            log = log,
                            plantName = plantName,
                            onDelete = {
                                scope.launch { journalViewModel.deleteLog(log) }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LogCard(
    log: LogEntity,
    plantName: String?,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("uk", "UA")) }
    val context = LocalContext.current
    var showImageViewer by remember { mutableStateOf(false) }
    val logPhotoModel = log.photoPath?.let { PhotoPathResolver.toAsyncImageModel(context, it) }

    if (showImageViewer) {
        ImageViewerDialog(images = listOf(logPhotoModel), onDismiss = { showImageViewer = false })
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    getActionIcon(log.actionType),
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        plantName ?: "Невідома рослина",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        dateFormat.format(Date(log.date)),
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                    )
                }
                var showMenu by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Меню")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Видалити") },
                            onClick = {
                                showMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.tertiaryContainer) {
                Text(
                    log.actionType,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    fontSize = MaterialTheme.typography.labelSmall.fontSize,
                    fontWeight = FontWeight.W500
                )
            }

            if (log.photoPath != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 150.dp)
                        .clickable { showImageViewer = true }
                ) {
                    AsyncImage(
                        model = logPhotoModel,
                        contentDescription = "Фото",
                        modifier = Modifier
                            .fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            if (!log.comment.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text("Коментар:", style = MaterialTheme.typography.labelSmall)
                Spacer(modifier = Modifier.height(4.dp))
                Text(log.comment, style = MaterialTheme.typography.bodySmall)
            }

            if (!log.aiDiagnosis.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFFF8E1),
                    border = ButtonDefaults.outlinedButtonBorder
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.SmartToy, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFFFA000))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Діагноз ШІ", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFFFFA000)))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
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
