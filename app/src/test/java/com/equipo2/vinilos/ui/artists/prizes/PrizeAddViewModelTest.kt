package com.equipo2.vinilos.ui.artists.prizes

import androidx.lifecycle.SavedStateHandle
import com.equipo2.vinilos.R
import com.equipo2.vinilos.data.model.ArtistPrize
import com.equipo2.vinilos.data.model.ArtistType
import com.equipo2.vinilos.data.model.Prize
import com.equipo2.vinilos.data.model.PrizeCandidate
import com.equipo2.vinilos.data.repository.DataError
import com.equipo2.vinilos.support.FakePrizeRepository
import com.equipo2.vinilos.support.MainDispatcherRule
import com.equipo2.vinilos.support.prizeCandidate
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
import java.time.LocalDate

class PrizeAddViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val repository = FakePrizeRepository()

    private val today = LocalDate.of(2026, 11, 10)
    private val awardDate = LocalDate.of(2000, 9, 13)

    private val grammy = prizeCandidate(100, "Grammy Award", "National Academy", LocalDate.of(1978, 12, 10))
    private val gardel = prizeCandidate(8, "Premio Gardel", "CAPIF")
    private val all = listOf(grammy, gardel)

    private fun viewModelWith(
        candidates: List<PrizeCandidate> = all,
        savedState: SavedStateHandle = SavedStateHandle(),
    ): PrizeAddViewModel {
        repository.candidates = { Result.success(candidates) }
        return PrizeAddViewModel(repository, artistId = 7, artistType = ArtistType.MUSICIAN, savedState, today = { today })
    }

    private fun failingViewModel(error: Throwable): PrizeAddViewModel {
        repository.candidates = { Result.failure(error) }
        return PrizeAddViewModel(repository, artistId = 7, artistType = ArtistType.MUSICIAN, today = { today })
    }

    // Carga

    @Test
    fun alAbrir_pideLosPremiosDelArtistaConSuTipo_yMuestraLaCargaHastaQueLlegan() {
        val response = CompletableDeferred<Result<List<PrizeCandidate>>>()
        repository.candidates = { response.await() }
        val viewModel = PrizeAddViewModel(repository, artistId = 7, artistType = ArtistType.BAND)

        assertEquals(UiState.Loading, viewModel.state.value)
        assertEquals(listOf(7 to ArtistType.BAND), repository.candidateRequests)

        response.complete(Result.success(all))
        assertEquals(UiState.Success(all), viewModel.state.value)
    }

    @Test
    fun sinPremiosEnElCatalogo_quedaVacio() {
        assertEquals(UiState.Empty, viewModelWith(emptyList()).state.value)
    }

    @Test
    fun sinConexion_muestraElError_yReintentarVuelveACargar() {
        val viewModel = failingViewModel(DataError.Network())
        assertEquals(UiState.Error(ErrorReason.CONNECTION), viewModel.state.value)
        assertFalse(viewModel.artistMissing.value)

        repository.candidates = { Result.success(all) }
        viewModel.retry()

        assertEquals(UiState.Success(all), viewModel.state.value)
        assertEquals(2, repository.candidateRequests.size)
    }

    @Test
    fun siElServidorFalla_muestraElErrorDelServidor() {
        assertEquals(UiState.Error(ErrorReason.SERVER), failingViewModel(DataError.Server(500)).state.value)
    }

    @Test
    fun siElArtistaYaNoExiste_loDiceEnLugarDelErrorDeCarga() {
        assertTrue(failingViewModel(DataError.NotFound()).artistMissing.value)
    }

    // Selección y fecha

    @Test
    fun alElegirUnPremio_quedaElegido_aunqueElArtistaYaLoTenga() {
        val viewModel = viewModelWith()

        viewModel.select(grammy)

        assertEquals(100, viewModel.selectedPrizeId.value)
    }

    @Test
    fun alElegirOtroPremio_reemplazaAlAnterior() {
        val viewModel = viewModelWith()

        viewModel.select(grammy)
        viewModel.select(gardel)

        assertEquals(8, viewModel.selectedPrizeId.value)
    }

    @Test
    fun elPremioYLaFecha_seConservanSiSeRecreaLaPantalla() {
        val savedState = SavedStateHandle()
        viewModelWith(savedState = savedState).apply {
            select(gardel)
            setDate(awardDate)
        }

        val recreated = viewModelWith(savedState = savedState)

        assertEquals(8, recreated.selectedPrizeId.value)
        assertEquals(awardDate, recreated.date.value)
    }

    @Test
    fun siElPremioElegidoYaNoEstaAlRecargar_seSueltaLaSeleccion() {
        val savedState = SavedStateHandle()
        viewModelWith(savedState = savedState).select(gardel)

        val recreated = viewModelWith(listOf(grammy), savedState)

        assertNull(recreated.selectedPrizeId.value)
    }

    // Validación

    @Test
    fun guardarSinPremioNiFecha_muestraLosDosErrores_yNoEnviaNada() {
        val viewModel = viewModelWith()

        assertFalse(viewModel.save())

        assertTrue(viewModel.prizeMissing.value)
        assertEquals(R.string.error_premiation_date_required, viewModel.dateError.value)
        assertTrue(repository.added.isEmpty())
    }

    @Test
    fun antesDeGuardar_noSeMuestraNingunError() {
        val viewModel = viewModelWith()

        assertFalse(viewModel.prizeMissing.value)
        assertNull(viewModel.dateError.value)
    }

    @Test
    fun guardarSoloConElPremio_pideLaFecha() {
        val viewModel = viewModelWith()
        viewModel.select(grammy)

        assertFalse(viewModel.save())

        assertFalse(viewModel.prizeMissing.value)
        assertEquals(R.string.error_premiation_date_required, viewModel.dateError.value)
    }

    @Test
    fun guardarSoloConLaFecha_pideElPremio() {
        val viewModel = viewModelWith()
        viewModel.setDate(awardDate)

        assertFalse(viewModel.save())

        assertTrue(viewModel.prizeMissing.value)
        assertNull(viewModel.dateError.value)
    }

    @Test
    fun trasGuardarConErrores_cadaErrorSeQuitaAlCorregirlo() {
        val viewModel = viewModelWith()
        viewModel.save()

        viewModel.select(grammy)
        assertFalse(viewModel.prizeMissing.value)
        assertEquals(R.string.error_premiation_date_required, viewModel.dateError.value)

        viewModel.setDate(awardDate)
        assertNull(viewModel.dateError.value)
    }

    @Test
    fun unaFechaFutura_noSeAcepta() {
        val viewModel = viewModelWith()
        viewModel.select(grammy)
        viewModel.setDate(today.plusDays(1))

        assertFalse(viewModel.save())

        assertEquals(R.string.error_premiation_date_future, viewModel.dateError.value)
        assertTrue(repository.added.isEmpty())
    }

    @Test
    fun laFechaDeHoy_seAcepta() {
        val viewModel = viewModelWith()
        viewModel.select(grammy)
        viewModel.setDate(today)

        assertTrue(viewModel.save())
    }

    @Test
    fun enElVacio_guardarPideElPremio() {
        val viewModel = viewModelWith(emptyList())
        viewModel.setDate(awardDate)

        assertFalse(viewModel.save())

        assertTrue(viewModel.prizeMissing.value)
    }

    // Envío

    @Test
    fun guardarConPremioYFecha_enviaElPremioAlArtista_yQuedaGuardando() {
        val viewModel = viewModelWith()
        viewModel.select(grammy)
        viewModel.setDate(awardDate)

        assertTrue(viewModel.save())

        assertEquals(listOf(FakePrizeRepository.Added(7, ArtistType.MUSICIAN, grammy.prize, awardDate)), repository.added)
        assertTrue(viewModel.isSaving.value)
    }

    @Test
    fun mientrasGuarda_noSeEnviaDosVeces_niSeCambiaLoElegido() {
        val viewModel = viewModelWith()
        viewModel.select(grammy)
        viewModel.setDate(awardDate)
        viewModel.save()

        viewModel.save()
        viewModel.select(gardel)
        viewModel.setDate(today)

        assertEquals(1, repository.added.size)
        assertEquals(100, viewModel.selectedPrizeId.value)
        assertEquals(awardDate, viewModel.date.value)
    }

    @Test
    fun siSeAgrega_avisaConLaEntrega_yDejaDeGuardar() = runTest {
        val viewModel = viewModelWith()
        viewModel.select(grammy)
        viewModel.setDate(awardDate)
        viewModel.save()

        val delivery = ArtistPrize(37, "Grammy Award", "National Academy", awardDate)
        repository.addResponse.complete(Result.success(delivery))

        assertEquals(PrizeAddEvent.Added(delivery), viewModel.events.first())
        assertFalse(viewModel.isSaving.value)
    }

    @Test
    fun sinConexionAlGuardar_avisa_yConservaElPremioYLaFecha() = runTest {
        val viewModel = savingViewModel()

        repository.addResponse.complete(Result.failure(DataError.Network()))

        assertEquals(PrizeAddEvent.Failed(PrizeAddFailure.CONNECTION), viewModel.events.first())
        assertFalse(viewModel.isSaving.value)
        assertEquals(100, viewModel.selectedPrizeId.value)
        assertEquals(awardDate, viewModel.date.value)
    }

    @Test
    fun siElServidorFallaAlGuardar_avisa_ySePuedeReintentar() = runTest {
        val viewModel = savingViewModel()
        repository.addResponse.complete(Result.failure(DataError.Server(500)))
        assertEquals(PrizeAddEvent.Failed(PrizeAddFailure.SERVER), viewModel.events.first())

        repository.addResponse = CompletableDeferred()
        assertTrue(viewModel.save())

        assertEquals(2, repository.added.size)
    }

    @Test
    fun siElPremioOElArtistaYaNoExisten_avisa_yRecargaElListado() = runTest {
        val viewModel = savingViewModel()

        repository.addResponse.complete(Result.failure(DataError.NotFound()))

        assertEquals(PrizeAddEvent.Failed(PrizeAddFailure.NOT_FOUND), viewModel.events.first())
        assertEquals(2, repository.candidateRequests.size)
    }

    @Test
    fun siLaApiNoAceptaLaFecha_elErrorQuedaEnLaFecha_hastaQueSeElijaOtra() = runTest {
        val viewModel = savingViewModel()

        repository.addResponse.complete(Result.failure(DataError.Request(400)))

        assertEquals(PrizeAddEvent.Failed(PrizeAddFailure.INVALID_DATE), viewModel.events.first())
        assertEquals(R.string.error_premiation_date_invalid, viewModel.dateError.value)
        // Con la misma fecha no se vuelve a enviar
        assertFalse(viewModel.save())
        assertEquals(1, repository.added.size)

        viewModel.setDate(awardDate.plusDays(1))
        assertNull(viewModel.dateError.value)
    }

    // Premio creado desde Crear premio (HU13)

    private val latinGrammy = Prize(205, "Latin Grammy", "Academia Latina")

    @Test
    fun alCrearUnPremio_quedaPrimero_elegido_yMarcadoComoNuevo_sinVolverAPedirElListado() {
        val viewModel = viewModelWith()

        viewModel.onPrizeCreated(latinGrammy)

        assertEquals(UiState.Success(listOf(PrizeCandidate(latinGrammy)) + all), viewModel.state.value)
        assertEquals(205, viewModel.selectedPrizeId.value)
        assertEquals(listOf(205), viewModel.newPrizeIds.value.toList())
        assertEquals(1, repository.candidateRequests.size)
    }

    @Test
    fun conElCatalogoVacio_elPremioCreadoEsElUnico() {
        val viewModel = viewModelWith(emptyList())

        viewModel.onPrizeCreated(latinGrammy)

        assertEquals(UiState.Success(listOf(PrizeCandidate(latinGrammy))), viewModel.state.value)
        assertEquals(205, viewModel.selectedPrizeId.value)
    }

    @Test
    fun alCrearUnPremio_seQuitaElErrorDeElegirUno() {
        val viewModel = viewModelWith()
        viewModel.save()
        assertTrue(viewModel.prizeMissing.value)

        viewModel.onPrizeCreated(latinGrammy)

        assertFalse(viewModel.prizeMissing.value)
    }

    @Test
    fun variosPremiosCreados_vanArriba_elMasRecientePrimero() {
        val viewModel = viewModelWith()
        val brit = Prize(206, "Brit Awards", "British Phonographic Industry")

        viewModel.onPrizeCreated(latinGrammy)
        viewModel.onPrizeCreated(brit)

        val ids = (viewModel.state.value as UiState.Success).data.map { it.prize.id }
        assertEquals(listOf(206, 205, 100, 8), ids)
        assertEquals(206, viewModel.selectedPrizeId.value)
    }

    @Test
    fun alRecargar_elPremioCreadoSigueArriba_yMarcado() {
        val viewModel = viewModelWith()
        viewModel.onPrizeCreated(latinGrammy)

        // La API lo devuelve en su orden alfabético, después de los otros
        repository.candidates = { Result.success(all + PrizeCandidate(latinGrammy)) }
        viewModel.retry()

        assertEquals(UiState.Success(listOf(PrizeCandidate(latinGrammy)) + all), viewModel.state.value)
        assertEquals(205, viewModel.selectedPrizeId.value)
    }

    @Test
    fun alGirar_seConservanElPremioElegidoYLaMarcaDeNuevo() {
        val savedState = SavedStateHandle()
        viewModelWith(savedState = savedState).onPrizeCreated(latinGrammy)

        repository.candidates = { Result.success(all + PrizeCandidate(latinGrammy)) }
        val recreated = PrizeAddViewModel(repository, artistId = 7, artistType = ArtistType.MUSICIAN, savedState, today = { today })

        assertEquals(205, recreated.selectedPrizeId.value)
        assertEquals(listOf(205), recreated.newPrizeIds.value.toList())
        assertEquals(205, (recreated.state.value as UiState.Success).data.first().prize.id)
    }

    /** ViewModel con el Grammy y la fecha elegidos y el envío esperando la respuesta. */
    private fun savingViewModel(): PrizeAddViewModel = viewModelWith().apply {
        select(grammy)
        setDate(awardDate)
        save()
    }
}
