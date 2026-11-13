package com.equipo2.vinilos.data.network.artist

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PrizeCandidatesTest {

    private val prizes = listOf(
        prize(100, "Grammy Award", "National Academy of Recording Arts & Sciences", 100 to "1978-12-10", 19 to "2001-02-21", 40 to "1990-02-21"),
        prize(8, "Premio Gardel", "CAPIF", 31 to "2005-05-05"),
        prize(6, "Billboard Music Award", "Billboard"),
    )

    @Test
    fun losPremios_vanEnOrdenAlfabetico_conSuOrganizacion() {
        val candidates = prizeCandidates(prizes, received = emptyList())

        assertEquals(listOf("Billboard Music Award", "Grammy Award", "Premio Gardel"), candidates.map { it.prize.name })
        assertEquals("CAPIF", candidates.last().prize.organization)
        assertEquals(8, candidates.last().prize.id)
    }

    @Test
    fun elOrden_noDistingueTildesNiMayusculas() {
        val candidates = prizeCandidates(
            listOf(prize(1, "premio Zeta", "A"), prize(2, "Ópera Prima", "B"), prize(3, "Oscar", "C")),
            received = emptyList(),
        )

        assertEquals(listOf("Ópera Prima", "Oscar", "premio Zeta"), candidates.map { it.prize.name })
    }

    @Test
    fun unPremioQueElArtistaYaRecibio_traeSusFechas_deLaMasAntiguaALaMasReciente() {
        // El artista recibió el Grammy dos veces, y la entrega 19 es de otro artista
        val received = listOf(PerformerPrizeDto(40, "1990-02-21T00:00:00.000Z"), PerformerPrizeDto(100, "1978-12-10T00:00:00.000Z"))

        val grammy = prizeCandidates(prizes, received).first { it.prize.id == 100 }

        assertEquals(listOf(LocalDate.of(1978, 12, 10), LocalDate.of(1990, 2, 21)), grammy.receivedDates)
    }

    @Test
    fun losPremiosQueNoHaRecibido_noTraenFechas_aunqueOtrosArtistasLosTengan() {
        val received = listOf(PerformerPrizeDto(100, "1978-12-10T00:00:00.000Z"))

        val candidates = prizeCandidates(prizes, received)

        assertTrue(candidates.first { it.prize.id == 8 }.receivedDates.isEmpty())
        assertTrue(candidates.first { it.prize.id == 6 }.receivedDates.isEmpty())
    }

    @Test
    fun sinPremiosEnElCatalogo_noHayCandidatos() {
        assertTrue(prizeCandidates(emptyList(), received = emptyList()).isEmpty())
    }

    private fun prize(id: Int, name: String, organization: String, vararg deliveries: Pair<Int, String>) =
        PrizeDto(id, name, organization, deliveries.map { (deliveryId, day) -> PerformerPrizeDto(deliveryId, "${day}T00:00:00.000Z") })
}
