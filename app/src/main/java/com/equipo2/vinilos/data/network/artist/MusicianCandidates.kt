package com.equipo2.vinilos.data.network.artist

import com.equipo2.vinilos.data.model.MusicianCandidate
import com.equipo2.vinilos.data.model.MusicianStatus
import com.equipo2.vinilos.data.model.byName
import com.equipo2.vinilos.data.repository.DataError

/**
 * Calcula el estado de cada músico frente a la banda [bandId].
 *
 * La API no dice en cada músico a qué banda pertenece. Eso solo aparece en la lista de integrantes
 * de cada banda, así que se cruzan las dos respuestas. Los que se pueden agregar van primero y los
 * que ya son integrantes al final, cada grupo ordenado por nombre.
 *
 * @throws DataError.NotFound si la banda ya no existe.
 */
internal fun musicianCandidates(
    bandId: Int,
    musicians: List<MusicianDto>,
    bands: List<BandDto>,
): List<MusicianCandidate> {
    if (bands.none { it.id == bandId }) throw DataError.NotFound()

    // Si un músico apareciera en esta banda y en otra, cuenta esta: va al final y toMap conserva el último
    val bandOfMusician = bands
        .sortedBy { it.id == bandId }
        .flatMap { band -> band.musicians.map { it.id to band } }
        .toMap()

    val candidates = musicians.map { musician ->
        val band = bandOfMusician[musician.id]
        val status = when {
            band == null -> MusicianStatus.NoBand
            band.id == bandId -> MusicianStatus.AlreadyMember
            else -> MusicianStatus.InOtherBand(band.id, band.name)
        }
        MusicianCandidate(musician.toArtist(), status)
    }

    return candidates.sortedWith(
        compareBy<MusicianCandidate> { !it.selectable }.then(byName { it.musician.name }),
    )
}
