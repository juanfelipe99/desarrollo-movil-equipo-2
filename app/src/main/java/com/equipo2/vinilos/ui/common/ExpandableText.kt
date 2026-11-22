package com.equipo2.vinilos.ui.common

import android.widget.TextView
import androidx.core.view.isVisible
import com.equipo2.vinilos.R

private const val PREVIEW_LENGTH = 170

/**
 * Muestra [content] en [text]. Los textos largos se cortan en la última palabra antes de 170 caracteres,
 * y [toggle] alterna entre «Ver más» y «Ver menos». Lo usan los detalles de álbum (HU02) y de artista (HU04).
 *
 * Empieza como diga [expanded] y avisa cada cambio con [onExpandedChange]. Quien lo guarde decide cuánto dura:
 * en el ViewModel sigue igual al redibujar la pantalla, al volver de otra y al girar.
 */
fun bindExpandableText(
    text: TextView,
    toggle: TextView,
    content: String,
    expanded: Boolean = false,
    onExpandedChange: (Boolean) -> Unit = {},
) {
    val isLong = content.length > PREVIEW_LENGTH
    val preview = content.take(PREVIEW_LENGTH).substringBeforeLast(' ').trimEnd() + "…"
    var isExpanded = expanded

    fun render() {
        text.text = if (isExpanded || !isLong) content else preview
        toggle.setText(if (isExpanded) R.string.description_collapse else R.string.description_expand)
    }

    toggle.isVisible = isLong
    toggle.setOnClickListener {
        isExpanded = !isExpanded
        onExpandedChange(isExpanded)
        render()
    }
    render()
}
