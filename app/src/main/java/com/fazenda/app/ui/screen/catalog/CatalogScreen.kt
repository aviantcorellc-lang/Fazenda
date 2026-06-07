package com.fazenda.app.ui.screen.catalog

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Category
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.fazenda.app.data.entity.CategoryEntity
import com.fazenda.app.data.entity.PlantEntity
import com.fazenda.app.ui.component.ImageViewerDialog
import com.fazenda.app.ui.util.PhotoPathResolver
import com.fazenda.app.ui.viewmodel.CatalogViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(
    onPlantClick: (Long) -> Unit,
    onPlantEdit: (Long) -> Unit = {},
    onAddPlant: () -> Unit = {},
    onZonesClick: () -> Unit = {},
    onCategoriesClick: () -> Unit = {},
    catalogViewModel: CatalogViewModel = viewModel()
) {
    val categories by catalogViewModel.categories.collectAsState()
    val allPlants by catalogViewModel.allPlants.collectAsState()
    val zones by catalogViewModel.zones.collectAsState()
    val isLoading by catalogViewModel.isLoading.collectAsState()

    val zoneMap = remember(zones) { zones.associateBy { it.id } }
    val categoryMap = remember(categories) { categories.associateBy { it.id } }

    var expanded by remember { mutableStateOf(false) }
    val selectedCategoryId by catalogViewModel.selectedCategoryId.collectAsState()
    val searchQuery by catalogViewModel.searchQuery.collectAsState()
    val showSearch by catalogViewModel.showSearch.collectAsState()

    val filteredPlants = if (selectedCategoryId == null) {
        allPlants
    } else {
        allPlants.filter { it.categoryId == selectedCategoryId }
    }

    val searchFilteredPlants = if (searchQuery.isBlank()) {
        filteredPlants
    } else {
        filteredPlants.filter { plant ->
            val zoneName = plant.zoneId?.let { zoneMap[it]?.name } ?: ""
            val catName = plant.categoryId?.let { categoryMap[it]?.name } ?: ""
            plant.name.contains(searchQuery, ignoreCase = true) ||
            catName.contains(searchQuery, ignoreCase = true) ||
            zoneName.contains(searchQuery, ignoreCase = true) ||
            plant.row?.toString()?.contains(searchQuery) == true ||
            plant.position?.toString()?.contains(searchQuery) == true
        }
    }

    Scaffold(
        topBar = {
            if (showSearch) {
                TopAppBar(
                    title = {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { catalogViewModel.searchQuery.value = it },
                            placeholder = { Text("Назва, категорія, ряд, номер...") },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { catalogViewModel.searchQuery.value = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Очистити")
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            catalogViewModel.showSearch.value = false
                            catalogViewModel.searchQuery.value = ""
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = { Text("Каталог рослин") },
                    actions = {
                        IconButton(onClick = onCategoriesClick) {
                            Icon(Icons.Default.Category, contentDescription = "Категорії")
                        }
                        IconButton(onClick = onZonesClick) {
                            Icon(Icons.Default.Hub, contentDescription = "Зони")
                        }
                        IconButton(onClick = { catalogViewModel.showSearch.value = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Пошук")
                        }
                        Box {
                            IconButton(onClick = { expanded = true }) {
                                Icon(Icons.Default.FilterList, contentDescription = "Фільтр")
                            }
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Всі рослини (А-Я)") },
                                    onClick = {
                                        catalogViewModel.selectedCategoryId.value = null
                                        expanded = false
                                    }
                                )
                                HorizontalDivider()
                                categories.forEach { category ->
                                    DropdownMenuItem(
                                        text = { Text(category.name) },
                                        onClick = {
                                            catalogViewModel.selectedCategoryId.value = category.id
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddPlant) {
                Icon(Icons.Default.Add, contentDescription = "Додати рослину")
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (searchFilteredPlants.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Grass,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.Gray.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Немає рослин у каталозі", style = MaterialTheme.typography.titleMedium)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(12.dp)
                ) {
                    if (selectedCategoryId == null) {
                        items(searchFilteredPlants) { plant ->
                            val catName = plant.categoryId?.let { categoryMap[it]?.name }
                            PlantCard(
                                plant = plant,
                                categoryName = catName,
                                zoneName = plant.zoneId?.let { zoneMap[it]?.name },
                                onClick = { onPlantClick(plant.id) },
                                onEdit = { onPlantEdit(plant.id) }
                            )
                        }
                    } else {
                        item {
                            val catName = selectedCategoryId?.let { categoryMap[it]?.name } ?: ""
                            Text(
                                catName,
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                        items(searchFilteredPlants) { plant ->
                            val catName = plant.categoryId?.let { categoryMap[it]?.name }
                            PlantCard(
                                plant = plant,
                                categoryName = catName,
                                zoneName = plant.zoneId?.let { zoneMap[it]?.name },
                                onClick = { onPlantClick(plant.id) },
                                onEdit = { onPlantEdit(plant.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlantCard(plant: PlantEntity, categoryName: String?, zoneName: String?, onClick: () -> Unit, onEdit: () -> Unit = {}) {
    val context = LocalContext.current
    val photoModel = PhotoPathResolver.toAsyncImageModel(context, plant.photoPath)
    var showImageViewer by remember { mutableStateOf(false) }

    if (showImageViewer) {
        ImageViewerDialog(imageModel = photoModel, onDismiss = { showImageViewer = false })
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        onClick = onClick
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                if (photoModel != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { showImageViewer = true },
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = photoModel,
                            contentDescription = plant.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                } else {
                    Icon(Icons.Default.Grass, contentDescription = null, tint = Color.Gray)
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            plant.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        if (categoryName != null) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    categoryName,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Редагувати", modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                if (zoneName != null || plant.row != null || plant.position != null) {
                    val parts = mutableListOf<String>()
                    zoneName?.let { parts.add(it) }
                    plant.row?.let { parts.add("Ряд ${it.toInt()}") }
                    plant.position?.let { parts.add("№${it.toInt()}") }
                    Text(
                        parts.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2
                    )
                }
            }
        }
    }
}
