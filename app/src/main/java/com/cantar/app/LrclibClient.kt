package com.cantar.app

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

data class LrclibTrack(
    val id: Long,
    val trackName: String,
    val artistName: String,
    val albumName: String,
    val duration: Int,
    val syncedLyrics: String?,
    val plainLyrics: String?
)

object LrclibClient {
    private const val BASE = "https://lrclib.net"

    suspend fun search(query: String): List<LrclibTrack> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val url = URL("$BASE/api/search?q=" + Uri.encode(query.trim()))
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10000
            readTimeout = 12000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("X-User-Agent", "Cantar/1.0 (https://github.com/mimtrix2236-png/Cantar)")
        }
        try {
            if (connection.responseCode !in 200..299) return@withContext emptyList()
            val json = JSONArray(connection.inputStream.bufferedReader().use { it.readText() })
            buildList {
                for (i in 0 until json.length()) {
                    val item = json.optJSONObject(i) ?: continue
                    add(
                        LrclibTrack(
                            id = item.optLong("id"),
                            trackName = item.optString("trackName"),
                            artistName = item.optString("artistName"),
                            albumName = item.optString("albumName"),
                            duration = item.optInt("duration"),
                            syncedLyrics = item.optString("syncedLyrics").takeUnless { it == "null" || it.isBlank() },
                            plainLyrics = item.optString("plainLyrics").takeUnless { it == "null" || it.isBlank() }
                        )
                    )
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    suspend fun getTrack(id: Long): LrclibTrack? = withContext(Dispatchers.IO) {
        val connection = (URL("$BASE/api/get/$id").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10000
            readTimeout = 12000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("X-User-Agent", "Cantar/1.0 (https://github.com/mimtrix2236-png/Cantar)")
        }
        try {
            if (connection.responseCode !in 200..299) return@withContext null
            val item = org.json.JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            LrclibTrack(
                id = item.optLong("id"),
                trackName = item.optString("trackName"),
                artistName = item.optString("artistName"),
                albumName = item.optString("albumName"),
                duration = item.optInt("duration"),
                syncedLyrics = item.optString("syncedLyrics").takeUnless { it == "null" || it.isBlank() },
                plainLyrics = item.optString("plainLyrics").takeUnless { it == "null" || it.isBlank() }
            )
        } finally {
            connection.disconnect()
        }
    }
}
