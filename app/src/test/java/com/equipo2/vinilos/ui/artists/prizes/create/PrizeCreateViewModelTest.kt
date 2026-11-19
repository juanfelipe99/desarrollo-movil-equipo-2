package com.equipo2.vinilos.ui.artists.prizes.create

import com.equipo2.vinilos.R
import com.equipo2.vinilos.data.model.NewPrize
import com.equipo2.vinilos.data.model.Prize
import com.equipo2.vinilos.data.repository.DataError
import com.equipo2.vinilos.support.FakePrizeRepository
import com.equipo2.vinilos.support.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PrizeCreateViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val repository = FakePrizeRepository()
    private val viewModel = PrizeCreateViewModel(repository)

    private fun fillValidForm() = viewModel.update {
        copy(
            name = "  Premio Gardel ",
            organization = "CAPIF",
            description = "Reconoce a los mejores artistas de música en español.\n",
        )
    }

    // Validación (T2)

    @Test
    fun antesDeCrear_noHayErroresAunqueElFormularioEsteVacio() {
        viewModel.update { copy(name = "") }

        assertTrue(viewModel.errors.value.isEmpty())
    }

    @Test
    fun crearVacio_muestraLosTresErrores_yNoLlamaALaApi() {
        assertFalse(viewModel.save())

        assertEquals(PrizeField.entries, viewModel.errors.value.keys.toList())
        assertTrue(repository.created.isEmpty())
        assertFalse(viewModel.isSaving.value)
    }

    @Test
    fun crearConSoloEspacios_noLlamaALaApi() {
        viewModel.update { copy(name = "   ", organization = " ", description = "\t") }

        assertFalse(viewModel.save())

        assertTrue(repository.created.isEmpty())
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

    // Envío (T3)

    @Test
    fun crearValido_enviaElPremioSinEspaciosSobrantes_yQuedaGuardando() {
        fillValidForm()

        assertTrue(viewModel.save())

        assertTrue(viewModel.errors.value.isEmpty())
        assertTrue(viewModel.isSaving.value)
        assertEquals(
            listOf(NewPrize("Premio Gardel", "CAPIF", "Reconoce a los mejores artistas de música en español.")),
            repository.created,
        )
    }

    @Test
    fun mientrasGuarda_crearOtraVezNoVuelveAEnviar() {
        fillValidForm()
        viewModel.save()

        assertTrue(viewModel.save())

        assertEquals(1, repository.created.size)
    }

    @Test
    fun siLaApiCreaElPremio_avisaConElPremioYSuId() = runTest {
        fillValidForm()
        viewModel.save()

        repository.createResponse.complete(Result.success(Prize(205, "Premio Gardel", "CAPIF")))

        assertEquals(PrizeCreateEvent.Created(Prize(205, "Premio Gardel", "CAPIF")), viewModel.events.first())
        assertFalse(viewModel.isSaving.value)
    }

    @Test
    fun siLaApiRechazaLosDatos_avisaYConservaElFormulario() = runTest {
        fillValidForm()
        viewModel.save()

        repository.createResponse.complete(Result.failure(DataError.Request(400)))

        assertEquals(PrizeCreateEvent.Failed, viewModel.events.first())
        assertFalse(viewModel.isSaving.value)
        assertEquals("  Premio Gardel ", viewModel.form.value.name)
    }

    @Test
    fun sinConexion_avisa_yPermiteVolverAIntentar() = runTest {
        fillValidForm()
        viewModel.save()

        repository.createResponse.complete(Result.failure(DataError.Network()))
        assertEquals(PrizeCreateEvent.Failed, viewModel.events.first())

        repository.createResponse = CompletableDeferred()
        assertTrue(viewModel.save())
        assertEquals(2, repository.created.size)
    }
}
