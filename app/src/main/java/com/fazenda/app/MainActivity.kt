package com.fazenda.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.fazenda.app.ui.navigation.AppNavigation
import com.fazenda.app.ui.theme.FazendaTheme
import java.io.File
import android.net.Uri

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val backupFile = File(cacheDir, "Fazenda_Backupt.zip")
        if (backupFile.exists()) {
            android.util.Log.i("TEST", "Found backup file in cache, restoring...")
            try {
                com.fazenda.app.service.BackupService(this).restoreBackup(Uri.fromFile(backupFile))
                android.util.Log.i("TEST", "Restore finished, trying to read from DB...")
                val db = com.fazenda.app.data.database.AppDatabase.getInstance(this)
                val count = db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM plants").use {
                    it.moveToFirst()
                    it.getLong(0)
                }
                android.util.Log.i("TEST", "Seeded/restored plant count: $count")
                // Delete backup file after successful run so it doesn't repeat on next start
                backupFile.delete()
            } catch (e: Exception) {
                android.util.Log.e("TEST", "Restore or DB open failed", e)
            }
        }

        enableEdgeToEdge()
        setContent {
            FazendaTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation()
                }
            }
        }
    }
}
