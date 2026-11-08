package com.equipo2.vinilos.ui.artists.detail

import androidx.lifecycle.SavedStateHandle
import com.equipo2.vinilos.data.model.ArtistDetail
import com.equipo2.vinilos.data.model.ArtistType
import com.equipo2.vinilos.data.repository.DataError
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
import org.junit.Rule
import org.junit.Test

class ArtistDetailViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val repository = FakeArtistRepository()

    private val queen = ArtistDetail(
        artist = band(101, "Queen"),
        description = "Banda británica de rock.",
        members = listOf(musician(10, "Brian May"), musician(9, "Freddie Mercury")),
    )

    private fun viewModel() = ArtistDetailViewModel(repository, artistId = 101, artistType = ArtistType.BAND)

    @Test
    fun alAbrir_pideElArtistaConSuTipo_yQuedaCargandoHastaQueLaApiResponda() {
        repository.detail = { CompletableDeferred<Result<ArtistDetail>>().await() }

        val viewModel = viewModel()

        assertEquals(UiState.Loading, viewModel.state.value)
        assertEquals(listOf(101 to ArtistType.BAND), repository.detailRequests)
    }

    @Test
    fun siLaApiResponde_muestraElArtista() {
        repository.detail = { Result.success(queen) }

        assertEquals(UiState.Success(queen), viewModel().state.value)
    }

    @Test
    fun siNoHayConexion_quedaEnErrorDeConexion() {
        repository.detail = { Result.failure(DataError.Network()) }

        assertEquals(UiState.Error(ErrorReason.CONNECTION), viewModel().state.value)
    }

    @Test
    fun siElServidorFalla_quedaEnErrorDelServidor() {
        repository.detail = { Result.failure(DataError.Server(500)) }

        assertEquals(UiState.Error(ErrorReason.SERVER), viewModel().state.value)
    }

    @Test
    fun alReintentar_vuelveAPedirlo_yMuestraElArtista() {
        repository.detail = { Result.failure(DataError.Network()) }
        val viewModel = viewModel()

        repository.detail = { Result.success(queen) }
        viewModel.retry()

        assertEquals(UiState.Success(queen), viewModel.state.value)
        assertEquals(2, repository.detailRequests.size)
    }

    @Test
    fun reintentarMientrasCarga_noRepiteLaPeticion() {
        repository.detail = { CompletableDeferred<Result<ArtistDetail>>().await() }
        val viewModel = viewModel()

        viewModel.retry()

        assertEquals(1, repository.detailRequests.size)
    }

    @Test
    fun alDeslizarParaRecargar_muestraLosDatosNuevos() {
        repository.detail = { Result.success(queen) }
        val viewModel = viewModel()

        val updated = queen.copy(members = emptyList())
        repository.detail = { Result.success(updated) }
        viewModel.refresh()

        assertEquals(UiState.Success(updated), viewModel.state.value)
        assertEquals(2, repository.detailRequests.size)
    }

    @Test
    fun siLaRecargaFalla_conservaElArtista_yAvisaElMotivo() = runTest {
        repository.detail = { Result.success(queen) }
        val viewModel = viewModel()

        repository.detail = { Result.failure(DataError.Network()) }
        viewModel.refresh()

        assertEquals(UiState.Success(queen), viewModel.state.value)
        assertEquals(ErrorReason.CONNECTION, viewModel.pullRefresh.failures.first())
    }

    @Test
    fun recargarDesdeElError_vuelveACargarDesdeCero() {
        repository.detail = { Result.failure(DataError.Network()) }
        val viewModel = viewModel()

        repository.detail = { CompletableDeferred<Result<ArtistDetail>>().await() }
        viewModel.refresh()

        assertEquals(UiState.Loading, viewModel.state.value)
    }

    @Test
    fun alAgregarUnIntegrante_apareceAlFinal_marcadoComoNuevo_sinVolverAPedirLaBanda() {
        repository.detail = { Result.success(queen) }
        val viewModel = viewModel()

        val roger = musician(11, "Roger Taylor")
        viewModel.onMemberAdded(roger)

        assertEquals(queen.members + roger, (viewModel.state.value as UiState.Success).data.members)
        assertEquals(listOf(11), viewModel.newMemberIds.value.toList())
        assertEquals(1, repository.detailRequests.size)
    }

    @Test
    fun siElIntegranteYaEstaEnLaLista_noSeRepite_peroSeMarcaComoNuevo() {
        repository.detail = { Result.success(queen) }
        val viewModel = viewModel()

        viewModel.onMemberAdded(musician(10, "Brian May"))
        viewModel.onMemberAdded(musician(10, "Brian May"))

        assertEquals(queen.members, (viewModel.state.value as UiState.Success).data.members)
        assertEquals(listOf(10), viewModel.newMemberIds.value.toList())
    }

    @Test
    fun losIntegrantesNuevos_seConservanSiSeRecreaLaPantalla() {
        repository.detail = { Result.success(queen) }
        val savedState = SavedStateHandle()
        ArtistDetailViewModel(repository, 101, ArtistType.BAND, savedState).onMemberAdded(musician(11, "Roger Taylor"))

        val recreated = ArtistDetailViewModel(repository, 101, ArtistType.BAND, savedState)

        assertEquals(listOf(11), recreated.newMemberIds.value.toList())
    }
}
