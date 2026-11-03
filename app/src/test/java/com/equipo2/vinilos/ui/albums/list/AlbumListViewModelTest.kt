package com.equipo2.vinilos.ui.albums.list

import androidx.lifecycle.SavedStateHandle
import com.equipo2.vinilos.data.model.Album
import com.equipo2.vinilos.data.repository.DataError
import com.equipo2.vinilos.support.FakeAlbumRepository
import com.equipo2.vinilos.support.MainDispatcherRule
import com.equipo2.vinilos.ui.state.ErrorReason
import com.equipo2.vinilos.ui.state.UiState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class AlbumListViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val repository = FakeAlbumRepository()

    /** Hace que el catálogo espere hasta que la prueba lo complete. */
    private fun pendingResponse(): CompletableDeferred<Result<List<Album>>> {
        val response = CompletableDeferred<Result<List<Album>>>()
        repository.albums = { response.await() }
        return response
    }

    @Test
    fun alAbrir_quedaCargandoHastaQueLaApiResponda() {
        pendingResponse()

        val viewModel = AlbumListViewModel(repository)

        assertEquals(UiState.Loading, viewModel.state.value)
        assertEquals(1, repository.albumRequests)
    }

    @Test
    fun siHayAlbumes_muestraElCatalogo() {
        val albums = listOf(album(100), album(101))
        repository.albums = { Result.success(albums) }

        val viewModel = AlbumListViewModel(repository)

        assertEquals(UiState.Success(albums.map { AlbumItem(it, isNew = false) }), viewModel.state.value)
    }

    @Test
    fun siNoHayAlbumes_quedaVacio() {
        repository.albums = { Result.success(emptyList()) }

        val viewModel = AlbumListViewModel(repository)

        assertEquals(UiState.Empty, viewModel.state.value)
    }

    @Test
    fun siLaApiFalla_quedaEnError() {
        repository.albums = { Result.failure(DataError.Network()) }

        val viewModel = AlbumListViewModel(repository)

        assertEquals(UiState.Error(ErrorReason.CONNECTION), viewModel.state.value)
    }

    @Test
    fun cadaTipoDeFallo_quedaEnError_conSuMotivo() {
        val failures = mapOf(
            DataError.Network() to ErrorReason.CONNECTION,
            DataError.Timeout() to ErrorReason.CONNECTION,
            DataError.NotFound() to ErrorReason.SERVER,
            DataError.Request(400) to ErrorReason.SERVER,
            DataError.Server(500) to ErrorReason.SERVER,
            DataError.InvalidData() to ErrorReason.SERVER,
            DataError.Unknown() to ErrorReason.SERVER,
        )

        failures.forEach { (failure, reason) ->
            repository.albums = { Result.failure(failure) }

            val viewModel = AlbumListViewModel(repository)

            assertEquals("Con ${failure::class.simpleName}", UiState.Error(reason), viewModel.state.value)
        }
    }

    @Test
    fun alReintentarDesdeVacio_muestraLosAlbumesQueLlegaron() {
        repository.albums = { Result.success(emptyList()) }
        val viewModel = AlbumListViewModel(repository)

        repository.albums = { Result.success(listOf(album(100))) }
        viewModel.retry()

        assertEquals(UiState.Success(listOf(AlbumItem(album(100), isNew = false))), viewModel.state.value)
    }

    @Test
    fun alReintentar_vuelveACargarYMuestraElCatalogo() {
        repository.albums = { Result.failure(DataError.Server(500)) }
        val viewModel = AlbumListViewModel(repository)

        val response = pendingResponse()
        viewModel.retry()

        assertEquals(UiState.Loading, viewModel.state.value)
        assertEquals(2, repository.albumRequests)

        val albums = listOf(album(100))
        response.complete(Result.success(albums))

        assertEquals(UiState.Success(albums.map { AlbumItem(it, isNew = false) }), viewModel.state.value)
    }

    @Test
    fun siElReintentoTambienFalla_vuelveAError() {
        repository.albums = { Result.failure(DataError.Timeout()) }
        val viewModel = AlbumListViewModel(repository)

        viewModel.retry()

        assertEquals(UiState.Error(ErrorReason.CONNECTION), viewModel.state.value)
        assertEquals(2, repository.albumRequests)
    }

    @Test
    fun reintentarMientrasCarga_noRepiteLaPeticion() {
        pendingResponse()
        val viewModel = AlbumListViewModel(repository)

        viewModel.retry()
        viewModel.retry()

        assertEquals(1, repository.albumRequests)
    }

    @Test
    fun alCrearUnAlbum_recargaYLoMuestraPrimeroComoNuevo() {
        repository.albums = { Result.success(listOf(album(100), album(101))) }
        val viewModel = AlbumListViewModel(repository)

        repository.albums = { Result.success(listOf(album(100), album(101), album(102))) }
        viewModel.onAlbumCreated(102)

        assertEquals(2, repository.albumRequests)
        assertEquals(
            UiState.Success(
                listOf(
                    AlbumItem(album(102), isNew = true),
                    AlbumItem(album(100), isNew = false),
                    AlbumItem(album(101), isNew = false),
                ),
            ),
            viewModel.state.value,
        )
    }

    @Test
    fun conVariosAlbumesCreados_elMasRecienteVaPrimero() {
        repository.albums = { Result.success(listOf(album(100), album(101), album(102))) }
        val viewModel = AlbumListViewModel(repository)

        viewModel.onAlbumCreated(101)
        viewModel.onAlbumCreated(102)

        val items = (viewModel.state.value as UiState.Success).data
        assertEquals(listOf(102, 101, 100), items.map { it.album.id })
        assertEquals(listOf(true, true, false), items.map { it.isNew })
    }

    @Test
    fun alCrearMientrasCarga_lanzaUnaPeticionNuevaConElAlbum() {
        pendingResponse()
        val viewModel = AlbumListViewModel(repository)

        repository.albums = { Result.success(listOf(album(100))) }
        viewModel.onAlbumCreated(100)

        assertEquals(2, repository.albumRequests)
        assertEquals(UiState.Success(listOf(AlbumItem(album(100), isNew = true))), viewModel.state.value)
    }

    @Test
    fun losAlbumesNuevos_seConservanSiSeRecreaLaPantalla() {
        repository.albums = { Result.success(listOf(album(100), album(101))) }
        val savedState = SavedStateHandle()
        AlbumListViewModel(repository, savedState).onAlbumCreated(101)

        val recreated = AlbumListViewModel(repository, savedState)

        val items = (recreated.state.value as UiState.Success).data
        assertEquals(AlbumItem(album(101), isNew = true), items.first())
    }

    @Test
    fun alDeslizarParaRecargar_conservaLosAlbumesMientrasCarga_yLuegoMuestraLosNuevos() {
        repository.albums = { Result.success(listOf(album(100))) }
        val viewModel = AlbumListViewModel(repository)

        val response = pendingResponse()
        viewModel.refresh()

        assertEquals(UiState.Success(listOf(AlbumItem(album(100), isNew = false))), viewModel.state.value)
        assertTrue(viewModel.pullRefresh.running.value)

        response.complete(Result.success(listOf(album(100), album(101))))

        assertEquals(2, (viewModel.state.value as UiState.Success).data.size)
        assertFalse(viewModel.pullRefresh.running.value)
    }

    @Test
    fun siLaRecargaFalla_conservaLosAlbumes_yAvisaElMotivo() = runTest {
        repository.albums = { Result.success(listOf(album(100))) }
        val viewModel = AlbumListViewModel(repository)

        repository.albums = { Result.failure(DataError.Network()) }
        viewModel.refresh()

        assertEquals(UiState.Success(listOf(AlbumItem(album(100), isNew = false))), viewModel.state.value)
        assertEquals(ErrorReason.CONNECTION, viewModel.pullRefresh.failures.first())
        assertFalse(viewModel.pullRefresh.running.value)
    }

    @Test
    fun alDeslizarDesdeElError_vuelveACargarComoAlReintentar() {
        repository.albums = { Result.failure(DataError.Server(500)) }
        val viewModel = AlbumListViewModel(repository)

        pendingResponse()
        viewModel.refresh()

        assertEquals(UiState.Loading, viewModel.state.value)
        assertFalse(viewModel.pullRefresh.running.value)
    }

    private fun album(id: Int) = Album(
        id = id,
        name = "Buscando América",
        cover = "https://ejemplo.com/portada.jpg",
        releaseDate = LocalDate.of(1984, 8, 1),
        description = "Álbum de Rubén Blades.",
        genre = "Salsa",
        recordLabel = "Elektra",
    )
}
