package com.equipo2.vinilos.ui.state

import androidx.annotation.DrawableRes
import androidx.core.view.isVisible
import com.equipo2.vinilos.databinding.ViewStateMessageBinding

/**
 * Estado vacío: título y texto, con el círculo del ícono solo si se pasa uno.
 * [compact] es para el mensaje dentro de una lista, debajo de su buscador o su conteo (ver [useSize]).
 */
fun ViewStateMessageBinding.showEmpty(
    title: CharSequence,
    body: CharSequence,
    @DrawableRes icon: Int? = null,
    compact: Boolean = false,
) {
    useSize(compact)
    stateIconCircle.isVisible = icon != null
    icon?.let(stateIcon::setImageResource)

    stateTitle.text = title
    stateBody.text = body

    stateRetry.isVisible = false
}
