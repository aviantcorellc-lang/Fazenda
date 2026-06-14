package com.fazenda.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fazenda.app.FazendaApplication
import com.fazenda.app.data.entity.ChemicalEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class KnowledgeBaseViewModel(application: Application) : AndroidViewModel(application) {
    private val chemicalRepository = (application as FazendaApplication).chemicalRepository

    private val _chemicals = MutableStateFlow<List<ChemicalEntity>>(emptyList())
    val chemicals: StateFlow<List<ChemicalEntity>> = _chemicals.asStateFlow()

    init {
        viewModelScope.launch {
            chemicalRepository.allChemicals.collect { list ->
                _chemicals.value = list
            }
        }
    }

    fun addChemical(name: String, purpose: String, waitingPeriodDays: Int, chemicalGroup: String) {
        viewModelScope.launch {
            val chemical = ChemicalEntity(
                name = name,
                purpose = purpose,
                waitingPeriodDays = waitingPeriodDays,
                chemicalGroup = chemicalGroup
            )
            chemicalRepository.insertChemical(chemical)
        }
    }
}
