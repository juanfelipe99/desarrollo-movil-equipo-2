package com.equipo2.vinilos.data.model

import java.time.LocalDate

/** Premio que recibió un artista, con la fecha en que se lo dieron. */
data class ArtistPrize(
    val id: Int,
    val name: String,
    val organization: String,
    val date: LocalDate,
)

/**
 * Detalle de un músico o de una banda (HU04).
 *
 * Un músico puede tener [band] y una banda tiene [members]; lo del otro tipo queda vacío.
 */
data class ArtistDetail(
    val artist: Artist,
    val description: String,
    val band: Artist? = null,
    val members: List<Artist> = emptyList(),
    val albums: List<Album> = emptyList(),
    val prizes: List<ArtistPrize> = emptyList(),
)
