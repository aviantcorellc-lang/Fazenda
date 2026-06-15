package com.fazenda.app.ui.screen.dashboard

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.nativeCanvas
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fazenda.app.data.entity.ScheduleEntity
import com.fazenda.app.service.UpdateInfo
import com.fazenda.app.service.UpdateService
import com.fazenda.app.ui.viewmodel.DashboardViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import com.fazenda.app.data.entity.CategoryEntity
import com.fazenda.app.ui.viewmodel.CategoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToCreateLog: () -> Unit,
    onNavigateToMap: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToSchedules: () -> Unit = {},
    onNavigateToKnowledgeBase: () -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    dashboardViewModel: DashboardViewModel = viewModel(),
    categoryViewModel: CategoryViewModel = viewModel()
) {
    val schedules by dashboardViewModel.schedules.collectAsState()
    val isLoading by dashboardViewModel.isLoading.collectAsState()
    val categories by categoryViewModel.categories.collectAsState()
    val categoryMap = remember(categories) { categories.associateBy { it.id } }
    val weatherAdvice by dashboardViewModel.weatherAdvice.collectAsState()
    val weatherCode by dashboardViewModel.currentWeatherCode.collectAsState()
    val weatherTemp by dashboardViewModel.currentWeatherTemp.collectAsState()
    val connectivityStatus by dashboardViewModel.connectivityStatus.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showUpdateDialog by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var isDownloading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val service = UpdateService(context)
        val info = withContext(Dispatchers.IO) { service.checkForUpdate() }
        if (info != null) {
            updateInfo = info
            showUpdateDialog = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("План дій") },
                actions = {
                    IconButton(onClick = onNavigateToSearch) {
                        Icon(Icons.Default.Search, contentDescription = "Пошук")
                    }
                    IconButton(onClick = onNavigateToMap) {
                        Icon(Icons.Default.Map, contentDescription = "Мапа")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Налаштування")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreateLog,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Новий запис")
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (connectivityStatus != com.fazenda.app.service.ConnectivityObserver.Status.Available) {
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudOff,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Text(
                                        text = "Офлайн-режим. Прогноз погоди та Вікіпедія недоступні.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }

                    item {
                        WeatherCard(
                            weatherAdvice = weatherAdvice,
                            weatherCode = weatherCode,
                            temp = weatherTemp,
                            onRefresh = { dashboardViewModel.loadWeather() }
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = onNavigateToSchedules,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            ) {
                                Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Плани", fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = onNavigateToKnowledgeBase,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            ) {
                                Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("База знань", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (schedules.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Немає запланованих обробок", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    } else {
                        items(schedules) { schedule ->
                            ScheduleCard(schedule, categoryMap)
                        }
                    }
                }
            }
        }
    }

    if (showUpdateDialog && updateInfo != null) {
        AlertDialog(
            onDismissRequest = {
                if (!isDownloading) showUpdateDialog = false
            },
            title = { Text("Доступне оновлення v${updateInfo!!.latestVersion}") },
            text = {
                Column {
                    Text("Нова версія застосунку доступна для завантаження.")
                    if (updateInfo!!.releaseNotes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(updateInfo!!.releaseNotes, style = MaterialTheme.typography.bodySmall)
                    }
                    if (isDownloading) {
                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (!isDownloading) {
                            isDownloading = true
                            scope.launch {
                                val service = UpdateService(context)
                                val success = withContext(Dispatchers.IO) {
                                    service.downloadAndInstall(updateInfo!!)
                                }
                                if (!success) {
                                    isDownloading = false
                                    Toast.makeText(context, "Помилка завантаження", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    enabled = !isDownloading
                ) {
                    Text(if (isDownloading) "Завантаження..." else "Оновити")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showUpdateDialog = false },
                    enabled = !isDownloading
                ) {
                    Text("Пізніше")
                }
            }
        )
    }
}

@Composable
fun ScheduleCard(schedule: ScheduleEntity, categoryMap: Map<Long, CategoryEntity>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        androidx.compose.foundation.layout.Column(modifier = Modifier.padding(16.dp)) {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        schedule.phaseTime,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    val categoryName = schedule.categoryId?.let { categoryMap[it]?.name } ?: "Всі"
                    Text(
                        categoryName,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        fontSize = 12.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text("Рецепт обробки:", style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(schedule.recipe, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun WeatherCard(
    weatherAdvice: String,
    weatherCode: Int?,
    temp: Double?,
    onRefresh: () -> Unit
) {
    val context = LocalContext.current
    val weatherDesc = remember(weatherCode) {
        if (weatherCode != null) {
            val service = com.fazenda.app.service.WeatherService(context)
            service.getWeatherDescription(weatherCode)
        } else {
            "Невідома погода"
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (temp != null) "${temp.toInt()}°C" else "--°C",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = weatherDesc,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
                IconButton(onClick = onRefresh) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Оновити погоду",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f))
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Default.WbSunny,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = weatherAdvice,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
fun BarChart(
    data: Map<String, Int>,
    modifier: Modifier = Modifier
) {
    val maxVal = remember(data) { (data.values.maxOrNull() ?: 0).coerceAtLeast(1) }
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Використання препаратів (Топ-5)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Canvas(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                val width = size.width
                val height = size.height
                
                val paddingLeft = 30f
                val paddingBottom = 40f
                val chartWidth = width - paddingLeft
                val chartHeight = height - paddingBottom
                
                val itemsCount = data.size
                val barWidth = (chartWidth / itemsCount) * 0.6f
                val spaceWidth = (chartWidth / itemsCount) * 0.4f
                
                data.entries.forEachIndexed { index, entry ->
                    val barHeight = (entry.value.toFloat() / maxVal) * chartHeight
                    val left = paddingLeft + (index * (barWidth + spaceWidth)) + spaceWidth / 2
                    val top = chartHeight - barHeight
                    
                    drawRoundRect(
                        color = primaryColor,
                        topLeft = androidx.compose.ui.geometry.Offset(left, top),
                        size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
                    )
                    
                    drawContext.canvas.nativeCanvas.drawText(
                        entry.value.toString(),
                        left + barWidth / 2,
                        top - 10f,
                        android.graphics.Paint().apply {
                            color = onSurfaceColor.toArgb()
                            textSize = 24f
                            textAlign = android.graphics.Paint.Align.CENTER
                            isFakeBoldText = true
                        }
                    )
                    
                    val truncatedLabel = if (entry.key.length > 8) entry.key.take(7) + ".." else entry.key
                    drawContext.canvas.nativeCanvas.drawText(
                        truncatedLabel,
                        left + barWidth / 2,
                        height - 10f,
                        android.graphics.Paint().apply {
                            color = onSurfaceColor.copy(alpha = 0.7f).toArgb()
                            textSize = 20f
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun LineChart(
    data: Map<String, Int>,
    modifier: Modifier = Modifier
) {
    val maxVal = remember(data) { (data.values.maxOrNull() ?: 0).coerceAtLeast(1) }
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Активність за останні 7 днів",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Canvas(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                val width = size.width
                val height = size.height
                
                val paddingLeft = 30f
                val paddingBottom = 40f
                val chartWidth = width - paddingLeft
                val chartHeight = height - paddingBottom
                
                val points = data.entries.toList()
                val stepX = chartWidth / (points.size - 1).coerceAtLeast(1)
                
                val path = Path()
                val fillPath = Path()
                
                points.forEachIndexed { index, entry ->
                    val x = paddingLeft + index * stepX
                    val y = chartHeight - (entry.value.toFloat() / maxVal) * chartHeight
                    
                    if (index == 0) {
                        path.moveTo(x, y)
                        fillPath.moveTo(x, chartHeight)
                        fillPath.lineTo(x, y)
                    } else {
                        path.lineTo(x, y)
                        fillPath.lineTo(x, y)
                    }
                    
                    if (index == points.size - 1) {
                        fillPath.lineTo(x, chartHeight)
                        fillPath.close()
                    }
                    
                    drawCircle(
                        color = primaryColor,
                        radius = 6f,
                        center = androidx.compose.ui.geometry.Offset(x, y)
                    )
                    
                    drawContext.canvas.nativeCanvas.drawText(
                        entry.value.toString(),
                        x,
                        y - 12f,
                        android.graphics.Paint().apply {
                            color = onSurfaceColor.toArgb()
                            textSize = 22f
                            textAlign = android.graphics.Paint.Align.CENTER
                            isFakeBoldText = true
                        }
                    )
                    
                    drawContext.canvas.nativeCanvas.drawText(
                        entry.key,
                        x,
                        height - 10f,
                        android.graphics.Paint().apply {
                            color = onSurfaceColor.copy(alpha = 0.7f).toArgb()
                            textSize = 18f
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                    )
                }
                
                drawPath(
                    path = path,
                    color = primaryColor,
                    style = Stroke(width = 4f)
                )
                
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(primaryColor.copy(alpha = 0.3f), Color.Transparent),
                        startY = 0f,
                        endY = chartHeight
                    )
                )
            }
        }
    }
}


