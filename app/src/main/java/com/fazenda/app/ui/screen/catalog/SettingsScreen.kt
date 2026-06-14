package com.fazenda.app.ui.screen.catalog

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fazenda.app.service.BackupService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onEditCategories: () -> Unit,
    onEditZones: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var backupFileToSave by remember { mutableStateOf<java.io.File?>(null) }

    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        val file = backupFileToSave
        backupFileToSave = null
        if (uri != null && file != null) {
            scope.launch {
                try {
                    val backupService = BackupService(context)
                    val success = withContext(Dispatchers.IO) {
                        backupService.saveBackupToUri(file, uri)
                    }
                    file.delete()
                    Toast.makeText(context, "Бекап збережено", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Помилка: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val backupService = BackupService(context)
                    val success = withContext(Dispatchers.IO) {
                        backupService.restoreBackup(uri)
                    }
                    if (success) {
                        Toast.makeText(context, "Імпорт виконано. Додаток буде перезапущено.", Toast.LENGTH_LONG).show()
                        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                        intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        context.startActivity(intent)
                        Runtime.getRuntime().exit(0)
                    } else {
                        Toast.makeText(context, "Помилка імпорту: невірний формат", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Помилка: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Налаштування") },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                "Дані",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )

            SettingsCard(
                icon = Icons.Default.Share,
                title = "Експортувати дані",
                subtitle = "Поділитися бекапом у ZIP",
                tint = MaterialTheme.colorScheme.primary
            ) {
                scope.launch {
                    try {
                        val backupService = BackupService(context)
                        backupService.shareBackup(withContext(Dispatchers.IO) { backupService.createBackup() })
                    } catch (e: Exception) {
                        Toast.makeText(context, "Помилка: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            SettingsCard(
                icon = Icons.Default.Save,
                title = "Зберегти на пристрій",
                subtitle = "Експортувати бекап у файл",
                tint = MaterialTheme.colorScheme.primary
            ) {
                scope.launch {
                    try {
                        val backupService = BackupService(context)
                        backupFileToSave = withContext(Dispatchers.IO) { backupService.createBackup() }
                        saveLauncher.launch("Fazenda_Backup.zip")
                    } catch (e: Exception) {
                        Toast.makeText(context, "Помилка: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            SettingsCard(
                icon = Icons.Default.RestorePage,
                title = "Імпортувати дані",
                subtitle = "Відновити з ZIP-бекапу",
                tint = MaterialTheme.colorScheme.primary
            ) {
                restoreLauncher.launch(arrayOf("application/zip", "*/*"))
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            Text(
                "Каталог",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )

            SettingsCard(
                icon = Icons.Default.Category,
                title = "Категорії",
                subtitle = "Додати, редагувати, видалити",
                tint = MaterialTheme.colorScheme.secondary
            ) { onEditCategories() }

            SettingsCard(
                icon = Icons.Default.Hub,
                title = "Зони",
                subtitle = "Додати, редагувати, видалити",
                tint = MaterialTheme.colorScheme.secondary
            ) { onEditZones() }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            Text(
                "Інше",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )

            SettingsCard(
                icon = Icons.Default.Language,
                title = "Мова",
                subtitle = "Українська",
                tint = MaterialTheme.colorScheme.tertiary,
                onClick = {}
            )

            SettingsCard(
                icon = Icons.Default.Info,
                title = "Про додаток",
                subtitle = "Розумний Сад v${com.fazenda.app.BuildConfig.VERSION_NAME}",
                tint = MaterialTheme.colorScheme.tertiary,
                onClick = {}
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingsCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = tint.copy(alpha = 0.12f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
