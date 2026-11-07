package com.equipo2.vinilos.ui.state

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.equipo2.vinilos.databinding.ViewEmptySectionBinding

/** Agrega la tarjeta punteada de sección vacía, por ejemplo cuando un álbum no tiene tracks. */
fun ViewGroup.addEmptySection(@DrawableRes icon: Int, title: CharSequence, body: CharSequence) {
    val binding = ViewEmptySectionBinding.inflate(LayoutInflater.from(context), this, true)
    binding.emptySectionIcon.setImageResource(icon)
    binding.emptySectionTitle.text = title
    binding.emptySectionBody.text = body
}

/** Agrega la tarjeta punteada con una sola línea de texto, como «Sin premios registrados.» en el detalle de artista. */
fun ViewGroup.addEmptyNote(text: CharSequence) {
    val binding = ViewEmptySectionBinding.inflate(LayoutInflater.from(context), this, true)
    binding.emptySectionIcon.isVisible = false
    binding.emptySectionTitle.isVisible = false
    binding.emptySectionBody.text = text
    binding.emptySectionBody.updateLayoutParams<ViewGroup.MarginLayoutParams> { topMargin = 0 }
    (binding.emptySectionBody.parent as ViewGroup).updateLayoutParams<ViewGroup.MarginLayoutParams> { marginStart = 0 }
}
