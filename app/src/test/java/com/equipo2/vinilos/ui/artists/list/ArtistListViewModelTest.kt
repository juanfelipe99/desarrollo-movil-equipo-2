package com.equipo2.vinilos.ui.artists.list

import androidx.lifecycle.SavedStateHandle
import com.equipo2.vinilos.support.FakeArtistRepository
import com.equipo2.vinilos.support.MainDispatcherRule
import com.equipo2.vinilos.support.band
import com.equipo2.vinilos.support.musician
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

class ArtistListViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val repository = FakeArtistRepository()

    private val queen = band(101, "Queen")
    private val ruben = musician(100, "Rubén Blades Bellido de Luna")
    private val adele = musician(2, "Adele Laurie Blue Adkins")
    private val angela = musician(9, "Ángela Aguilar")

    @Test
    fun alCrearse_muestraCargando_hastaQueResponda() {
        val viewModel = ArtistListViewModel(repository)

        assertEquals(UiState.Loading, viewModel.state.value)
        assertEquals(1, repository.calls)
    }

    @Test
    fun conArtistas_muestraMusicosYBandas_enOrdenAlfabeticoSinImportarTildes() {
        val viewModel = ArtistListViewModel(repository)

        repository.response.complete(Result.success(listOf(ruben, queen, angela, adele)))

        assertEquals(UiState.Success(listOf(adele, angela, queen, ruben)), viewModel.state.value)
    }

    @Test
    fun sinArtistas_muestraVacio() {
        val viewModel = ArtistListViewModel(repository)

        repository.response.complete(Result.success(emptyList()))

        assertEquals(UiState.Empty, viewModel.state.value)
    }

    @Test
    fun siFallaLaConsulta_muestraError() {
        val viewModel = ArtistListViewModel(repository)

        repository.response.complete(Result.failure(IOException("Sin conexión")))

        assertEquals(UiState.Error(ErrorReason.CONNECTION), viewModel.state.value)
    }

    @Test
    fun reintentar_vuelveACargar_yMuestraLosArtistas() {
        val viewModel = ArtistListViewModel(repository)
        repository.response.complete(Result.failure(IOException("Sin conexión")))
        repository.response = CompletableDeferred()

        viewModel.retry()
        assertEquals(UiState.Loading, viewModel.state.value)
        repository.response.complete(Result.success(listOf(queen)))

        assertEquals(UiState.Success(listOf(queen)), viewModel.state.value)
        assertEquals(2, repository.calls)
    }

    @Test
    fun reintentarMientrasCarga_noRepiteLaConsulta() {
        val viewModel = ArtistListViewModel(repository)

        viewModel.retry()

        assertEquals(1, repository.calls)
    }

    @Test
    fun porDefecto_elFiltroEsTodos() {
        val viewModel = ArtistListViewModel(repository)

        assertEquals(ArtistFilter.ALL, viewModel.filter.value)
    }

    @Test
    fun filtroMusicos_muestraSoloMusicos() {
        val viewModel = ArtistListViewModel(repository)
        repository.response.complete(Result.success(listOf(ruben, queen, adele)))

        viewModel.selectFilter(ArtistFilter.MUSICIANS)

        assertEquals(UiState.Success(listOf(adele, ruben)), viewModel.state.value)
    }

    @Test
    fun filtroBandas_muestraSoloBandas_yTodosVuelveAMostrarlosTodos() {
        val viewModel = ArtistListViewModel(repository)
        repository.response.complete(Result.success(listOf(ruben, queen)))

        viewModel.selectFilter(ArtistFilter.BANDS)
        assertEquals(UiState.Success(listOf(queen)), viewModel.state.value)

        viewModel.selectFilter(ArtistFilter.ALL)
        assertEquals(UiState.Success(listOf(queen, ruben)), viewModel.state.value)
    }

    @Test
    fun filtroSinArtistasDeEseTipo_muestraVacio() {
        val viewModel = ArtistListViewModel(repository)
        repository.response.complete(Result.success(listOf(ruben)))

        viewModel.selectFilter(ArtistFilter.BANDS)

        assertEquals(UiState.Empty, viewModel.state.value)
    }

    @Test
    fun elFiltroElegido_seConservaAlRecrearLaPantalla() {
        val savedState = SavedStateHandle()
        ArtistListViewModel(repository, savedState).selectFilter(ArtistFilter.BANDS)

        val recreated = ArtistListViewModel(repository, savedState)
        repository.response.complete(Result.success(listOf(ruben, queen)))

        assertEquals(ArtistFilter.BANDS, recreated.filter.value)
        assertEquals(UiState.Success(listOf(queen)), recreated.state.value)
    }

    @Test
    fun alDeslizarParaRecargar_conservaLosArtistasMientrasCarga_yLuegoMuestraLosNuevos() {
        val viewModel = ArtistListViewModel(repository)
        repository.response.complete(Result.success(listOf(ruben)))

        repository.response = CompletableDeferred()
        viewModel.refresh()

        assertEquals(UiState.Success(listOf(ruben)), viewModel.state.value)
        assertTrue(viewModel.pullRefresh.running.value)

        repository.response.complete(Result.success(listOf(ruben, queen)))

        assertEquals(UiState.Success(listOf(queen, ruben)), viewModel.state.value)
        assertFalse(viewModel.pullRefresh.running.value)
    }

    @Test
    fun siLaRecargaFalla_conservaLosArtistas_yAvisaElMotivo() = runTest {
        val viewModel = ArtistListViewModel(repository)
        repository.response.complete(Result.success(listOf(ruben)))

        repository.response = CompletableDeferred(Result.failure(IOException("Sin conexión")))
        viewModel.refresh()

        assertEquals(UiState.Success(listOf(ruben)), viewModel.state.value)
        assertEquals(ErrorReason.CONNECTION, viewModel.pullRefresh.failures.first())
    }
}
