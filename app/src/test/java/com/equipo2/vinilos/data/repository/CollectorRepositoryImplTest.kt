package com.equipo2.vinilos.data.repository

import com.equipo2.vinilos.data.model.AlbumStatus
import com.equipo2.vinilos.data.model.Collector
import com.equipo2.vinilos.data.model.CollectorAlbum
import com.equipo2.vinilos.data.model.CollectorComment
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

    @Test
    fun getCollectors_otraVez_usaLoGuardadoSinVolverALaApi() = runTest {
        server.respond("collectors", COLLECTORS_JSON)

        val first = repository.getCollectors().getOrThrow()
        val second = repository.getCollectors().getOrThrow()

        assertEquals(first, second)
        assertEquals(listOf("collectors"), server.requests)
    }

    @Test
    fun getCollectors_despuesDeClearCache_vuelveALaApi() = runTest {
        server.respond("collectors", COLLECTORS_JSON)

        repository.getCollectors()
        repository.clearCache()
        repository.getCollectors()

        assertEquals(listOf("collectors", "collectors"), server.requests)
    }

    // Detalle (HU06)

    private fun respondDetail() {
        server.respond("collectors/100", COLLECTOR_100_JSON)
        server.respond("collectors/100/albums", COLLECTOR_100_ALBUMS_JSON)
        server.respond("albums", COMMENTED_ALBUMS_JSON)
    }

    @Test
    fun getCollectorDetail_juntaElColeccionistaSusAlbumesYElAlbumDeCadaComentario() = runTest {
        respondDetail()

        val detail = repository.getCollectorDetail(100).getOrThrow()

        assertEquals(setOf("collectors/100", "collectors/100/albums", "albums"), server.requests.toSet())
        assertEquals(Collector(100, "Manolo Bellon", "manollo@caracol.com.co", albumCount = 2), detail.collector)
        assertEquals("3502457896", detail.telephone)
        assertEquals(listOf("Rubén Blades Bellido de Luna"), detail.favorites.map { it.name })
        assertEquals(
            listOf(
                CollectorAlbum(101, "A Day at the Races", "https://ejemplo.com/101.jpg", 40, AlbumStatus.INACTIVE),
                CollectorAlbum(100, "Buscando América", "https://ejemplo.com/100.jpg", 35, AlbumStatus.ACTIVE),
            ),
            detail.albums,
        )
        assertEquals(
            listOf(CollectorComment(100, 100, "Buscando América", 5, "The most relevant album of Ruben Blades")),
            detail.comments,
        )
    }

    @Test
    fun getCollectorDetail_otraVez_usaLoGuardadoSinVolverALaApi() = runTest {
        respondDetail()

        val first = repository.getCollectorDetail(100).getOrThrow()
        val second = repository.getCollectorDetail(100).getOrThrow()

        assertEquals(first, second)
        assertEquals(3, server.requests.size)
    }

    @Test
    fun getCollectorDetail_despuesDeClearCache_vuelveAPedirTodo() = runTest {
        respondDetail()

        repository.getCollectorDetail(100)
        repository.clearCache()
        repository.getCollectorDetail(100)

        assertEquals(6, server.requests.size)
        assertEquals(2, server.requests.count { it == "albums" })
    }

    @Test
    fun getCollectorDetail_siNoExiste_devuelveNoEncontrado() = runTest {
        // Sin configurar, FakeApiServer responde 404
        server.respond("albums", COMMENTED_ALBUMS_JSON)

        assertTrue(repository.getCollectorDetail(999).exceptionOrNull() is DataError.NotFound)
    }

    @Test
    fun getCollectorDetail_siFallanLosAlbumes_fallaElDetalle() = runTest {
        respondDetail()
        server.respond("collectors/100/albums", """{"message":"Internal error"}""", code = 500)

        val error = repository.getCollectorDetail(100).exceptionOrNull()

        assertTrue(error is DataError.Server)
    }

    @Test
    fun getCollectorDetail_siLaRespuestaNoSePuedeLeer_devuelveDatosInvalidos() = runTest {
        respondDetail()
        server.respond("collectors/100", """{"id": "no es un número"}""")

        assertTrue(repository.getCollectorDetail(100).exceptionOrNull() is DataError.InvalidData)
    }

    @Test
    fun getCollectorDetail_sinServidor_devuelveErrorDeRed() = runTest {
        val freePort = ServerSocket(0).use { it.localPort }
        val offline = repositoryFor("http://127.0.0.1:$freePort/")

        assertTrue(offline.getCollectorDetail(100).exceptionOrNull() is DataError.Network)
    }

    private companion object {
        const val COLLECTOR_100_JSON = """
            {"id": 100, "name": "Manolo Bellon", "telephone": "3502457896", "email": "manollo@caracol.com.co",
             "comments": [{"id": 100, "description": "The most relevant album of Ruben Blades", "rating": 5}],
             "favoritePerformers": [{"id": 100, "name": "Rubén Blades Bellido de Luna", "image": "https://ejemplo.com/r.jpg",
                                     "description": "Cantante panameño", "birthDate": "1948-07-16T00:00:00.000Z"}],
             "collectorAlbums": [{"id": 100, "price": 35, "status": "Active"}, {"id": 101, "price": 40, "status": "Inactive"}]}
        """

        const val COLLECTOR_100_ALBUMS_JSON = """
            [
              {"id": 100, "price": 35, "status": "Active",
               "album": {"id": 100, "name": "Buscando América", "cover": "https://ejemplo.com/100.jpg", "genre": "Salsa"}},
              {"id": 101, "price": 40, "status": "Inactive",
               "album": {"id": 101, "name": "A Day at the Races", "cover": "https://ejemplo.com/101.jpg", "genre": "Rock"}}
            ]
        """

        const val COMMENTED_ALBUMS_JSON = """
            [
              {"id": 100, "name": "Buscando América", "cover": "c", "releaseDate": "1984-08-01T00:00:00.000Z",
               "comments": [{"id": 100, "description": "The most relevant album of Ruben Blades", "rating": 5}]},
              {"id": 101, "name": "A Day at the Races", "comments": []}
            ]
        """

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
