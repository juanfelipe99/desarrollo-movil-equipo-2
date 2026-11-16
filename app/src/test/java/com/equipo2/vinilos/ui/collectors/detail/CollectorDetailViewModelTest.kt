package com.equipo2.vinilos.ui.collectors.detail

import com.equipo2.vinilos.data.model.AlbumStatus
import com.equipo2.vinilos.data.model.CollectorAlbum
import com.equipo2.vinilos.data.model.CollectorComment
import com.equipo2.vinilos.data.model.CollectorDetail
import com.equipo2.vinilos.data.repository.DataError
import com.equipo2.vinilos.support.FakeCollectorRepository
import com.equipo2.vinilos.support.MainDispatcherRule
import com.equipo2.vinilos.support.band
import com.equipo2.vinilos.support.collector
import com.equipo2.vinilos.ui.state.ErrorReason
import com.equipo2.vinilos.ui.state.UiState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CollectorDetailViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val repository = FakeCollectorRepository()

    private val jaime = CollectorDetail(
        collector = collector(101, "Jaime Monsalve", albumCount = 1),
        telephone = "3012357936",
        favorites = listOf(band(101, "Queen")),
        albums = listOf(CollectorAlbum(102, "A Night at the Opera", "", 25, AlbumStatus.ACTIVE)),
        comments = listOf(CollectorComment(101, 102, "A Night at the Opera", 5, "I love this album of Queen")),
    )

    private fun viewModel() = CollectorDetailViewModel(repository, collectorId = 101)

    @Test
    fun alAbrir_pideElColeccionista_yQuedaCargandoHastaQueLaApiResponda() {
        repository.detail = { CompletableDeferred<Result<CollectorDetail>>().await() }

        val viewModel = viewModel()

        assertEquals(UiState.Loading, viewModel.state.value)
        assertEquals(listOf(101), repository.detailRequests)
    }

    @Test
    fun siLaApiResponde_muestraElColeccionista() {
        repository.detail = { Result.success(jaime) }

        assertEquals(UiState.Success(jaime), viewModel().state.value)
    }

    @Test
    fun sinFavoritosAlbumesNiComentarios_igualMuestraElColeccionista() {
        val empty = jaime.copy(favorites = emptyList(), albums = emptyList(), comments = emptyList())
        repository.detail = { Result.success(empty) }

        // Los vacíos se muestran en cada sección, la pantalla no queda en vacío
        assertEquals(UiState.Success(empty), viewModel().state.value)
    }

    @Test
    fun siNoHayConexion_quedaEnErrorDeConexion() {
        repository.detail = { Result.failure(DataError.Network()) }

        assertEquals(UiState.Error(ErrorReason.CONNECTION), viewModel().state.value)
    }

    @Test
    fun siLaApiTardaDemasiado_quedaEnErrorDeConexion() {
        repository.detail = { Result.failure(DataError.Timeout()) }

        assertEquals(UiState.Error(ErrorReason.CONNECTION), viewModel().state.value)
    }

    @Test
    fun siElServidorFalla_quedaEnErrorDelServidor() {
        repository.detail = { Result.failure(DataError.Server(500)) }

        assertEquals(UiState.Error(ErrorReason.SERVER), viewModel().state.value)
    }

    @Test
    fun alReintentar_vuelveAPedirlo_yMuestraElColeccionista() {
        repository.detail = { Result.failure(DataError.Network()) }
        val viewModel = viewModel()

        repository.detail = { Result.success(jaime) }
        viewModel.retry()

        assertEquals(UiState.Success(jaime), viewModel.state.value)
        assertEquals(listOf(101, 101), repository.detailRequests)
    }

    @Test
    fun reintentarMientrasCarga_noRepiteLaPeticion() {
        repository.detail = { CompletableDeferred<Result<CollectorDetail>>().await() }
        val viewModel = viewModel()

        viewModel.retry()

        assertEquals(1, repository.detailRequests.size)
    }

    @Test
    fun alDeslizarParaRecargar_olvidaLoGuardado_yMuestraLosDatosNuevos() {
        repository.detail = { Result.success(jaime) }
        val viewModel = viewModel()

        val updated = jaime.copy(comments = emptyList())
        repository.detail = { Result.success(updated) }
        viewModel.refresh()

        assertEquals(UiState.Success(updated), viewModel.state.value)
        assertEquals(1, repository.cacheClears)
        assertEquals(2, repository.detailRequests.size)
    }

    @Test
    fun siLaRecargaFalla_conservaElColeccionista_yAvisaElMotivo() = runTest {
        repository.detail = { Result.success(jaime) }
        val viewModel = viewModel()

        repository.detail = { Result.failure(DataError.Network()) }
        viewModel.refresh()

        assertEquals(UiState.Success(jaime), viewModel.state.value)
        assertEquals(ErrorReason.CONNECTION, viewModel.pullRefresh.failures.first())
    }

    @Test
    fun recargarDesdeElError_vuelveACargarDesdeCero() {
        repository.detail = { Result.failure(DataError.Network()) }
        val viewModel = viewModel()

        repository.detail = { CompletableDeferred<Result<CollectorDetail>>().await() }
        viewModel.refresh()

        assertEquals(UiState.Loading, viewModel.state.value)
    }
}
