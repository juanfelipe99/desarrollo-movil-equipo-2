package com.equipo2.vinilos.ui.common

import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.equipo2.vinilos.R

/** Calificación máxima de un comentario. */
const val MAX_RATING = 5

/** Cinco estrellas, rellenas hasta la calificación. Reemplaza las que hubiera, así sirve en filas recicladas. */
fun LinearLayout.showStars(rating: Int) {
    removeAllViews()
    val size = resources.getDimensionPixelSize(R.dimen.rating_star)
    val gap = resources.getDimensionPixelSize(R.dimen.rating_star_gap)
    repeat(MAX_RATING) { index ->
        val star = ImageView(context)
        star.setImageResource(R.drawable.ic_star)
        val color = if (index < rating) R.color.md_tertiary else R.color.md_outline_variant
        star.setColorFilter(ContextCompat.getColor(context, color))
        addView(star, LinearLayout.LayoutParams(size, size).apply { if (index > 0) marginStart = gap })
    }
}
