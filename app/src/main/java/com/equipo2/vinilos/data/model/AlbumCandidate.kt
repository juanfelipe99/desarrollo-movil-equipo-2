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
}
