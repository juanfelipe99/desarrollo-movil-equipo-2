package com.equipo2.vinilos.ui.artists.list

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.equipo2.vinilos.VinilosApplication
import com.equipo2.vinilos.data.model.Artist
import com.equipo2.vinilos.data.model.ArtistType
import com.equipo2.vinilos.data.model.byName
import com.equipo2.vinilos.data.repository.ArtistRepository
import com.equipo2.vinilos.ui.state.PullRefresh
import com.equipo2.vinilos.ui.state.UiState
import com.equipo2.vinilos.ui.state.toErrorReason
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ArtistFilter(private val type: ArtistType?) {
    ALL(null),
    MUSICIANS(ArtistType.MUSICIAN),
    BANDS(ArtistType.BAND);

    fun matches(artist: Artist) = type == null || artist.type == type
}

/** Listado de artistas (HU03): carga, éxito, vacío o error, con el filtro elegido, y se conserva al girar. */
class ArtistListViewModel(
    private val repository: ArtistRepository,
    private val savedState: SavedStateHandle = SavedStateHandle(),
) : ViewModel() {

    private val artists = MutableStateFlow<UiState<List<Artist>>>(UiState.Loading)

    val filter: StateFlow<ArtistFilter> = savedState.getStateFlow(FILTER, ArtistFilter.ALL)

    /** Estado de la pantalla con solo los artistas del filtro, y vacío si el filtro no deja ninguno. */
    val state: StateFlow<UiState<List<Artist>>> = combine(artists, filter) { artists, filter ->
        if (artists !is UiState.Success) return@combine artists
        val filtered = artists.data.filter(filter::matches)
        if (filtered.isEmpty()) UiState.Empty else UiState.Success(filtered)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Loading)

    private var loading: Job? = null

    val pullRefresh = PullRefresh(viewModelScope)

    init {
        load()
    }

    fun retry() = load()

    /** Recarga al deslizar hacia abajo. Los artistas siguen a la vista mientras llegan los nuevos. */
    fun refresh() {
        if (artists.value !is UiState.Success) return load()
        if (loading?.isActive == true) return
        loading = pullRefresh.launch({ repository.getArtists() }) { artists -> this.artists.value = artistsOf(artists) }
    }

    fun selectFilter(filter: ArtistFilter) {
        savedState[FILTER] = filter
    }

    private fun load() {
        // Si ya hay una petición en curso no se lanza otra
        if (loading?.isActive == true) return

        artists.value = UiState.Loading
        loading = viewModelScope.launch {
            artists.value = repository.getArtists().fold(
                onSuccess = ::artistsOf,
                onFailure = { UiState.Error(it.toErrorReason()) },
            )
        }
    }

    private fun artistsOf(artists: List<Artist>): UiState<List<Artist>> =
        if (artists.isEmpty()) UiState.Empty else UiState.Success(artists.sortedWith(byName { it.name }))

    companion object {
        private const val FILTER = "filter"

        val Factory = viewModelFactory {
            initializer { ArtistListViewModel(VinilosApplication.container.artistRepository, createSavedStateHandle()) }
        }
    }
}
