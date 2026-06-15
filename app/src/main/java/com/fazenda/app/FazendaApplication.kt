package com.fazenda.app

import android.app.Application
import com.fazenda.app.data.database.AppDatabase
import com.fazenda.app.data.repository.*
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration

class FazendaApplication : Application() {

    private var startedActivities = 0
    private var isChangingConfiguration = false

    override fun onCreate() {
        super.onCreate()
        Configuration.getInstance().apply {
            userAgentValue = packageName
            osmdroidTileCache = cacheDir.resolve("osmdroid")
        }

        registerActivityLifecycleCallbacks(object : android.app.Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: android.app.Activity, savedInstanceState: android.os.Bundle?) {}
            override fun onActivityStarted(activity: android.app.Activity) {
                startedActivities++
            }
            override fun onActivityResumed(activity: android.app.Activity) {}
            override fun onActivityPaused(activity: android.app.Activity) {}
            override fun onActivityStopped(activity: android.app.Activity) {
                startedActivities--
                isChangingConfiguration = activity.isChangingConfigurations
                if (startedActivities == 0 && !isChangingConfiguration) {
                    performAutoBackup()
                }
            }
            override fun onActivitySaveInstanceState(activity: android.app.Activity, outState: android.os.Bundle) {}
            override fun onActivityDestroyed(activity: android.app.Activity) {}
        })
    }

    @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
    private fun performAutoBackup() {
        val sharedPrefs = getSharedPreferences("fazenda_prefs", android.content.Context.MODE_PRIVATE)
        val backupUri = sharedPrefs.getString("backup_uri", null)
        if (backupUri != null) {
            kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val backupService = com.fazenda.app.service.BackupService(this@FazendaApplication)
                    backupService.autoBackup(backupUri)
                } catch (e: Exception) {
                    android.util.Log.e("FazendaApplication", "AutoBackup failed", e)
                }
            }
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
