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

    /** Álbumes con una descripción más larga que length, que en el detalle se corta con «Ver más». */
    fun albumNamesWithDescriptionLongerThan(length: Int): List<String> {
        val albums = JSONArray(request("albums", "GET"))
        return List(albums.length()) { albums.getJSONObject(it) }
            .filter { it.getString("description").length > length }
            .map { it.getString("name") }
    }

    /** Lo que el detalle de un artista (HU04) muestra en sus contadores. */
    data class ArtistSummary(val name: String, val description: String, val albums: Int, val prizes: Int)

    /** Bandas con al menos un integrante, con los nombres de sus integrantes. */
    fun bandMembers(): Map<ArtistSummary, List<String>> {
        val bands = JSONArray(request("bands", "GET"))
        return List(bands.length()) { bands.getJSONObject(it) }
            .associate { band ->
                val members = band.optJSONArray("musicians") ?: JSONArray()
                band.toSummary() to List(members.length()) { members.getJSONObject(it).getString("name") }
            }
            .filterValues { it.isNotEmpty() }
    }

    /** Músicos que no pertenecen a ninguna banda. */
    fun musiciansWithoutBand(): List<ArtistSummary> {
        val inBand = bandMembers().values.flatten().toSet()
        return summaries("musicians").filter { it.name !in inBand }
    }

    /** Músicos y bandas cuya descripción se corta con «Ver más». */
    fun artistsWithDescriptionLongerThan(length: Int): List<ArtistSummary> =
        (summaries("musicians") + summaries("bands")).filter { it.description.length > length }

    private fun summaries(path: String): List<ArtistSummary> {
        val items = JSONArray(request(path, "GET"))
        return List(items.length()) { items.getJSONObject(it).toSummary() }
    }

    private fun JSONObject.toSummary() = ArtistSummary(
        name = getString("name"),
        description = optString("description"),
        albums = optJSONArray("albums")?.length() ?: 0,
        prizes = optJSONArray("performerPrizes")?.length() ?: 0,
    )

    fun musicianNames(): List<String> = names("musicians")

    fun bandNames(): List<String> = names("bands")

    private fun names(path: String): List<String> {
        val items = JSONArray(request(path, "GET"))
        return List(items.length()) { items.getJSONObject(it).getString("name") }
    }

    /** Crea un álbum de prueba sin tracks y devuelve su id. */
    fun createAlbum(name: String): Int {
        val album = JSONObject()
            .put("name", name)
            .put("cover", "https://ejemplo.com/portada.jpg")
            .put("releaseDate", "2020-01-01")
            .put("description", "Álbum creado por una prueba E2E.")
            .put("genre", "Rock")
            .put("recordLabel", "EMI")
        return JSONObject(request("albums", "POST", album.toString())).getInt("id")
    }

    /** Nombres de los tracks del álbum, en orden. */
    fun trackNames(albumId: Int): List<String> {
        val tracks = JSONObject(request("albums/$albumId", "GET")).getJSONArray("tracks")
        return List(tracks.length()) { tracks.getJSONObject(it).getString("name") }
    }

    /** Borra los álbumes con este prefijo para no dejar datos de prueba. */
    fun deleteAlbumsStartingWith(prefix: String) {
        val albums = JSONArray(request("albums", "GET"))
        for (i in 0 until albums.length()) {
            val album = albums.getJSONObject(i)
            if (!album.getString("name").startsWith(prefix)) continue

            // La API no borra un álbum que tenga tracks, así que primero se borran ellos
            val id = album.getInt("id")
            val tracks = album.getJSONArray("tracks")
            for (t in 0 until tracks.length()) request("albums/$id/tracks/${tracks.getJSONObject(t).getInt("id")}", "DELETE")
            request("albums/$id", "DELETE")
        }
    }

    /** Crea una banda de prueba sin integrantes y devuelve su id (HU12). */
    fun createBand(name: String): Int {
        val band = JSONObject()
            .put("name", name)
            .put("image", "https://ejemplo.com/banda.jpg")
            .put("description", "Banda creada por una prueba E2E.")
            .put("creationDate", "2000-01-01")
        return JSONObject(request("bands", "POST", band.toString())).getInt("id")
    }

    /** Crea un músico de prueba sin banda y devuelve su id (HU12). */
    fun createMusician(name: String): Int {
        val musician = JSONObject()
            .put("name", name)
            .put("image", "https://ejemplo.com/musico.jpg")
            .put("description", "Músico creado por una prueba E2E.")
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

    /** Borra los músicos y las bandas con este prefijo para no dejar datos de prueba. */
    fun deleteArtistsStartingWith(prefix: String) {
        // Primero los músicos, para que ninguna banda se borre con integrantes
        for (path in listOf("musicians", "bands")) {
            val items = JSONArray(request(path, "GET"))
            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                if (item.getString("name").startsWith(prefix)) request("$path/${item.getInt("id")}", "DELETE")
            }
        }
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
