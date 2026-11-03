package com.equipo2.vinilos.data.repository

import com.equipo2.vinilos.data.model.NewTrack
import com.equipo2.vinilos.data.model.Track
import com.equipo2.vinilos.data.network.ApiClient
import com.equipo2.vinilos.data.network.album.AlbumServiceAdapter
import com.equipo2.vinilos.support.FakeApiServer
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.ServerSocket
import java.time.LocalDate

/** Prueba el repositorio con Retrofit y el adaptador reales contra un servidor local. */
class AlbumRepositoryImplTest {

    private val server = FakeApiServer()
    private val repository = repositoryFor(server.baseUrl)

    @After
    fun stopServer() = server.close()

    private fun repositoryFor(baseUrl: String) = AlbumRepositoryImpl(AlbumServiceAdapter(ApiClient(baseUrl)))

    @Test
    fun getAlbums_conAlbumes_devuelveElCatalogoConvertido() = runTest {
        server.respond("albums", ALBUMS_JSON)

        val albums = repository.getAlbums().getOrThrow()

        assertEquals(listOf("albums"), server.requests)
        assertEquals(listOf(100, 101, 102), albums.map { it.id })
        assertEquals("Buscando América", albums[0].name)
        assertEquals(LocalDate.of(1984, 8, 1), albums[0].releaseDate)
        assertEquals("Rubén Blades Bellido de Luna", albums[0].performers.single().name)
        assertEquals("Decisiones", albums[0].tracks.single().name)
        assertEquals("Poeta del pueblo", albums[1].name)
    }

    @Test
    fun getAlbums_conMusicosYBandas_leeCadaInterprete() = runTest {
        server.respond("albums", ALBUMS_JSON)

        val albums = repository.getAlbums().getOrThrow()

        // Los músicos traen birthDate y las bandas creationDate, y ninguno de los dos debe hacer fallar la lectura
        assertEquals("Rubén Blades Bellido de Luna", albums[0].performers.single().name)
        assertEquals("Queen", albums[2].performers.single().name)
    }

    @Test
    fun getAlbums_sinAlbumes_devuelveUnaListaVacia() = runTest {
        server.respond("albums", "[]")

        assertEquals(emptyList<Any>(), repository.getAlbums().getOrThrow())
    }

    @Test
    fun getAlbums_siElServidorFalla_devuelveErrorDelServidor() = runTest {
        server.respond("albums", """{"message":"Internal error"}""", code = 500)

        val error = repository.getAlbums().exceptionOrNull()

        assertTrue(error is DataError.Server)
        assertEquals(500, (error as DataError.Server).code)
    }

    @Test
    fun getAlbums_siLaRespuestaNoEsUnaLista_devuelveDatosInvalidos() = runTest {
        server.respond("albums", """{"albums": "no es una lista"}""")

        assertTrue(repository.getAlbums().exceptionOrNull() is DataError.InvalidData)
    }

    @Test
    fun getAlbums_siFaltaUnCampo_devuelveDatosInvalidos() = runTest {
        server.respond("albums", """[{"id": 100, "name": "Sin portada"}]""")

        assertTrue(repository.getAlbums().exceptionOrNull() is DataError.InvalidData)
    }

    @Test
    fun getAlbums_sinServidor_devuelveErrorDeRed() = runTest {
        // Un puerto que se libera justo antes, así nadie responde en él
        val freePort = ServerSocket(0).use { it.localPort }
        val offline = repositoryFor("http://127.0.0.1:$freePort/")

        assertTrue(offline.getAlbums().exceptionOrNull() is DataError.Network)
    }

    @Test
    fun getAlbumById_conElAlbum_loDevuelveConLosAutoresDeLosComentarios() = runTest {
        server.respond("albums/100", ALBUM_WITH_COMMENT_JSON)
        server.respond("collectors", """[{"name": "Manolo Bellon", "comments": [{"id": 7}]}]""")

        val album = repository.getAlbumById(100).getOrThrow()

        assertEquals("Buscando América", album.name)
        assertEquals("Manolo Bellon", album.comments.single().author)
    }

    @Test
    fun getAlbumById_siNoExiste_devuelveNoEncontrado() = runTest {
        server.respond("collectors", "[]")

        assertTrue(repository.getAlbumById(999).exceptionOrNull() is DataError.NotFound)
    }

    @Test
    fun addTrack_enviaNombreYDuracion_yDevuelveElTrackCreado() = runTest {
        server.respond("albums/100/tracks", TRACK_CREATED_JSON)

        val track = repository.addTrack(100, NewTrack(name = "Todos vuelven", duration = "4:56")).getOrThrow()

        assertEquals(listOf("albums/100/tracks"), server.requests)
        val sent = Json.parseToJsonElement(server.bodies.getValue("albums/100/tracks")).jsonObject
        assertEquals("Todos vuelven", sent.getValue("name").jsonPrimitive.content)
        assertEquals("4:56", sent.getValue("duration").jsonPrimitive.content)
        assertEquals(Track(id = 45, name = "Todos vuelven", duration = "4:56"), track)
    }

    @Test
    fun addTrack_siElAlbumNoExiste_devuelveNoEncontrado() = runTest {
        val error = repository.addTrack(999, NewTrack(name = "Todos vuelven", duration = "4:56")).exceptionOrNull()

        assertTrue(error is DataError.NotFound)
    }

    @Test
    fun addTrack_siElServidorFalla_devuelveErrorDelServidor() = runTest {
        server.respond("albums/100/tracks", """{"message":"Internal error"}""", code = 500)

        val error = repository.addTrack(100, NewTrack(name = "Todos vuelven", duration = "4:56")).exceptionOrNull()

        assertTrue(error is DataError.Server)
    }

    private companion object {
        // Así responde BackVynils: el track con su id y el álbum al que quedó asociado
        const val TRACK_CREATED_JSON = """
            {"name": "Todos vuelven", "duration": "4:56",
             "album": {"id": 100, "name": "Buscando América", "cover": "https://ejemplo.com/a.jpg",
                       "releaseDate": "1984-08-01T00:00:00.000Z", "description": "Rubén Blades",
                       "genre": "Salsa", "recordLabel": "Elektra"},
             "id": 45}
        """

        const val ALBUMS_JSON = """
            [
              {"id": 100, "name": "Buscando América", "cover": "https://ejemplo.com/a.jpg",
               "releaseDate": "1984-08-01T00:00:00.000Z", "description": "Rubén Blades",
               "genre": "Salsa", "recordLabel": "Elektra",
               "tracks": [{"id": 1, "name": "Decisiones", "duration": "5:05"}],
               "performers": [{"id": 1, "name": "Rubén Blades Bellido de Luna", "image": "https://ejemplo.com/r.jpg",
                               "description": "Cantante panameño", "birthDate": "1948-07-16T00:00:00.000Z"}],
               "comments": []},
              {"id": 101, "name": "Poeta del pueblo", "cover": "https://ejemplo.com/b.jpg",
               "releaseDate": "1984-08-01T00:00:00.000Z", "description": "Recopilación",
               "genre": "Salsa", "recordLabel": "Elektra"},
              {"id": 102, "name": "A Night at the Opera", "cover": "https://ejemplo.com/c.jpg",
               "releaseDate": "1975-11-21T00:00:00.000Z", "description": "Queen",
               "genre": "Rock", "recordLabel": "EMI",
               "performers": [{"id": 101, "name": "Queen", "image": "https://ejemplo.com/q.jpg",
                               "description": "Banda británica", "creationDate": "1970-01-01T00:00:00.000Z"}]}
            ]
        """

        const val ALBUM_WITH_COMMENT_JSON = """
            {"id": 100, "name": "Buscando América", "cover": "https://ejemplo.com/a.jpg",
             "releaseDate": "1984-08-01T00:00:00.000Z", "description": "Rubén Blades",
             "genre": "Salsa", "recordLabel": "Elektra",
             "comments": [{"id": 7, "description": "Excelente", "rating": 5}]}
        """
    }
}
