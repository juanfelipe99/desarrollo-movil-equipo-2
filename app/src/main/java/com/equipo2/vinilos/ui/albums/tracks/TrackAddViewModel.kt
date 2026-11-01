package com.equipo2.vinilos.ui.albums.tracks

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/** Hoja de Agregar track: lo escrito y sus errores, que se conservan al girar. */
class TrackAddViewModel : ViewModel() {

    private val currentForm = MutableStateFlow(TrackForm())
    private val currentErrors = MutableStateFlow<Map<TrackField, Int>>(emptyMap())

    val form: StateFlow<TrackForm> = currentForm
    val errors: StateFlow<Map<TrackField, Int>> = currentErrors

    // Los errores aparecen al tocar Agregar y desde ahí se actualizan con cada cambio
    private var validateOnChange = false

    fun update(change: TrackForm.() -> TrackForm) {
        currentForm.update(change)
        if (validateOnChange) currentErrors.value = currentForm.value.errors()
    }

    /** Muestra los errores y devuelve si el track es válido. */
    fun add(): Boolean {
        validateOnChange = true
        currentErrors.value = currentForm.value.errors()
        return currentErrors.value.isEmpty()
    }
}
