package com.fazenda.app.service

import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import androidx.core.content.FileProvider
import com.fazenda.app.data.database.AppDatabase
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class BackupService(private val context: Context) {

    private val fileService = FileService(context)
    private val dbName = "fazenda_db"

    fun createBackup(): File {
        val dbPath = context.getDatabasePath(dbName)
        checkpointWal()

        val addedInZip = mutableSetOf<String>()
        val assetRefs = collectAssetPhotoRefs()

        val tempDir = File(context.cacheDir, "temp").also { it.mkdirs() }
        val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
        val zipFile = File(tempDir, "fazenda_backup_$timestamp.zip")

        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            addDbFilesToZip(zos, dbPath, addedInZip)

            val photosDir = fileService.getPhotosDirectory()
            if (photosDir.exists()) {
                photosDir.listFiles()?.forEach { photoFile ->
                    if (photoFile.isFile) {
                        val entry = "photos/${photoFile.name}"
                        if (entry !in addedInZip) {
                            addedInZip.add(entry)
                            zos.putNextEntry(ZipEntry(entry))
                            FileInputStream(photoFile).use { it.copyTo(zos) }
                            zos.closeEntry()
                        }
                    }
                }
            }

            for (assetPath in assetRefs) {
                val assetRelative = assetPath.removePrefix("assets/")
                val fileName = assetRelative.removePrefix("images/")
                val entry = "photos/$fileName"
                if (entry in addedInZip) continue
                try {
                    context.assets.open(assetRelative).use { input ->
                        addedInZip.add(entry)
                        zos.putNextEntry(ZipEntry(entry))
                        input.copyTo(zos)
                        zos.closeEntry()
                    }
                } catch (_: Exception) {
                }
            }
        }

        return zipFile
    }

    private fun collectAssetPhotoRefs(): Set<String> {
        val refs = mutableSetOf<String>()
        try {
            val db = AppDatabase.getInstance(context)
            val cursor = db.openHelper.readableDatabase.query(
                """SELECT DISTINCT photoPath FROM (
                    SELECT photoPath FROM plants WHERE photoPath LIKE 'assets/images/%'
                    UNION ALL
                    SELECT photoPath FROM plant_photos WHERE photoPath LIKE 'assets/images/%'
                    UNION ALL
                    SELECT photoPath FROM logs WHERE photoPath LIKE 'assets/images/%'
                )"""
            )
            while (cursor.moveToNext()) {
                val path: String = cursor.getString(0) ?: continue
                refs.add(path)
            }
            cursor.close()
        } catch (_: Exception) {
        }
        return refs
    }

    private fun checkpointWal() {
        try {
            val db = AppDatabase.getInstance(context)
            db.openHelper.writableDatabase.execSQL("PRAGMA wal_checkpoint(TRUNCATE)")
        } catch (_: Exception) {
        }
    }

    private fun addDbFilesToZip(zos: ZipOutputStream, dbPath: File, added: MutableSet<String>) {
        val walFile = File(dbPath.absolutePath + "-wal")
        val shmFile = File(dbPath.absolutePath + "-shm")

        if (dbPath.exists()) {
            added.add(dbName)
            zos.putNextEntry(ZipEntry(dbName))
            FileInputStream(dbPath).use { it.copyTo(zos) }
            zos.closeEntry()
        }
        if (walFile.exists()) {
            val entry = "$dbName-wal"
            added.add(entry)
            zos.putNextEntry(ZipEntry(entry))
            FileInputStream(walFile).use { it.copyTo(zos) }
            zos.closeEntry()
        }
        if (shmFile.exists()) {
            val entry = "$dbName-shm"
            added.add(entry)
            zos.putNextEntry(ZipEntry(entry))
            FileInputStream(shmFile).use { it.copyTo(zos) }
            zos.closeEntry()
        }
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

        context.startActivity(Intent.createChooser(intent, "Поділитися бекапом"))
    }

    fun saveBackupToUri(sourceFile: File, targetUri: Uri): Boolean {
        return try {
            context.contentResolver.openOutputStream(targetUri)?.use { output ->
                FileInputStream(sourceFile).use { input ->
                    input.copyTo(output)
                }
            } != null
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun restoreBackup(uri: Uri): Boolean {
        return try {
            val tempDir = File(context.cacheDir, "temp").also { it.mkdirs() }
            val tempZip = File(tempDir, "restore_temp.zip")

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempZip).use { output ->
                    input.copyTo(output)
                }
            } ?: return false

            AppDatabase.closeDatabase()

            val dbPath = context.getDatabasePath(dbName)
            val walFile = File(dbPath.absolutePath + "-wal")
            val shmFile = File(dbPath.absolutePath + "-shm")

            dbPath.parentFile?.mkdirs()
            dbPath.delete()
            walFile.delete()
            shmFile.delete()

            val photosDir = fileService.getPhotosDirectory()
            val existingPhotos = photosDir.listFiles()?.toSet() ?: emptySet()
            existingPhotos.forEach { it.delete() }

            var restoredPhotoCount = 0

            ZipInputStream(FileInputStream(tempZip)).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    val entryName = entry.name
                    when {
                        entryName == dbName -> {
                            FileOutputStream(dbPath).use { output ->
                                zis.copyTo(output)
                            }
                        }
                        entryName == "$dbName-wal" -> {
                            FileOutputStream(walFile).use { output ->
                                zis.copyTo(output)
                            }
                        }
                        entryName == "$dbName-shm" -> {
                            FileOutputStream(shmFile).use { output ->
                                zis.copyTo(output)
                            }
                        }
                        entryName.startsWith("photos/") -> {
                            val fileName = entryName.removePrefix("photos/")
                            if (fileName.isNotBlank()) {
                                val photoFile = File(photosDir, fileName)
                                photoFile.parentFile?.mkdirs()
                                FileOutputStream(photoFile).use { output ->
                                    zis.copyTo(output)
                                }
                                restoredPhotoCount++
                            }
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            migrateAssetPathsToPhotos(dbPath)

            val missingPhotos = verifyPhotoIntegrity(dbPath)
            tempZip.delete()

            if (missingPhotos > 0) {
                android.util.Log.w("BackupService", "Відновлення завершено, але $missingPhotos фото відсутні")
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun migrateAssetPathsToPhotos(dbPath: File) {
        if (!dbPath.exists()) return
        try {
            val db = SQLiteDatabase.openDatabase(dbPath.absolutePath, null, 0)
            db.execSQL("UPDATE plants SET photoPath = REPLACE(photoPath, 'assets/images/', 'photos/') WHERE photoPath LIKE 'assets/images/%'")
            db.execSQL("UPDATE plant_photos SET photoPath = REPLACE(photoPath, 'assets/images/', 'photos/') WHERE photoPath LIKE 'assets/images/%'")
            db.execSQL("UPDATE logs SET photoPath = REPLACE(photoPath, 'assets/images/', 'photos/') WHERE photoPath LIKE 'assets/images/%'")
            db.close()
        } catch (_: Exception) {
        }
    }

    private fun verifyPhotoIntegrity(dbPath: File): Int {
        if (!dbPath.exists()) return 0
        var missing = 0
        try {
            val photosDir = fileService.getPhotosDirectory()
            val db = SQLiteDatabase.openDatabase(dbPath.absolutePath, null, 0)
            val cursor = db.rawQuery(
                """SELECT DISTINCT photoPath FROM (
                    SELECT photoPath FROM plants WHERE photoPath LIKE 'photos/%'
                    UNION ALL
                    SELECT photoPath FROM plant_photos WHERE photoPath LIKE 'photos/%'
                    UNION ALL
                    SELECT photoPath FROM logs WHERE photoPath LIKE 'photos/%'
                )""", null
            )
            while (cursor.moveToNext()) {
                val path = cursor.getString(0) ?: continue
                val file = File(photosDir, path.removePrefix("photos/"))
                if (!file.exists()) {
                    missing++
                }
            }
            cursor.close()
            db.close()
        } catch (_: Exception) {
        }
        return missing
    }
}
