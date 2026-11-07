package com.equipo2.vinilos.ui.common

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.updateLayoutParams
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.Fragment
import com.equipo2.vinilos.MainActivity
import com.equipo2.vinilos.R
import com.google.android.material.snackbar.Snackbar

/** Aviso de la app: ícono de éxito o de error, el mensaje y una X para cerrarlo. */
object AppSnackbar {

    private const val DURATION_MS = 4000

    /** Muestra el aviso, y con anchor aparece encima de esa vista. */
    fun show(root: View, message: CharSequence, isError: Boolean = false, anchor: View? = null): Snackbar {
        val snackbar = Snackbar.make(root, message, DURATION_MS)
        snackbar.anchorView = anchor
        // Material mide el anchor sin descontar el padding de root, que ya incluye las barras
        if (anchor != null) snackbar.view.updateLayoutParams<ViewGroup.MarginLayoutParams> { bottomMargin -= root.paddingBottom }
        snackbar.customize(isError)
        snackbar.show()
        return snackbar
    }

    private fun Snackbar.customize(isError: Boolean) {
        // El texto del aviso es una vista interna de Material, por eso se busca por su id
        val text = view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
        text.maxLines = Int.MAX_VALUE
        text.addStatusIcon(isError)
        text.addCloseButton { dismiss() }
    }

    private fun TextView.addStatusIcon(isError: Boolean) {
        val iconRes = if (isError) R.drawable.ic_error else R.drawable.ic_check
        val icon = AppCompatResources.getDrawable(context, iconRes)!!.mutate()
        icon.setTint(currentTextColor)

        val size = resources.getDimensionPixelSize(R.dimen.snackbar_icon)
        icon.setBounds(0, 0, size, size)
        setCompoundDrawablesRelative(icon, null, null, null)
        compoundDrawablePadding = resources.getDimensionPixelSize(R.dimen.snackbar_icon_padding)
    }

    private fun TextView.addCloseButton(onClick: () -> Unit) {
        val content = parent as ViewGroup
        val close = LayoutInflater.from(context).inflate(R.layout.view_snackbar_close, content, false) as ImageView
        ImageViewCompat.setImageTintList(close, ColorStateList.valueOf(currentTextColor))
        close.setOnClickListener { onClick() }
        content.addView(close)
    }
}

/** Muestra un aviso desde un fragment, encima del botón registrado o de la barra inferior. */
fun Fragment.showSnackbar(message: CharSequence, isError: Boolean = false) {
    (requireActivity() as MainActivity).showSnackbar(message, isError)
}

/** Los avisos aparecen encima de esta vista hasta que se destruya la vista del fragment. */
fun Fragment.keepSnackbarsAbove(view: View) {
    val activity = requireActivity() as MainActivity
    activity.snackbarAnchor = view

    viewLifecycleOwner.lifecycle.doOnDestroy {
        if (activity.snackbarAnchor === view) activity.snackbarAnchor = null
    }
}
