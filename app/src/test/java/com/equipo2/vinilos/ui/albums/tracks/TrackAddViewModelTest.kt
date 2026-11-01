package com.equipo2.vinilos.ui.albums.tracks

import com.equipo2.vinilos.R
import com.equipo2.vinilos.data.model.NewTrack
import com.equipo2.vinilos.data.model.Track
import com.equipo2.vinilos.data.repository.DataError
import com.equipo2.vinilos.support.FakeAlbumRepository
import com.equipo2.vinilos.support.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TrackAddViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val repository = FakeAlbumRepository()
    private val viewModel = TrackAddViewModel(repository, albumId = 100)

    private fun fillValidForm() = viewModel.update { copy(name = " Todos vuelven ", duration = "4:56") }

    @Test
    fun antesDeAgregar_noHayErroresAunqueElFormularioEsteVacio() {
        viewModel.update { copy(name = "") }

        assertTrue(viewModel.errors.value.isEmpty())
    }

    @Test
    fun agregarVacio_muestraLosErrores_yNoLlamaALaApi() {
        val sent = viewModel.add()

        assertFalse(sent)
        assertEquals(TrackField.entries, viewModel.errors.value.keys.toList())
        assertTrue(repository.addedTracks.isEmpty())
        assertFalse(viewModel.isSaving.value)
    }

    @Test
    fun despuesDeAgregar_cadaCambioActualizaLosErrores() {
        viewModel.add()

        viewModel.update { copy(name = "Todos vuelven") }
        assertFalse(TrackField.NAME in viewModel.errors.value)

        viewModel.update { copy(duration = "456") }
        assertEquals(R.string.error_track_duration_invalid, viewModel.errors.value[TrackField.DURATION])
    }

    @Test
    fun agregarValido_enviaElTrackSinEspacios_yQuedaAgregandoHastaQueLaApiResponda() {
        fillValidForm()

        assertTrue(viewModel.add())

        assertEquals(listOf(100 to NewTrack(name = "Todos vuelven", duration = "4:56")), repository.addedTracks)
        assertTrue(viewModel.isSaving.value)
    }

    @Test
    fun siLaApiAgregaElTrack_avisaConElTrack() = runTest {
        fillValidForm()
        viewModel.add()

        val track = Track(id = 45, name = "Todos vuelven", duration = "4:56")
        repository.trackResponse.complete(Result.success(track))

        assertEquals(TrackAddEvent.Added(track), viewModel.events.first())
        assertFalse(viewModel.isSaving.value)
    }

    @Test
    fun siLaApiFalla_avisaYConservaLoEscrito() = runTest {
        fillValidForm()
        viewModel.add()

        repository.trackResponse.complete(Result.failure(DataError.Network()))

        assertEquals(TrackAddEvent.Failed, viewModel.events.first())
        assertEquals(" Todos vuelven ", viewModel.form.value.name)
        assertFalse(viewModel.isSaving.value)
    }
}
