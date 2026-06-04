package com.fazenda.app.service

import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class BackupService(private val context: Context) {

    private val fileService = FileService(context)

    fun createBackup(): File {
        val tempDir = File(context.cacheDir, "temp").also { it.mkdirs() }
        val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
        val zipFile = File(tempDir, "fazenda_backup_$timestamp.zip")

        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            // Add database
            val dbFile = context.getDatabasePath("fazenda_app.db")
            if (dbFile.exists()) {
                zos.putNextEntry(ZipEntry("fazenda_app.db"))
                FileInputStream(dbFile).use { it.copyTo(zos) }
                zos.closeEntry()
            }

            // Add all photos
            val photosDir = fileService.getPhotosDirectory()
            if (photosDir.exists()) {
                photosDir.listFiles()?.forEach { photoFile ->
                    if (photoFile.isFile) {
                        zos.putNextEntry(ZipEntry("photos/${photoFile.name}"))
                        FileInputStream(photoFile).use { it.copyTo(zos) }
                        zos.closeEntry()
                    }
                }
            }
        }

        return zipFile
    }

    fun shareBackup(backupFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            backupFile
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Fazenda App Backup")
            putExtra(Intent.EXTRA_TEXT, "Бекап бази даних та фото додатку Розумний Сад")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "Зберегти бекап"))
    }
}
