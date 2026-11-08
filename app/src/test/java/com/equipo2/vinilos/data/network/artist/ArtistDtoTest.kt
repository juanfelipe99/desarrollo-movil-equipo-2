package com.equipo2.vinilos.data.network.artist

import com.equipo2.vinilos.data.model.Artist
import com.equipo2.vinilos.data.model.ArtistType
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ArtistDtoTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun musico_seConvierteConSuFechaDeNacimiento() {
        val artist = MusicianDto(
            id = 100,
            name = "Rubén Blades Bellido de Luna",
            image = "https://ejemplo.com/ruben.jpg",
            birthDate = "1948-07-16T00:00:00.000Z",
        ).toArtist()

        assertEquals(
            Artist(100, "Rubén Blades Bellido de Luna", "https://ejemplo.com/ruben.jpg", ArtistType.MUSICIAN, LocalDate.of(1948, 7, 16)),
            artist,
        )
    }

    @Test
    fun banda_seConvierteConSuFechaDeCreacion() {
        val artist = BandDto(
            id = 101,
            name = "Queen",
            image = "https://ejemplo.com/queen.jpg",
            creationDate = "1970-01-01T00:00:00.000Z",
        ).toArtist()

        assertEquals(ArtistType.BAND, artist.type)
        assertEquals(LocalDate.of(1970, 1, 1), artist.date)
    }

    @Test
    fun respuestaDeLaApi_ignoraLosCamposQueNoSeUsan_yLaImagenEsOpcional() {
        val musician = json.decodeFromString<MusicianDto>(
            """{"id":4,"name":"Freddie Mercury","birthDate":"1946-09-05T00:00:00.000Z","description":"…","albums":[],"performerPrizes":[]}""",
        )

        assertEquals("", musician.image)
        assertEquals(1946, musician.toArtist().date.year)
    }

    @Test
    fun bandaDelListado_leeLosIdsDeSusIntegrantes() {
        val band = json.decodeFromString<BandDto>(
            """{"id":101,"name":"Queen","creationDate":"1970-01-01T00:00:00.000Z","musicians":[{"id":9,"name":"Freddie Mercury"}]}""",
        )

        assertEquals(listOf(9), band.musicians.map { it.id })
    }

    @Test
    fun premios_seAgrupanPorCadaEntrega() {
        val grammy = PrizeDto(100, "Grammy Award", "NARAS", listOf(PerformerPrizeDto(1, "1978-12-10T00:00:00.000Z")))
        val loNuestro = PrizeDto(101, "Premios lo Nuestro", "Univisión", listOf(PerformerPrizeDto(2, "1980-12-10T00:00:00.000Z")))

        assertEquals(mapOf(1 to grammy, 2 to loNuestro), listOf(grammy, loNuestro).byPerformerPrize())
    }

    @Test
    fun detalleDeMusico_seConvierteConSuBanda() {
        val queen = BandDto(101, "Queen", creationDate = "1970-01-01T00:00:00.000Z").toArtist()
        val detail = MusicianDetailDto(
            id = 9,
            name = "Freddie Mercury",
            description = "Voz de Queen.",
            birthDate = "1946-09-05T00:00:00.000Z",
        ).toArtistDetail(queen, emptyMap())

        assertEquals(ArtistType.MUSICIAN, detail.artist.type)
        assertEquals(LocalDate.of(1946, 9, 5), detail.artist.date)
        assertEquals("Voz de Queen.", detail.description)
        assertEquals(queen, detail.band)
    }
}
