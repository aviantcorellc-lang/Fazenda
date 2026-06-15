package com.fazenda.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fazenda.app.FazendaApplication
import com.fazenda.app.data.entity.CategoryEntity
import com.fazenda.app.data.entity.LogActionTypes
import com.fazenda.app.data.entity.LogEntity
import com.fazenda.app.data.entity.ScheduleEntity
import com.fazenda.app.service.ScheduleAlarmManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SchedulesViewModel(application: Application) : AndroidViewModel(application) {
    private val scheduleRepository = (application as FazendaApplication).scheduleRepository
    private val categoryRepository = (application as FazendaApplication).categoryRepository
    private val logRepository = (application as FazendaApplication).logRepository
    private val alarmManager = ScheduleAlarmManager(application)

    private val _schedules = MutableStateFlow<List<ScheduleEntity>>(emptyList())
    val schedules: StateFlow<List<ScheduleEntity>> = _schedules.asStateFlow()

    private val _categories = MutableStateFlow<List<CategoryEntity>>(emptyList())
    val categories: StateFlow<List<CategoryEntity>> = _categories.asStateFlow()

    init {
        viewModelScope.launch {
            scheduleRepository.allSchedules.collect { list ->
                _schedules.value = list
                // Спробуємо автоматично встановити аларми для майбутніх невиконаних подій
                val now = System.currentTimeMillis()
                list.filter { !it.isCompleted && it.startDate > now }.forEach { schedule ->
                    alarmManager.scheduleAlarm(schedule)
                }
            }
        }
        viewModelScope.launch {
            categoryRepository.allCategories.collect { list ->
                _categories.value = list
            }
        }
    }

    fun addSchedule(phaseTime: String, categoryId: Long?, recipe: String, startDate: Long, endDate: Long) {
        viewModelScope.launch {
            val schedule = ScheduleEntity(
                phaseTime = phaseTime,
                categoryId = categoryId,
                recipe = recipe,
                startDate = startDate,
                endDate = endDate,
                isCompleted = false
            )
            val id = scheduleRepository.insertSchedule(schedule)
            alarmManager.scheduleAlarm(schedule.copy(id = id))
        }
    }

    fun completeSchedule(schedule: ScheduleEntity) {
        viewModelScope.launch {
            scheduleRepository.markAsCompleted(schedule.id)
            alarmManager.cancelAlarm(schedule.id)
            
            // Automatically log this action in the journal as a group treatment
            val log = LogEntity(
                date = System.currentTimeMillis(),
                plantId = null,
                zoneId = null,
                categoryId = schedule.categoryId,
                actionType = LogActionTypes.SPRAYING,
                comment = "Автоматичний запис: виконано план обробки.\nФаза: ${schedule.phaseTime}\nРецепт: ${schedule.recipe}"
            )
            logRepository.insertLog(log)
        }
    }

    fun deleteSchedule(schedule: ScheduleEntity) {
        viewModelScope.launch {
            scheduleRepository.deleteSchedule(schedule)
            alarmManager.cancelAlarm(schedule.id)
        }
    }
}
