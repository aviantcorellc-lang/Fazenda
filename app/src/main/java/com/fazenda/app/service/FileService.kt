package com.fazenda.app.service

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class FileService(private val context: Context) {

    private val photosDir: File
        get() = File(context.filesDir, "photos").also { it.mkdirs() }

    fun savePhoto(sourcePath: String): String {
        val sourceFile = File(sourcePath)
        return savePhotoFromFile(sourceFile)
    }

    fun savePhotoFromFile(sourceFile: File): String {
        val fileName = "photo_${UUID.randomUUID().toString().take(8)}.jpg"
        val destFile = File(photosDir, fileName)
        sourceFile.copyTo(destFile, overwrite = true)
        return "photos/$fileName"
    }

    fun savePhotoFromUri(uri: Uri): String {
        val fileName = "photo_${UUID.randomUUID().toString().take(8)}.jpg"
        val destFile = File(photosDir, fileName)
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        } ?: throw IllegalStateException("Не вдалося відкрити фото для збереження")
        return "photos/$fileName"
    }

    fun getFullPath(relativePath: String): String {
        return File(context.filesDir, relativePath).absolutePath
    }

    fun photoExists(relativePath: String): Boolean {
        return File(context.filesDir, relativePath).exists()
    }

    fun deletePhoto(relativePath: String) {
        File(context.filesDir, relativePath).delete()
    }

    fun isManagedInternalPhotoPath(path: String?): Boolean {
        return path?.startsWith("photos/") == true
    }

    fun getPhotosDirectory(): File = photosDir

    fun getTempDirectory(): File = File(context.cacheDir, "temp").also { it.mkdirs() }

    fun cleanupTempFiles() {
        File(context.cacheDir, "temp").let { dir ->
            if (dir.exists()) {
                dir.listFiles()?.filter { it.name.contains("fazenda_backup") }?.forEach { it.delete() }
            }
        }
    }
}
