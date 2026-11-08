package com.equipo2.vinilos.ui.common

import android.text.Editable
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.view.children
import androidx.fragment.app.Fragment

/**
 * Al destruirse la vista del fragment, suelta el texto de cada campo de [root].
 *
 * Con un servicio de accesibilidad activo, Android se queda con el texto del último campo consultado aunque la pantalla
 * ya se haya cerrado. Ese texto apunta al campo por sus spans y filtros, el campo a sus padres y la vista raíz al
 * fragment, así que la pantalla entera seguía en memoria. Sin spans ni filtros el texto ya no lleva a ninguna parte.
 * El contenido escrito no cambia, y el fragment ya guardó el estado de sus vistas antes de destruirlas.
 */
fun Fragment.releaseTextFieldsOnDestroyView(root: View) {
    viewLifecycleOwner.lifecycle.doOnDestroy { root.forEachTextField { it.text?.release() } }
}

private fun View.forEachTextField(action: (EditText) -> Unit) {
    if (this is EditText) action(this)
    if (this is ViewGroup) children.forEach { it.forEachTextField(action) }
}

private fun Editable.release() {
    clearSpans()
    filters = emptyArray()
}
