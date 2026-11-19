package com.equipo2.vinilos.ui.artists.prizes.create

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Formulario de Crear premio (HU13) y sus errores, que se conservan al girar. */
class PrizeCreateViewModel : ViewModel() {

    private val currentForm = MutableStateFlow(PrizeForm())
    private val currentErrors = MutableStateFlow<Map<PrizeField, Int>>(emptyMap())

    val form: StateFlow<PrizeForm> = currentForm.asStateFlow()
    val errors: StateFlow<Map<PrizeField, Int>> = currentErrors.asStateFlow()

    // Los errores aparecen al tocar Crear premio y desde ahí se actualizan con cada cambio
    private var validateOnChange = false

    fun update(change: PrizeForm.() -> PrizeForm) {
        currentForm.update(change)
        if (validateOnChange) currentErrors.value = currentForm.value.errors()
    }

    /** Devuelve si el formulario es válido, y si no lo es muestra los errores. */
    fun save(): Boolean {
        validateOnChange = true
        currentErrors.value = currentForm.value.errors()
        return currentErrors.value.isEmpty()
    }
}
