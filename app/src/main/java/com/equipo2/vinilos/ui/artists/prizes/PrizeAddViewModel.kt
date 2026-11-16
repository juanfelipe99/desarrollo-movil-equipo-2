package com.equipo2.vinilos.ui.artists.prizes

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.equipo2.vinilos.R
import com.equipo2.vinilos.VinilosApplication
import com.equipo2.vinilos.data.model.ArtistPrize
import com.equipo2.vinilos.data.model.ArtistType
import com.equipo2.vinilos.data.model.Prize
import com.equipo2.vinilos.data.model.PrizeCandidate
import com.equipo2.vinilos.data.repository.DataError
import com.equipo2.vinilos.data.repository.PrizeRepository
import com.equipo2.vinilos.data.repository.toDataError
import com.equipo2.vinilos.ui.state.ErrorReason
import com.equipo2.vinilos.ui.state.UiState
import com.equipo2.vinilos.ui.state.toErrorReason
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Por qué no se pudo agregar el premio, que decide qué le muestra la pantalla al usuario. */
enum class PrizeAddFailure {
    /** Sin conexión o sin respuesta a tiempo. Se conservan el premio y la fecha para volver a tocar Guardar. */
    CONNECTION,

    /** El premio o el artista ya no existen. Se avisa y se recarga el listado. */
    NOT_FOUND,

    /** La API no aceptó la fecha. El error queda en el campo de fecha hasta que se elija otra. */
    INVALID_DATE,

    /** El servidor respondió con un error. */
    SERVER,
}

/** Lo que la pantalla debe hacer una sola vez tras tocar Guardar. */
sealed interface PrizeAddEvent {

    data class Added(val prize: ArtistPrize) : PrizeAddEvent

    /** No se pudo agregar, y la pantalla conserva el premio y la fecha elegidos. */
    data class Failed(val failure: PrizeAddFailure) : PrizeAddEvent
}

/**
 * Agregar premio a un artista (HU14): el listado de premios, el premio y la fecha elegidos, sus errores y el envío.
 * El premio y la fecha se conservan al girar.
 */
class PrizeAddViewModel(
    private val repository: PrizeRepository,
    private val artistId: Int,
    private val artistType: ArtistType,
    private val savedState: SavedStateHandle = SavedStateHandle(),
    private val today: () -> LocalDate = LocalDate::now,
) : ViewModel() {

    private val candidates = MutableStateFlow<UiState<List<PrizeCandidate>>>(UiState.Loading)
    private val currentDate = MutableStateFlow(savedState.get<String>(DATE)?.let(LocalDate::parse))
    private val currentPrizeMissing = MutableStateFlow(false)
    private val currentDateError = MutableStateFlow<Int?>(null)
    private val currentSaving = MutableStateFlow(false)
    private val currentArtistMissing = MutableStateFlow(false)
    private val pendingEvents = Channel<PrizeAddEvent>(Channel.BUFFERED)

    /** Premios del catálogo. Es [UiState.Empty] si todavía no se ha creado ninguno. */
    val state: StateFlow<UiState<List<PrizeCandidate>>> = candidates.asStateFlow()

    val selectedPrizeId: StateFlow<Int?> = savedState.getStateFlow(SELECTED, null)

    val date: StateFlow<LocalDate?> = currentDate.asStateFlow()

    /** Se tocó Guardar sin elegir un premio. Se quita al elegir uno. */
    val prizeMissing: StateFlow<Boolean> = currentPrizeMissing.asStateFlow()

    /** Mensaje de error de la fecha, o null si no tiene. */
    val dateError: StateFlow<Int?> = currentDateError.asStateFlow()

    /** Mientras se envía, la pantalla muestra la carga y no deja cambiar nada ni volver a tocar Guardar. */
    val isSaving: StateFlow<Boolean> = currentSaving.asStateFlow()

    /**
     * El artista ya no existe (404 al pedir el listado). No hay a quién agregarle premios ni sirve reintentar,
     * así que la pantalla lo dice en lugar del error de carga.
     */
    val artistMissing: StateFlow<Boolean> = currentArtistMissing.asStateFlow()

    val events: Flow<PrizeAddEvent> = pendingEvents.receiveAsFlow()

    private var loading: Job? = null

    // Los errores aparecen al tocar Guardar y desde ahí se actualizan con cada cambio
    private var validateOnChange = false

    // Fecha que la API rechazó, que sigue con error mientras no se cambie
    private var rejectedDate: LocalDate? = null

    init {
        load()
    }

    fun retry() = load()

    fun select(candidate: PrizeCandidate) {
        if (currentSaving.value) return
        savedState[SELECTED] = candidate.prize.id
        currentPrizeMissing.value = false
    }

    fun setDate(date: LocalDate) {
        if (currentSaving.value) return
        savedState[DATE] = date.toString()
        currentDate.value = date
        if (validateOnChange) currentDateError.value = dateError()
    }

    /** Envía el premio con su fecha si los dos son válidos, y si no muestra los errores y devuelve false. */
    fun save(): Boolean {
        if (currentSaving.value) return true

        validateOnChange = true
        val candidate = currentSelection()
        currentPrizeMissing.value = candidate == null
        currentDateError.value = dateError()

        val date = currentDate.value
        if (candidate == null || date == null || currentDateError.value != null) return false
        submit(candidate.prize, date)
        return true
    }

    private fun submit(prize: Prize, date: LocalDate) {
        currentSaving.value = true
        viewModelScope.launch {
            val result = repository.addPrize(artistId, artistType, prize, date)
            currentSaving.value = false
            result.fold(
                onSuccess = { pendingEvents.send(PrizeAddEvent.Added(it)) },
                onFailure = { error ->
                    val failure = error.toPrizeAddFailure()
                    if (failure == PrizeAddFailure.INVALID_DATE) {
                        rejectedDate = date
                        currentDateError.value = dateError()
                    }
                    pendingEvents.send(PrizeAddEvent.Failed(failure))
                    // Algo cambió en el servidor, así que se vuelve a pedir el listado
                    if (failure == PrizeAddFailure.NOT_FOUND) load()
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
            repository.getPrizeCandidates(artistId, artistType).fold(
                onSuccess = ::show,
                onFailure = { error ->
                    currentArtistMissing.value = error.toDataError() is DataError.NotFound
                    candidates.value = UiState.Error(error.toErrorReason())
                },
            )
        }
    }

    private fun show(list: List<PrizeCandidate>) {
        // Si el premio elegido ya no está, se suelta la selección
        if (list.none { it.prize.id == selectedPrizeId.value }) savedState[SELECTED] = null
        candidates.value = if (list.isEmpty()) UiState.Empty else UiState.Success(list)
    }

    private fun currentSelection(): PrizeCandidate? {
        val id = selectedPrizeId.value ?: return null
        val list = (candidates.value as? UiState.Success)?.data ?: return null
        return list.firstOrNull { it.prize.id == id }
    }

    @StringRes
    private fun dateError(): Int? {
        val date = currentDate.value
        return when {
            date == null -> R.string.error_premiation_date_required
            date.isAfter(today()) -> R.string.error_premiation_date_future
            date == rejectedDate -> R.string.error_premiation_date_invalid
            else -> null
        }
    }

    companion object {
        private const val SELECTED = "selectedPrizeId"
        private const val DATE = "premiationDate"

        fun factory(artistId: Int, artistType: ArtistType) = viewModelFactory {
            initializer {
                PrizeAddViewModel(VinilosApplication.container.prizeRepository, artistId, artistType, createSavedStateHandle())
            }
        }
    }
}

private fun Throwable.toPrizeAddFailure(): PrizeAddFailure {
    val error = toDataError()
    return when {
        error is DataError.NotFound -> PrizeAddFailure.NOT_FOUND
        // La API solo valida la fecha, así que un 400 es que no la aceptó
        error is DataError.Request && error.code == 400 -> PrizeAddFailure.INVALID_DATE
        toErrorReason() == ErrorReason.CONNECTION -> PrizeAddFailure.CONNECTION
        else -> PrizeAddFailure.SERVER
    }
}
