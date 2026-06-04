package com.fazenda.app.ui.util

import android.content.Context
import java.io.File

object PhotoPathResolver {

    fun toAsyncImageModel(context: Context, photoPath: String?): Any? {
        val path = photoPath?.trim().orEmpty()
        if (path.isEmpty()) return null

        return when {
            path.startsWith("assets/") -> {
                "file:///android_asset/${path.removePrefix("assets/")}"
            }
            path.startsWith("photos/") -> {
                File(context.filesDir, path).absolutePath
            }
            path.startsWith("content://") || path.startsWith("file://") -> path
            File(path).isAbsolute -> path
            else -> path
        }
    }

    fun isManagedInternalPhotoPath(photoPath: String?): Boolean {
        return photoPath?.startsWith("photos/") == true
    }
}
