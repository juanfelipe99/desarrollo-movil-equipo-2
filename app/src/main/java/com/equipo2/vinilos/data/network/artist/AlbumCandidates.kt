package com.equipo2.vinilos.data.network.artist

import com.equipo2.vinilos.data.model.Album
import com.equipo2.vinilos.data.model.AlbumCandidate
import com.equipo2.vinilos.data.model.byName

/**
 * Álbumes del catálogo frente al artista, marcando los que ya están entre sus álbumes.
 *
 * [artistAlbumIds] son los álbumes que trae el detalle del artista. Los que se pueden agregar van
 * primero y los que ya tiene al final, cada grupo ordenado por nombre.
 */
internal fun albumCandidates(albums: List<Album>, artistAlbumIds: Set<Int>): List<AlbumCandidate> =
    albums
        .map { AlbumCandidate(it, alreadyAdded = it.id in artistAlbumIds) }
        .sortedWith(compareBy<AlbumCandidate> { !it.selectable }.then(byName { it.album.name }))
