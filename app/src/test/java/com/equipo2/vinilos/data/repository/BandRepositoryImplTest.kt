package com.equipo2.vinilos.data.repository

import com.equipo2.vinilos.data.model.MusicianStatus
import com.equipo2.vinilos.data.network.ApiClient
import com.equipo2.vinilos.data.network.artist.BandServiceAdapter
import com.equipo2.vinilos.support.FakeApiServer
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.ServerSocket
import java.time.LocalDate

/** Prueba el repositorio con Retrofit y el adaptador reales contra un servidor local. */
class BandRepositoryImplTest {

    private val server = FakeApiServer()
    private val repository = repositoryFor(server.baseUrl)

    @After
    fun stopServer() = server.close()

    private fun repositoryFor(baseUrl: String) = BandRepositoryImpl(BandServiceAdapter(ApiClient(baseUrl)))

    @Test
    fun getMusicianCandidates_pideMusicosYBandas_yCalculaElEstado() = runTest {
        server.respond("musicians", MUSICIANS_JSON)
        server.respond("bands", BANDS_JSON)

        val candidates = repository.getMusicianCandidates(101).getOrThrow()

        assertEquals(setOf("musicians", "bands"), server.requests.toSet())
        assertEquals(
            listOf(
                "Celia Cruz" to MusicianStatus.NoBand,
                "Gustavo Cerati" to MusicianStatus.InOtherBand(18, "Soda Stereo"),
                "Freddie Mercury" to MusicianStatus.AlreadyMember,
            ),
            candidates.map { it.musician.name to it.status },
        )
        assertEquals(LocalDate.of(1946, 9, 5), candidates.last().musician.date)
    }

    @Test
    fun getMusicianCandidates_siLaBandaNoExiste_devuelveNoEncontrado() = runTest {
        server.respond("musicians", MUSICIANS_JSON)
        server.respond("bands", BANDS_JSON)

        assertTrue(repository.getMusicianCandidates(999).exceptionOrNull() is DataError.NotFound)
    }

    @Test
    fun getMusicianCandidates_siFallanLasBandas_fallaElListado() = runTest {
        server.respond("musicians", MUSICIANS_JSON)
        server.respond("bands", """{"message":"Internal error"}""", code = 500)

        assertTrue(repository.getMusicianCandidates(101).exceptionOrNull() is DataError.Server)
    }

    @Test
    fun getMusicianCandidates_siFallanLosMusicos_fallaElListado() = runTest {
        server.respond("musicians", """{"message":"Internal error"}""", code = 500)
        server.respond("bands", BANDS_JSON)

        assertTrue(repository.getMusicianCandidates(101).exceptionOrNull() is DataError.Server)
    }

    @Test
    fun getMusicianCandidates_conDatosInvalidos_devuelveDatosInvalidos() = runTest {
        server.respond("musicians", """[{"id": 3, "name": "Sin fecha"}]""")
        server.respond("bands", BANDS_JSON)

        assertTrue(repository.getMusicianCandidates(101).exceptionOrNull() is DataError.InvalidData)
    }

    @Test
    fun getMusicianCandidates_sinServidor_devuelveErrorDeRed() = runTest {
        // Un puerto que se libera justo antes, así nadie responde en él
        val freePort = ServerSocket(0).use { it.localPort }
        val offline = repositoryFor("http://127.0.0.1:$freePort/")

        assertTrue(offline.getMusicianCandidates(101).exceptionOrNull() is DataError.Network)
    }

    private companion object {
        // Así responde BackVynils: los músicos no dicen su banda, cada banda trae sus integrantes
        const val MUSICIANS_JSON = """
            [
              {"id": 9, "name": "Freddie Mercury", "image": "https://ejemplo.com/f.jpg", "description": "Voz de Queen",
               "birthDate": "1946-09-05T00:00:00.000Z", "albums": [], "performerPrizes": []},
              {"id": 11, "name": "Gustavo Cerati", "image": "https://ejemplo.com/g.jpg", "description": "Voz de Soda Stereo",
               "birthDate": "1959-08-11T00:00:00.000Z", "albums": [], "performerPrizes": []},
              {"id": 3, "name": "Celia Cruz", "image": "https://ejemplo.com/c.jpg", "description": "Reina de la salsa",
               "birthDate": "1925-10-21T00:00:00.000Z", "albums": [], "performerPrizes": []}
            ]
        """

        const val BANDS_JSON = """
            [
              {"id": 101, "name": "Queen", "image": "https://ejemplo.com/q.jpg", "description": "Banda británica",
               "creationDate": "1970-01-01T00:00:00.000Z", "albums": [], "performerPrizes": [],
               "musicians": [{"id": 9, "name": "Freddie Mercury", "image": "https://ejemplo.com/f.jpg",
                              "description": "Voz de Queen", "birthDate": "1946-09-05T00:00:00.000Z"}]},
              {"id": 18, "name": "Soda Stereo", "image": "https://ejemplo.com/s.jpg", "description": "Banda argentina",
               "creationDate": "1982-01-01T00:00:00.000Z", "albums": [], "performerPrizes": [],
               "musicians": [{"id": 11, "name": "Gustavo Cerati", "image": "https://ejemplo.com/g.jpg",
                              "description": "Voz de Soda Stereo", "birthDate": "1959-08-11T00:00:00.000Z"}]}
            ]
        """
    }
}
