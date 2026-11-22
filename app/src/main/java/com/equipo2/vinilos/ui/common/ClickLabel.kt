package com.equipo2.vinilos.ui.common

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat

/**
 * Dice a TalkBack qué hace el toque, por ejemplo «toca dos veces para ver su detalle».
 * La acción va aquí y no en la descripción, porque TalkBack ya anuncia que se puede tocar y
 * una descripción con «Toca para…» lo repite (lo marca Accessibility Scanner).
 */
fun View.setClickLabel(label: CharSequence) {
    ViewCompat.replaceAccessibilityAction(this, AccessibilityActionCompat.ACTION_CLICK, label, null)
}
