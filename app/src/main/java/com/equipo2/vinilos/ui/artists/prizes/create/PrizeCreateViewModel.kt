package com.equipo2.vinilos.ui.artists.prizes.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.equipo2.vinilos.VinilosApplication
import com.equipo2.vinilos.data.model.Prize
import com.equipo2.vinilos.data.repository.PrizeRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Resultado de enviar el premio a la API. */
sealed interface PrizeCreateEvent {

    /** Trae el premio con su id, para elegirlo al volver a Agregar premio. */
    data class Created(val prize: Prize) : PrizeCreateEvent

    /** No se pudo crear, y el formulario conserva lo escrito. */
    data object Failed : PrizeCreateEvent
}

/** Formulario de Crear premio (HU13), sus errores y el envío, que se conservan al girar. */
class PrizeCreateViewModel(private val repository: PrizeRepository) : ViewModel() {

    private val currentForm = MutableStateFlow(PrizeForm())
    private val currentErrors = MutableStateFlow<Map<PrizeField, Int>>(emptyMap())
    private val currentSaving = MutableStateFlow(false)
    private val pendingEvents = Channel<PrizeCreateEvent>(Channel.BUFFERED)

    val form: StateFlow<PrizeForm> = currentForm.asStateFlow()
    val errors: StateFlow<Map<PrizeField, Int>> = currentErrors.asStateFlow()

    /** Mientras se envía no se vuelve a enviar aunque se toque Crear premio otra vez. */
    val isSaving: StateFlow<Boolean> = currentSaving.asStateFlow()

    /** Resultado de cada envío, que la pantalla recibe una sola vez. */
    val events: Flow<PrizeCreateEvent> = pendingEvents.receiveAsFlow()

    // Los errores aparecen al tocar Crear premio y desde ahí se actualizan con cada cambio
    private var validateOnChange = false

    fun update(change: PrizeForm.() -> PrizeForm) {
        currentForm.update(change)
        if (validateOnChange) currentErrors.value = currentForm.value.errors()
    }

    /** Envía el premio si es válido, y si no muestra los errores y devuelve false. */
    fun save(): Boolean {
        if (currentSaving.value) return true

        validateOnChange = true
        currentErrors.value = currentForm.value.errors()
        if (currentErrors.value.isNotEmpty()) return false

        currentSaving.value = true
        viewModelScope.launch {
            val result = repository.createPrize(currentForm.value.toNewPrize())
            currentSaving.value = false
            pendingEvents.send(result.fold({ PrizeCreateEvent.Created(it) }, { PrizeCreateEvent.Failed }))
        }
        return true
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { PrizeCreateViewModel(VinilosApplication.container.prizeRepository) }
        }
    }
}
