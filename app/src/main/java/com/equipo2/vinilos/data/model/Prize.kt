package com.equipo2.vinilos.data.model

import java.time.LocalDate

/** Premio del catálogo, que se puede entregar a cualquier artista. */
data class Prize(
    val id: Int,
    val name: String,
    val organization: String,
)

/** Premio por crear (HU13). La API exige los tres campos. */
data class NewPrize(
    val name: String,
    val organization: String,
    val description: String,
)

/**
 * Premio que se ofrece para agregar a un artista (HU14), con las fechas en que ya lo recibió.
 * Ninguno se descarta por haberlo recibido, porque el mismo premio se puede entregar otra vez con otra fecha.
 */
data class PrizeCandidate(
    val prize: Prize,
    val receivedDates: List<LocalDate> = emptyList(),
)
