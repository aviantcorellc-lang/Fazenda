package com.fazenda.app.service

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class PlantInfoService {

    suspend fun fetchFromWikipedia(query: String, language: String = "uk", isRetry: Boolean = false): WikipediaResult? {
        return withContext(Dispatchers.IO) {
            try {
                val encoded = URLEncoder.encode(query, "UTF-8")
                val url = "https://$language.wikipedia.org/api/rest_v1/page/summary/$encoded"
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.setRequestProperty("User-Agent", "FazendaApp/1.0")

                if (connection.responseCode == 200) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val json = reader.use { it.readText() }
                    parseWikipediaResponse(json)
                } else if (!isRetry) {
                    val titles = searchWikipedia(query, language)
                    if (titles.isNotEmpty()) {
                        fetchFromWikipedia(titles.first(), language, isRetry = true)
                    } else if (language == "uk") {
                        fetchFromWikipedia(query, "en", isRetry = true)
                    } else null
                } else null
            } catch (e: Exception) {
                Log.e("PlantInfoService", "Failed to fetch from Wikipedia", e)
                null
            }
        }
    }

    suspend fun searchWikipedia(query: String, language: String = "uk"): List<String> {
        return withContext(Dispatchers.IO) {
            try {
                val encoded = URLEncoder.encode(query, "UTF-8")
                val url = "https://$language.wikipedia.org/w/api.php?action=opensearch&search=$encoded&limit=5&format=json"
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.setRequestProperty("User-Agent", "FazendaApp/1.0")

                if (connection.responseCode == 200) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val json = reader.use { it.readText() }
                    val titles = mutableListOf<String>()
                    val titleArrayRegex = """\["([^"]+)"(,"([^"]+)")*\]""".toRegex()
                    val match = titleArrayRegex.find(json)
                    if (match != null) {
                        val parts = match.value.removeSurrounding("[", "]").split(",").map { it.trim().removeSurrounding("\"") }
                        titles.addAll(parts)
                    }
                    titles
                } else emptyList()
            } catch (e: Exception) {
                Log.e("PlantInfoService", "Failed to search Wikipedia", e)
                emptyList()
            }
        }
    }

    private fun parseWikipediaResponse(json: String): WikipediaResult? {
        return try {
            val extract = extractJsonString(json, "extract") ?: return null
            val title = extractJsonString(json, "title") ?: ""
            val thumbnailUrl = extractJsonString(json, "source", "\"thumbnail\":\\{[^}]*\"source\":\"([^\"]+)\"")
            val description = extractJsonString(json, "description")

            WikipediaResult(
                title = title,
                description = description ?: "",
                extract = cleanExtract(extract),
                thumbnailUrl = thumbnailUrl
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun extractJsonString(json: String, key: String, customPattern: String? = null): String? {
        val pattern = customPattern ?: "\"$key\":\"([^\"]+)\""
        val regex = pattern.toRegex()
        val match = regex.find(json) ?: return null
        return match.groupValues[1].replace("\\\"", "\"").replace("\\n", "\n")
    }

    private fun cleanExtract(extract: String): String {
        return extract
            .replace(Regex("\\[\\d+\\]"), "")
            .replace(Regex("<[^>]+>"), "")
            .trim()
    }
}

data class WikipediaResult(
    val title: String,
    val description: String,
    val extract: String,
    val thumbnailUrl: String?
)
