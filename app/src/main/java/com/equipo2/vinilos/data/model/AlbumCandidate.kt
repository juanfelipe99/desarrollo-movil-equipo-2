package com.equipo2.vinilos.data.model

/**
 * Álbum del catálogo que se ofrece para agregar a un artista (HU15), con sus intérpretes actuales.
 * Si el artista ya está entre sus intérpretes, se muestra pero no se puede volver a agregar.
 */
data class AlbumCandidate(
    val album: Album,
    val alreadyAdded: Boolean = false,
) {
    val selectable: Boolean
        get() = !alreadyAdded

    /**
     * Intérpretes que el álbum perdería al agregarlo a este artista. Al agregarlo a una banda, la banda
     * reemplaza a todos los que tenga; a un músico se le suma sin quitar a nadie, así que no hay ninguno.
     */
    fun performersReplacedBy(artistId: Int, type: ArtistType): List<Performer> = when (type) {
        ArtistType.MUSICIAN -> emptyList()
        ArtistType.BAND -> album.performers.filterNot { it.id == artistId && it.type == ArtistType.BAND }
    }
}
