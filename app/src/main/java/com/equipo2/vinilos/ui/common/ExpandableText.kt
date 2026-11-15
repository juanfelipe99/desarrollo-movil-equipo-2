package com.equipo2.vinilos.ui.common

import android.widget.TextView
import androidx.core.view.isVisible
import com.equipo2.vinilos.R

private const val PREVIEW_LENGTH = 170

/** Texto que se está mostrando y si está expandido. */
private data class ExpandedText(val content: String, val expanded: Boolean)

/**
 * Muestra [content] en [text]. Los textos largos se cortan en la última palabra antes de 170 caracteres,
 * y [toggle] alterna entre «Ver más» y «Ver menos». Lo usan los detalles de álbum (HU02) y de artista (HU04).
 *
 * Si la pantalla se vuelve a dibujar con el mismo texto, por ejemplo al agregar un track o un premio, sigue como estaba.
 */
fun bindExpandableText(text: TextView, toggle: TextView, content: String) {
    val isLong = content.length > PREVIEW_LENGTH
    val preview = content.take(PREVIEW_LENGTH).substringBeforeLast(' ').trimEnd() + "…"
    val shown = text.getTag(R.id.expandable_text_state) as? ExpandedText
    var expanded = shown?.content == content && shown.expanded

    fun render() {
        text.text = if (expanded || !isLong) content else preview
        toggle.setText(if (expanded) R.string.description_collapse else R.string.description_expand)
        text.setTag(R.id.expandable_text_state, ExpandedText(content, expanded))
    }

    toggle.isVisible = isLong
    toggle.setOnClickListener {
        expanded = !expanded
        render()
    }
    render()
}
