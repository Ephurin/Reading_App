package com.example.reading_app.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

// Data model for online book
 data class OnlineBook(
    val id: String,
    val title: String,
    val subtitle: String?,
    val authors: List<String>,
    val publisher: String?,
    val publishedDate: String?,
    val pageCount: Int?,
    val categories: List<String>,
    val averageRating: Double?,
    val ratingsCount: Int?,
    val coverUrl: String?,
    val description: String?,
    val previewLink: String?,
    val infoLink: String?,
    val buyLink: String?,
    val saleability: String?,
    val isEpubAvailable: Boolean,
    val epubDownloadLink: String?,
    val isPdfAvailable: Boolean,
    val pdfDownloadLink: String?,
    val downloadUrl: String?
)

object GoogleBooksApi {
    private const val BASE_URL = "https://www.googleapis.com/books/v1/volumes"

    suspend fun searchBooks(query: String, maxResults: Int = 20): List<OnlineBook> = withContext(Dispatchers.IO) {
        val encoded = try {
            URLEncoder.encode(query, "UTF-8")
        } catch (_: Exception) {
            query.replace(" ", "+")
        }
        val url = "$BASE_URL?q=$encoded&maxResults=$maxResults"
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 5000
        connection.readTimeout = 5000
        val books = mutableListOf<OnlineBook>()
        try {
            val response = connection.inputStream.bufferedReader().readText()
            val json = JSONObject(response)
            val items = json.optJSONArray("items") ?: JSONArray()
            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                val volumeInfo = item.getJSONObject("volumeInfo")
                val title = volumeInfo.optString("title", "Unknown Title")
                val subtitle = volumeInfo.optString("subtitle", null)
                val authors = volumeInfo.optJSONArray("authors")?.let { arr ->
                    List(arr.length()) { arr.getString(it) }
                } ?: emptyList()
                val publisher = volumeInfo.optString("publisher", null)
                val publishedDate = volumeInfo.optString("publishedDate", null)
                val pageCount = if (volumeInfo.has("pageCount")) volumeInfo.optInt("pageCount") else null
                val categories = volumeInfo.optJSONArray("categories")?.let { arr ->
                    List(arr.length()) { arr.getString(it) }
                } ?: emptyList()
                val averageRating = if (volumeInfo.has("averageRating")) volumeInfo.optDouble("averageRating") else null
                val ratingsCount = if (volumeInfo.has("ratingsCount")) volumeInfo.optInt("ratingsCount") else null
                val imageLinks = volumeInfo.optJSONObject("imageLinks")
                var coverUrl: String? = null
                if (imageLinks != null) {
                    coverUrl = imageLinks.optString("thumbnail", null) ?: imageLinks.optString("smallThumbnail", null) ?: imageLinks.optString("small", null)
                    if (coverUrl != null && coverUrl.startsWith("http:")) {
                        coverUrl = coverUrl.replaceFirst("http:", "https:")
                    }
                }
                val description = volumeInfo.optString("description", null)
                val previewLink = volumeInfo.optString("previewLink", null)
                val infoLink = volumeInfo.optString("infoLink", null)
                // Try to get download link (PDF/EPUB)
                val accessInfo = item.optJSONObject("accessInfo")
                val epubObj = accessInfo?.optJSONObject("epub")
                val pdfObj = accessInfo?.optJSONObject("pdf")
                val isEpubAvailable = epubObj?.optBoolean("isAvailable") ?: false
                val epubDownloadLink = epubObj?.optString("downloadLink", null)
                val isPdfAvailable = pdfObj?.optBoolean("isAvailable") ?: false
                val pdfDownloadLink = pdfObj?.optString("downloadLink", null)
                val downloadUrl = pdfDownloadLink ?: epubDownloadLink
                val saleInfo = item.optJSONObject("saleInfo")
                val saleability = saleInfo?.optString("saleability", null)
                val buyLink = saleInfo?.optString("buyLink", null)
                books.add(
                    OnlineBook(
                        id = item.optString("id"),
                        title = title,
                        subtitle = subtitle,
                        authors = authors,
                        publisher = publisher,
                        publishedDate = publishedDate,
                        pageCount = pageCount,
                        categories = categories,
                        averageRating = averageRating,
                        ratingsCount = ratingsCount,
                        coverUrl = coverUrl,
                        description = description,
                        previewLink = previewLink,
                        infoLink = infoLink,
                        buyLink = buyLink,
                        saleability = saleability,
                        isEpubAvailable = isEpubAvailable,
                        epubDownloadLink = epubDownloadLink,
                        isPdfAvailable = isPdfAvailable,
                        pdfDownloadLink = pdfDownloadLink,
                        downloadUrl = downloadUrl
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            connection.disconnect()
        }
        books
    }
}
