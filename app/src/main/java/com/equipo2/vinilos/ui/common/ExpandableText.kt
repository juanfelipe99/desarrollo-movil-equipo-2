package com.equipo2.vinilos.ui.common

import android.widget.TextView
import androidx.core.view.isVisible
import com.equipo2.vinilos.R

private const val PREVIEW_LENGTH = 170

/**
 * Muestra [content] en [text]. Los textos largos se cortan en la última palabra antes de 170 caracteres,
 * y [toggle] alterna entre «Ver más» y «Ver menos». Lo usan los detalles de álbum (HU02) y de artista (HU04).
 */
fun bindExpandableText(text: TextView, toggle: TextView, content: String) {
    val isLong = content.length > PREVIEW_LENGTH
    val preview = content.take(PREVIEW_LENGTH).substringBeforeLast(' ').trimEnd() + "…"
    var expanded = false

    fun render() {
        text.text = if (expanded || !isLong) content else preview
        toggle.setText(if (expanded) R.string.description_collapse else R.string.description_expand)
    }

    toggle.isVisible = isLong
    toggle.setOnClickListener {
        expanded = !expanded
        render()
    }
    render()
}
