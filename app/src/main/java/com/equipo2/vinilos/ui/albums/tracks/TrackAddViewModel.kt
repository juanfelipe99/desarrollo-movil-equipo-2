package com.equipo2.vinilos.ui.albums.tracks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.equipo2.vinilos.VinilosApplication
import com.equipo2.vinilos.data.model.Track
import com.equipo2.vinilos.data.repository.AlbumRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Resultado de enviar el track a la API. */
sealed interface TrackAddEvent {
    data class Added(val track: Track) : TrackAddEvent

    /** No se pudo agregar, y la hoja conserva lo escrito. */
    data object Failed : TrackAddEvent
}

/** Hoja de Agregar track: lo escrito, sus errores y el envío, que se conservan al girar. */
class TrackAddViewModel(
    private val repository: AlbumRepository,
    private val albumId: Int,
) : ViewModel() {

    private val currentForm = MutableStateFlow(TrackForm())
    private val currentErrors = MutableStateFlow<Map<TrackField, Int>>(emptyMap())
    private val currentSaving = MutableStateFlow(false)
    private val pendingEvents = Channel<TrackAddEvent>(Channel.BUFFERED)

    val form: StateFlow<TrackForm> = currentForm
    val errors: StateFlow<Map<TrackField, Int>> = currentErrors
    val isSaving: StateFlow<Boolean> = currentSaving

    /** Resultado de cada envío, que la hoja recibe una sola vez. */
    val events: Flow<TrackAddEvent> = pendingEvents.receiveAsFlow()

    // Los errores aparecen al tocar Agregar y desde ahí se actualizan con cada cambio
    private var validateOnChange = false

    fun update(change: TrackForm.() -> TrackForm) {
        currentForm.update(change)
        if (validateOnChange) currentErrors.value = currentForm.value.errors()
    }

    /** Envía el track si es válido, y si no muestra los errores y devuelve false. */
    fun add(): Boolean {
        validateOnChange = true
        currentErrors.value = currentForm.value.errors()
        if (currentErrors.value.isNotEmpty()) return false

        currentSaving.value = true
        viewModelScope.launch {
            val result = repository.addTrack(albumId, currentForm.value.toNewTrack())
            currentSaving.value = false
            pendingEvents.send(result.fold({ TrackAddEvent.Added(it) }, { TrackAddEvent.Failed }))
        }
        return true
    }

    companion object {
        fun factory(albumId: Int) = viewModelFactory {
            initializer { TrackAddViewModel(VinilosApplication.container.albumRepository, albumId) }
        }
    }
}
