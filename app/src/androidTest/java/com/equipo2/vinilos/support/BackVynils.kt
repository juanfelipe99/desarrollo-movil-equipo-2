package com.equipo2.vinilos.support

import com.equipo2.vinilos.BuildConfig
import org.json.JSONArray
import org.json.JSONObject
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

    /** Descripción de los álbumes y artistas que crean las pruebas. */
    const val TEST_DESCRIPTION = "Creado por una prueba E2E."

    /** Crea un álbum de prueba sin tracks y devuelve su id. */
    fun createAlbum(name: String, description: String = TEST_DESCRIPTION): Int {
        val album = JSONObject()
            .put("name", name)
            .put("cover", "https://ejemplo.com/portada.jpg")
            .put("releaseDate", "2020-01-01")
            .put("description", description)
            .put("genre", "Rock")
            .put("recordLabel", "EMI")
        return JSONObject(request("albums", "POST", album.toString())).getInt("id")
    }

    /** Nombres de los tracks del álbum, en orden. */
    fun trackNames(albumId: Int): List<String> {
        val tracks = JSONObject(request("albums/$albumId", "GET")).getJSONArray("tracks")
        return List(tracks.length()) { tracks.getJSONObject(it).getString("name") }
    }

    /**
     * Borra los álbumes con este prefijo, para los que se crean desde la app y no se conoce su id.
     * El prefijo debe llevar [RUN_TAG], así no se borran los de otra ejecución que esté corriendo al mismo tiempo.
     */
    fun deleteAlbumsStartingWith(prefix: String) {
        val albums = JSONArray(request("albums", "GET"))
        for (i in 0 until albums.length()) {
            val album = albums.getJSONObject(i)
            if (album.getString("name").startsWith(prefix)) deleteAlbum(album.getInt("id"))
        }
    }

    /** Borra el álbum, primero sus tracks, porque la API no borra un álbum que los tenga. */
    fun deleteAlbum(id: Int) {
        val tracks = JSONObject(request("albums/$id", "GET")).optJSONArray("tracks") ?: JSONArray()
        for (t in 0 until tracks.length()) request("albums/$id/tracks/${tracks.getJSONObject(t).getInt("id")}", "DELETE")
        request("albums/$id", "DELETE")
    }

    /** Asocia el álbum al músico, que queda como su intérprete. */
    fun addAlbumToMusician(musicianId: Int, albumId: Int) {
        // Sin cuerpo, pero el servidor publicado exige el Content-Length
        request("musicians/$musicianId/albums/$albumId", "POST", "")
    }

    /** Crea una banda de prueba sin integrantes y devuelve su id. */
    fun createBand(name: String): Int {
        val band = JSONObject()
            .put("name", name)
            .put("image", "https://ejemplo.com/banda.jpg")
            .put("description", "Banda creada por una prueba E2E.")
            .put("creationDate", "2000-01-01")
        return JSONObject(request("bands", "POST", band.toString())).getInt("id")
    }

    /** Crea un músico de prueba sin banda y devuelve su id. */
    fun createMusician(name: String, description: String = TEST_DESCRIPTION): Int {
        val musician = JSONObject()
            .put("name", name)
            .put("image", "https://ejemplo.com/musico.jpg")
            .put("description", description)
            .put("birthDate", "1980-01-01")
        return JSONObject(request("musicians", "POST", musician.toString())).getInt("id")
    }

    /** Agrega el músico a la banda; si estaba en otra, la deja. */
    fun addMusicianToBand(bandId: Int, musicianId: Int) {
        // Sin cuerpo, pero el servidor publicado exige el Content-Length
        request("bands/$bandId/musicians/$musicianId", "POST", "")
    }

    /** Nombres de los integrantes de la banda. */
    fun bandMemberNames(bandId: Int): List<String> {
        val members = JSONObject(request("bands/$bandId", "GET")).optJSONArray("musicians") ?: JSONArray()
        return List(members.length()) { members.getJSONObject(it).getString("name") }
    }

    /** Borra los músicos y luego las bandas indicados, para que ninguna banda se borre con integrantes. */
    fun deleteArtists(musicianIds: List<Int>, bandIds: List<Int>) {
        musicianIds.forEach { request("musicians/$it", "DELETE") }
        bandIds.forEach { request("bands/$it", "DELETE") }
    }

    /**
     * El servidor publicado a veces responde 404 o 5xx un momento mientras despierta o se reinicia.
     * Esas respuestas dicen que no hizo nada, así que se reintenta sin riesgo de crear datos dos veces.
     */
    private fun request(path: String, method: String, body: String? = null): String {
        repeat(RETRIES) { attempt ->
            val (code, text) = send(path, method, body)
            if (code in 200..299) return text
            // Al limpiar, si ya no existe es porque otra ejecución contra la misma API lo borró antes
            if (method == "DELETE" && code == 404) return text
            if (code !in RETRYABLE_CODES || attempt == RETRIES - 1) {
                throw IllegalStateException("$method $path respondió $code: ${text.take(200)}")
            }
            Thread.sleep(RETRY_DELAY_MS * (attempt + 1))
        }
        error("No se llega aquí")
    }

    private fun send(path: String, method: String, body: String?): Pair<Int, String> {
        val connection = URL(BuildConfig.API_BASE_URL + path).openConnection() as HttpURLConnection
        connection.requestMethod = method
        connection.connectTimeout = TIMEOUT_MS
        connection.readTimeout = TIMEOUT_MS
        if (body != null) {
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.outputStream.use { it.write(body.toByteArray()) }
        }
        return try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            code to (stream?.bufferedReader()?.use { it.readText() } ?: "")
        } finally {
            connection.disconnect()
        }
    }

    private const val RETRIES = 3
    private const val RETRY_DELAY_MS = 3_000L
    private val RETRYABLE_CODES = setOf(404, 408, 429, 500, 502, 503, 504)
}
