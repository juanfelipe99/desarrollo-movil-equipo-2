package com.equipo2.vinilos.ui.common

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/** Pantalla con una lista que vuelve al inicio al tocar otra vez su pestaña. */
interface ScrollsToTop {
    fun scrollToTop()
}

/** Sube al inicio con animación. Si está muy abajo, primero salta cerca del inicio para no tardar. */
fun RecyclerView.smoothScrollToTop() {
    val layoutManager = layoutManager as? LinearLayoutManager ?: return
    if (layoutManager.findFirstVisibleItemPosition() > JUMP_BEFORE_SCROLL) scrollToPosition(JUMP_BEFORE_SCROLL)
    smoothScrollToPosition(0)
}

private const val JUMP_BEFORE_SCROLL = 10
