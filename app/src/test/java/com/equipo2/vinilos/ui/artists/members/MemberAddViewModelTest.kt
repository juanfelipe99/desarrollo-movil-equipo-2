package com.equipo2.vinilos.ui.artists.members

import androidx.lifecycle.SavedStateHandle
import com.equipo2.vinilos.data.model.MusicianCandidate
import com.equipo2.vinilos.data.model.MusicianStatus
import com.equipo2.vinilos.data.repository.DataError
import com.equipo2.vinilos.support.FakeBandRepository
import com.equipo2.vinilos.support.MainDispatcherRule
import com.equipo2.vinilos.support.candidate
import com.equipo2.vinilos.ui.state.ErrorReason
import com.equipo2.vinilos.ui.state.UiState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MemberAddViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val repository = FakeBandRepository()

    private val celia = candidate(3, "Celia Cruz")
    private val ruben = candidate(100, "Rubén Blades")
    private val cerati = candidate(11, "Gustavo Cerati", MusicianStatus.InOtherBand(18, "Soda Stereo"))
    private val freddie = candidate(9, "Freddie Mercury", MusicianStatus.AlreadyMember)
    private val all = listOf(celia, ruben, cerati, freddie)

    private fun viewModelWith(
        candidates: List<MusicianCandidate> = all,
        savedState: SavedStateHandle = SavedStateHandle(),
    ): MemberAddViewModel {
        repository.candidates = { Result.success(candidates) }
        return MemberAddViewModel(repository, bandId = 101, savedState)
    }

    // Carga

    @Test
    fun alAbrir_muestraLaCargaHastaQueLlegaElListado() {
        val response = CompletableDeferred<Result<List<MusicianCandidate>>>()
        repository.candidates = { response.await() }
        val viewModel = MemberAddViewModel(repository, bandId = 101)

        assertEquals(UiState.Loading, viewModel.state.value)
        assertEquals(listOf(101), repository.candidateRequests)

        response.complete(Result.success(all))
        assertEquals(UiState.Success(all), viewModel.state.value)
    }

    @Test
    fun siNoHayMusicosQueAgregar_quedaVacioAunqueEstenLosIntegrantes() {
        val viewModel = viewModelWith(listOf(freddie))

        assertEquals(UiState.Empty, viewModel.state.value)
    }

    @Test
    fun sinConexion_muestraElError_yReintentarVuelveACargar() {
        repository.candidates = { Result.failure(DataError.Network()) }
        val viewModel = MemberAddViewModel(repository, bandId = 101)

        assertEquals(UiState.Error(ErrorReason.CONNECTION), viewModel.state.value)

        repository.candidates = { Result.success(all) }
        viewModel.retry()

        assertEquals(UiState.Success(all), viewModel.state.value)
        assertEquals(2, repository.candidateRequests.size)
    }

    @Test
    fun siLaBandaNoExiste_loIndica_enLugarDelErrorDeCarga() {
        repository.candidates = { Result.failure(DataError.NotFound()) }
        val viewModel = MemberAddViewModel(repository, bandId = 101)

        assertTrue(viewModel.bandMissing.value)
        assertTrue(viewModel.state.value is UiState.Error)
    }

    @Test
    fun unErrorDeConexion_noDiceQueLaBandaNoExista() {
        repository.candidates = { Result.failure(DataError.Network()) }
        val viewModel = MemberAddViewModel(repository, bandId = 101)

        assertFalse(viewModel.bandMissing.value)
    }

    // Búsqueda

    @Test
    fun buscar_filtraPorNombreSinImportarTildesNiMayusculas() {
        val viewModel = viewModelWith()

        viewModel.search("  RUBEN ")

        assertEquals(UiState.Success(listOf(ruben)), viewModel.state.value)
    }

    @Test
    fun siLaBusquedaNoEncuentraANadie_esUnaListaVaciaYNoElEstadoVacio() {
        val viewModel = viewModelWith()

        viewModel.search("Shakira")

        assertEquals(UiState.Success(emptyList<MusicianCandidate>()), viewModel.state.value)
    }

    @Test
    fun laBusquedaYLaSeleccion_seConservanAlGirar() {
        val savedState = SavedStateHandle()
        viewModelWith(savedState = savedState).apply {
            search("cel")
            select(celia)
        }

        val recreated = viewModelWith(savedState = savedState)

        assertEquals("cel", recreated.query.value)
        assertEquals(celia, recreated.selected.value)
    }

    // Selección

    @Test
    fun elegir_marcaElMusico_yLaBusquedaNoLoSuelta() {
        val viewModel = viewModelWith()

        viewModel.select(ruben)
        viewModel.search("celia")

        assertEquals(ruben, viewModel.selected.value)
    }

    @Test
    fun unIntegranteActual_noSePuedeElegir() {
        val viewModel = viewModelWith()

        viewModel.select(freddie)

        assertNull(viewModel.selected.value)
    }

    // Agregar

    @Test
    fun agregarSinElegir_pideElegirUnMusico_yNoLlamaALaApi() {
        val viewModel = viewModelWith()

        viewModel.add()

        assertTrue(viewModel.missingSelection.value)
        assertTrue(repository.added.isEmpty())

        viewModel.select(celia)
        assertFalse(viewModel.missingSelection.value)
    }

    @Test
    fun agregarUnMusicoSinBanda_loEnvia_yQuedaEnviandoHastaQueLaApiResponda() {
        val viewModel = viewModelWith()
        viewModel.select(celia)

        viewModel.add()

        assertEquals(listOf(101 to 3), repository.added)
        assertTrue(viewModel.isSubmitting.value)
    }

    @Test
    fun mientrasSeEnvia_noSeEnviaDosVecesNiSeCambiaLaSeleccion() {
        val viewModel = viewModelWith()
        viewModel.select(celia)
        viewModel.add()

        viewModel.add()
        viewModel.confirmAdd()
        viewModel.select(ruben)

        assertEquals(listOf(101 to 3), repository.added)
        assertEquals(celia, viewModel.selected.value)
    }

    @Test
    fun siLaApiLoAgrega_avisaConElMusico() = runTest {
        val viewModel = viewModelWith()
        viewModel.select(celia)
        viewModel.add()

        repository.addResponse.complete(Result.success(Unit))

        assertEquals(MemberAddEvent.Added(celia.musician), viewModel.events.first())
        assertFalse(viewModel.isSubmitting.value)
    }

    @Test
    fun unMusicoDeOtraBanda_pideConfirmarAntesDeEnviar() = runTest {
        val viewModel = viewModelWith()
        viewModel.select(cerati)

        viewModel.add()

        assertEquals(MemberAddEvent.ConfirmBandChange(cerati.musician, "Soda Stereo"), viewModel.events.first())
        assertTrue(repository.added.isEmpty())

        viewModel.confirmAdd()
        assertEquals(listOf(101 to 11), repository.added)
    }

    @Test
    fun sinConexionAlEnviar_avisa_conservaLaSeleccion_yReintentarLoEnviaDeNuevo() = runTest {
        val viewModel = viewModelWith()
        viewModel.select(celia)
        viewModel.add()

        repository.addResponse.complete(Result.failure(DataError.Network()))

        assertEquals(MemberAddEvent.Failed(MemberAddFailure.CONNECTION), viewModel.events.first())
        assertEquals(celia, viewModel.selected.value)
        assertFalse(viewModel.isSubmitting.value)

        repository.addResponse = CompletableDeferred()
        viewModel.confirmAdd()
        assertEquals(listOf(101 to 3, 101 to 3), repository.added)
    }

    @Test
    fun siElServidorFallaAlEnviar_avisaConErrorDelServidor() = runTest {
        val viewModel = viewModelWith()
        viewModel.select(celia)
        viewModel.add()

        repository.addResponse.complete(Result.failure(DataError.Server(500)))

        assertEquals(MemberAddEvent.Failed(MemberAddFailure.SERVER), viewModel.events.first())
    }

    @Test
    fun siElMusicoYaNoExiste_avisa_recargaElListado_ySueltaLaSeleccion() = runTest {
        val viewModel = viewModelWith()
        viewModel.select(celia)
        viewModel.add()

        repository.candidates = { Result.success(listOf(ruben, freddie)) }
        repository.addResponse.complete(Result.failure(DataError.NotFound()))

        assertEquals(MemberAddEvent.Failed(MemberAddFailure.NOT_FOUND), viewModel.events.first())
        assertEquals(2, repository.candidateRequests.size)
        assertEquals(UiState.Success(listOf(ruben, freddie)), viewModel.state.value)
        assertNull(viewModel.selected.value)
        assertFalse(viewModel.bandMissing.value)
    }

    @Test
    fun siLaBandaYaNoExisteAlEnviar_avisa_yAlRecargarIndicaQueNoExiste() = runTest {
        val viewModel = viewModelWith()
        viewModel.select(celia)
        viewModel.add()

        repository.candidates = { Result.failure(DataError.NotFound()) }
        repository.addResponse.complete(Result.failure(DataError.NotFound()))

        assertEquals(MemberAddEvent.Failed(MemberAddFailure.NOT_FOUND), viewModel.events.first())
        assertTrue(viewModel.bandMissing.value)
        assertTrue(viewModel.state.value is UiState.Error)
    }
}
