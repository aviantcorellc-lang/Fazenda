package com.fazenda.app.ui.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchableDropdown(
    value: String,
    onValueChange: (String) -> Unit,
    options: List<String>,
    label: String = "",
    readOnly: Boolean = false,
    modifier: Modifier = Modifier
) {
    var showDropdown by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredOptions = if (searchQuery.isBlank()) {
        options
    } else {
        options.filter { it.contains(searchQuery, ignoreCase = true) }
    }

    ExposedDropdownMenuBox(
        expanded = showDropdown,
        onExpandedChange = {
            showDropdown = it
            if (it) searchQuery = ""
        }
    ) {
        OutlinedTextField(
            value = if (searchQuery.isNotBlank()) searchQuery else value,
            onValueChange = {
                searchQuery = it
                onValueChange(it)
                showDropdown = true
            },
            label = { Text(label) },
            singleLine = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showDropdown) },
            readOnly = readOnly,
            modifier = modifier.menuAnchor()
        )
        if (showDropdown) {
            ExposedDropdownMenu(
                expanded = showDropdown,
                onDismissRequest = {
                    showDropdown = false
                    searchQuery = ""
                }
            ) {
                if (filteredOptions.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("Немає варіантів") },
                        onClick = {
                            showDropdown = false
                            searchQuery = ""
                        }
                    )
                } else {
                    filteredOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                onValueChange(option)
                                showDropdown = false
                                searchQuery = ""
                            }
                        )
                    }
                }
            }
        }
    }
}
