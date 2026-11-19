package com.equipo2.vinilos.ui.artists.prizes.create

import com.equipo2.vinilos.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrizeCreateViewModelTest {

    private val viewModel = PrizeCreateViewModel()

    private fun fillValidForm() = viewModel.update {
        copy(
            name = "Premio Gardel",
            organization = "CAPIF",
            description = "Reconoce a los mejores artistas de música en español.",
        )
    }

    @Test
    fun antesDeCrear_noHayErroresAunqueElFormularioEsteVacio() {
        viewModel.update { copy(name = "") }

        assertTrue(viewModel.errors.value.isEmpty())
    }

    @Test
    fun crearVacio_muestraLosTresErrores() {
        assertFalse(viewModel.save())

        assertEquals(PrizeField.entries, viewModel.errors.value.keys.toList())
    }

    @Test
    fun corregirUnCampo_quitaSoloSuError() {
        viewModel.save()

        viewModel.update { copy(organization = "CAPIF") }

        assertEquals(listOf(PrizeField.NAME, PrizeField.DESCRIPTION), viewModel.errors.value.keys.toList())
    }

    @Test
    fun dejarUnCampoConSoloEspacios_vuelveAMostrarSuError() {
        fillValidForm()
        viewModel.save()

        viewModel.update { copy(description = "   ") }

        assertEquals(
            mapOf(PrizeField.DESCRIPTION to R.string.error_prize_description_required),
            viewModel.errors.value,
        )
    }

    @Test
    fun crearValido_noMuestraErrores_yConservaLoEscrito() {
        fillValidForm()

        assertTrue(viewModel.save())

        assertTrue(viewModel.errors.value.isEmpty())
        assertEquals("Premio Gardel", viewModel.form.value.name)
    }
}
