package com.equipo2.vinilos.support

import com.equipo2.vinilos.BuildConfig
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

/** Llamadas directas a BackVynils para preparar y limpiar datos de prueba. */
object BackVynils {

    private const val TIMEOUT_MS = 90_000

    /** Despierta el servidor publicado, que puede tardar en responder la primera vez. */
    fun wakeUp() {
        request("albums", "GET")
    }

    fun albumNames(): List<String> {
        val albums = JSONArray(request("albums", "GET"))
        return List(albums.length()) { albums.getJSONObject(it).getString("name") }
    }

    /** Álbumes con una descripción más larga que length, que en el detalle se corta con «Ver más». */
    fun albumNamesWithDescriptionLongerThan(length: Int): List<String> {
        val albums = JSONArray(request("albums", "GET"))
        return List(albums.length()) { albums.getJSONObject(it) }
            .filter { it.getString("description").length > length }
            .map { it.getString("name") }
    }

    fun musicianNames(): List<String> = names("musicians")

    fun bandNames(): List<String> = names("bands")

    private fun names(path: String): List<String> {
        val items = JSONArray(request(path, "GET"))
        return List(items.length()) { items.getJSONObject(it).getString("name") }
    }

    /** Borra los álbumes con este prefijo para no dejar datos de prueba. */
    fun deleteAlbumsStartingWith(prefix: String) {
        val albums = JSONArray(request("albums", "GET"))
        for (i in 0 until albums.length()) {
            val album = albums.getJSONObject(i)
            if (album.getString("name").startsWith(prefix)) request("albums/${album.getInt("id")}", "DELETE")
        }
    }

    private fun request(path: String, method: String): String {
        val connection = URL(BuildConfig.API_BASE_URL + path).openConnection() as HttpURLConnection
        connection.requestMethod = method
        connection.connectTimeout = TIMEOUT_MS
        connection.readTimeout = TIMEOUT_MS
        return try {
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}
