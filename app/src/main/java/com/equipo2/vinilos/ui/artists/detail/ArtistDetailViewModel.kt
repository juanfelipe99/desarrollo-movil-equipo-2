package com.equipo2.vinilos.ui.artists.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.equipo2.vinilos.VinilosApplication
import com.equipo2.vinilos.data.model.Artist
import com.equipo2.vinilos.data.model.ArtistDetail
import com.equipo2.vinilos.data.model.ArtistPrize
import com.equipo2.vinilos.data.model.ArtistType
import com.equipo2.vinilos.data.repository.ArtistRepository
import com.equipo2.vinilos.ui.state.PullRefresh
import com.equipo2.vinilos.ui.state.UiState
import com.equipo2.vinilos.ui.state.toErrorReason
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Detalle de un músico o de una banda (HU04), con los integrantes (HU12) y los premios (HU14) agregados desde aquí. Se conserva al girar. */
class ArtistDetailViewModel(
    private val repository: ArtistRepository,
    private val artistId: Int,
    private val artistType: ArtistType,
    private val savedState: SavedStateHandle = SavedStateHandle(),
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<ArtistDetail>>(UiState.Loading)
    val state: StateFlow<UiState<ArtistDetail>> = _state.asStateFlow()

    /** Ids de los integrantes agregados desde esta pantalla, que se marcan como «Nuevo». */
    val newMemberIds: StateFlow<IntArray> = savedState.getStateFlow(NEW_MEMBER_IDS, IntArray(0))

    /** Ids de las entregas de premios agregadas desde esta pantalla, que se marcan como «Nuevo». */
    val newPrizeIds: StateFlow<IntArray> = savedState.getStateFlow(NEW_PRIZE_IDS, IntArray(0))

    private var loading: Job? = null

    val pullRefresh = PullRefresh(viewModelScope)

    init {
        load()
    }

    fun retry() = load()

    /** Agrega el músico al final de los integrantes sin volver a pedir la banda, y lo marca como «Nuevo». */
    fun onMemberAdded(musician: Artist) {
        if (musician.id !in newMemberIds.value) savedState[NEW_MEMBER_IDS] = newMemberIds.value + musician.id
        val detail = (_state.value as? UiState.Success)?.data ?: return
        if (detail.members.none { it.id == musician.id }) {
            _state.value = UiState.Success(detail.copy(members = detail.members + musician))
        }
    }

    /** Pone el premio primero sin volver a pedir el artista, y lo marca como «Nuevo». */
    fun onPrizeAdded(prize: ArtistPrize) {
        if (prize.id !in newPrizeIds.value) savedState[NEW_PRIZE_IDS] = newPrizeIds.value + prize.id
        val detail = (_state.value as? UiState.Success)?.data ?: return
        if (detail.prizes.none { it.id == prize.id }) {
            _state.value = UiState.Success(detail.copy(prizes = listOf(prize) + detail.prizes))
        }
    }

    /** Recarga al deslizar hacia abajo. El artista sigue a la vista mientras llegan sus datos nuevos. */
    fun refresh() {
        // Recargar siempre va a la API
        repository.clearCache()
        if (_state.value !is UiState.Success) return load()
        if (loading?.isActive == true) return
        loading = pullRefresh.launch({ repository.getArtistDetail(artistId, artistType) }) { detail ->
            _state.value = UiState.Success(detail)
        }
    }

    private fun load() {
        // Si ya hay una petición en curso no se lanza otra
        if (loading?.isActive == true) return

        _state.value = UiState.Loading
        loading = viewModelScope.launch {
            _state.value = repository.getArtistDetail(artistId, artistType).fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(it.toErrorReason()) },
            )
        }
    }

    companion object {
        private const val NEW_MEMBER_IDS = "newMemberIds"
        private const val NEW_PRIZE_IDS = "newPrizeIds"

        fun factory(artistId: Int, artistType: ArtistType) = viewModelFactory {
            initializer {
                ArtistDetailViewModel(VinilosApplication.container.artistRepository, artistId, artistType, createSavedStateHandle())
            }
        }
    }
}
