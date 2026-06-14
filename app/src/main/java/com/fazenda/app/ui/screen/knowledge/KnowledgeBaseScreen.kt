package com.fazenda.app.ui.screen.knowledge

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fazenda.app.data.entity.ChemicalEntity
import com.fazenda.app.ui.viewmodel.KnowledgeBaseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnowledgeBaseScreen(
    onNavigateBack: () -> Unit,
    viewModel: KnowledgeBaseViewModel = viewModel()
) {
    val chemicals by viewModel.chemicals.collectAsState()
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Довідник", "Сумісність сумішей")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("База знань препаратів") },
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
            TabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) }
                    )
                }
            }

            when (selectedTabIndex) {
                0 -> DirectoryTab(chemicals = chemicals, onAddChemical = { name, purpose, waitDays, group ->
                    viewModel.addChemical(name, purpose, waitDays, group)
                })
                1 -> CompatibilityTab(chemicals = chemicals)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectoryTab(
    chemicals: List<ChemicalEntity>,
    onAddChemical: (String, String, Int, String) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var purpose by remember { mutableStateOf("") }
    var waitingPeriod by remember { mutableStateOf("") }
    var selectedGroup by remember { mutableStateOf("Інше") }
    var showGroupDropdown by remember { mutableStateOf(false) }

    val groups = listOf(
        "Мідьвмісні", "Сірковмісні", "Фосфорорганічні", 
        "Біопрепарати", "Залізовмісні", "Олійні", 
        "Лужні", "Піретроїди", "Інше"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        if (chemicals.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Список препаратів порожній", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(chemicals) { chemical ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = chemical.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = chemical.chemicalGroup,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Призначення: ${chemical.purpose}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.HourglassEmpty,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Термін очікування: ${chemical.waitingPeriodDays} днів",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Default.Add, contentDescription = "Додати препарат")
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Новий препарат") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Назва препарату") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = purpose,
                        onValueChange = { purpose = it },
                        label = { Text("Призначення (напр. Фунгіцид)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = waitingPeriod,
                        onValueChange = { waitingPeriod = it },
                        label = { Text("Термін очікування (днів)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    ExposedDropdownMenuBox(
                        expanded = showGroupDropdown,
                        onExpandedChange = { showGroupDropdown = it }
                    ) {
                        OutlinedTextField(
                            value = selectedGroup,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Хімічна група") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showGroupDropdown) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = showGroupDropdown,
                            onDismissRequest = { showGroupDropdown = false }
                        ) {
                            groups.forEach { group ->
                                DropdownMenuItem(
                                    text = { Text(group) },
                                    onClick = {
                                        selectedGroup = group
                                        showGroupDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val waitDays = waitingPeriod.toIntOrNull() ?: 0
                        if (name.isNotBlank()) {
                            onAddChemical(name, purpose, waitDays, selectedGroup)
                            name = ""
                            purpose = ""
                            waitingPeriod = ""
                            selectedGroup = "Інше"
                            showAddDialog = false
                        }
                    }
                ) {
                    Text("Додати")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Скасувати")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompatibilityTab(chemicals: List<ChemicalEntity>) {
    var selectedChemicals by remember { mutableStateOf(setOf<ChemicalEntity>()) }
    
    val compatibilityResult = remember(selectedChemicals) {
        checkCompatibility(selectedChemicals.toList())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Compatibility banner
        Card(
            colors = CardDefaults.cardColors(
                containerColor = compatibilityResult.second.copy(alpha = 0.15f)
            ),
            modifier = Modifier.fillMaxWidth(),
            border = androidx.compose.foundation.BorderStroke(1.dp, compatibilityResult.second)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (compatibilityResult.second == Color.Red) Icons.Default.Cancel else Icons.Default.Info,
                    contentDescription = null,
                    tint = compatibilityResult.second,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = if (compatibilityResult.second == Color.Red) "Несумісна суміш!" else "Сумісність суміші",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = compatibilityResult.second
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = compatibilityResult.first,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Selected chemicals quick list
        if (selectedChemicals.isNotEmpty()) {
            Text("Обрані препарати для суміші:", style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                selectedChemicals.forEach { chemical ->
                    InputChip(
                        selected = true,
                        onClick = { selectedChemicals = selectedChemicals - chemical },
                        label = { Text(chemical.name) },
                        trailingIcon = {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Видалити",
                                modifier = Modifier.clickable { selectedChemicals = selectedChemicals - chemical }
                            )
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Text("Виберіть препарати зі списку:", style = MaterialTheme.typography.labelMedium)
        Spacer(modifier = Modifier.height(8.dp))

        if (chemicals.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Немає доступних препаратів", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) {
                items(chemicals) { chemical ->
                    val isSelected = selectedChemicals.any { it.id == chemical.id }
                    Card(
                        onClick = {
                            selectedChemicals = if (isSelected) {
                                selectedChemicals.filterNot { it.id == chemical.id }.toSet()
                            } else {
                                selectedChemicals + chemical
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = chemical.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Група: ${chemical.chemicalGroup} · ${chemical.purpose}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = {
                                    selectedChemicals = if (isSelected) {
                                        selectedChemicals.filterNot { it.id == chemical.id }.toSet()
                                    } else {
                                        selectedChemicals + chemical
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

fun checkCompatibility(selected: List<ChemicalEntity>): Pair<String, Color> {
    if (selected.size <= 1) {
        return Pair("Оберіть 2 або більше препаратів зі списку нижче, щоб автоматично перевірити сумісність у баковій суміші.", Color(0xFF673AB7)) // Purple
    }
    
    val groups = selected.map { it.chemicalGroup }.toSet()
    
    // Rule 1: Copper + Sulfur
    if (groups.contains("Мідьвмісні") && groups.contains("Сірковмісні")) {
        return Pair("Несумісні! Суміш міді та сірки утворює токсичний сульфід міді, що викликає сильні опіки рослин та листя.", Color.Red)
    }
    
    // Rule 2: Copper + Organophosphates
    if (groups.contains("Мідьвмісні") && groups.contains("Фосфорорганічні")) {
        return Pair("Несумісні! Мідні препарати мають лужну реакцію та руйнують фосфорорганічні сполуки, зводячи їх ефективність нанівець.", Color.Red)
    }

    // Rule 3: Biopreparations + Copper
    if (groups.contains("Біопрепарати") && groups.contains("Мідьвмісні")) {
        return Pair("Несумісні! Препарати на основі міді є сильними фунгіцидами та повністю знищують корисні мікроорганізми у біопрепаратах.", Color.Red)
    }

    // Rule 4: Iron + Copper
    if (groups.contains("Залізовмісні") && groups.contains("Мідьвмісні")) {
        return Pair("Несумісні! Не можна змішувати залізний купорос та препарати міді. Вони взаємодіють і блокують дію один одного.", Color.Red)
    }

    // Rule 5: Sulfur + Oils
    if (groups.contains("Сірковмісні") && groups.contains("Олійні")) {
        return Pair("Несумісні! Обробка сіркою разом або невдовзі після олійних емульсій викликає сильні опіки листя рослин.", Color.Red)
    }

    // Rule 6: Alkaline + Organophosphates/Pyrethroids
    if (groups.contains("Лужні") && (groups.contains("Фосфорорганічні") || groups.contains("Піретроїди"))) {
        return Pair("Несумісні! Лужне середовище викликає миттєвий гідроліз пестицидів та повну втрату їх інсектицидної сили.", Color.Red)
    }
    
    // Rule 7: Alkaline + Biopreparations
    if (groups.contains("Лужні") && groups.contains("Біопрепарати")) {
        return Pair("Несумісні! Сильна лужна реакція (наприклад, Бордоської рідини чи вапна) знищує живі культури бактерій.", Color.Red)
    }

    return Pair("Сумісні! Цю бакову суміш можна застосовувати. УВАГА: завжди спочатку проводьте тест на випадіння осаду у невеликій тарі (повинно бути без пластівців, осаду та розшарування)!", Color(0xFF2E7D32)) // Green
}
