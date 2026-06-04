package com.fazenda.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras

class PlantDetailsViewModelFactory(
    private val plantId: Long
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
        val savedStateHandle = SavedStateHandle(mapOf("plantId" to plantId))
        @Suppress("UNCHECKED_CAST")
        return PlantDetailsViewModel(application, savedStateHandle) as T
    }
}
