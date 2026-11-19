package com.equipo2.vinilos.ui.artists.prizes.create

import com.equipo2.vinilos.R

/** Lo escrito en el formulario de Crear premio. */
data class PrizeForm(
    val name: String = "",
    val organization: String = "",
    val description: String = "",
)

/** Campos del formulario en el orden en que aparecen en pantalla. */
enum class PrizeField { NAME, ORGANIZATION, DESCRIPTION }

/** Mensaje de error de cada campo vacío o con solo espacios, en el orden de la pantalla. */
fun PrizeForm.errors(): Map<PrizeField, Int> = buildMap {
    if (name.isBlank()) put(PrizeField.NAME, R.string.error_prize_name_required)
    if (organization.isBlank()) put(PrizeField.ORGANIZATION, R.string.error_prize_organization_required)
    if (description.isBlank()) put(PrizeField.DESCRIPTION, R.string.error_prize_description_required)
}
