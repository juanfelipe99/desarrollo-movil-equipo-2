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
    // Dentro de una lista ya se anuncia el conteo («0 resultados»), así que solo se anuncia el vacío de pantalla completa
    if (!compact) announceTitleWhenShown()
}

/**
 * El título es una región viva, pero Android solo anuncia los cambios de una vista visible, y las pantallas escriben
 * el mensaje antes de mostrarlo. Al borrar y escribir otra vez el título cuando ya se ve (el mismo texto no cuenta como cambio), TalkBack lo lee sin mover el
 * foco, por ejemplo «No pudimos cargar los álbumes» al abrir la app sin conexión.
 */
internal fun ViewStateMessageBinding.announceTitleWhenShown() {
    stateTitle.post {
        if (stateTitle.isShown) {
            val title = stateTitle.text
            stateTitle.text = null
            stateTitle.text = title
        }
    }
}
