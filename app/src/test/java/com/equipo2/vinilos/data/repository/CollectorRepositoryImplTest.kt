package com.equipo2.vinilos.data.repository

import com.equipo2.vinilos.data.model.Collector
import com.equipo2.vinilos.data.network.ApiClient
import com.equipo2.vinilos.data.network.collector.CollectorServiceAdapter
import com.equipo2.vinilos.support.FakeApiServer
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.ServerSocket

/** Prueba el repositorio con Retrofit y el adaptador reales contra un servidor local. */
class CollectorRepositoryImplTest {

    private val server = FakeApiServer()
    private val repository = repositoryFor(server.baseUrl)

    @After
    fun stopServer() = server.close()

    private fun repositoryFor(baseUrl: String) = CollectorRepositoryImpl(CollectorServiceAdapter(ApiClient(baseUrl)))

    @Test
    fun getCollectors_conColeccionistas_devuelveNombreCorreoYCantidadDeAlbumes() = runTest {
        server.respond("collectors", COLLECTORS_JSON)

        val collectors = repository.getCollectors().getOrThrow()

        assertEquals(listOf("collectors"), server.requests)
        assertEquals(
            listOf(
                Collector(id = 100, name = "Manolo Bellon", email = "manollo@caracol.com.co", albumCount = 2),
                Collector(id = 101, name = "Jaime Monsalve", email = "jmonsalve@rtvc.com.co", albumCount = 0),
            ),
            collectors,
        )
    }

    @Test
    fun getCollectors_sinColeccionistas_devuelveUnaListaVacia() = runTest {
        server.respond("collectors", "[]")

        assertEquals(emptyList<Collector>(), repository.getCollectors().getOrThrow())
    }

    @Test
    fun getCollectors_siElServidorFalla_devuelveErrorDelServidor() = runTest {
        server.respond("collectors", """{"message":"Internal error"}""", code = 500)

        val error = repository.getCollectors().exceptionOrNull()

        assertTrue(error is DataError.Server)
        assertEquals(500, (error as DataError.Server).code)
    }

    @Test
    fun getCollectors_siLaRespuestaNoEsUnaLista_devuelveDatosInvalidos() = runTest {
        server.respond("collectors", """{"collectors": "no es una lista"}""")

        assertTrue(repository.getCollectors().exceptionOrNull() is DataError.InvalidData)
    }

    @Test
    fun getCollectors_sinServidor_devuelveErrorDeRed() = runTest {
        // Un puerto que se libera justo antes, así nadie responde en él
        val freePort = ServerSocket(0).use { it.localPort }
        val offline = repositoryFor("http://127.0.0.1:$freePort/")

        assertTrue(offline.getCollectors().exceptionOrNull() is DataError.Network)
    }

    private companion object {
        // Así responde BackVynils: teléfono, comentarios e intérpretes favoritos llegan pero la lista no los usa
        const val COLLECTORS_JSON = """
            [
              {"id": 100, "name": "Manolo Bellon", "telephone": "3502457896", "email": "manollo@caracol.com.co",
               "comments": [{"id": 100, "description": "The most relevant album of Ruben Blades", "rating": 5}],
               "favoritePerformers": [{"id": 100, "name": "Rubén Blades Bellido de Luna", "image": "https://ejemplo.com/r.jpg",
                                       "description": "Cantante panameño", "birthDate": "1948-07-16T00:00:00.000Z"}],
               "collectorAlbums": [{"id": 100, "price": 35, "status": "Active"}, {"id": 101, "price": 25, "status": "Inactive"}]},
              {"id": 101, "name": "Jaime Monsalve", "telephone": "3012357936", "email": "jmonsalve@rtvc.com.co",
               "comments": [], "favoritePerformers": [], "collectorAlbums": []}
            ]
        """
    }
}
