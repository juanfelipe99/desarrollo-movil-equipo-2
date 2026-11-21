package com.equipo2.vinilos.data.network.artist

import com.equipo2.vinilos.data.model.ArtistType
import com.equipo2.vinilos.data.network.album.AlbumDto
import com.equipo2.vinilos.data.network.album.PerformerDto
import com.equipo2.vinilos.data.network.album.toAlbum
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlbumCandidatesTest {

    private val albums = listOf(
        album(1, "Siembra", PerformerDto(1, "Willie Colón", "", "1950-04-28T00:00:00.000Z")),
        album(2, "Abbey Road", PerformerDto(15, "The Beatles", "")),
        album(3, "Buscando América"),
        album(4, "Canción animal"),
    )

    @Test
    fun sinAlbumesDelArtista_todosSePuedenAgregar_enOrdenAlfabetico() {
        val candidates = albumCandidates(albums, artistAlbumIds = emptySet())

        assertEquals(listOf("Abbey Road", "Buscando América", "Canción animal", "Siembra"), candidates.map { it.album.name })
        assertTrue(candidates.all { it.selectable })
    }

    @Test
    fun losQueElArtistaYaTiene_vanAlFinalDeshabilitados_tambienEnOrdenAlfabetico() {
        // Abbey Road y Buscando América irían primero por nombre, pero el artista ya los tiene
        val candidates = albumCandidates(albums, artistAlbumIds = setOf(3, 2))

        assertEquals(listOf("Canción animal", "Siembra", "Abbey Road", "Buscando América"), candidates.map { it.album.name })
        assertEquals(listOf(true, true, false, false), candidates.map { it.selectable })
    }

    @Test
    fun unAlbumDelArtista_quedaMarcadoComoYaAgregado() {
        val siembra = albumCandidates(albums, artistAlbumIds = setOf(1)).first { it.album.id == 1 }

        assertTrue(siembra.alreadyAdded)
        assertFalse(siembra.selectable)
    }

    @Test
    fun cadaAlbum_conservaSusIntérpretesConSuTipo_paraAvisarAQuienReemplazaUnaBanda() {
        val candidates = albumCandidates(albums, artistAlbumIds = emptySet()).associateBy { it.album.id }

        assertEquals(ArtistType.MUSICIAN, candidates.getValue(1).album.performers.single().type)
        assertEquals(ArtistType.BAND, candidates.getValue(2).album.performers.single().type)
        assertTrue(candidates.getValue(3).album.performers.isEmpty())
    }

    @Test
    fun elOrden_noDistingueTildesNiMayusculas() {
        val candidates = albumCandidates(
            listOf(album(1, "zeta"), album(2, "Ópera"), album(3, "Oasis")),
            artistAlbumIds = emptySet(),
        )

        assertEquals(listOf("Oasis", "Ópera", "zeta"), candidates.map { it.album.name })
    }

    @Test
    fun unCatalogoVacio_noTraeCandidatos() {
        assertTrue(albumCandidates(emptyList(), artistAlbumIds = setOf(1)).isEmpty())
    }

    // Los álbumes llegan de la API y se convierten como en la app, para revisar también el tipo de cada intérprete
    private fun album(id: Int, name: String, vararg performers: PerformerDto) = AlbumDto(
        id = id,
        name = name,
        cover = "https://ejemplo.com/$id.jpg",
        releaseDate = "1978-01-01T00:00:00.000Z",
        description = "Descripción",
        genre = "Salsa",
        recordLabel = "Fania Records",
        performers = performers.toList(),
    ).toAlbum()
}
