package com.tuempresa.fugas.domain

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL

object ModelUpdater {
    private const val MODEL_FILENAME = "leak_detection.tflite"

    suspend fun downloadModel(context: Context, url: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val connection = URL(url).openConnection()
            connection.connect()
            val input = connection.getInputStream()
            val file = File(context.filesDir, MODEL_FILENAME)
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getModelFile(context: Context): File? {
        val file = File(context.filesDir, MODEL_FILENAME)
        return if (file.exists()) file else null
    }
}
