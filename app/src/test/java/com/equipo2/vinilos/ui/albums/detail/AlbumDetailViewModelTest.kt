package com.equipo2.vinilos.ui.albums.detail

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
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class AlbumDetailViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val repository = FakeAlbumRepository()

    private val album = Album(
        id = 100,
        name = "Buscando América",
        cover = "https://ejemplo.com/portada.jpg",
        releaseDate = LocalDate.of(1984, 8, 1),
        description = "Álbum de Rubén Blades.",
        genre = "Salsa",
        recordLabel = "Elektra",
    )

    @Test
    fun alAbrir_pideElAlbum_yQuedaCargandoHastaQueLaApiResponda() {
        repository.detail = { CompletableDeferred<Result<Album>>().await() }

        val viewModel = AlbumDetailViewModel(repository, albumId = 100)

        assertEquals(UiState.Loading, viewModel.state.value)
        assertEquals(listOf(100), repository.detailRequests)
    }

    @Test
    fun siLaApiResponde_muestraElAlbum() {
        repository.detail = { Result.success(album) }

        val viewModel = AlbumDetailViewModel(repository, albumId = 100)

        assertEquals(UiState.Success(album), viewModel.state.value)
    }

    @Test
    fun siLaApiFalla_quedaEnError() {
        repository.detail = { Result.failure(DataError.Network()) }

        val viewModel = AlbumDetailViewModel(repository, albumId = 100)

        assertEquals(UiState.Error(ErrorReason.CONNECTION), viewModel.state.value)
    }

    @Test
    fun alReintentar_vuelveAPedirlo_yMuestraElAlbum() {
        repository.detail = { Result.failure(DataError.Network()) }
        val viewModel = AlbumDetailViewModel(repository, albumId = 100)

        repository.detail = { Result.success(album) }
        viewModel.retry()

        assertEquals(UiState.Success(album), viewModel.state.value)
        assertEquals(listOf(100, 100), repository.detailRequests)
    }

    @Test
    fun reintentarMientrasCarga_noRepiteLaPeticion() {
        repository.detail = { CompletableDeferred<Result<Album>>().await() }
        val viewModel = AlbumDetailViewModel(repository, albumId = 100)

        viewModel.retry()

        assertEquals(listOf(100), repository.detailRequests)
    }

    @Test
    fun alDeslizarParaRecargar_muestraLosDatosNuevosDelAlbum() {
        repository.detail = { Result.success(album) }
        val viewModel = AlbumDetailViewModel(repository, albumId = 100)

        val updated = album.copy(description = "Descripción actualizada.")
        repository.detail = { Result.success(updated) }
        viewModel.refresh()

        assertEquals(UiState.Success(updated), viewModel.state.value)
        assertEquals(listOf(100, 100), repository.detailRequests)
    }

    @Test
    fun siLaRecargaFalla_conservaElAlbum_yAvisaElMotivo() = runTest {
        repository.detail = { Result.success(album) }
        val viewModel = AlbumDetailViewModel(repository, albumId = 100)

        repository.detail = { Result.failure(DataError.Server(500)) }
        viewModel.refresh()

        assertEquals(UiState.Success(album), viewModel.state.value)
        assertEquals(ErrorReason.SERVER, viewModel.pullRefresh.failures.first())
    }
}
