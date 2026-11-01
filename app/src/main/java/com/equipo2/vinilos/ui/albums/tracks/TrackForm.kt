package com.equipo2.vinilos.ui.albums.tracks

import androidx.annotation.StringRes
import com.equipo2.vinilos.R

/** Lo escrito en la hoja de Agregar track. */
data class TrackForm(
    val name: String = "",
    val duration: String = "",
)

/** Campos de la hoja en el orden en que aparecen en pantalla. */
enum class TrackField { NAME, DURATION }

// Minutos de una o dos cifras y segundos de 00 a 59, como 4:35 o 12:05
private val DURATION = Regex("""^\d{1,2}:[0-5]\d$""")

fun isValidDuration(duration: String): Boolean = DURATION.matches(duration.trim())

/** Mensaje de error de cada campo que no cumple su regla, en el orden de la pantalla. */
fun TrackForm.errors(): Map<TrackField, Int> = buildMap {
    if (name.isBlank()) put(TrackField.NAME, R.string.error_track_name_required)
    durationError()?.let { put(TrackField.DURATION, it) }
}

@StringRes
private fun TrackForm.durationError(): Int? = when {
    duration.isBlank() -> R.string.error_track_duration_required
    !isValidDuration(duration) -> R.string.error_track_duration_invalid
    else -> null
}
