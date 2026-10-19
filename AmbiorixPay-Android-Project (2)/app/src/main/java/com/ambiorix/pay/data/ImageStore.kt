package com.ambiorix.pay.data

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object ImageStore {

    /**
     * Copia una imagen seleccionada por el usuario al almacenamiento privado interno de la app.
     * De esta forma la app retiene el acceso permanente a la imagen sin requerir permisos de lectura externa
     * ni depender de la vigencia del URI del ContentProvider tras reiniciar el dispositivo.
     */
    fun saveImageToInternalStorage(context: Context, sourceUri: Uri, fileName: String): String? {
        return try {
            val dir = File(context.filesDir, "images")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            val destinationFile = File(dir, fileName)

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destinationFile).use { output ->
                    input.copyTo(output)
                }
            }
            destinationFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun deleteFile(path: String?): Boolean {
        if (path.isNullOrBlank()) return false
        return try {
            val file = File(path)
            if (file.exists()) file.delete() else false
        } catch (e: Exception) {
            false
        }
    }
}
