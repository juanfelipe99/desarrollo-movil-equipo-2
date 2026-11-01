package com.equipo2.vinilos.ui.albums.tracks

import com.equipo2.vinilos.R
import com.equipo2.vinilos.data.model.NewTrack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackFormTest {

    private val valid = TrackForm(name = "Todos vuelven", duration = "4:56")

    @Test
    fun formularioVacio_tieneUnErrorPorCampo_enElOrdenDeLaPantalla() {
        val errors = TrackForm().errors()

        assertEquals(TrackField.entries, errors.keys.toList())
        assertEquals(R.string.error_track_name_required, errors[TrackField.NAME])
        assertEquals(R.string.error_track_duration_required, errors[TrackField.DURATION])
    }

    @Test
    fun formularioCompleto_noTieneErrores() {
        assertTrue(valid.errors().isEmpty())
    }

    @Test
    fun nombreYDuracionConSoloEspacios_cuentanComoVacios() {
        val errors = valid.copy(name = "   ", duration = " ").errors()

        assertEquals(R.string.error_track_name_required, errors[TrackField.NAME])
        assertEquals(R.string.error_track_duration_required, errors[TrackField.DURATION])
    }

    @Test
    fun duracionSinDosPuntos_muestraElErrorDeFormato() {
        val errors = valid.copy(duration = "456").errors()

        assertEquals(mapOf(TrackField.DURATION to R.string.error_track_duration_invalid), errors)
    }

    @Test
    fun duracionesValidas() {
        listOf("4:35", "0:59", "12:05", "10:00", "0:00", " 5:05 ").forEach { duration ->
            assertTrue(duration, isValidDuration(duration))
        }
    }

    @Test
    fun duracionesInvalidas() {
        listOf("", "456", "4:60", "4:5", "4:355", "123:00", ":35", "4:", "4.35", "a:bc").forEach { duration ->
            assertFalse(duration, isValidDuration(duration))
        }
    }

    @Test
    fun toNewTrack_quitaLosEspaciosDeLosTextos() {
        val track = TrackForm(name = "  Todos vuelven ", duration = " 4:56 ").toNewTrack()

        assertEquals(NewTrack(name = "Todos vuelven", duration = "4:56"), track)
    }
}
