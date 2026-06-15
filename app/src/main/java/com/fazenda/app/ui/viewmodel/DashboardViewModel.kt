package com.fazenda.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fazenda.app.FazendaApplication
import com.fazenda.app.data.entity.ScheduleEntity
import com.fazenda.app.service.ConnectivityObserver
import com.fazenda.app.service.NetworkConnectivityObserver
import com.fazenda.app.service.WeatherForecast
import com.fazenda.app.service.WeatherService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val scheduleRepository = (application as FazendaApplication).scheduleRepository
    private val logRepository = (application as FazendaApplication).logRepository
    private val weatherService = WeatherService(application)
    private val connectivityObserver = NetworkConnectivityObserver(application)

    private val _schedules = MutableStateFlow<List<ScheduleEntity>>(emptyList())
    val schedules: StateFlow<List<ScheduleEntity>> = _schedules.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _weatherAdvice = MutableStateFlow<String>("Отримання прогнозу погоди...")
    val weatherAdvice: StateFlow<String> = _weatherAdvice.asStateFlow()

    private val _currentWeatherCode = MutableStateFlow<Int?>(null)
    val currentWeatherCode: StateFlow<Int?> = _currentWeatherCode.asStateFlow()

    private val _currentWeatherTemp = MutableStateFlow<Double?>(null)
    val currentWeatherTemp: StateFlow<Double?> = _currentWeatherTemp.asStateFlow()

    private val _connectivityStatus = MutableStateFlow(ConnectivityObserver.Status.Unavailable)
    val connectivityStatus: StateFlow<ConnectivityObserver.Status> = _connectivityStatus.asStateFlow()

    val chemicalUsageStats: StateFlow<Map<String, Int>> = logRepository.allLogsWithChemicals
        .map { logs ->
            logs.flatMap { it.chemicals }
                .groupBy { it.name }
                .mapValues { it.value.size }
                .toList()
                .sortedByDescending { it.second }
                .take(5)
                .toMap()
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    val lastWeekActivityStats: StateFlow<Map<String, Int>> = logRepository.allLogs
        .map { logs ->
            val sdf = java.text.SimpleDateFormat("dd.MM", java.util.Locale.getDefault())
            
            // Створимо мапу за останні 7 днів з нулями
            val last7Days = (0..6).map { i ->
                val cal = java.util.Calendar.getInstance()
                cal.add(java.util.Calendar.DAY_OF_YEAR, -i)
                sdf.format(cal.time)
            }.reversed()
            
            val initialMap = last7Days.associateWith { 0 }.toMutableMap()
            
            logs.forEach { log ->
                val dateStr = sdf.format(java.util.Date(log.date))
                if (initialMap.containsKey(dateStr)) {
                    initialMap[dateStr] = (initialMap[dateStr] ?: 0) + 1
                }
            }
            initialMap
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    init {
        viewModelScope.launch {
            scheduleRepository.allSchedules.collect { list ->
                _schedules.value = list
                _isLoading.value = false
            }
        }
        viewModelScope.launch {
            connectivityObserver.observe().collect { status ->
                _connectivityStatus.value = status
                if (status == ConnectivityObserver.Status.Available) {
                    loadWeather()
                }
            }
        }
    }

    fun loadWeather() {
        viewModelScope.launch {
            try {
                val location = weatherService.getCurrentLocation()
                val lat = location?.latitude ?: 50.4501
                val lon = location?.longitude ?: 30.5234
                val forecast = weatherService.getForecast(lat, lon)
                if (forecast != null) {
                    _weatherAdvice.value = weatherService.getFarmingAdvice(forecast)
                    _currentWeatherCode.value = forecast.weatherCodes.firstOrNull()
                    _currentWeatherTemp.value = forecast.maxTemps.firstOrNull()
                } else {
                    _weatherAdvice.value = "Не вдалося завантажити прогноз погоди"
                }
            } catch (e: Exception) {
                _weatherAdvice.value = "Помилка при отриманні погоди: ${e.localizedMessage}"
            }
        }
    }
}
