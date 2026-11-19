package com.equipo2.vinilos.data.repository

import com.equipo2.vinilos.data.model.ArtistPrize
import com.equipo2.vinilos.data.model.ArtistType
import com.equipo2.vinilos.data.model.NewPrize
import com.equipo2.vinilos.data.model.Prize
import com.equipo2.vinilos.data.network.ApiClient
import com.equipo2.vinilos.data.network.artist.PrizeServiceAdapter
import com.equipo2.vinilos.support.FakeApiServer
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.ServerSocket
import java.time.LocalDate

/** Prueba el repositorio con Retrofit y el adaptador reales contra un servidor local. */
class PrizeRepositoryImplTest {

    private val server = FakeApiServer()
    private val repository = repositoryFor(server.baseUrl)

    private val grammy = Prize(100, "Grammy Award", "National Academy of Recording Arts & Sciences")
    private val date = LocalDate.of(2000, 9, 13)

    @After
    fun stopServer() = server.close()

    private fun repositoryFor(baseUrl: String, onPrizesChanged: () -> Unit = {}) =
        PrizeRepositoryImpl(PrizeServiceAdapter(ApiClient(baseUrl)), onPrizesChanged)

    // Listado

    @Test
    fun getPrizeCandidates_deUnMusico_pideLosPremiosYElMusico_yMarcaLosQueYaRecibio() = runTest {
        server.respond("prizes", PRIZES_JSON)
        server.respond("musicians/100", MUSICIAN_JSON)

        val candidates = repository.getPrizeCandidates(100, ArtistType.MUSICIAN).getOrThrow()

        assertEquals(setOf("prizes", "musicians/100"), server.requests.toSet())
        assertEquals(listOf("Grammy Award", "Premio Gardel"), candidates.map { it.prize.name })
        assertEquals(listOf(LocalDate.of(1978, 12, 10)), candidates.first().receivedDates)
        assertTrue(candidates.last().receivedDates.isEmpty())
    }

    @Test
    fun getPrizeCandidates_deUnaBanda_pideLaBanda() = runTest {
        server.respond("prizes", PRIZES_JSON)
        server.respond("bands/101", BAND_JSON)

        val candidates = repository.getPrizeCandidates(101, ArtistType.BAND).getOrThrow()

        assertEquals(setOf("prizes", "bands/101"), server.requests.toSet())
        assertEquals(listOf(LocalDate.of(2005, 5, 5)), candidates.last().receivedDates)
    }

    @Test
    fun getPrizeCandidates_siElArtistaNoExiste_devuelveNoEncontrado() = runTest {
        server.respond("prizes", PRIZES_JSON)

        assertTrue(repository.getPrizeCandidates(999, ArtistType.MUSICIAN).exceptionOrNull() is DataError.NotFound)
    }

    @Test
    fun getPrizeCandidates_siFallanLosPremios_fallaElListado() = runTest {
        server.respond("prizes", """{"message":"Internal error"}""", code = 500)
        server.respond("musicians/100", MUSICIAN_JSON)

        assertTrue(repository.getPrizeCandidates(100, ArtistType.MUSICIAN).exceptionOrNull() is DataError.Server)
    }

    @Test
    fun getPrizeCandidates_conDatosInvalidos_devuelveDatosInvalidos() = runTest {
        server.respond("prizes", """[{"id": 1, "name": "Sin organización"}]""")
        server.respond("musicians/100", MUSICIAN_JSON)

        assertTrue(repository.getPrizeCandidates(100, ArtistType.MUSICIAN).exceptionOrNull() is DataError.InvalidData)
    }

    @Test
    fun getPrizeCandidates_sinServidor_devuelveErrorDeRed() = runTest {
        // Un puerto que se libera justo antes, así nadie responde en él
        val freePort = ServerSocket(0).use { it.localPort }
        val offline = repositoryFor("http://127.0.0.1:$freePort/")

        assertTrue(offline.getPrizeCandidates(100, ArtistType.MUSICIAN).exceptionOrNull() is DataError.Network)
    }

    // Envío

    @Test
    fun addPrize_aUnMusico_enviaLaFechaComoLaEsperaLaApi_yDevuelveLaEntrega() = runTest {
        server.respond("prizes/100/musicians/7", DELIVERY_JSON)

        val prize = repository.addPrize(7, ArtistType.MUSICIAN, grammy, date).getOrThrow()

        assertEquals("""{"premiationDate":"2000-09-13T00:00:00.000Z"}""", server.bodies["prizes/100/musicians/7"])
        assertEquals(ArtistPrize(37, "Grammy Award", "National Academy of Recording Arts & Sciences", date), prize)
    }

    @Test
    fun addPrize_aUnaBanda_usaLaRutaDeLasBandas() = runTest {
        server.respond("prizes/100/bands/101", DELIVERY_JSON)

        repository.addPrize(101, ArtistType.BAND, grammy, date).getOrThrow()

        assertEquals(listOf("prizes/100/bands/101"), server.requests)
    }

    @Test
    fun addPrize_siElPremioOElArtistaNoExisten_devuelveNoEncontrado() = runTest {
        server.respond(
            "prizes/100/musicians/999",
            """{"statusCode":404,"message":"The performer with the given id was not found"}""",
            code = 404,
        )

        assertTrue(repository.addPrize(999, ArtistType.MUSICIAN, grammy, date).exceptionOrNull() is DataError.NotFound)
    }

    @Test
    fun addPrize_siLaApiNoAceptaLaFecha_devuelveLaSolicitudRechazada() = runTest {
        server.respond(
            "prizes/100/musicians/7",
            """{"statusCode":400,"message":"ValidationError: \"premiationDate\" must be a valid date"}""",
            code = 400,
        )

        val error = repository.addPrize(7, ArtistType.MUSICIAN, grammy, date).exceptionOrNull()

        assertEquals(400, (error as DataError.Request).code)
    }

    @Test
    fun addPrize_sinServidor_devuelveErrorDeRed() = runTest {
        val freePort = ServerSocket(0).use { it.localPort }
        val offline = repositoryFor("http://127.0.0.1:$freePort/")

        assertTrue(offline.addPrize(7, ArtistType.MUSICIAN, grammy, date).exceptionOrNull() is DataError.Network)
    }

    @Test
    fun addPrize_alAgregarlo_avisaQueLosPremiosCambiaron() = runTest {
        var changes = 0
        val repository = repositoryFor(server.baseUrl) { changes++ }
        server.respond("prizes/100/musicians/7", DELIVERY_JSON)

        repository.addPrize(7, ArtistType.MUSICIAN, grammy, date)

        assertEquals(1, changes)
    }

    @Test
    fun addPrize_siFalla_noAvisaNingunCambio() = runTest {
        var changes = 0
        val repository = repositoryFor(server.baseUrl) { changes++ }
        server.respond("prizes/100/musicians/7", """{"message":"Internal error"}""", code = 500)

        repository.addPrize(7, ArtistType.MUSICIAN, grammy, date)

        assertEquals(0, changes)
    }

    // Creación (HU13)

    private val gardel = NewPrize("Premio Gardel", "CAPIF", "Reconoce a los mejores artistas de música en español")

    @Test
    fun createPrize_enviaLosTresCampos_yDevuelveElPremioConSuId() = runTest {
        server.respond("prizes", CREATED_PRIZE_JSON)

        val prize = repository.createPrize(gardel).getOrThrow()

        assertEquals(listOf("prizes"), server.requests)
        assertEquals(
            """{"name":"Premio Gardel","organization":"CAPIF","description":"Reconoce a los mejores artistas de música en español"}""",
            server.bodies["prizes"],
        )
        assertEquals(Prize(205, "Premio Gardel", "CAPIF"), prize)
    }

    @Test
    fun createPrize_siLaApiRechazaLosDatos_devuelveLaSolicitudRechazada() = runTest {
        // Así responde BackVynils cuando la validación con Joi falla
        server.respond(
            "prizes",
            """{"statusCode":400,"message":"ValidationError: \"name\" is not allowed to be empty"}""",
            code = 400,
        )

        val error = repository.createPrize(gardel).exceptionOrNull()

        assertEquals(400, (error as DataError.Request).code)
    }

    @Test
    fun createPrize_siElServidorFalla_devuelveErrorDelServidor() = runTest {
        server.respond("prizes", """{"message":"Internal error"}""", code = 500)

        assertTrue(repository.createPrize(gardel).exceptionOrNull() is DataError.Server)
    }

    @Test
    fun createPrize_sinServidor_devuelveErrorDeRed() = runTest {
        val freePort = ServerSocket(0).use { it.localPort }
        val offline = repositoryFor("http://127.0.0.1:$freePort/")

        assertTrue(offline.createPrize(gardel).exceptionOrNull() is DataError.Network)
    }

    @Test
    fun createPrize_noAvisaQueLosPremiosCambiaron() = runTest {
        var changes = 0
        val repository = repositoryFor(server.baseUrl) { changes++ }
        server.respond("prizes", CREATED_PRIZE_JSON)

        repository.createPrize(gardel).getOrThrow()

        assertEquals(0, changes)
    }

    private companion object {
        // El premio guardado vuelve sin la lista de entregas
        const val CREATED_PRIZE_JSON = """
            {"name": "Premio Gardel", "organization": "CAPIF",
             "description": "Reconoce a los mejores artistas de música en español", "id": 205}
        """

        // Así responde BackVynils: cada premio trae sus entregas, sin decir a quién, y cada artista trae las suyas
        const val PRIZES_JSON = """
            [
              {"id": 8, "organization": "CAPIF", "name": "Premio Gardel", "description": "Premio argentino",
               "performerPrizes": [{"id": 31, "premiationDate": "2005-05-05T00:00:00.000Z"}]},
              {"id": 100, "organization": "National Academy of Recording Arts & Sciences", "name": "Grammy Award",
               "description": "Premio de la academia",
               "performerPrizes": [{"id": 100, "premiationDate": "1978-12-10T00:00:00.000Z"},
                                   {"id": 19, "premiationDate": "2001-02-21T00:00:00.000Z"}]}
            ]
        """

        const val MUSICIAN_JSON = """
            {"id": 100, "name": "Rubén Blades Bellido de Luna", "image": "https://ejemplo.com/r.jpg", "description": "Cantante",
             "birthDate": "1948-07-16T00:00:00.000Z", "albums": [],
             "performerPrizes": [{"id": 100, "premiationDate": "1978-12-10T00:00:00.000Z"}]}
        """

        const val BAND_JSON = """
            {"id": 101, "name": "Queen", "image": "https://ejemplo.com/q.jpg", "description": "Banda británica",
             "creationDate": "1970-01-01T00:00:00.000Z", "albums": [], "musicians": [],
             "performerPrizes": [{"id": 31, "premiationDate": "2005-05-05T00:00:00.000Z"}]}
        """

        // La entrega llega con el premio y el artista completos, de los que la app solo usa el id de la entrega
        const val DELIVERY_JSON = """
            {"premiationDate": "2000-09-13T00:00:00.000Z",
             "prize": {"id": 100, "organization": "National Academy of Recording Arts & Sciences", "name": "Grammy Award",
                       "description": "Premio de la academia"},
             "performer": {"id": 7, "name": "Prueba", "image": "https://ejemplo.com/p.jpg", "description": "Prueba"},
             "id": 37}
        """
    }
}
