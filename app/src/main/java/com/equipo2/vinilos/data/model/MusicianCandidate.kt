package com.equipo2.vinilos.data.model

/** Situación de un músico frente a la banda a la que se quiere agregar (HU12). */
sealed interface MusicianStatus {

    data object NoBand : MusicianStatus

    /** Ya pertenece a otra banda, que dejará si se agrega a esta. */
    data class InOtherBand(val bandId: Int, val bandName: String) : MusicianStatus

    /** Ya es integrante de esta banda, así que no se puede volver a agregar. */
    data object AlreadyMember : MusicianStatus
}

/** Músico que se ofrece para agregar a una banda, con su estado frente a ella. */
data class MusicianCandidate(
    val musician: Artist,
    val status: MusicianStatus,
) {
    val selectable: Boolean
        get() = status != MusicianStatus.AlreadyMember
}
