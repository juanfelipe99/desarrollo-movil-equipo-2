package com.equipo2.vinilos.ui.collectors.list

import com.equipo2.vinilos.support.FakeCollectorRepository
import com.equipo2.vinilos.support.MainDispatcherRule
import com.equipo2.vinilos.support.collector
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
import java.io.IOException

class CollectorListViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val repository = FakeCollectorRepository()

    private val manolo = collector(100, "Manolo Bellon", albumCount = 3)
    private val jaime = collector(101, "Jaime Monsalve", albumCount = 1)
    private val angela = collector(102, "Ángela Restrepo")

    @Test
    fun alCrearse_muestraCargando_hastaQueResponda() {
        val viewModel = CollectorListViewModel(repository)

        assertEquals(UiState.Loading, viewModel.state.value)
        assertEquals(0, viewModel.count.value)
        assertEquals(1, repository.calls)
    }

    @Test
    fun conColeccionistas_losMuestraEnOrdenAlfabeticoSinImportarTildes() {
        val viewModel = CollectorListViewModel(repository)

        repository.response.complete(Result.success(listOf(manolo, jaime, angela)))

        assertEquals(UiState.Success(listOf(angela, jaime, manolo)), viewModel.state.value)
    }

    @Test
    fun elTotal_esLaCantidadDeColeccionistas() {
        val viewModel = CollectorListViewModel(repository)

        repository.response.complete(Result.success(listOf(manolo, jaime, angela)))

        assertEquals(3, viewModel.count.value)
    }

    @Test
    fun sinColeccionistas_muestraVacio_conElTotalEnCero() {
        val viewModel = CollectorListViewModel(repository)

        repository.response.complete(Result.success(emptyList()))

        assertEquals(UiState.Empty, viewModel.state.value)
        assertEquals(0, viewModel.count.value)
    }

    @Test
    fun siFallaLaConexion_muestraErrorDeConexion_conElTotalEnCero() {
        val viewModel = CollectorListViewModel(repository)

        repository.response.complete(Result.failure(IOException("Sin conexión")))

        assertEquals(UiState.Error(ErrorReason.CONNECTION), viewModel.state.value)
        assertEquals(0, viewModel.count.value)
    }

    @Test
    fun reintentar_vuelveACargar_yMuestraLosColeccionistas() {
        val viewModel = CollectorListViewModel(repository)
        repository.response.complete(Result.failure(IOException("Sin conexión")))
        repository.response = CompletableDeferred()

        viewModel.retry()
        assertEquals(UiState.Loading, viewModel.state.value)
        repository.response.complete(Result.success(listOf(manolo)))

        assertEquals(UiState.Success(listOf(manolo)), viewModel.state.value)
        assertEquals(1, viewModel.count.value)
        assertEquals(2, repository.calls)
    }

    @Test
    fun reintentarMientrasCarga_noRepiteLaConsulta() {
        val viewModel = CollectorListViewModel(repository)

        viewModel.retry()

        assertEquals(1, repository.calls)
    }

    @Test
    fun alDeslizarParaRecargar_conservaLosColeccionistasMientrasCarga_yLuegoMuestraLosNuevos() {
        val viewModel = CollectorListViewModel(repository)
        repository.response.complete(Result.success(listOf(manolo)))

        repository.response = CompletableDeferred()
        viewModel.refresh()

        assertEquals(UiState.Success(listOf(manolo)), viewModel.state.value)
        assertTrue(viewModel.pullRefresh.running.value)

        repository.response.complete(Result.success(listOf(manolo, jaime)))

        assertEquals(UiState.Success(listOf(jaime, manolo)), viewModel.state.value)
        assertEquals(2, viewModel.count.value)
        assertFalse(viewModel.pullRefresh.running.value)
    }

    @Test
    fun siLaRecargaFalla_conservaLosColeccionistas_yAvisaElMotivo() = runTest {
        val viewModel = CollectorListViewModel(repository)
        repository.response.complete(Result.success(listOf(manolo)))

        repository.response = CompletableDeferred(Result.failure(IOException("Sin conexión")))
        viewModel.refresh()

        assertEquals(UiState.Success(listOf(manolo)), viewModel.state.value)
        assertEquals(ErrorReason.CONNECTION, viewModel.pullRefresh.failures.first())
    }
}
