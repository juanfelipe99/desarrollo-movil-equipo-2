package com.equipo2.vinilos.ui.artists.prizes.create

import com.equipo2.vinilos.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PrizeFormTest {

    private val valid = PrizeForm(
        name = "Premio Gardel",
        organization = "CAPIF",
        description = "Reconoce a los mejores artistas de música en español.",
    )

    @Test
    fun formularioVacio_tieneUnErrorPorCampo_enElOrdenDeLaPantalla() {
        val errors = PrizeForm().errors()

        assertEquals(PrizeField.entries, errors.keys.toList())
        assertEquals(R.string.error_prize_name_required, errors[PrizeField.NAME])
        assertEquals(R.string.error_prize_organization_required, errors[PrizeField.ORGANIZATION])
        assertEquals(R.string.error_prize_description_required, errors[PrizeField.DESCRIPTION])
    }

    @Test
    fun formularioCompleto_noTieneErrores() {
        assertTrue(valid.errors().isEmpty())
    }

    @Test
    fun camposConSoloEspacios_cuentanComoVacios() {
        val errors = valid.copy(name = "   ", organization = "\t", description = " \n ").errors()

        assertEquals(PrizeField.entries, errors.keys.toList())
    }

    @Test
    fun soloElCampoVacioTieneError() {
        val errors = valid.copy(organization = "  ").errors()

        assertEquals(mapOf(PrizeField.ORGANIZATION to R.string.error_prize_organization_required), errors)
    }
}
