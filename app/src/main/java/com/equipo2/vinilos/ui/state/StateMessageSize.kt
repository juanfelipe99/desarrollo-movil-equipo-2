package com.equipo2.vinilos.ui.state

import android.view.View
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.core.widget.TextViewCompat
import com.equipo2.vinilos.R
import com.equipo2.vinilos.databinding.ViewStateMessageBinding

/**
 * Tamaño del mensaje. A pantalla completa (los errores) va con el círculo grande y a 120 dp de arriba.
 * Dentro de una lista, debajo de su buscador o su conteo (vacío y sin resultados), el prototipo lo dibuja
 * más arriba, con el círculo y el título más chicos.
 */
internal fun ViewStateMessageBinding.useSize(compact: Boolean) {
    val resources = root.context.resources
    val circle = resources.getDimensionPixelSize(
        if (compact) R.dimen.state_compact_icon_circle_size else R.dimen.state_icon_circle_size,
    )
    val icon = resources.getDimensionPixelSize(if (compact) R.dimen.state_compact_icon_size else R.dimen.state_icon_size)
    stateIconCircle.updateLayoutParams {
        width = circle
        height = circle
    }
    stateIcon.updateLayoutParams {
        width = icon
        height = icon
    }
    TextViewCompat.setTextAppearance(
        stateTitle,
        if (compact) R.style.TextAppearance_Vinilos_StateTitleCompact else R.style.TextAppearance_Vinilos_HeadlineSmall,
    )
    (stateIconCircle.parent as View).updatePadding(
        top = resources.getDimensionPixelSize(if (compact) R.dimen.state_compact_padding_top else R.dimen.state_message_padding_top),
    )
}
