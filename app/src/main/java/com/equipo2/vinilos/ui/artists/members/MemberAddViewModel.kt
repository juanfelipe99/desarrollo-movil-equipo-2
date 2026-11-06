package com.equipo2.vinilos.ui.artists.members

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.equipo2.vinilos.VinilosApplication
import com.equipo2.vinilos.data.model.Artist
import com.equipo2.vinilos.data.model.MusicianCandidate
import com.equipo2.vinilos.data.model.MusicianStatus
import com.equipo2.vinilos.data.repository.BandRepository
import com.equipo2.vinilos.data.repository.DataError
import com.equipo2.vinilos.data.repository.toDataError
import com.equipo2.vinilos.ui.state.ErrorReason
import com.equipo2.vinilos.ui.state.UiState
import com.equipo2.vinilos.ui.state.toErrorReason
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.Normalizer

/** Por qué no se pudo agregar el integrante, que decide qué le ofrece la pantalla al usuario. */
enum class MemberAddFailure {
    /** Sin conexión o sin respuesta a tiempo. Se ofrece Reintentar y se conserva la selección. */
    CONNECTION,

    /** La banda o el músico ya no existen. Se avisa y se recarga el listado. */
    NOT_FOUND,

    /** El servidor respondió con un error. */
    SERVER,
}

/** Lo que la pantalla debe hacer una sola vez tras tocar Agregar o cargar el listado. */
sealed interface MemberAddEvent {

    /** El músico está en otra banda: hay que confirmar que la dejará antes de enviarlo. */
    data class ConfirmBandChange(val musician: Artist, val currentBandName: String) : MemberAddEvent

    data class Added(val musician: Artist) : MemberAddEvent

    /** No se pudo agregar, y la pantalla conserva el músico elegido. */
    data class Failed(val failure: MemberAddFailure) : MemberAddEvent

    /** La banda ya no existe, así que no hay a dónde agregar integrantes. */
    data object BandNotFound : MemberAddEvent
}

/**
 * Agregar integrante a una banda (HU12): el listado de músicos, la búsqueda, el músico elegido y el envío.
 * La búsqueda y la selección se conservan al girar.
 */
class MemberAddViewModel(
    private val repository: BandRepository,
    private val bandId: Int,
    private val savedState: SavedStateHandle = SavedStateHandle(),
) : ViewModel() {

    private val candidates = MutableStateFlow<UiState<List<MusicianCandidate>>>(UiState.Loading)
    private val currentSubmitting = MutableStateFlow(false)
    private val currentMissingSelection = MutableStateFlow(false)
    private val pendingEvents = Channel<MemberAddEvent>(Channel.BUFFERED)

    private val selectedId: StateFlow<Int?> = savedState.getStateFlow(SELECTED, null)

    val query: StateFlow<String> = savedState.getStateFlow(QUERY, "")

    /**
     * Músicos que coinciden con la búsqueda, con los integrantes actuales al final.
     * Es [UiState.Empty] si no hay ningún músico que se pueda agregar; si solo la búsqueda
     * no encuentra a nadie, es un [UiState.Success] con la lista vacía.
     */
    val state: StateFlow<UiState<List<MusicianCandidate>>> = combine(candidates, query) { candidates, query ->
        if (candidates !is UiState.Success) return@combine candidates
        UiState.Success(candidates.data.filter { it.musician.name.containsIgnoringAccents(query) })
    }.stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Loading)

    /** Músico elegido, que sigue elegido aunque la búsqueda lo oculte. */
    val selected: StateFlow<MusicianCandidate?> = combine(candidates, selectedId) { _, _ -> currentSelection() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Mientras se envía, la pantalla muestra la carga y no deja elegir ni volver a tocar Agregar. */
    val isSubmitting: StateFlow<Boolean> = currentSubmitting

    /** Se tocó Agregar sin elegir un músico. Se quita al elegir uno. */
    val missingSelection: StateFlow<Boolean> = currentMissingSelection

    val events: Flow<MemberAddEvent> = pendingEvents.receiveAsFlow()

    private var loading: Job? = null

    init {
        load()
    }

    fun retry() = load()

    fun search(query: String) {
        savedState[QUERY] = query
    }

    /** Elige el músico, salvo que ya sea integrante o haya un envío en curso. */
    fun select(candidate: MusicianCandidate) {
        if (currentSubmitting.value || !candidate.selectable) return
        savedState[SELECTED] = candidate.musician.id
        currentMissingSelection.value = false
    }

    /** Agrega el músico elegido, o pide confirmar el cambio si pertenece a otra banda. */
    fun add() {
        if (currentSubmitting.value) return
        val candidate = currentSelection()
        if (candidate == null) {
            currentMissingSelection.value = true
            return
        }
        when (val status = candidate.status) {
            is MusicianStatus.InOtherBand ->
                pendingEvents.trySend(MemberAddEvent.ConfirmBandChange(candidate.musician, status.bandName))
            else -> submit(candidate)
        }
    }

    /** Agrega el músico elegido sin volver a preguntar: tras confirmar el cambio de banda o al tocar Reintentar. */
    fun confirmAdd() {
        if (currentSubmitting.value) return
        currentSelection()?.let(::submit)
    }

    private fun submit(candidate: MusicianCandidate) {
        currentSubmitting.value = true
        viewModelScope.launch {
            val result = repository.addMusician(bandId, candidate.musician.id)
            currentSubmitting.value = false
            result.fold(
                onSuccess = { pendingEvents.send(MemberAddEvent.Added(candidate.musician)) },
                onFailure = { error ->
                    val failure = error.toMemberAddFailure()
                    pendingEvents.send(MemberAddEvent.Failed(failure))
                    // Algo cambió en el servidor, así que se vuelve a pedir el listado
                    if (failure == MemberAddFailure.NOT_FOUND) load()
                },
            )
        }
    }

    private fun load() {
        // Si ya hay una petición en curso no se lanza otra
        if (loading?.isActive == true) return

        candidates.value = UiState.Loading
        loading = viewModelScope.launch {
            repository.getMusicianCandidates(bandId).fold(
                onSuccess = ::show,
                onFailure = { error ->
                    candidates.value = UiState.Error(error.toErrorReason())
                    if (error is DataError.NotFound) pendingEvents.send(MemberAddEvent.BandNotFound)
                },
            )
        }
    }

    private fun show(list: List<MusicianCandidate>) {
        // Si el elegido ya no está o ahora es integrante, se suelta la selección
        if (list.none { it.selectable && it.musician.id == selectedId.value }) savedState[SELECTED] = null
        candidates.value = if (list.none { it.selectable }) UiState.Empty else UiState.Success(list)
    }

    private fun currentSelection(): MusicianCandidate? {
        val id = selectedId.value ?: return null
        val list = (candidates.value as? UiState.Success)?.data ?: return null
        return list.firstOrNull { it.selectable && it.musician.id == id }
    }

    companion object {
        private const val QUERY = "query"
        private const val SELECTED = "selectedMusicianId"

        fun factory(bandId: Int) = viewModelFactory {
            initializer {
                MemberAddViewModel(VinilosApplication.container.bandRepository, bandId, createSavedStateHandle())
            }
        }
    }
}

private fun Throwable.toMemberAddFailure(): MemberAddFailure = when {
    toDataError() is DataError.NotFound -> MemberAddFailure.NOT_FOUND
    toErrorReason() == ErrorReason.CONNECTION -> MemberAddFailure.CONNECTION
    else -> MemberAddFailure.SERVER
}

// Así «ruben» encuentra a «Rubén Blades»
private fun String.containsIgnoringAccents(query: String): Boolean =
    query.isBlank() || withoutAccents().contains(query.trim().withoutAccents(), ignoreCase = true)

private fun String.withoutAccents(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")
