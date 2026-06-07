package com.fazenda.app.service

import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

class WeatherService(private val context: Context) {

    companion object {
        private const val OPEN_METEO_API = "https://api.open-meteo.com/v1/forecast"
    }

    suspend fun getForecast(latitude: Double, longitude: Double): WeatherForecast? {
        return withContext(Dispatchers.IO) {
            try {
                val url = "$OPEN_METEO_API?latitude=$latitude&longitude=$longitude&daily=temperature_2m_max,temperature_2m_min,precipitation_sum,weathercode&forecast_days=7"
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                if (connection.responseCode == 200) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = reader.use { it.readText() }
                    parseWeatherResponse(response)
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.e("WeatherService", "Failed to get forecast", e)
                null
            }
        }
    }

    suspend fun getCurrentLocation(): Location? {
        return withContext(Dispatchers.IO) {
            try {
                val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                val lastKnown = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                lastKnown
            } catch (e: Exception) {
                Log.e("WeatherService", "Failed to get current location", e)
                null
            }
        }
    }

    private fun parseWeatherResponse(json: String): WeatherForecast? {
        return try {
            val dailyRegex = "\"daily\":\\{[^}]*\"temperature_2m_max\":\\[([^\\]]*)\\]".toRegex()
            val tempMaxMatch = dailyRegex.find(json)
            val temps = tempMaxMatch?.groupValues?.get(1)?.split(",")?.map { it.trim().toDouble() } ?: emptyList()

            val precipRegex = "\"precipitation_sum\":\\[([^\\]]*)\\]".toRegex()
            val precipMatch = precipRegex.find(json)
            val precip = precipMatch?.groupValues?.get(1)?.split(",")?.map { it.trim().toDouble() } ?: emptyList()

            val weatherCodeRegex = "\"weathercode\":\\[([^\\]]*)\\]".toRegex()
            val codeMatch = weatherCodeRegex.find(json)
            val codes = codeMatch?.groupValues?.get(1)?.split(",")?.map { it.trim().toInt() } ?: emptyList()

            WeatherForecast(
                maxTemps = temps,
                precipitation = precip,
                weatherCodes = codes
            )
        } catch (e: Exception) {
            null
        }
    }

    fun getWeatherDescription(code: Int): String {
        return when (code) {
            0 -> "☀️ Ясно"
            1, 2, 3 -> "⛅ Мінлива хмарність"
            45, 48 -> "🌫️ Туман"
            51, 53, 55 -> "🌦️ Мряка"
            61, 63, 65 -> "️ Дощ"
            71, 73, 75 -> "️ Сніг"
            80, 81, 82 -> "🌦️ Злива"
            95, 96, 99 -> "️ Гроза"
            else -> "️ Помірна погода"
        }
    }

    fun getFarmingAdvice(forecast: WeatherForecast?): String {
        if (forecast == null) return "Немає даних про погоду"
        
        val maxTemp = forecast.maxTemps.maxOrNull() ?: 0.0
        val totalPrecip = forecast.precipitation.sum()
        
        return buildString {
            append("📊 Прогноз на 7 днів:\n")
            append("️ Макс. температура: ${String.format("%.1f", maxTemp)}°C\n")
            append("🌧️ Опади: ${String.format("%.1f", totalPrecip)} мм\n\n")
            
            if (maxTemp < 5) {
                append("️ Холодно! Обприскування не рекомендується.\n")
            } else if (maxTemp > 30) {
                append(" Спекотно! Обробляти вранці або ввечері.\n")
            } else {
                append("✅ Температура сприятлива для обробок.\n")
            }
            
            if (totalPrecip > 20) {
                append("🌧️ Очікуються дощі. Відкласть обприскування.\n")
            } else if (totalPrecip > 5) {
                append(" Можливий дощ. Перевірте прогноз перед обробкою.\n")
            } else {
                append("☀️ Суха погода - ідеально для обробок!\n")
            }
        }
    }
}

data class WeatherForecast(
    val maxTemps: List<Double>,
    val precipitation: List<Double>,
    val weatherCodes: List<Int>
)
