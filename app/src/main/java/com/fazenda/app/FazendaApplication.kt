package com.fazenda.app

import android.app.Application
import com.fazenda.app.data.database.AppDatabase
import com.fazenda.app.data.repository.*
import org.osmdroid.config.Configuration

class FazendaApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Configuration.getInstance().apply {
            userAgentValue = packageName
            osmdroidTileCache = cacheDir.resolve("osmdroid")
        }
    }

    val database by lazy { AppDatabase.getInstance(this) }

    val logRepository by lazy { LogRepository(database.logDao()) }
    val plantRepository by lazy { PlantRepository(database.plantDao(), logRepository) }
    val plantPhotoRepository by lazy { PlantPhotoRepository(database.plantPhotoDao()) }
    val chemicalRepository by lazy { ChemicalRepository(database.chemicalDao()) }
    val scheduleRepository by lazy { ScheduleRepository(database.scheduleDao()) }
    val zoneRepository by lazy { ZoneRepository(database.zoneDao()) }
    val categoryRepository by lazy { CategoryRepository(database.categoryDao()) }
}
