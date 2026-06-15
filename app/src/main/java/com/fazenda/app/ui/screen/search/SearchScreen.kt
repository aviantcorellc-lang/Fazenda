package com.fazenda.app.ui.screen.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fazenda.app.data.entity.ChemicalEntity
import com.fazenda.app.data.entity.LogWithChemicals
import com.fazenda.app.data.entity.PlantWithPhotos
import com.fazenda.app.ui.screen.catalog.PlantCard
import com.fazenda.app.ui.screen.journal.LogCard
import com.fazenda.app.ui.viewmodel.SearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onNavigateBack: () -> Unit,
    onPlantClick: (Long) -> Unit,
    onPlantEdit: (Long) -> Unit,
    searchViewModel: SearchViewModel = viewModel()
) {
    val searchQuery by searchViewModel.searchQuery.collectAsState()
    val searchResults by searchViewModel.searchResults.collectAsState()
    val categories by searchViewModel.categories.collectAsState()
    val zones by searchViewModel.zones.collectAsState()

    val zoneMap = remember(zones) { zones.associateBy { it.id } }
    val categoryMap = remember(categories) { categories.associateBy { it.id } }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Рослини", "Журнал", "Препарати")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchViewModel.searchQuery.value = it },
                        placeholder = { Text("Пошук рослин, логів, препаратів...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchViewModel.searchQuery.value = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Очистити")
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 8.dp),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            if (searchQuery.isNotBlank()) {
                TabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { index, title ->
                        val count = when (index) {
                            0 -> searchResults.plants.size
                            1 -> searchResults.logs.size
                            2 -> searchResults.chemicals.size
                            else -> 0
                        }
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text("$title ($count)") }
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (selectedTab) {
                        0 -> PlantsResultList(
                            plants = searchResults.plants,
                            categoryMap = categoryMap.mapValues { it.value.name },
                            zoneMap = zoneMap.mapValues { it.value.name },
                            onPlantClick = onPlantClick,
                            onPlantEdit = onPlantEdit
                        )
                        1 -> LogsResultList(
                            logs = searchResults.logs,
                            plantMap = searchResults.plants.associate { it.plant.id to it.plant },
                            categoryMap = categoryMap,
                            zoneMap = zoneMap
                        )
                        2 -> ChemicalsResultList(
                            chemicals = searchResults.chemicals
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Введіть запит для пошуку",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Шукайте за назвою рослин, зоною, типом роботи, коментарями або назвою хімікатів",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PlantsResultList(
    plants: List<PlantWithPhotos>,
    categoryMap: Map<Long, String>,
    zoneMap: Map<Long, String>,
    onPlantClick: (Long) -> Unit,
    onPlantEdit: (Long) -> Unit
) {
    if (plants.isEmpty()) {
        EmptyResultsView()
    } else {
        LazyColumn(
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(plants, key = { it.plant.id }) { pwp ->
                PlantCard(
                    plantWithPhotos = pwp,
                    categoryName = pwp.plant.categoryId?.let { categoryMap[it] },
                    zoneName = pwp.plant.zoneId?.let { zoneMap[it] },
                    onClick = { onPlantClick(pwp.plant.id) },
                    onEdit = { onPlantEdit(pwp.plant.id) }
                )
            }
        }
    }
}

@Composable
fun LogsResultList(
    logs: List<LogWithChemicals>,
    plantMap: Map<Long, com.fazenda.app.data.entity.PlantEntity>,
    categoryMap: Map<Long, com.fazenda.app.data.entity.CategoryEntity>,
    zoneMap: Map<Long, com.fazenda.app.data.entity.ZoneEntity>
) {
    if (logs.isEmpty()) {
        EmptyResultsView()
    } else {
        LazyColumn(
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(logs, key = { it.log.id }) { logWithChems ->
                LogCard(
                    logWithChems = logWithChems,
                    plantName = logWithChems.log.plantId?.let { plantMap[it]?.name },
                    zoneName = logWithChems.log.zoneId?.let { zoneMap[it]?.name },
                    categoryName = logWithChems.log.categoryId?.let { categoryMap[it]?.name },
                    onDelete = {} // Не дозволяємо видалення з результатів пошуку задля безпеки
                )
            }
        }
    }
}

@Composable
fun ChemicalsResultList(
    chemicals: List<ChemicalEntity>
) {
    if (chemicals.isEmpty()) {
        EmptyResultsView()
    } else {
        LazyColumn(
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(chemicals, key = { it.id }) { chem ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Science,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = chem.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Група: ${chem.chemicalGroup} · Період очікування: ${chem.waitingPeriodDays} дн.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = chem.purpose,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyResultsView() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Нічого не знайдено",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}
