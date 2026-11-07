package com.equipo2.vinilos.data.network.artist

import com.equipo2.vinilos.data.model.MusicianStatus
import com.equipo2.vinilos.data.repository.DataError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicianCandidatesTest {

    private val musicians = listOf(
        musician(9, "Freddie Mercury"),
        musician(11, "Gustavo Cerati"),
        musician(10, "Brian May"),
        musician(3, "Celia Cruz"),
    )

    private val bands = listOf(
        band(101, "Queen", members = listOf(9, 10)),
        band(18, "Soda Stereo", members = listOf(11)),
        band(16, "Pink Floyd"),
    )

    @Test
    fun cadaMusico_recibeSuEstadoFrenteALaBanda() {
        val statuses = musicianCandidates(101, musicians, bands).associate { it.musician.name to it.status }

        assertEquals(MusicianStatus.NoBand, statuses["Celia Cruz"])
        assertEquals(MusicianStatus.InOtherBand(18, "Soda Stereo"), statuses["Gustavo Cerati"])
        assertEquals(MusicianStatus.AlreadyMember, statuses["Freddie Mercury"])
        assertEquals(MusicianStatus.AlreadyMember, statuses["Brian May"])
    }

    @Test
    fun losIntegrantesActuales_vanAlFinalYNoSonSeleccionables() {
        val candidates = musicianCandidates(101, musicians, bands)

        // Disponibles primero y luego integrantes, cada grupo por nombre
        assertEquals(
            listOf("Celia Cruz", "Gustavo Cerati", "Brian May", "Freddie Mercury"),
            candidates.map { it.musician.name },
        )
        assertEquals(listOf(true, true, false, false), candidates.map { it.selectable })
    }

    @Test
    fun elOrdenPorNombre_ignoraTildesYMayusculas() {
        val candidates = musicianCandidates(
            16,
            listOf(musician(1, "Óscar D'León"), musician(2, "andrés Cepeda"), musician(3, "Nino Bravo")),
            bands,
        )

        assertEquals(listOf("andrés Cepeda", "Nino Bravo", "Óscar D'León"), candidates.map { it.musician.name })
    }

    @Test
    fun bandaSinIntegrantes_todosSonSeleccionables() {
        val candidates = musicianCandidates(16, musicians, bands)

        assertTrue(candidates.all { it.selectable })
        assertTrue(candidates.none { it.status == MusicianStatus.AlreadyMember })
    }

    @Test
    fun sinMusicos_devuelveUnaListaVacia() {
        assertEquals(emptyList<Any>(), musicianCandidates(101, emptyList(), bands))
    }

    @Test
    fun siLaBandaNoExiste_fallaConNoEncontrado() {
        val error = runCatching { musicianCandidates(999, musicians, bands) }.exceptionOrNull()

        assertTrue(error is DataError.NotFound)
    }

    @Test
    fun siUnMusicoApareceEnDosBandas_cuentaLaBandaActual() {
        val duplicated = bands + band(30, "Otra banda", members = listOf(9))

        val freddie = musicianCandidates(101, musicians, duplicated).single { it.musician.id == 9 }

        assertFalse(freddie.selectable)
    }

    private fun musician(id: Int, name: String) =
        MusicianDto(id = id, name = name, birthDate = "1950-01-01T00:00:00.000Z")

    private fun band(id: Int, name: String, members: List<Int> = emptyList()) =
        BandDto(id = id, name = name, creationDate = "1970-01-01T00:00:00.000Z", musicians = members.map(::BandMemberDto))
}
