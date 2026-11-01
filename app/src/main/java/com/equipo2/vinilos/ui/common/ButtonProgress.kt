package com.equipo2.vinilos.ui.common

import android.content.Context
import androidx.core.content.ContextCompat
import com.equipo2.vinilos.R
import com.google.android.material.progressindicator.CircularProgressIndicatorSpec
import com.google.android.material.progressindicator.IndeterminateDrawable

/** Círculo de progreso para el ícono de un botón mientras envía, como «Guardando…» o «Agregando…». */
fun Context.buttonProgress(): IndeterminateDrawable<CircularProgressIndicatorSpec> {
    val spec = CircularProgressIndicatorSpec(this, null)
    spec.indicatorSize = resources.getDimensionPixelSize(R.dimen.button_progress)
    spec.trackThickness = resources.getDimensionPixelSize(R.dimen.button_progress_thickness)
    spec.indicatorInset = 0
    spec.indicatorColors = intArrayOf(ContextCompat.getColor(this, R.color.md_on_surface_variant))
    return IndeterminateDrawable.createCircularDrawable(this, spec)
}
