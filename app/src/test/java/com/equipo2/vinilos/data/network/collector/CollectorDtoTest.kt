package com.equipo2.vinilos.data.network.collector

import com.equipo2.vinilos.data.model.AlbumStatus
import com.equipo2.vinilos.data.model.Artist
import com.equipo2.vinilos.data.model.ArtistType
import com.equipo2.vinilos.data.model.Collector
import com.equipo2.vinilos.data.model.CollectorAlbum
import com.equipo2.vinilos.data.model.CollectorComment
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class CollectorDtoTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun collectorAlbum(id: Int, name: String, price: Int = 25, status: String = "Active") =
        CollectorAlbumDetailDto(id = id, price = price, status = status, album = CollectorAlbumInfoDto(id, name, "https://ejemplo.com/$id.jpg"))

    @Test
    fun favoritos_musicoConFechaDeNacimiento_yBandaConFechaDeCreacion() {
        val ruben = FavoritePerformerDto(100, "Rubén Blades", "https://ejemplo.com/r.jpg", birthDate = "1948-07-16T00:00:00.000Z")
        val queen = FavoritePerformerDto(101, "Queen", creationDate = "1970-01-01T00:00:00.000Z")

        assertEquals(
            Artist(100, "Rubén Blades", "https://ejemplo.com/r.jpg", ArtistType.MUSICIAN, LocalDate.of(1948, 7, 16)),
            ruben.toArtist(),
        )
        assertEquals(Artist(101, "Queen", "", ArtistType.BAND, LocalDate.of(1970, 1, 1)), queen.toArtist())
    }

    @Test(expected = SerializationException::class)
    fun favoritoSinFecha_noSePuedeLeer() {
        FavoritePerformerDto(1, "Sin fecha").toArtist()
    }

    @Test
    fun albumDeLaColeccion_activoOInactivo() {
        assertEquals(
            CollectorAlbum(100, "Buscando América", "https://ejemplo.com/100.jpg", 35, AlbumStatus.ACTIVE),
            collectorAlbum(100, "Buscando América", price = 35, status = "Active").toCollectorAlbum(),
        )
        assertEquals(AlbumStatus.INACTIVE, collectorAlbum(103, "A Day at the Races", status = "Inactive").toCollectorAlbum()?.status)
        // Un estado que la app no conoce no se presenta como disponible
        assertEquals(AlbumStatus.INACTIVE, collectorAlbum(104, "Otro", status = "Reserved").toCollectorAlbum()?.status)
    }

    @Test
    fun albumDeLaColeccionSinAlbum_seDescarta() {
        assertNull(CollectorAlbumDetailDto(id = 1, price = 10, status = "Active").toCollectorAlbum())
    }

    @Test
    fun detalle_juntaContactoFavoritosAlbumesYComentarios_ordenadosPorNombre() {
        val dto = CollectorDetailDto(
            id = 101,
            name = "Jaime Monsalve",
            telephone = "3012357936",
            email = "jmonsalve@rtvc.com.co",
            favoritePerformers = listOf(
                FavoritePerformerDto(101, "Queen", creationDate = "1970-01-01T00:00:00.000Z"),
                FavoritePerformerDto(100, "Bob Dylan", birthDate = "1941-05-24T00:00:00.000Z"),
            ),
            comments = listOf(
                CollectorCommentDto(101, "I love this album of Queen", 5),
                CollectorCommentDto(7, "Light My Fire en su versión larga es otra cosa.", 4),
                // Sin álbum en la lista de álbumes: no se puede enlazar
                CollectorCommentDto(999, "Huérfano", 3),
            ),
        )
        val albumByComment = listOf(
            CommentedAlbumDto(102, "The Doors", listOf(CommentRefDto(7))),
            CommentedAlbumDto(103, "A Night at the Opera", listOf(CommentRefDto(101))),
        ).byComment()

        val detail = dto.toCollectorDetail(listOf(collectorAlbum(2, "Poeta del pueblo"), collectorAlbum(3, "A Day at the Races", status = "Inactive")), albumByComment)

        assertEquals(Collector(101, "Jaime Monsalve", "jmonsalve@rtvc.com.co", albumCount = 2), detail.collector)
        assertEquals("3012357936", detail.telephone)
        assertEquals(listOf("Bob Dylan", "Queen"), detail.favorites.map { it.name })
        assertEquals(listOf("A Day at the Races", "Poeta del pueblo"), detail.albums.map { it.name })
        assertEquals(
            listOf(
                CollectorComment(101, 103, "A Night at the Opera", 5, "I love this album of Queen"),
                CollectorComment(7, 102, "The Doors", 4, "Light My Fire en su versión larga es otra cosa."),
            ),
            detail.comments,
        )
    }

    @Test
    fun respuestaDeLaApi_ignoraLosCamposQueNoSeUsan() {
        val dto = json.decodeFromString<CollectorDetailDto>(
            """{"id":100,"name":"Manolo Bellon","telephone":"3502457896","email":"manollo@caracol.com.co",
               "comments":[{"id":100,"description":"The most relevant album of Ruben Blades","rating":5}],
               "favoritePerformers":[{"id":100,"name":"Rubén Blades","image":"x","description":"…","birthDate":"1948-07-16T00:00:00.000Z"}],
               "collectorAlbums":[{"id":100,"price":35,"status":"Active"}]}""",
        )
        val albums = json.decodeFromString<List<CollectorAlbumDetailDto>>(
            """[{"id":100,"price":35,"status":"Active",
                 "album":{"id":100,"name":"Buscando América","cover":"c","releaseDate":"1984-08-01T00:00:00.000Z","genre":"Salsa"},
                 "collector":{"id":100,"name":"Manolo Bellon"}}]""",
        )

        assertEquals(1, dto.favoritePerformers.size)
        assertEquals("Buscando América", albums.single().album?.name)
    }
}
