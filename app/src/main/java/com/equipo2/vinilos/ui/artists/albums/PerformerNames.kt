package com.equipo2.vinilos.ui.artists.albums

import android.content.Context
import com.equipo2.vinilos.R
import com.equipo2.vinilos.data.model.Performer

/** Nombres de los intérpretes como se dicen en una frase: «Rubén Blades», «Willie Colón y Rubén Blades» o «A, B y C». */
fun Context.performerNames(performers: List<Performer>): String {
    val names = performers.map { it.name }
    if (names.size <= 1) return names.joinToString()
    val first = names.dropLast(1).joinToString(getString(R.string.performer_names_separator))
    return getString(R.string.performer_names_last, first, names.last())
}
