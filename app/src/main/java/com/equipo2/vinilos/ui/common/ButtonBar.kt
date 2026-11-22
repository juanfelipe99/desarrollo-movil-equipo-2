package com.equipo2.vinilos.ui.common

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import androidx.core.view.isVisible

/**
 * Barra de botones de los formularios, como Cancelar y Guardar. Los pone uno al lado del otro, del mismo ancho,
 * y los apila uno debajo del otro cuando el texto de alguno no cabe en una línea, por ejemplo con la letra
 * del sistema al 200 % en una pantalla angosta. Así el botón nunca parte una palabra («Cancel» y «ar»).
 *
 * Los hijos se declaran como en un LinearLayout horizontal con layout_width="0dp" y layout_weight="1", y el espacio
 * entre botones como marginEnd. Al apilarlos ese mismo espacio queda arriba de cada botón, menos del primero.
 */
class ButtonBar @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {

    private var stacked = false
    private var gap = -1

    init {
        orientation = HORIZONTAL
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (gap < 0) gap = buttons().dropLast(1).maxOfOrNull { (it.layoutParams as LayoutParams).marginEnd } ?: 0
        if (MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.UNSPECIFIED) {
            val available = MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight
            val stack = needsStacking(available)
            if (stack != stacked) arrange(stack)
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    private fun buttons(): List<View> = (0 until childCount).map(::getChildAt).filter { it.isVisible }

    /** Cada botón recibe la misma parte del ancho. Se apilan si alguno necesita más para su texto en una línea. */
    private fun needsStacking(available: Int): Boolean {
        val buttons = buttons()
        if (buttons.size < 2) return false
        val share = (available - gap * (buttons.size - 1)) / buttons.size
        return buttons.any { view ->
            val button = view as? Button ?: return@any false
            val text = button.paint.measureText(button.text.toString())
            text + button.totalPaddingLeft + button.totalPaddingRight > share
        }
    }

    private fun arrange(stack: Boolean) {
        stacked = stack
        orientation = if (stack) VERTICAL else HORIZONTAL
        val buttons = buttons()
        buttons.forEachIndexed { index, button ->
            val params = button.layoutParams as LayoutParams
            params.width = if (stack) LayoutParams.MATCH_PARENT else 0
            params.weight = if (stack) 0f else 1f
            params.topMargin = if (stack && index > 0) gap else 0
            params.marginEnd = if (!stack && index < buttons.lastIndex) gap else 0
        }
    }
}
