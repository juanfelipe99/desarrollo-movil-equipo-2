package com.equipo2.vinilos.ui.artists.albums

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.equipo2.vinilos.VinilosApplication
import com.equipo2.vinilos.data.model.Album
import com.equipo2.vinilos.data.model.AlbumCandidate
import com.equipo2.vinilos.data.model.ArtistType
import com.equipo2.vinilos.data.model.Performer
import com.equipo2.vinilos.data.repository.ArtistAlbumRepository
import com.equipo2.vinilos.data.repository.DataError
import com.equipo2.vinilos.data.repository.toDataError
import com.equipo2.vinilos.ui.common.containsIgnoringAccents
import com.equipo2.vinilos.ui.state.ErrorReason
import com.equipo2.vinilos.ui.state.UiState
import com.equipo2.vinilos.ui.state.toErrorReason
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Por qué no se pudo agregar el álbum, que decide qué le ofrece la pantalla al usuario. */
enum class AlbumAddFailure {
    /** Sin conexión o sin respuesta a tiempo. Se conserva el álbum elegido para volver a tocar Agregar. */
    CONNECTION,

    /** El álbum o el artista ya no existen. Se avisa y se recarga el listado. */
    NOT_FOUND,

    /** El servidor respondió con un error. */
    SERVER,
}

/** Lo que la pantalla debe hacer una sola vez tras tocar Agregar. */
sealed interface AlbumAddEvent {

    /** El artista es una banda y el álbum tiene otros intérpretes: hay que confirmar que los reemplazará antes de enviarlo. */
    data class ConfirmReplace(val album: Album, val replaced: List<Performer>) : AlbumAddEvent

    /** El álbum quedó en el artista. El detalle lo usa para mostrarlo primero y marcado como Nuevo. */
    data class Added(val album: Album) : AlbumAddEvent

    /** No se pudo agregar, y la pantalla conserva el álbum elegido. */
    data class Failed(val failure: AlbumAddFailure) : AlbumAddEvent
}

/**
 * Agregar álbum a un artista (HU15): el listado de álbumes del catálogo, el álbum elegido y el envío.
 * La selección se conserva al girar y al reintentar la carga.
 */
class AlbumAddViewModel(
    private val repository: ArtistAlbumRepository,
    private val artistId: Int,
    private val artistType: ArtistType,
    private val savedState: SavedStateHandle = SavedStateHandle(),
) : ViewModel() {

    private val candidates = MutableStateFlow<UiState<List<AlbumCandidate>>>(UiState.Loading)
    private val currentSubmitting = MutableStateFlow(false)
    private val currentMissingSelection = MutableStateFlow(false)
    private val currentArtistMissing = MutableStateFlow(false)
    private val currentAllAlreadyAdded = MutableStateFlow(false)
    private val pendingEvents = Channel<AlbumAddEvent>(Channel.BUFFERED)

    private val selectedId: StateFlow<Int?> = savedState.getStateFlow(SELECTED, null)

    val query: StateFlow<String> = savedState.getStateFlow(QUERY, "")

    /**
     * Álbumes del catálogo que coinciden con la búsqueda, con los que el artista ya tiene al final.
     * Es [UiState.Empty] si no queda ningún álbum que se le pueda agregar. Si solo la búsqueda
     * no encuentra ninguno, es un [UiState.Success] con la lista vacía.
     */
    val state: StateFlow<UiState<List<AlbumCandidate>>> = combine(candidates, query) { candidates, query ->
        if (candidates !is UiState.Success) return@combine candidates
        UiState.Success(candidates.data.filter { it.matches(query) })
    }.stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Loading)

    /** Álbum elegido, que sigue elegido aunque la búsqueda lo oculte. */
    val selected: StateFlow<AlbumCandidate?> = combine(candidates, selectedId) { _, _ -> currentSelection() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Mientras se envía, la pantalla muestra la carga y no deja elegir ni volver a tocar Agregar. */
    val isSubmitting: StateFlow<Boolean> = currentSubmitting.asStateFlow()

    /** Se tocó Agregar sin elegir un álbum, y la pantalla muestra «Elige un álbum». Se quita al elegir uno. */
    val missingSelection: StateFlow<Boolean> = currentMissingSelection.asStateFlow()

    /**
     * El artista ya no existe (404 al pedir el listado). No hay a quién agregarle álbumes ni sirve reintentar,
     * así que la pantalla lo dice en lugar del error de carga.
     */
    val artistMissing: StateFlow<Boolean> = currentArtistMissing.asStateFlow()

    /**
     * Con [state] en [UiState.Empty], dice por qué: el catálogo tiene álbumes pero el artista ya los tiene todos.
     * Si es false, es que el catálogo no tiene ninguno.
     */
    val allAlreadyAdded: StateFlow<Boolean> = currentAllAlreadyAdded.asStateFlow()

    val events: Flow<AlbumAddEvent> = pendingEvents.receiveAsFlow()

    private var loading: Job? = null

    init {
        load()
    }

    fun retry() = load()

    /** Filtra por el nombre del álbum o de cualquiera de sus intérpretes, sin importar tildes ni mayúsculas. */
    fun search(query: String) {
        savedState[QUERY] = query
    }

    /** Elige el álbum, salvo que el artista ya lo tenga o haya un envío en curso. */
    fun select(candidate: AlbumCandidate) {
        if (currentSubmitting.value || !candidate.selectable) return
        savedState[SELECTED] = candidate.album.id
        currentMissingSelection.value = false
    }

    /**
     * Agrega el álbum elegido, o pide confirmar el reemplazo si el artista es una banda y el álbum tiene otros intérpretes.
     * Si no hay ninguno elegido, pide elegirlo sin llamar a la API.
     */
    fun add() {
        if (currentSubmitting.value) return
        val candidate = currentSelection()
        if (candidate == null) {
            currentMissingSelection.value = true
            return
        }
        val replaced = candidate.performersReplacedBy(artistId, artistType)
        if (replaced.isEmpty()) {
            submit(candidate)
        } else {
            pendingEvents.trySend(AlbumAddEvent.ConfirmReplace(candidate.album, replaced))
        }
    }

    /** Agrega el álbum elegido sin volver a preguntar, después de confirmar el reemplazo de intérpretes. */
    fun confirmAdd() {
        if (currentSubmitting.value) return
        currentSelection()?.let(::submit)
    }

    private fun submit(candidate: AlbumCandidate) {
        currentSubmitting.value = true
        viewModelScope.launch {
            val result = repository.addAlbum(artistId, artistType, candidate.album.id)
            currentSubmitting.value = false
            result.fold(
                onSuccess = { pendingEvents.send(AlbumAddEvent.Added(candidate.album)) },
                onFailure = { error ->
                    val failure = error.toAlbumAddFailure()
                    pendingEvents.send(AlbumAddEvent.Failed(failure))
                    // Algo cambió en el servidor, así que se vuelve a pedir el listado
                    if (failure == AlbumAddFailure.NOT_FOUND) load()
                },
            )
        }
    }

    private fun load() {
        // Si ya hay una petición en curso no se lanza otra
        if (loading?.isActive == true) return

        candidates.value = UiState.Loading
        currentArtistMissing.value = false
        loading = viewModelScope.launch {
            repository.getAlbumCandidates(artistId, artistType).fold(
                onSuccess = ::show,
                onFailure = { error ->
                    // La selección se queda guardada, así reintentar la recupera
                    currentArtistMissing.value = error.toDataError() is DataError.NotFound
                    candidates.value = UiState.Error(error.toErrorReason())
                },
            )
        }
    }

    private fun show(list: List<AlbumCandidate>) {
        // Si el elegido ya no está o ahora el artista ya lo tiene, se suelta la selección
        if (list.none { it.selectable && it.album.id == selectedId.value }) savedState[SELECTED] = null
        currentAllAlreadyAdded.value = list.isNotEmpty() && list.none { it.selectable }
        candidates.value = if (list.none { it.selectable }) UiState.Empty else UiState.Success(list)
    }

    private fun currentSelection(): AlbumCandidate? {
        val id = selectedId.value ?: return null
        val list = (candidates.value as? UiState.Success)?.data ?: return null
        return list.firstOrNull { it.selectable && it.album.id == id }
    }

    companion object {
        private const val QUERY = "query"
        private const val SELECTED = "selectedAlbumId"

        fun factory(artistId: Int, artistType: ArtistType) = viewModelFactory {
            initializer {
                AlbumAddViewModel(VinilosApplication.container.artistAlbumRepository, artistId, artistType, createSavedStateHandle())
            }
        }
    }
}

private fun AlbumCandidate.matches(query: String): Boolean =
    album.name.containsIgnoringAccents(query) || album.performers.any { it.name.containsIgnoringAccents(query) }

private fun Throwable.toAlbumAddFailure(): AlbumAddFailure = when {
    toDataError() is DataError.NotFound -> AlbumAddFailure.NOT_FOUND
    toErrorReason() == ErrorReason.CONNECTION -> AlbumAddFailure.CONNECTION
    else -> AlbumAddFailure.SERVER
}
