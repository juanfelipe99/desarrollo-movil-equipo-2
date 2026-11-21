package com.equipo2.vinilos.data.repository

import com.equipo2.vinilos.data.model.Album
import com.equipo2.vinilos.data.model.ArtistType
import com.equipo2.vinilos.data.network.ApiClient
import com.equipo2.vinilos.data.network.artist.ArtistAlbumServiceAdapter
import com.equipo2.vinilos.support.FakeApiServer
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.ServerSocket
import java.time.LocalDate

/** Prueba el repositorio con Retrofit y el adaptador reales contra un servidor local. */
class ArtistAlbumRepositoryImplTest {

    private val server = FakeApiServer()
    private val repository = repositoryFor(server.baseUrl)

    @After
    fun stopServer() = server.close()

    private fun repositoryFor(baseUrl: String, onAlbumsChanged: () -> Unit = {}) =
        ArtistAlbumRepositoryImpl(ArtistAlbumServiceAdapter(ApiClient(baseUrl)), onAlbumsChanged)

    // Listado

    @Test
    fun getAlbumCandidates_deUnMusico_pideElCatalogoYElMusico_yDejaAlFinalLosQueYaTiene() = runTest {
        server.respond("albums", ALBUMS_JSON)
        server.respond("musicians/100", MUSICIAN_JSON)

        val candidates = repository.getAlbumCandidates(100, ArtistType.MUSICIAN).getOrThrow()

        assertEquals(setOf("albums", "musicians/100"), server.requests.toSet())
        assertEquals(listOf("Abbey Road", "Buscando América"), candidates.map { it.album.name })
        assertEquals(listOf(false, true), candidates.map { it.alreadyAdded })
    }

    @Test
    fun getAlbumCandidates_deUnaBanda_pideLaBanda() = runTest {
        server.respond("albums", ALBUMS_JSON)
        server.respond("bands/15", BAND_JSON)

        val candidates = repository.getAlbumCandidates(15, ArtistType.BAND).getOrThrow()

        assertEquals(setOf("albums", "bands/15"), server.requests.toSet())
        assertEquals(listOf("Buscando América", "Abbey Road"), candidates.map { it.album.name })
        assertEquals(listOf(false, true), candidates.map { it.alreadyAdded })
    }

    @Test
    fun getAlbumCandidates_traeLosIntérpretesDeCadaAlbum() = runTest {
        server.respond("albums", ALBUMS_JSON)
        server.respond("bands/15", BAND_JSON)

        val buscando = repository.getAlbumCandidates(15, ArtistType.BAND).getOrThrow().first { it.album.id == 100 }

        assertEquals(listOf("Rubén Blades Bellido de Luna"), buscando.album.performers.map { it.name })
        assertEquals(ArtistType.MUSICIAN, buscando.album.performers.single().type)
    }

    @Test
    fun getAlbumCandidates_siElArtistaNoExiste_devuelveNoEncontrado() = runTest {
        server.respond("albums", ALBUMS_JSON)

        assertTrue(repository.getAlbumCandidates(999, ArtistType.MUSICIAN).exceptionOrNull() is DataError.NotFound)
    }

    @Test
    fun getAlbumCandidates_siFallaElCatalogo_fallaElListado() = runTest {
        server.respond("albums", """{"message":"Internal error"}""", code = 500)
        server.respond("musicians/100", MUSICIAN_JSON)

        assertTrue(repository.getAlbumCandidates(100, ArtistType.MUSICIAN).exceptionOrNull() is DataError.Server)
    }

    @Test
    fun getAlbumCandidates_conDatosInvalidos_devuelveDatosInvalidos() = runTest {
        server.respond("albums", """[{"id": 1, "name": "Sin portada"}]""")
        server.respond("musicians/100", MUSICIAN_JSON)

        assertTrue(repository.getAlbumCandidates(100, ArtistType.MUSICIAN).exceptionOrNull() is DataError.InvalidData)
    }

    @Test
    fun getAlbumCandidates_sinServidor_devuelveErrorDeRed() = runTest {
        // Un puerto que se libera justo antes, así nadie responde en él
        val freePort = ServerSocket(0).use { it.localPort }
        val offline = repositoryFor("http://127.0.0.1:$freePort/")

        assertTrue(offline.getAlbumCandidates(100, ArtistType.MUSICIAN).exceptionOrNull() is DataError.Network)
    }

    // Envío

    @Test
    fun addAlbum_aUnMusico_usaLaRutaDeLosMusicos() = runTest {
        server.respond("musicians/100/albums/173", MUSICIAN_JSON)

        val result = repository.addAlbum(100, ArtistType.MUSICIAN, 173)

        assertTrue(result.isSuccess)
        assertEquals(listOf("musicians/100/albums/173"), server.requests)
    }

    @Test
    fun addAlbum_aUnaBanda_usaLaRutaDeLasBandas() = runTest {
        server.respond("bands/15/albums/100", BAND_JSON)

        val result = repository.addAlbum(15, ArtistType.BAND, 100)

        assertTrue(result.isSuccess)
        assertEquals(listOf("bands/15/albums/100"), server.requests)
    }

    @Test
    fun addAlbum_siElAlbumOElArtistaNoExisten_devuelveNoEncontrado() = runTest {
        server.respond(
            "musicians/999/albums/173",
            """{"statusCode":404,"message":"The musician with the given id was not found"}""",
            code = 404,
        )

        assertTrue(repository.addAlbum(999, ArtistType.MUSICIAN, 173).exceptionOrNull() is DataError.NotFound)
    }

    @Test
    fun addAlbum_sinServidor_devuelveErrorDeRed() = runTest {
        val freePort = ServerSocket(0).use { it.localPort }
        val offline = repositoryFor("http://127.0.0.1:$freePort/")

        assertTrue(offline.addAlbum(100, ArtistType.MUSICIAN, 173).exceptionOrNull() is DataError.Network)
    }

    @Test
    fun addAlbum_alAgregarlo_avisaQueLosAlbumesCambiaron() = runTest {
        var changes = 0
        val repository = repositoryFor(server.baseUrl) { changes++ }
        server.respond("bands/15/albums/100", BAND_JSON)

        repository.addAlbum(15, ArtistType.BAND, 100)

        assertEquals(1, changes)
    }

    @Test
    fun addAlbum_siFalla_noAvisaNingunCambio() = runTest {
        var changes = 0
        val repository = repositoryFor(server.baseUrl) { changes++ }
        server.respond("bands/15/albums/100", """{"message":"Internal error"}""", code = 500)

        repository.addAlbum(15, ArtistType.BAND, 100)

        assertEquals(0, changes)
    }

    // Catálogo de la app

    @Test
    fun getAlbumCandidates_conElCatalogoDeLaApp_noPideLosAlbumes_ySoloPideElArtista() = runTest {
        server.respond("musicians/100", MUSICIAN_JSON)
        val catalog = listOf(album(1, "Buscando América"), album(173, "Abbey Road"))
        val adapter = ArtistAlbumServiceAdapter(ApiClient(server.baseUrl), catalog = { catalog })

        val candidates = ArtistAlbumRepositoryImpl(adapter).getAlbumCandidates(100, ArtistType.MUSICIAN).getOrThrow()

        assertEquals(listOf("musicians/100"), server.requests)
        assertEquals(listOf("Abbey Road", "Buscando América"), candidates.map { it.album.name })
    }

    @Test
    fun getAlbumCandidates_siElCatalogoDeLaAppFalla_fallaElListadoConSuError() = runTest {
        server.respond("musicians/100", MUSICIAN_JSON)
        val adapter = ArtistAlbumServiceAdapter(ApiClient(server.baseUrl), catalog = { throw DataError.Network() })

        val error = ArtistAlbumRepositoryImpl(adapter).getAlbumCandidates(100, ArtistType.MUSICIAN).exceptionOrNull()

        assertTrue(error is DataError.Network)
    }

    private fun album(id: Int, name: String) = Album(
        id = id, name = name, cover = "", releaseDate = LocalDate.of(1984, 1, 1), description = "", genre = "Salsa",
        recordLabel = "Elektra",
    )

    private companion object {
        // Así responde BackVynils: cada álbum trae sus intérpretes, y los músicos se distinguen por la fecha de nacimiento
        const val ALBUMS_JSON = """
            [
              {"id": 100, "name": "Buscando América", "cover": "https://ejemplo.com/b.jpg",
               "releaseDate": "1984-08-01T00:00:00.000Z", "description": "Salsa", "genre": "Salsa", "recordLabel": "Elektra",
               "tracks": [], "comments": [],
               "performers": [{"id": 100, "name": "Rubén Blades Bellido de Luna", "image": "https://ejemplo.com/r.jpg",
                               "description": "Cantante", "birthDate": "1948-07-16T00:00:00.000Z"}]},
              {"id": 173, "name": "Abbey Road", "cover": "https://ejemplo.com/a.jpg",
               "releaseDate": "1969-09-26T00:00:00.000Z", "description": "Rock", "genre": "Rock", "recordLabel": "EMI",
               "tracks": [], "comments": [],
               "performers": [{"id": 15, "name": "The Beatles", "image": "https://ejemplo.com/t.jpg",
                               "description": "Banda", "creationDate": "1960-01-01T00:00:00.000Z"}]}
            ]
        """

        // El detalle del artista trae sus álbumes sin intérpretes; de ellos solo se usa el id
        const val MUSICIAN_JSON = """
            {"id": 100, "name": "Rubén Blades Bellido de Luna", "image": "https://ejemplo.com/r.jpg", "description": "Cantante",
             "birthDate": "1948-07-16T00:00:00.000Z", "performerPrizes": [],
             "albums": [{"id": 100, "name": "Buscando América", "cover": "https://ejemplo.com/b.jpg",
                         "releaseDate": "1984-08-01T00:00:00.000Z", "description": "Salsa", "genre": "Salsa",
                         "recordLabel": "Elektra"}]}
        """

        const val BAND_JSON = """
            {"id": 15, "name": "The Beatles", "image": "https://ejemplo.com/t.jpg", "description": "Banda",
             "creationDate": "1960-01-01T00:00:00.000Z", "musicians": [], "performerPrizes": [],
             "albums": [{"id": 173, "name": "Abbey Road", "cover": "https://ejemplo.com/a.jpg",
                         "releaseDate": "1969-09-26T00:00:00.000Z", "description": "Rock", "genre": "Rock",
                         "recordLabel": "EMI"}]}
        """
    }
}
