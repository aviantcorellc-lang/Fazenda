package com.fazenda.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fazenda.app.FazendaApplication
import com.fazenda.app.data.entity.ScheduleEntity
import com.fazenda.app.service.WeatherForecast
import com.fazenda.app.service.WeatherService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val scheduleRepository = (application as FazendaApplication).scheduleRepository
    private val weatherService = WeatherService(application)

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

    init {
        viewModelScope.launch {
            scheduleRepository.allSchedules.collect { list ->
                _schedules.value = list
                _isLoading.value = false
            }
        }
        loadWeather()
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
