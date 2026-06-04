package com.fazenda.app.ui.screen.catalog

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fazenda.app.data.entity.ZoneEntity
import com.fazenda.app.ui.viewmodel.ZoneViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZonesScreen(
    onNavigateBack: () -> Unit,
    zoneViewModel: ZoneViewModel = viewModel()
) {
    val zones by zoneViewModel.zones.collectAsState()
    val isLoading by zoneViewModel.isLoading.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingZone by remember { mutableStateOf<ZoneEntity?>(null) }
    var deleteZone by remember { mutableStateOf<ZoneEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Зони") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Додати зону")
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
        } else if (zones.isEmpty()) {
            Box(
                modifier = Modifier.padding(paddingValues).fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Немає зон", style = MaterialTheme.typography.titleMedium)
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(paddingValues),
                contentPadding = PaddingValues(12.dp)
            ) {
                items(zones) { zone ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                zone.name,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { editingZone = zone }) {
                                Icon(Icons.Default.Edit, contentDescription = "Редагувати")
                            }
                            IconButton(onClick = { deleteZone = zone }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Видалити",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        ZoneEditDialog(
            title = "Додати зону",
            initialName = "",
            onDismiss = { showAddDialog = false },
            onSave = { name ->
                zoneViewModel.addZone(name)
                showAddDialog = false
            }
        )
    }

    editingZone?.let { zone ->
        ZoneEditDialog(
            title = "Редагувати зону",
            initialName = zone.name,
            onDismiss = { editingZone = null },
            onSave = { name ->
                zoneViewModel.updateZone(zone.copy(name = name))
                editingZone = null
            }
        )
    }

    deleteZone?.let { zone ->
        AlertDialog(
            onDismissRequest = { deleteZone = null },
            title = { Text("Видалити зону?") },
            text = { Text("Видалити зону \"${zone.name}\"? Рослини в цій зоні залишаться без зони.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        zoneViewModel.deleteZone(zone)
                        deleteZone = null
                    }
                ) {
                    Text("Видалити", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteZone = null }) {
                    Text("Скасувати")
                }
            }
        )
    }
}

@Composable
fun ZoneEditDialog(
    title: String,
    initialName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Назва зони") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onSave(name) },
                enabled = name.isNotBlank()
            ) {
                Text("Зберегти")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Скасувати")
            }
        }
    )
}
