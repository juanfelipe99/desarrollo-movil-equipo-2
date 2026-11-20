package com.equipo2.vinilos.ui.state

import androidx.core.view.isVisible
import com.equipo2.vinilos.R
import com.equipo2.vinilos.databinding.ViewStateMessageBinding

/** Estado de error: «No pudimos cargar {objeto}», qué hacer según el motivo y el botón «Reintentar». */
fun ViewStateMessageBinding.showError(objectName: CharSequence, reason: ErrorReason, onRetry: () -> Unit) {
    val context = root.context

    // El error ocupa la pantalla completa, aunque antes el mismo mensaje se haya mostrado compacto
    useSize(compact = false)
    stateIconCircle.isVisible = true
    stateIcon.setImageResource(if (reason == ErrorReason.CONNECTION) R.drawable.ic_cloud_off else R.drawable.ic_error)

    stateTitle.text = context.getString(R.string.error_load_title, objectName)
    stateBody.text = context.getString(if (reason == ErrorReason.CONNECTION) R.string.error_network_body else R.string.error_server_body)

    stateRetry.isVisible = true
    stateRetry.setOnClickListener { onRetry() }
}
