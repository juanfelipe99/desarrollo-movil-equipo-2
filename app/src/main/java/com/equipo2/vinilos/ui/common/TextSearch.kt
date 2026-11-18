package com.equipo2.vinilos.ui.common

import java.text.Normalizer

/** Si el texto contiene la búsqueda sin importar tildes, mayúsculas ni espacios al borde. Así «ruben» encuentra a «Rubén Blades». */
fun String.containsIgnoringAccents(query: String): Boolean =
    query.isBlank() || withoutAccents().contains(query.trim().withoutAccents(), ignoreCase = true)

private val ACCENTS = Regex("\\p{Mn}+")

private fun String.withoutAccents(): String = Normalizer.normalize(this, Normalizer.Form.NFD).replace(ACCENTS, "")
