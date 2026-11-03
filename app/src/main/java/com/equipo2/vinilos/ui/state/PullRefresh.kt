package com.equipo2.vinilos.ui.state

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/** Recarga con el gesto de deslizar hacia abajo: lo que ya se ve se queda y, si falla, solo se avisa. */
class PullRefresh(private val scope: CoroutineScope) {

    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running.asStateFlow()

    private val _failures = Channel<ErrorReason>(Channel.BUFFERED)
    val failures: Flow<ErrorReason> = _failures.receiveAsFlow()

    fun <T> launch(fetch: suspend () -> Result<T>, onSuccess: (T) -> Unit): Job = scope.launch {
        _running.value = true
        try {
            fetch().fold(onSuccess) { _failures.send(it.toErrorReason()) }
        } finally {
            // También si se cancela, por ejemplo porque otra carga la reemplazó
            _running.value = false
        }
    }
}
