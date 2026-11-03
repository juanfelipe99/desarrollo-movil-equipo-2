package com.equipo2.vinilos.ui.common

import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.equipo2.vinilos.R
import com.equipo2.vinilos.ui.state.ErrorReason
import com.equipo2.vinilos.ui.state.PullRefresh

/**
 * Deslizar hacia abajo recarga la pantalla, con el círculo en los colores de la app.
 * Solo se activa si la vista desplazable que se ve está arriba del todo.
 */
fun Fragment.bindPullToRefresh(
    layout: SwipeRefreshLayout,
    refresh: PullRefresh,
    vararg scrollables: View,
    onRefresh: () -> Unit,
) {
    layout.setColorSchemeResources(R.color.md_primary)
    layout.setProgressBackgroundColorSchemeResource(R.color.md_surface_container_high)
    layout.setOnChildScrollUpCallback { _, _ -> scrollables.any { it.isVisible && it.canScrollVertically(-1) } }

    layout.setOnRefreshListener {
        onRefresh()
        // Si no hay nada que mostrar, la pantalla pasa a cargando y el círculo no hace falta
        layout.isRefreshing = refresh.running.value
    }

    viewLifecycleOwner.collectWhileVisible(refresh.running) { layout.isRefreshing = it }
    viewLifecycleOwner.collectWhileVisible(refresh.failures) { reason -> showSnackbar(getString(reason.refreshFailedRes), isError = true) }
}

private val ErrorReason.refreshFailedRes: Int
    get() = when (this) {
        ErrorReason.CONNECTION -> R.string.refresh_failed_network
        ErrorReason.SERVER -> R.string.refresh_failed_server
    }
