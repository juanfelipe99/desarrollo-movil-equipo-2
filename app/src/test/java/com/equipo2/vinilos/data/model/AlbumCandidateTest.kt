package com.equipo2.vinilos.data.model

import com.equipo2.vinilos.support.albumCandidate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlbumCandidateTest {

    private val ruben = Performer(100, "Rubén Blades", "", ArtistType.MUSICIAN)
    private val willie = Performer(1, "Willie Colón", "", ArtistType.MUSICIAN)
    private val queen = Performer(101, "Queen", "", ArtistType.BAND)

    @Test
    fun aUnaBanda_reemplazaATodosLosInterpretesDelAlbum() {
        val siembra = albumCandidate(178, "Siembra", false, willie, ruben)

        assertEquals(listOf(willie, ruben), siembra.performersReplacedBy(101, ArtistType.BAND))
    }

    @Test
    fun aUnaBanda_tambienReemplazaAOtraBanda() {
        val album = albumCandidate(1, "Compartido", false, queen)

        assertEquals(listOf(queen), album.performersReplacedBy(15, ArtistType.BAND))
    }

    @Test
    fun aUnaBanda_noSeReemplazaASiMisma() {
        val album = albumCandidate(176, "News of the World", false, queen)

        assertTrue(album.performersReplacedBy(101, ArtistType.BAND).isEmpty())
    }

    @Test
    fun unMusicoConElMismoIdQueLaBanda_siSeReemplaza() {
        // Hoy la API no repite ids entre músicos y bandas, pero se compara también el tipo por si lo hiciera
        val musicianWithSameId = Performer(101, "Otro", "", ArtistType.MUSICIAN)
        val album = albumCandidate(1, "Álbum", false, musicianWithSameId)

        assertEquals(listOf(musicianWithSameId), album.performersReplacedBy(101, ArtistType.BAND))
    }

    @Test
    fun unAlbumSinInterpretes_noReemplazaANadie() {
        assertTrue(albumCandidate(183, "Cantora 1").performersReplacedBy(101, ArtistType.BAND).isEmpty())
    }

    @Test
    fun aUnMusico_seLeSumaSinReemplazarANadie() {
        val siembra = albumCandidate(178, "Siembra", false, willie, ruben)

        assertTrue(siembra.performersReplacedBy(3, ArtistType.MUSICIAN).isEmpty())
    }
}
