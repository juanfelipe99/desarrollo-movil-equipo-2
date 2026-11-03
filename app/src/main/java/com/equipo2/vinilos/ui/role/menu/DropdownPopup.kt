package com.equipo2.vinilos.ui.role.menu

import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.appcompat.content.res.AppCompatResources
import com.equipo2.vinilos.R
import com.equipo2.vinilos.ui.common.dimBehind

private const val DIM_AMOUNT = 0.32f

/** Muestra el contenido como menú desplegable debajo del anchor, alineado a su derecha. */
internal fun showDropdown(anchor: View, content: View): PopupWindow {
    val resources = anchor.resources
    // Con la letra grande el menú crece con ella, sin pasar del ancho de la pantalla
    val maxWidth = resources.displayMetrics.widthPixels - 2 * resources.getDimensionPixelSize(R.dimen.role_menu_screen_margin)
    val width = minOf(
        (resources.getDimensionPixelSize(R.dimen.role_menu_width) * maxOf(1f, resources.configuration.fontScale)).toInt(),
        maxWidth,
    )
    val offset = resources.getDimensionPixelOffset(R.dimen.role_menu_offset)

    // Se mide antes de mostrarlo para saber si cabe debajo del anchor
    content.measure(
        View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
    )

    return PopupWindow(content, width, ViewGroup.LayoutParams.WRAP_CONTENT, true).apply {
        setBackgroundDrawable(AppCompatResources.getDrawable(anchor.context, R.drawable.bg_role_menu))
        elevation = resources.getDimension(R.dimen.role_menu_elevation)
        // Si no cabe, toma el alto disponible y el contenido se desplaza
        height = minOf(content.measuredHeight, getMaxAvailableHeight(anchor, offset))
        showAsDropDown(anchor, 0, offset, Gravity.END)
        dimBehind(DIM_AMOUNT)
    }
}
