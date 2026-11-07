package com.equipo2.vinilos.data.repository

import com.equipo2.vinilos.data.model.ArtistType
import com.equipo2.vinilos.data.network.ApiClient
import com.equipo2.vinilos.data.network.artist.ArtistServiceAdapter
import com.equipo2.vinilos.support.FakeApiServer
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.ServerSocket
import java.time.LocalDate

/** Prueba el detalle de artista (HU04) con Retrofit y el adaptador reales contra un servidor local. */
class ArtistRepositoryImplTest {

    private val server = FakeApiServer()
    private val repository = repositoryFor(server.baseUrl)

    @After
    fun stopServer() = server.close()

    private fun repositoryFor(baseUrl: String) = ArtistRepositoryImpl(ArtistServiceAdapter(ApiClient(baseUrl)))

    @Test
    fun detalleDeUnMusico_pideElMusicoLasBandasYLosPremios() = runTest {
        server.respond("musicians/10", BRIAN_MAY_JSON)
        server.respond("bands", BANDS_JSON)
        server.respond("prizes", PRIZES_JSON)

        repository.getArtistDetail(10, ArtistType.MUSICIAN).getOrThrow()

        assertEquals(setOf("musicians/10", "bands", "prizes"), server.requests.toSet())
    }

    @Test
    fun detalleDeUnMusico_traeSuFechaDescripcionYLaBandaALaQuePertenece() = runTest {
        server.respond("musicians/10", BRIAN_MAY_JSON)
        server.respond("bands", BANDS_JSON)
        server.respond("prizes", PRIZES_JSON)

        val detail = repository.getArtistDetail(10, ArtistType.MUSICIAN).getOrThrow()

        assertEquals("Brian May", detail.artist.name)
        assertEquals(ArtistType.MUSICIAN, detail.artist.type)
        assertEquals(LocalDate.of(1947, 7, 19), detail.artist.date)
        assertEquals("Guitarrista de Queen.", detail.description)
        assertEquals(101, detail.band?.id)
        assertEquals("Queen", detail.band?.name)
        assertEquals(ArtistType.BAND, detail.band?.type)
    }

    @Test
    fun detalleDeUnMusicoSinBanda_noTraeBanda() = runTest {
        server.respond("musicians/100", RUBEN_JSON)
        server.respond("bands", BANDS_JSON)
        server.respond("prizes", PRIZES_JSON)

        val detail = repository.getArtistDetail(100, ArtistType.MUSICIAN).getOrThrow()

        assertNull(detail.band)
        assertEquals(emptyList<Any>(), detail.members)
    }

    @Test
    fun detalleDeUnMusico_nombraSusPremiosConLaOrganizacionYLaFecha() = runTest {
        server.respond("musicians/100", RUBEN_JSON)
        server.respond("bands", BANDS_JSON)
        server.respond("prizes", PRIZES_JSON)

        val prize = repository.getArtistDetail(100, ArtistType.MUSICIAN).getOrThrow().prizes.single()

        assertEquals("Grammy Award", prize.name)
        assertEquals("National Academy of Recording Arts & Sciences", prize.organization)
        assertEquals(LocalDate.of(1978, 12, 10), prize.date)
    }

    @Test
    fun detalleDeUnaBanda_traeSusIntegrantesEnOrdenAlfabetico_ySinPedirLasBandas() = runTest {
        server.respond("bands/101", QUEEN_JSON)
        server.respond("prizes", PRIZES_JSON)

        val detail = repository.getArtistDetail(101, ArtistType.BAND).getOrThrow()

        assertEquals(setOf("bands/101", "prizes"), server.requests.toSet())
        assertEquals(LocalDate.of(1970, 1, 1), detail.artist.date)
        assertEquals(listOf("Brian May", "Freddie Mercury"), detail.members.map { it.name })
        assertTrue(detail.members.all { it.type == ArtistType.MUSICIAN })
        assertNull(detail.band)
    }

    @Test
    fun detalleDeUnaBanda_traeSusAlbumesPorFecha_ySusPremiosPorFecha() = runTest {
        server.respond("bands/101", QUEEN_JSON)
        server.respond("prizes", PRIZES_JSON)

        val detail = repository.getArtistDetail(101, ArtistType.BAND).getOrThrow()

        assertEquals(listOf("A Night at the Opera", "A Day at the Races"), detail.albums.map { it.name })
        assertEquals("Rock", detail.albums.first().genre)
        assertEquals(listOf("Premios lo Nuestro", "Grammy Award"), detail.prizes.map { it.name })
    }

    @Test
    fun artistaSinAlbumesNiPremios_devuelveListasVacias() = runTest {
        server.respond("bands/200", """{"id": 200, "name": "Seis del Solar", "creationDate": "1982-01-01T00:00:00.000Z"}""")
        server.respond("prizes", PRIZES_JSON)

        val detail = repository.getArtistDetail(200, ArtistType.BAND).getOrThrow()

        assertEquals("", detail.description)
        assertEquals(emptyList<Any>(), detail.members)
        assertEquals(emptyList<Any>(), detail.albums)
        assertEquals(emptyList<Any>(), detail.prizes)
    }

    @Test
    fun premioQueNoApareceEnPrizes_noSeMuestra() = runTest {
        server.respond("bands/101", QUEEN_JSON)
        server.respond("prizes", "[]")

        assertEquals(emptyList<Any>(), repository.getArtistDetail(101, ArtistType.BAND).getOrThrow().prizes)
    }

    @Test
    fun siElArtistaNoExiste_devuelveNoEncontrado() = runTest {
        server.respond("bands", BANDS_JSON)
        server.respond("prizes", PRIZES_JSON)

        assertTrue(repository.getArtistDetail(999, ArtistType.MUSICIAN).exceptionOrNull() is DataError.NotFound)
    }

    @Test
    fun siFallanLosPremios_fallaElDetalle() = runTest {
        server.respond("bands/101", QUEEN_JSON)
        server.respond("prizes", """{"message":"Internal error"}""", code = 500)

        val error = repository.getArtistDetail(101, ArtistType.BAND).exceptionOrNull()

        assertTrue(error is DataError.Server)
        assertEquals(500, (error as DataError.Server).code)
    }

    @Test
    fun siFallanLasBandas_fallaElDetalleDelMusico() = runTest {
        server.respond("musicians/10", BRIAN_MAY_JSON)
        server.respond("bands", """{"message":"Internal error"}""", code = 500)
        server.respond("prizes", PRIZES_JSON)

        assertTrue(repository.getArtistDetail(10, ArtistType.MUSICIAN).exceptionOrNull() is DataError.Server)
    }

    @Test
    fun sinServidor_devuelveErrorDeRed() = runTest {
        // Un puerto que se libera justo antes, así nadie responde en él
        val freePort = ServerSocket(0).use { it.localPort }
        val offline = repositoryFor("http://127.0.0.1:$freePort/")

        assertTrue(offline.getArtistDetail(101, ArtistType.BAND).exceptionOrNull() is DataError.Network)
    }

    @Test
    fun abrirOtraVezUnArtista_usaLoGuardadoSinVolverALaApi() = runTest {
        server.respond("bands/101", QUEEN_JSON)
        server.respond("prizes", PRIZES_JSON)

        val first = repository.getArtistDetail(101, ArtistType.BAND).getOrThrow()
        val second = repository.getArtistDetail(101, ArtistType.BAND).getOrThrow()

        assertEquals(first, second)
        assertEquals(listOf("bands/101", "prizes"), server.requests.sorted())
    }

    @Test
    fun deUnaBandaAUnIntegrante_noVuelveAPedirLosPremios() = runTest {
        server.respond("bands/101", QUEEN_JSON)
        server.respond("musicians/10", BRIAN_MAY_JSON)
        server.respond("bands", BANDS_JSON)
        server.respond("prizes", PRIZES_JSON)

        repository.getArtistDetail(101, ArtistType.BAND).getOrThrow()
        repository.getArtistDetail(10, ArtistType.MUSICIAN).getOrThrow()

        assertEquals(1, server.requests.count { it == "prizes" })
    }

    @Test
    fun despuesDelListado_elDetalleDeUnMusicoNoVuelveAPedirLasBandas() = runTest {
        server.respond("musicians", "[]")
        server.respond("bands", BANDS_JSON)
        server.respond("musicians/10", BRIAN_MAY_JSON)
        server.respond("prizes", PRIZES_JSON)

        repository.getArtists().getOrThrow()
        val detail = repository.getArtistDetail(10, ArtistType.MUSICIAN).getOrThrow()

        assertEquals(1, server.requests.count { it == "bands" })
        assertEquals("Queen", detail.band?.name)
    }

    @Test
    fun despuesDeClearCache_elDetalleVuelveAPedirTodoALaApi() = runTest {
        server.respond("musicians/10", BRIAN_MAY_JSON)
        server.respond("bands", BANDS_JSON)
        server.respond("prizes", PRIZES_JSON)

        repository.getArtistDetail(10, ArtistType.MUSICIAN).getOrThrow()
        repository.clearCache()
        repository.getArtistDetail(10, ArtistType.MUSICIAN).getOrThrow()

        assertEquals(listOf("bands", "bands", "musicians/10", "musicians/10", "prizes", "prizes"), server.requests.sorted())
    }

    @Test
    fun siFallanLosPremios_laSiguienteVezVuelveAPedirlos() = runTest {
        server.respond("bands/101", QUEEN_JSON)
        server.respond("prizes", """{"message":"Internal error"}""", code = 500)
        repository.getArtistDetail(101, ArtistType.BAND)
        server.respond("prizes", PRIZES_JSON)

        assertTrue(repository.getArtistDetail(101, ArtistType.BAND).isSuccess)
        assertEquals(2, server.requests.count { it == "prizes" })
    }

    private companion object {
        // Así responde BackVynils, con campos que la app no usa
        const val BRIAN_MAY_JSON = """
            {"id": 10, "name": "Brian May", "image": "https://ejemplo.com/brian.jpg",
             "description": "Guitarrista de Queen.", "birthDate": "1947-07-19T00:00:00.000Z",
             "albums": [], "performerPrizes": []}
        """

        const val RUBEN_JSON = """
            {"id": 100, "name": "Rubén Blades Bellido de Luna", "image": "https://ejemplo.com/ruben.jpg",
             "description": "Cantante panameño.", "birthDate": "1948-07-16T00:00:00.000Z",
             "albums": [{"id": 100, "name": "Buscando América", "cover": "https://ejemplo.com/a.jpg",
                         "releaseDate": "1984-08-01T00:00:00.000Z", "description": "Salsa",
                         "genre": "Salsa", "recordLabel": "Elektra"}],
             "performerPrizes": [{"id": 100, "premiationDate": "1978-12-10T00:00:00.000Z"}]}
        """

        const val QUEEN_JSON = """
            {"id": 101, "name": "Queen", "image": "https://ejemplo.com/queen.jpg",
             "description": "Banda británica de rock.", "creationDate": "1970-01-01T00:00:00.000Z",
             "albums": [
               {"id": 103, "name": "A Day at the Races", "cover": "https://ejemplo.com/c.jpg",
                "releaseDate": "1976-12-10T00:00:00.000Z", "description": "Quinto álbum",
                "genre": "Rock", "recordLabel": "EMI"},
               {"id": 102, "name": "A Night at the Opera", "cover": "https://ejemplo.com/b.jpg",
                "releaseDate": "1975-11-21T00:00:00.000Z", "description": "Cuarto álbum",
                "genre": "Rock", "recordLabel": "EMI"}],
             "musicians": [
               {"id": 9, "name": "Freddie Mercury", "image": "", "description": "Voz",
                "birthDate": "1946-09-05T00:00:00.000Z"},
               {"id": 10, "name": "Brian May", "image": "", "description": "Guitarra",
                "birthDate": "1947-07-19T00:00:00.000Z"}],
             "performerPrizes": [
               {"id": 12, "premiationDate": "2001-03-19T00:00:00.000Z"},
               {"id": 101, "premiationDate": "1980-12-10T00:00:00.000Z"}]}
        """

        const val BANDS_JSON = """
            [{"id": 101, "name": "Queen", "image": "https://ejemplo.com/queen.jpg",
              "description": "Banda británica de rock.", "creationDate": "1970-01-01T00:00:00.000Z",
              "musicians": [{"id": 9, "name": "Freddie Mercury"}, {"id": 10, "name": "Brian May"}]},
             {"id": 200, "name": "Seis del Solar", "creationDate": "1982-01-01T00:00:00.000Z", "musicians": []}]
        """

        const val PRIZES_JSON = """
            [{"id": 100, "name": "Grammy Award", "organization": "National Academy of Recording Arts & Sciences",
              "description": "Premio anual", "performerPrizes": [{"id": 100, "premiationDate": "1978-12-10T00:00:00.000Z"},
                                                                {"id": 12, "premiationDate": "2001-03-19T00:00:00.000Z"}]},
             {"id": 101, "name": "Premios lo Nuestro", "organization": "Univisión",
              "description": "Música latina", "performerPrizes": [{"id": 101, "premiationDate": "1980-12-10T00:00:00.000Z"}]}]
        """
    }
}
