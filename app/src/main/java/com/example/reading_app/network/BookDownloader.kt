package com.example.reading_app.network

import android.content.Context
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object BookDownloader {
    /**
     * Downloads a book file from the given URL and saves it to the app's books directory.
     * Returns the absolute file path, or null if failed.
     */
    suspend fun downloadBook(context: Context, url: String, fileName: String): String? {
        return try {
            val booksDir = File(context.filesDir, "books")
            if (!booksDir.exists()) booksDir.mkdirs()
            val destFile = File(booksDir, fileName)
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.requestMethod = "GET"
            connection.connect()
            destFile.outputStream().use { output ->
                connection.inputStream.use { input ->
                    input.copyTo(output)
                }
            }
            connection.disconnect()
            destFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
