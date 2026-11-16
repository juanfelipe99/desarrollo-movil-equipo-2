package com.equipo2.vinilos.data.network.collector

import com.equipo2.vinilos.data.model.AlbumStatus
import com.equipo2.vinilos.data.model.Artist
import com.equipo2.vinilos.data.model.ArtistType
import com.equipo2.vinilos.data.model.Collector
import com.equipo2.vinilos.data.model.CollectorAlbum
import com.equipo2.vinilos.data.model.CollectorComment
import com.equipo2.vinilos.data.model.CollectorDetail
import com.equipo2.vinilos.data.model.byName
import com.equipo2.vinilos.data.network.fromApiDate
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable

/** Respuesta de collectors/{id}. Sus álbumes no traen el nombre y sus comentarios no dicen de qué álbum son. */
@Serializable
data class CollectorDetailDto(
    val id: Int,
    val name: String,
    val telephone: String = "",
    val email: String = "",
    val favoritePerformers: List<FavoritePerformerDto> = emptyList(),
    val comments: List<CollectorCommentDto> = emptyList(),
)

/** Artista favorito. Los músicos traen fecha de nacimiento y las bandas fecha de creación. */
@Serializable
data class FavoritePerformerDto(
    val id: Int,
    val name: String,
    val image: String = "",
    val birthDate: String? = null,
    val creationDate: String? = null,
)

@Serializable
data class CollectorCommentDto(
    val id: Int,
    val description: String = "",
    val rating: Int,
)

/** Respuesta de collectors/{id}/albums: cada álbum de la colección con su precio, su estado y el álbum completo. */
@Serializable
data class CollectorAlbumDetailDto(
    val id: Int,
    val price: Int,
    val status: String,
    // Si el álbum se borró, la colección puede seguir apuntando a él sin traerlo
    val album: CollectorAlbumInfoDto? = null,
)

/** Del álbum solo se necesita lo que se muestra en la fila. */
@Serializable
data class CollectorAlbumInfoDto(
    val id: Int,
    val name: String,
    val cover: String = "",
)

/** Álbum de albums visto solo por sus comentarios, para saber de qué álbum es cada comentario. */
@Serializable
data class CommentedAlbumDto(
    val id: Int,
    val name: String,
    val comments: List<CommentRefDto> = emptyList(),
)

@Serializable
data class CommentRefDto(val id: Int)

/** Álbum comentado, por el id del comentario. */
internal data class CommentedAlbum(val id: Int, val name: String)

internal fun List<CommentedAlbumDto>.byComment(): Map<Int, CommentedAlbum> =
    flatMap { album -> album.comments.map { it.id to CommentedAlbum(album.id, album.name) } }.toMap()

internal fun CollectorDetailDto.toCollectorDetail(
    albums: List<CollectorAlbumDetailDto>,
    albumByComment: Map<Int, CommentedAlbum>,
): CollectorDetail {
    val collection = albums.mapNotNull { it.toCollectorAlbum() }.sortedWith(byName { it.name })
    return CollectorDetail(
        collector = Collector(id = id, name = name, email = email, albumCount = collection.size),
        telephone = telephone,
        favorites = favoritePerformers.map { it.toArtist() }.sortedWith(byName { it.name }),
        albums = collection,
        comments = comments.toCollectorComments(albumByComment),
    )
}

internal fun FavoritePerformerDto.toArtist(): Artist {
    val isMusician = birthDate != null
    val date = birthDate ?: creationDate ?: throw SerializationException("El artista $id no trae fecha")
    return Artist(
        id = id,
        name = name,
        image = image,
        type = if (isMusician) ArtistType.MUSICIAN else ArtistType.BAND,
        date = fromApiDate(date),
    )
}

// La API solo usa «Active» e «Inactive». Cualquier otro valor no se puede presentar como disponible
internal fun CollectorAlbumDetailDto.toCollectorAlbum(): CollectorAlbum? {
    val album = album ?: return null
    return CollectorAlbum(
        albumId = album.id,
        name = album.name,
        cover = album.cover,
        price = price,
        status = if (status.equals(ACTIVE, ignoreCase = true)) AlbumStatus.ACTIVE else AlbumStatus.INACTIVE,
    )
}

// Un comentario cuyo álbum no aparece en albums no se puede enlazar ni nombrar, así que no se muestra
private fun List<CollectorCommentDto>.toCollectorComments(albumByComment: Map<Int, CommentedAlbum>): List<CollectorComment> =
    mapNotNull { comment ->
        val album = albumByComment[comment.id] ?: return@mapNotNull null
        CollectorComment(comment.id, album.id, album.name, comment.rating, comment.description)
    }.sortedWith(byName<CollectorComment> { it.albumName }.thenBy { it.id })

private const val ACTIVE = "Active"
