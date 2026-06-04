package com.fazenda.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fazenda.app.FazendaApplication
import com.fazenda.app.data.entity.CategoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CategoryViewModel(application: Application) : AndroidViewModel(application) {

    private val categoryRepository = (application as FazendaApplication).categoryRepository

    private val _categories = MutableStateFlow<List<CategoryEntity>>(emptyList())
    val categories: StateFlow<List<CategoryEntity>> = _categories.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        viewModelScope.launch {
            categoryRepository.allCategories.collect { list ->
                _categories.value = list
                _isLoading.value = false
            }
        }
    }

    fun addCategory(name: String) {
        viewModelScope.launch {
            categoryRepository.insertCategory(CategoryEntity(name = name))
        }
    }

    fun updateCategory(category: CategoryEntity) {
        viewModelScope.launch {
            categoryRepository.updateCategory(category)
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            categoryRepository.deleteCategory(category)
        }
    }

    suspend fun findOrCreateCategory(name: String): Long? {
        val existing = _categories.value.find { it.name.equals(name, ignoreCase = true) }
        if (existing != null) return existing.id
        return withContext(Dispatchers.IO) {
            val id = categoryRepository.insertCategory(CategoryEntity(name = name))
            id
        }
    }
}
