package com.equipo2.vinilos.ui.albums.tracks

import com.equipo2.vinilos.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackAddViewModelTest {

    private val viewModel = TrackAddViewModel()

    @Test
    fun antesDeAgregar_noHayErroresAunqueElFormularioEsteVacio() {
        viewModel.update { copy(name = "") }

        assertTrue(viewModel.errors.value.isEmpty())
    }

    @Test
    fun agregarVacio_muestraLosErrores() {
        val valid = viewModel.add()

        assertFalse(valid)
        assertEquals(TrackField.entries, viewModel.errors.value.keys.toList())
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
    fun agregarValido_noTieneErrores() {
        viewModel.update { copy(name = "Todos vuelven", duration = "4:56") }

        assertTrue(viewModel.add())
        assertTrue(viewModel.errors.value.isEmpty())
    }
}
