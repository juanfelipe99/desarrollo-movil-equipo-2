package com.equipo2.vinilos.data.model

/** Si el coleccionista tiene el álbum disponible en su colección. */
enum class AlbumStatus { ACTIVE, INACTIVE }

/** Álbum de la colección de un coleccionista, con el precio y el estado que él le dio. */
data class CollectorAlbum(
    val albumId: Int,
    val name: String,
    val cover: String,
    val price: Int,
    val status: AlbumStatus,
)

/** Comentario de un coleccionista sobre un álbum, con su calificación de 1 a 5. */
data class CollectorComment(
    val id: Int,
    val albumId: Int,
    val albumName: String,
    val rating: Int,
    val description: String,
)

/** Detalle de un coleccionista (HU06): contacto, artistas favoritos, colección de álbumes y comentarios. */
data class CollectorDetail(
    val collector: Collector,
    val telephone: String,
    val favorites: List<Artist> = emptyList(),
    val albums: List<CollectorAlbum> = emptyList(),
    val comments: List<CollectorComment> = emptyList(),
)
