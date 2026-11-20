package com.equipo2.vinilos.ui.artists.albums

import androidx.lifecycle.SavedStateHandle
import com.equipo2.vinilos.data.model.AlbumCandidate
import com.equipo2.vinilos.data.model.ArtistType
import com.equipo2.vinilos.data.model.Performer
import com.equipo2.vinilos.data.repository.DataError
import com.equipo2.vinilos.support.FakeArtistAlbumRepository
import com.equipo2.vinilos.support.FakeArtistAlbumRepository.Added
import com.equipo2.vinilos.support.MainDispatcherRule
import com.equipo2.vinilos.support.albumCandidate
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

class AlbumAddViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val repository = FakeArtistAlbumRepository()

    private val abbeyRoad = albumCandidate(173, "Abbey Road")
    private val siembra = albumCandidate(178, "Siembra")
    private val buscando = albumCandidate(100, "Buscando América", alreadyAdded = true)
    private val all = listOf(abbeyRoad, siembra, buscando)

    private fun viewModelWith(
        candidates: List<AlbumCandidate> = all,
        type: ArtistType = ArtistType.MUSICIAN,
        savedState: SavedStateHandle = SavedStateHandle(),
    ): AlbumAddViewModel {
        repository.candidates = { Result.success(candidates) }
        return AlbumAddViewModel(repository, artistId = 100, artistType = type, savedState = savedState)
    }

    // Carga

    @Test
    fun alAbrir_muestraLaCargaHastaQueLlegaElListado_yLoPideConElTipoDelArtista() {
        val response = CompletableDeferred<Result<List<AlbumCandidate>>>()
        repository.candidates = { response.await() }
        val viewModel = AlbumAddViewModel(repository, artistId = 15, artistType = ArtistType.BAND)

        assertEquals(UiState.Loading, viewModel.state.value)
        assertEquals(listOf(15 to ArtistType.BAND), repository.candidateRequests)

        response.complete(Result.success(all))
        assertEquals(UiState.Success(all), viewModel.state.value)
    }

    @Test
    fun siNoHayAlbumesQueAgregar_quedaVacioAunqueEstenLosQueYaTiene() {
        val viewModel = viewModelWith(listOf(buscando))

        assertEquals(UiState.Empty, viewModel.state.value)
        assertTrue(viewModel.allAlreadyAdded.value)
    }

    @Test
    fun siElCatalogoEstaVacio_quedaVacio_yNoDiceQueYaLosTengaTodos() {
        val viewModel = viewModelWith(emptyList())

        assertEquals(UiState.Empty, viewModel.state.value)
        assertFalse(viewModel.allAlreadyAdded.value)
    }

    @Test
    fun sinConexion_muestraElError_yReintentarVuelveACargar() {
        repository.candidates = { Result.failure(DataError.Network()) }
        val viewModel = AlbumAddViewModel(repository, artistId = 100, artistType = ArtistType.MUSICIAN)

        assertEquals(UiState.Error(ErrorReason.CONNECTION), viewModel.state.value)

        repository.candidates = { Result.success(all) }
        viewModel.retry()

        assertEquals(UiState.Success(all), viewModel.state.value)
        assertEquals(2, repository.candidateRequests.size)
    }

    @Test
    fun siElServidorFallaAlCargar_muestraElErrorDelServidor() {
        repository.candidates = { Result.failure(DataError.Server(500)) }
        val viewModel = AlbumAddViewModel(repository, artistId = 100, artistType = ArtistType.MUSICIAN)

        assertEquals(UiState.Error(ErrorReason.SERVER), viewModel.state.value)
        assertFalse(viewModel.artistMissing.value)
    }

    @Test
    fun mientrasCarga_reintentarNoLanzaOtraPeticion() {
        val response = CompletableDeferred<Result<List<AlbumCandidate>>>()
        repository.candidates = { response.await() }
        val viewModel = AlbumAddViewModel(repository, artistId = 100, artistType = ArtistType.MUSICIAN)

        viewModel.retry()

        assertEquals(1, repository.candidateRequests.size)
    }

    @Test
    fun siElArtistaNoExiste_loIndica_enLugarDelErrorDeCarga() {
        repository.candidates = { Result.failure(DataError.NotFound()) }
        val viewModel = AlbumAddViewModel(repository, artistId = 100, artistType = ArtistType.MUSICIAN)

        assertTrue(viewModel.artistMissing.value)
        assertTrue(viewModel.state.value is UiState.Error)
    }

    @Test
    fun unErrorDeConexion_noDiceQueElArtistaNoExista() {
        repository.candidates = { Result.failure(DataError.Network()) }
        val viewModel = AlbumAddViewModel(repository, artistId = 100, artistType = ArtistType.MUSICIAN)

        assertFalse(viewModel.artistMissing.value)
    }

    // Búsqueda

    private val ruben = Performer(100, "Rubén Blades", "", ArtistType.MUSICIAN)
    private val willie = Performer(1, "Willie Colón", "", ArtistType.MUSICIAN)
    private val beatles = Performer(15, "The Beatles", "", ArtistType.BAND)
    private val abbeyRoadByBeatles = albumCandidate(173, "Abbey Road", false, beatles)
    private val siembraByBoth = albumCandidate(178, "Siembra", false, willie, ruben)
    private val buscandoByRuben = albumCandidate(100, "Buscando América", true, ruben)
    private val withPerformers = listOf(abbeyRoadByBeatles, siembraByBoth, buscandoByRuben)

    @Test
    fun sinBuscar_muestraTodoElCatalogo() {
        val viewModel = viewModelWith(withPerformers)

        assertEquals("", viewModel.query.value)
        assertEquals(UiState.Success(withPerformers), viewModel.state.value)
    }

    @Test
    fun buscar_filtraPorElNombreDelAlbum_sinImportarTildesMayusculasNiEspacios() {
        val viewModel = viewModelWith(withPerformers)

        viewModel.search("  AMERICA ")

        assertEquals(UiState.Success(listOf(buscandoByRuben)), viewModel.state.value)
    }

    @Test
    fun buscar_filtraPorCualquieraDeSusInterpretes_yConservaElOrden() {
        val viewModel = viewModelWith(withPerformers)

        viewModel.search("ruben")

        // Siembra tiene dos intérpretes y basta con que coincida uno. Buscando América sigue al final porque ya lo tiene
        assertEquals(UiState.Success(listOf(siembraByBoth, buscandoByRuben)), viewModel.state.value)
    }

    @Test
    fun buscar_porUnaBanda_encuentraSusAlbumes() {
        val viewModel = viewModelWith(withPerformers)

        viewModel.search("beatles")

        assertEquals(UiState.Success(listOf(abbeyRoadByBeatles)), viewModel.state.value)
    }

    @Test
    fun siLaBusquedaNoEncuentraNada_esUnaListaVaciaYNoElEstadoVacio() {
        val viewModel = viewModelWith(withPerformers)

        viewModel.search("zeppelin")

        assertEquals(UiState.Success(emptyList<AlbumCandidate>()), viewModel.state.value)
    }

    @Test
    fun borrarLaBusqueda_vuelveAMostrarTodo() {
        val viewModel = viewModelWith(withPerformers)
        viewModel.search("zeppelin")

        viewModel.search("")

        assertEquals(UiState.Success(withPerformers), viewModel.state.value)
    }

    @Test
    fun laSeleccion_sigueAunqueLaBusquedaLaOculte_ySePuedeAgregar() {
        val viewModel = viewModelWith(withPerformers)
        viewModel.select(siembraByBoth)

        viewModel.search("beatles")
        viewModel.add()

        assertEquals(siembraByBoth, viewModel.selected.value)
        assertEquals(listOf(Added(100, ArtistType.MUSICIAN, 178)), repository.added)
    }

    @Test
    fun laBusquedaYLaSeleccion_seConservanAlGirar() {
        val savedState = SavedStateHandle()
        viewModelWith(withPerformers, savedState = savedState).apply {
            search("sie")
            select(siembraByBoth)
        }

        val recreated = viewModelWith(withPerformers, savedState = savedState)

        assertEquals("sie", recreated.query.value)
        assertEquals(UiState.Success(listOf(siembraByBoth)), recreated.state.value)
        assertEquals(siembraByBoth, recreated.selected.value)
    }

    @Test
    fun mientrasCarga_buscarNoCambiaLaCarga() {
        val response = CompletableDeferred<Result<List<AlbumCandidate>>>()
        repository.candidates = { response.await() }
        val viewModel = AlbumAddViewModel(repository, artistId = 100, artistType = ArtistType.MUSICIAN)

        viewModel.search("sie")

        assertEquals(UiState.Loading, viewModel.state.value)
    }

    // Selección

    @Test
    fun elegir_marcaElAlbum_yElegirOtroLoCambia() {
        val viewModel = viewModelWith()

        viewModel.select(abbeyRoad)
        assertEquals(abbeyRoad, viewModel.selected.value)

        viewModel.select(siembra)
        assertEquals(siembra, viewModel.selected.value)
    }

    @Test
    fun unAlbumQueElArtistaYaTiene_noSePuedeElegir() {
        val viewModel = viewModelWith()

        viewModel.select(buscando)

        assertNull(viewModel.selected.value)
    }

    @Test
    fun laSeleccion_seConservaAlGirar() {
        val savedState = SavedStateHandle()
        viewModelWith(savedState = savedState).select(siembra)

        val recreated = viewModelWith(savedState = savedState)

        assertEquals(siembra, recreated.selected.value)
    }

    @Test
    fun siLaCargaFallaYSeReintenta_conservaLaSeleccion() {
        val savedState = SavedStateHandle()
        viewModelWith(savedState = savedState).select(siembra)
        repository.candidates = { Result.failure(DataError.Network()) }
        val recreated = AlbumAddViewModel(repository, 100, ArtistType.MUSICIAN, savedState)

        repository.candidates = { Result.success(all) }
        recreated.retry()

        assertEquals(siembra, recreated.selected.value)
    }

    @Test
    fun siAlRecargarElArtistaYaTieneElAlbumElegido_sueltaLaSeleccion() {
        val savedState = SavedStateHandle()
        viewModelWith(savedState = savedState).select(siembra)

        val recreated = viewModelWith(
            listOf(abbeyRoad, albumCandidate(178, "Siembra", alreadyAdded = true)),
            savedState = savedState,
        )

        assertNull(recreated.selected.value)
    }

    // Agregar

    @Test
    fun agregarSinElegir_pideElegirUnAlbum_yNoLlamaALaApi() {
        val viewModel = viewModelWith()

        viewModel.add()

        assertTrue(viewModel.missingSelection.value)
        assertTrue(repository.added.isEmpty())

        viewModel.select(abbeyRoad)
        assertFalse(viewModel.missingSelection.value)
    }

    @Test
    fun agregar_loEnviaConElTipoDelArtista_yQuedaEnviandoHastaQueLaApiResponda() {
        val viewModel = viewModelWith(type = ArtistType.BAND)
        viewModel.select(siembra)

        viewModel.add()

        assertEquals(listOf(Added(100, ArtistType.BAND, 178)), repository.added)
        assertTrue(viewModel.isSubmitting.value)
    }

    @Test
    fun mientrasSeEnvia_noSeEnviaDosVecesNiSeCambiaLaSeleccion() {
        val viewModel = viewModelWith()
        viewModel.select(abbeyRoad)
        viewModel.add()

        viewModel.add()
        viewModel.add()
        viewModel.select(siembra)

        assertEquals(listOf(Added(100, ArtistType.MUSICIAN, 173)), repository.added)
        assertEquals(abbeyRoad, viewModel.selected.value)
    }

    // Reemplazo de intérpretes en una banda

    @Test
    fun aUnaBanda_unAlbumConOtrosInterpretes_pideConfirmarAntesDeEnviar() = runTest {
        val viewModel = viewModelWith(withPerformers, type = ArtistType.BAND)
        viewModel.select(siembraByBoth)

        viewModel.add()

        assertEquals(AlbumAddEvent.ConfirmReplace(siembraByBoth.album, listOf(willie, ruben)), viewModel.events.first())
        assertTrue(repository.added.isEmpty())
        assertFalse(viewModel.isSubmitting.value)

        viewModel.confirmAdd()
        assertEquals(listOf(Added(100, ArtistType.BAND, 178)), repository.added)
        assertTrue(viewModel.isSubmitting.value)
    }

    @Test
    fun aUnaBanda_unAlbumSinInterpretes_seEnviaSinPreguntar() {
        val viewModel = viewModelWith(type = ArtistType.BAND)
        viewModel.select(abbeyRoad)

        viewModel.add()

        assertEquals(listOf(Added(100, ArtistType.BAND, 173)), repository.added)
    }

    @Test
    fun aUnMusico_unAlbumConInterpretes_seEnviaSinPreguntar() {
        val viewModel = viewModelWith(withPerformers, type = ArtistType.MUSICIAN)
        viewModel.select(siembraByBoth)

        viewModel.add()

        assertEquals(listOf(Added(100, ArtistType.MUSICIAN, 178)), repository.added)
    }

    @Test
    fun confirmarSinElegir_noEnviaNada() {
        val viewModel = viewModelWith(type = ArtistType.BAND)

        viewModel.confirmAdd()

        assertTrue(repository.added.isEmpty())
    }

    @Test
    fun mientrasSeEnvia_confirmarOtraVezNoEnviaDosVeces() {
        val viewModel = viewModelWith(withPerformers, type = ArtistType.BAND)
        viewModel.select(siembraByBoth)
        viewModel.confirmAdd()

        viewModel.confirmAdd()
        viewModel.add()

        assertEquals(1, repository.added.size)
    }

    @Test
    fun siLaApiLoAgrega_avisaConElAlbum() = runTest {
        val viewModel = viewModelWith()
        viewModel.select(abbeyRoad)
        viewModel.add()

        repository.addResponse.complete(Result.success(Unit))

        assertEquals(AlbumAddEvent.Added(abbeyRoad.album), viewModel.events.first())
        assertFalse(viewModel.isSubmitting.value)
    }

    @Test
    fun sinConexionAlEnviar_avisa_conservaLaSeleccion_yReintentarLoEnviaDeNuevo() = runTest {
        val viewModel = viewModelWith()
        viewModel.select(abbeyRoad)
        viewModel.add()

        repository.addResponse.complete(Result.failure(DataError.Timeout()))

        assertEquals(AlbumAddEvent.Failed(AlbumAddFailure.CONNECTION), viewModel.events.first())
        assertEquals(abbeyRoad, viewModel.selected.value)
        assertFalse(viewModel.isSubmitting.value)

        repository.addResponse = CompletableDeferred()
        viewModel.add()
        assertEquals(2, repository.added.size)
        assertEquals(repository.added.first(), repository.added.last())
    }

    @Test
    fun siElServidorFallaAlEnviar_avisaConErrorDelServidor_yNoRecarga() = runTest {
        val viewModel = viewModelWith()
        viewModel.select(abbeyRoad)
        viewModel.add()

        repository.addResponse.complete(Result.failure(DataError.Server(500)))

        assertEquals(AlbumAddEvent.Failed(AlbumAddFailure.SERVER), viewModel.events.first())
        assertEquals(1, repository.candidateRequests.size)
        assertEquals(abbeyRoad, viewModel.selected.value)
    }

    @Test
    fun siElAlbumYaNoExiste_avisa_recargaElListado_ySueltaLaSeleccion() = runTest {
        val viewModel = viewModelWith()
        viewModel.select(abbeyRoad)
        viewModel.add()

        repository.candidates = { Result.success(listOf(siembra, buscando)) }
        repository.addResponse.complete(Result.failure(DataError.NotFound()))

        assertEquals(AlbumAddEvent.Failed(AlbumAddFailure.NOT_FOUND), viewModel.events.first())
        assertEquals(2, repository.candidateRequests.size)
        assertEquals(UiState.Success(listOf(siembra, buscando)), viewModel.state.value)
        assertNull(viewModel.selected.value)
        assertFalse(viewModel.artistMissing.value)
    }

    @Test
    fun siElArtistaYaNoExisteAlEnviar_avisa_yAlRecargarIndicaQueNoExiste() = runTest {
        val viewModel = viewModelWith()
        viewModel.select(abbeyRoad)
        viewModel.add()

        repository.candidates = { Result.failure(DataError.NotFound()) }
        repository.addResponse.complete(Result.failure(DataError.NotFound()))

        assertEquals(AlbumAddEvent.Failed(AlbumAddFailure.NOT_FOUND), viewModel.events.first())
        assertTrue(viewModel.artistMissing.value)
        assertTrue(viewModel.state.value is UiState.Error)
    }
}
